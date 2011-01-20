/*
 * JBoss, a division of Red Hat
 * Copyright 2011, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.management.portalobjects.common.exportimport;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.config.model.PortalConfig;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.binding.api.BindingProvider;
import org.gatein.management.portalobjects.api.exportimport.ImportContext;
import org.gatein.management.portalobjects.api.exportimport.ImportHandler;
import org.gatein.management.portalobjects.common.utils.PortalObjectsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PortalObjectsImportHandler implements ImportHandler
{
   private static final Logger log = LoggerFactory.getLogger(PortalObjectsImportHandler.class);

   private BindingProvider bindingProvider;
   private DataStorage dataStorage;

   public PortalObjectsImportHandler(BindingProvider bindingProvider, DataStorage dataStorage)
   {
      this.bindingProvider = bindingProvider;
      this.dataStorage = dataStorage;
   }

   @Override
   public ImportContext createContext(InputStream in) throws IOException
   {
      return ExportImportUtils.importFromZip(bindingProvider, in);
   }

   @Override
   public void importContext(ImportContext context) throws Exception
   {
      PortalObjectsContext rollbackContext = new PortalObjectsContext();
      PortalObjectsContext deleteRollbackContext = new PortalObjectsContext();
      try
      {
         importPortalConfig(context, dataStorage, rollbackContext, deleteRollbackContext);
         importPages(context, dataStorage, rollbackContext, deleteRollbackContext);
         importNavigation(context, dataStorage, rollbackContext, deleteRollbackContext);
      }
      catch (Exception e)
      {
         log.error("Exception during import, will attempt to rollback.", e);
         rollback(dataStorage, rollbackContext, deleteRollbackContext);
      }
   }

   private void importPortalConfig(ImportContext context, DataStorage dataStorage,
                                   PortalObjectsContext rollbackContext, PortalObjectsContext deleteRollbackContext)
      throws Exception
   {
      for (PortalConfig pc : context.getPortalConfigs())
      {
         PortalConfig existing = dataStorage.getPortalConfig(pc.getName());
         if (existing == null)
         {
            dataStorage.create(pc);
            deleteRollbackContext.addToContext(pc);
         }
         else
         {
            dataStorage.save(pc);
            rollbackContext.addToContext(existing);
         }
      }
   }

   private void importPages(ImportContext context, DataStorage dataStorage,
                            PortalObjectsContext rollbackContext, PortalObjectsContext deleteRollbackContext)
      throws Exception
   {
      for (List<Page> pages : context.getPages())
      {
         for (Page page : pages)
         {
            Page existing = dataStorage.getPage(page.getPageId());
            if (existing == null)
            {
               dataStorage.create(page);
               deleteRollbackContext.addToContext(page);
            }
            else
            {
               dataStorage.save(page);
               rollbackContext.addToContext(existing);
            }
         }
      }
   }

   private void importNavigation(ImportContext context, DataStorage dataStorage,
                                 PortalObjectsContext rollbackContext, PortalObjectsContext deleteRollbackContext)
      throws Exception
   {
      for (PageNavigation navigation : context.getNavigations())
      {
         PageNavigation existing = dataStorage.getPageNavigation(navigation.getOwnerType(), navigation.getOwnerId());
         if (existing == null)
         {
            dataStorage.create(navigation);
            deleteRollbackContext.addToContext(navigation);
         }
         else
         {
            dataStorage.save(navigation);
            rollbackContext.addToContext(existing);
         }
      }
      for (PageNode node : context.getNavigationNodes())
      {
         String[] ownerInfo = context.getOwnerInfoForNode(node);
         PageNavigation navigation = dataStorage.getPageNavigation(ownerInfo[0], ownerInfo[1]);
         // Only add the navigation snapshot before we started altering anything
         if (!containsNavigation(rollbackContext, navigation) && !containsNavigation(deleteRollbackContext, navigation))
         {
            rollbackContext.addToContext(navigation);
         }

         List<PageNode> siblings;
         String parentUri = PortalObjectsUtils.getParentUri(node.getUri());
         if (parentUri == null)
         {
            siblings = navigation.getNodes();
         }
         else
         {
            PageNode parent = PortalObjectsUtils.findNodeByUri(navigation.getNodes(), parentUri);
            siblings = parent.getNodes();
         }
         PageNode existing = PortalObjectsUtils.findNodeByUri(siblings, node.getUri());
         if (existing == null)
         {
            siblings.add(node);
         }
         else
         {
            int index = siblings.indexOf(existing);
            siblings.set(index, node);
         }
      }
   }

   private void rollback(DataStorage dataStorage, PortalObjectsContext rollbackContext, PortalObjectsContext deleteRollbackContext)
   {
      for (PortalConfig pc : rollbackContext.getPortalConfigs())
      {
         try
         {
            dataStorage.save(pc);
            log.info("Successfully rolled back (reverted) portal config " + pc.getName());
         }
         catch (Exception e)
         {
            log.error("Exception during rollback, trying to revert portal config " + pc.getName());
         }
      }
      for (PortalConfig pc : deleteRollbackContext.getPortalConfigs())
      {
         try
         {
            dataStorage.remove(pc);
            log.info("Successfully rolled back (deleted) portal config " + pc.getName());
         }
         catch (Exception e)
         {
            log.error("Exception during rollback, trying to delete portal config " + pc.getName());
         }
      }
      for (List<Page> pages : rollbackContext.getPages())
      {
         for (Page page : pages)
         {
            try
            {
               dataStorage.save(page);
               log.info("Successfully rolled back (reverted) page " + page.getPageId());
            }
            catch (Exception e)
            {
               log.error("Exception during rollback, trying to revert page " + page.getPageId());
            }
         }
      }
      for (List<Page> pages : deleteRollbackContext.getPages())
      {
         for (Page page : pages)
         {
            try
            {
               dataStorage.remove(page);
               log.info("Successfully rolled back (deleted) page " + page.getPageId());
            }
            catch (Exception e)
            {
               log.error("Exception during rollback, trying to delete page " + page.getPageId());
            }
         }
      }
      for (PageNavigation navigation : rollbackContext.getNavigations())
      {
         try
         {
            dataStorage.save(navigation);
            log.info("Successfully rolled back (reverted) navigation for ownerType " +
               navigation.getOwnerType() + " and ownerId " + navigation.getOwnerId());
         }
         catch (Exception e)
         {
            log.error("Exception during rollback, trying to revert navigation for ownerType " +
               navigation.getOwnerType() + " and ownerid " + navigation.getOwnerId());
         }
      }
      for (PageNavigation navigation : deleteRollbackContext.getNavigations())
      {
         try
         {
            dataStorage.remove(navigation);
            log.info("Successfully rolled back (deleted) navigation for ownerType " +
               navigation.getOwnerType() + " and ownerId " + navigation.getOwnerId());
         }
         catch (Exception e)
         {
            log.error("Exception during rollback, trying to delete navigation for ownerType " +
               navigation.getOwnerType() + " and ownerId " + navigation.getOwnerId());
         }
      }
   }

   private boolean containsNavigation(PortalObjectsContext context, PageNavigation navigation)
   {
      for (PageNavigation nv : context.getNavigations())
      {
         if (nv.getOwnerType().equals(navigation.getOwnerType()) && nv.getOwnerId().equals(navigation.getOwnerId()))
         {
            return true;
         }
      }

      return false;
   }
}