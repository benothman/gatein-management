/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.gatein.management.portalobjects.rest;

import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.NavigationData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.core.rest.ComponentRequestCallback;
import org.gatein.management.core.rest.ComponentRequestCallbackNoResult;
import org.gatein.management.portalobjects.common.utils.PortalObjectsUtils;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
//TODO: Add debugging
public class SiteResource extends BasePortalObjectsResource
{
   private static final Logger log = LoggerFactory.getLogger(SiteResource.class);

   @Override
   public Logger getLogger()
   {
      return log;
   }

   @GET
   @RolesAllowed("administrators")
   @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_XHTML_XML})
   public Response getPortalData(@Context UriInfo uriInfo, @QueryParam("ownerType") String type)
   {
      final String ownerType = checkOwnerType(type);

      if (log.isDebugEnabled())
      {
         log.debug("Retrieving all site layouts for ownerType " + ownerType);
      }

      return doRequest(uriInfo, new ComponentRequestCallback<ModelDataStorage, GenericEntity<List<PortalData>>>()
      {
         @Override
         public GenericEntity<List<PortalData>> inRequest(ModelDataStorage dataStorage) throws Exception
         {
            Query<PageData> pageQuery = new Query<PageData>(ownerType, null, PageData.class);
            Query<NavigationData> navigationQuery = new Query<NavigationData>(ownerType, null, NavigationData.class);

            List<PageData> pages = dataStorage.find(pageQuery).getAll();
            List<NavigationData> navigations = dataStorage.find(navigationQuery).getAll();
            Map<String,PortalData> portalDataMap = new HashMap<String,PortalData>();
            for (PageData page : pages)
            {
               String key = PortalObjectsUtils.createKey(page.getOwnerType(), page.getOwnerId());
               populatePortalDataMap(dataStorage, portalDataMap, key);
            }
            for (NavigationData nav : navigations)
            {
               String key = PortalObjectsUtils.createKey(nav.getOwnerType(), nav.getOwnerId());
               populatePortalDataMap(dataStorage, portalDataMap, key);
            }

            return new GenericEntity<List<PortalData>>(new ArrayList<PortalData>(portalDataMap.values())){};
         }
      });
   }

   @GET
   @Path("/{owner-id:.*}")
   @RolesAllowed("administrators")
   @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_XHTML_XML})
   public Response getPortalData(@Context UriInfo uriInfo,
                                 @QueryParam("ownerType") String type,
                                 @PathParam("owner-id") String id)
   {
      final String ownerType = checkOwnerType(type);
      final String ownerId = checkOwnerId(ownerType, id);

      if (log.isDebugEnabled())
      {
         log.debug(createMessage("Retrieving site layout", ownerType, ownerId));
      }

      return doRequest(uriInfo, new ComponentRequestCallback<ModelDataStorage, PortalData>()
      {
         @Override
         public PortalData inRequest(ModelDataStorage dataStorage) throws Exception
         {
            return dataStorage.getPortalConfig(new PortalKey(ownerType, ownerId));
         }
      });
   }

   @POST
   @Path("/{owner-id:.*}")
   @RolesAllowed("administrators")
   @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_XHTML_XML})
   public Response updatePortalData(@Context UriInfo uriInfo,
                                    @QueryParam("ownerType") String type,
                                    @PathParam("owner-id") String id,
                                    PortalData data)
   {
      final String ownerType = checkOwnerType(type);
      final String ownerId = checkOwnerId(ownerType, id);

      final PortalData portalData = PortalObjectsUtils.fixOwner(ownerType, ownerId, data);

      return doRequest(uriInfo, new ComponentRequestCallbackNoResult<ModelDataStorage>()
      {
         @Override
         public void inRequestNoResult(ModelDataStorage dataStorage) throws Exception
         {
            PortalData pd = dataStorage.getPortalConfig(new PortalKey(ownerType, ownerId));
            if (pd == null)
            {
               throw ownerException("Site does not exist", ownerType, ownerId, Response.Status.NOT_FOUND);
            }

            dataStorage.save(portalData);
         }
      });
   }

   private void populatePortalDataMap(ModelDataStorage dataStorage, Map<String, PortalData> portalDataMap, String key) throws Exception
   {
      if (!portalDataMap.containsKey(key))
      {
         PortalData data = dataStorage.getPortalConfig(PortalKey.create(key));
         if (data != null)
         {
            portalDataMap.put(key, data);
         }
      }
   }
}
