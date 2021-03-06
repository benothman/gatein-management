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

package org.gatein.management.portalobjects.binding.impl.page;

import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.gatein.management.portalobjects.binding.impl.AbstractMarshallerTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageDataMarshallerTest extends AbstractMarshallerTest
{
   @Test
   public void testHomePageUnMarshalling()
   {
      PageDataMarshaller marshaller = new PageDataMarshaller();
      PageData page = marshaller.unmarshal(getClass().getResourceAsStream("/home-page.xml"));
      assertNotNull(page);
      assertEquals("homepage", page.getName());
      assertEquals("Home Page", page.getTitle());
      assertEquals("Everyone", Utils.join(";", page.getAccessPermissions()));
      assertEquals("*:/platform/administrators", page.getEditPermission());
      assertNotNull(page.getChildren());
      assertEquals(1, page.getChildren().size());
      ComponentData component = page.getChildren().get(0);
      assertTrue(component instanceof ApplicationData);
      @SuppressWarnings("unchecked")
      ApplicationData<Portlet> application = (ApplicationData<Portlet>) component;
      assertTrue(application.getType() == ApplicationType.PORTLET);
      ApplicationState<Portlet> state = application.getState();
      assertNotNull(state);
      assertTrue(state instanceof TransientApplicationState);
      TransientApplicationState<Portlet> tas = (TransientApplicationState<Portlet>) state;
      assertEquals("web/HomePagePortlet", tas.getContentId());
      Portlet portlet = tas.getContentState();
      int count = 0;
      for (Preference pref : portlet)
      {
         count++;
      }
      assertEquals(1, count);
      Preference pref = portlet.getPreference("template");
      assertNotNull(pref);
      assertEquals("template", pref.getName());
      assertEquals("system:/templates/groovy/webui/component/UIHomePagePortlet.gtmpl", pref.getValue());
      assertFalse(pref.isReadOnly());

      assertNull(application.getTheme());
      assertEquals("Home Page portlet", application.getTitle());
      assertEquals("Everyone", Utils.join(";", application.getAccessPermissions()));
      assertFalse(application.isShowInfoBar());
      assertFalse(application.isShowApplicationState());
      assertFalse(application.isShowApplicationMode());
      assertNull(application.getDescription());
      assertNull(application.getIcon());
      assertNull(application.getWidth());
      assertNull(application.getHeight());
   }

   @Test
   public void testLoadedPageUnmarshalling()
   {
      PageDataMarshaller marshaller = new PageDataMarshaller();
      PageData page = marshaller.unmarshal(getClass().getResourceAsStream("/loaded-page.xml"));
      assertNotNull(page);

      // Verify page properties
      assertEquals("loaded-page", page.getName());
      assertEquals("Loaded Page", page.getTitle());
      assertEquals("manager:/platform/administrators;manager:/platform/users", Utils.join(";", page.getAccessPermissions()));
      assertEquals("*:/platform/administrators", page.getEditPermission());

      // Verify page children
      assertNotNull(page.getChildren());
      assertEquals(1, page.getChildren().size());
      ComponentData component = page.getChildren().get(0);
      assertTrue(component instanceof ContainerData);

      // Verify root container
      ContainerData rootContainer = (ContainerData) component;
      assertEquals(rootContainer.getTemplate(), "system:/groovy/portal/webui/container/UIContainer.gtmpl");
      assertEquals("Everyone", Utils.join(";", rootContainer.getAccessPermissions()));

      // Verify root container children
      List<ComponentData> rootChildren = rootContainer.getChildren();
      assertNotNull(rootChildren);
      assertEquals(3, rootChildren.size());

      // Verify container 1
      ComponentData c1 = rootChildren.get(0);
      assertNotNull(c1);
      assertTrue(c1 instanceof ContainerData);
      ContainerData container1 = (ContainerData) c1;
      assertEquals(container1.getTemplate(), "system:/groovy/portal/webui/container/UIContainer.gtmpl");
      assertEquals("*:/platform/users", Utils.join(";", container1.getAccessPermissions()));
      {
         // Verify homepage application
         assertNotNull(container1.getChildren());
         assertEquals(1, container1.getChildren().size());
         ComponentData homeComponent = container1.getChildren().get(0);
         assertTrue(homeComponent instanceof ApplicationData);
         @SuppressWarnings("unchecked")
         ApplicationData<Portlet> application = (ApplicationData<Portlet>) homeComponent;
         assertTrue(application.getType() == ApplicationType.PORTLET);
         ApplicationState<Portlet> state = application.getState();
         assertNotNull(state);
         assertTrue(state instanceof TransientApplicationState);
         TransientApplicationState<Portlet> tas = (TransientApplicationState<Portlet>) state;
         assertEquals("web/HomePagePortlet", tas.getContentId());
         Portlet portlet = tas.getContentState();
         int count = 0;
         for (Preference pref : portlet)
         {
            count++;
         }
         assertEquals(1, count);
         Preference pref = portlet.getPreference("template");
         assertNotNull(pref);
         assertEquals("template", pref.getName());
         assertEquals("system:/templates/groovy/webui/component/UIHomePagePortlet.gtmpl", pref.getValue());
         assertTrue(pref.isReadOnly());

         assertEquals("Mac:MacTheme::Default:DefaultTheme::Vista:VistaTheme", application.getTheme());
         assertEquals("Home Page portlet", application.getTitle());
         assertEquals("Everyone", Utils.join(";", application.getAccessPermissions()));
         assertTrue(application.isShowInfoBar());
         assertTrue(application.isShowApplicationState());
         assertTrue(application.isShowApplicationMode());
         assertNull(application.getDescription());
         assertNull(application.getIcon());
         assertNull(application.getWidth());
         assertNull(application.getHeight());
      }

      // Verify container 2
      ComponentData c2 = rootChildren.get(1);
      assertNotNull(c2);
      assertTrue(c2 instanceof ContainerData);
      ContainerData container2 = (ContainerData) c2;
      assertEquals(container2.getTemplate(), "system:/groovy/portal/webui/container/UITableColumnContainer.gtmpl");
      assertEquals("*:/platform/guests", Utils.join(";", container2.getAccessPermissions()));
      assertEquals("TableColumnContainer", container2.getFactoryId());
      assertNotNull(container2.getChildren());
      assertEquals(2, container2.getChildren().size());

      {
         // Verify column 1 of container 2
         ComponentData appregComp = container2.getChildren().get(0);
         assertTrue(appregComp instanceof ContainerData);
         ContainerData appregContainer = (ContainerData) appregComp;
         assertEquals(appregContainer.getTemplate(), "system:/groovy/portal/webui/container/UIContainer.gtmpl");
         assertEquals("300px", appregContainer.getWidth());
         assertEquals("400px", appregContainer.getHeight());
         assertEquals("Everyone", Utils.join(";", appregContainer.getAccessPermissions()));
         {
            // Verify app registry application
            assertNotNull(appregContainer.getChildren());
            assertEquals(1, appregContainer.getChildren().size());
            ComponentData appregComponent = appregContainer.getChildren().get(0);
            assertTrue(appregComponent instanceof ApplicationData);
            @SuppressWarnings("unchecked")
            ApplicationData<Portlet> application = (ApplicationData<Portlet>) appregComponent;
            assertTrue(application.getType() == ApplicationType.PORTLET);
            ApplicationState<Portlet> state = application.getState();
            assertNotNull(state);
            assertTrue(state instanceof TransientApplicationState);
            TransientApplicationState<Portlet> tas = (TransientApplicationState<Portlet>) state;
            assertEquals("exoadmin/ApplicationRegistryPortlet", tas.getContentId());
            assertNull(tas.getContentState());

            assertEquals("Default:DefaultTheme::Mac:MacTheme::Vista:VistaTheme", application.getTheme());
            assertEquals("Application Registry", application.getTitle());
            assertEquals("*:/platform/administrators;*:/organization/management/executive-board", Utils.join(";", application.getAccessPermissions()));
            assertFalse(application.isShowInfoBar());
            assertTrue(application.isShowApplicationState());
            assertFalse(application.isShowApplicationMode());
            assertEquals("Application Registry", application.getDescription());
            assertEquals("PortletIcon", application.getIcon());
            assertEquals("250px", application.getWidth());
            assertEquals("350px", application.getHeight());
         }

         // Verify column 2 of container 2
         ComponentData orgComp = container2.getChildren().get(1);
         assertTrue(orgComp instanceof ContainerData);
         ContainerData orgContainer = (ContainerData) orgComp;
         assertEquals(orgContainer.getTemplate(), "system:/groovy/portal/webui/container/UIContainer.gtmpl");
         assertEquals("200px", orgContainer.getWidth());
         assertEquals("300px", orgContainer.getHeight());
         assertEquals("/platform/users", Utils.join(";", orgContainer.getAccessPermissions()));
         {
            // Verify org mgmt application
            assertNotNull(orgContainer.getChildren());
            assertEquals(1, orgContainer.getChildren().size());
            ComponentData orgappComp = orgContainer.getChildren().get(0);
            assertTrue(orgappComp instanceof ApplicationData);
            @SuppressWarnings("unchecked")
            ApplicationData<Portlet> application = (ApplicationData<Portlet>) orgappComp;
            assertTrue(application.getType() == ApplicationType.PORTLET);
            ApplicationState<Portlet> state = application.getState();
            assertNotNull(state);
            assertTrue(state instanceof TransientApplicationState);
            TransientApplicationState<Portlet> tas = (TransientApplicationState<Portlet>) state;
            assertEquals("exoadmin/OrganizationPortlet", tas.getContentId());
            assertNull(tas.getContentState());

            assertEquals("Vista:VistaTheme::Mac:MacTheme::Default:DefaultTheme", application.getTheme());
            assertEquals("Organization Management", application.getTitle());
            assertEquals("*:/platform/administrators;*:/organization/management/executive-board", Utils.join(";", application.getAccessPermissions()));
            assertTrue(application.isShowInfoBar());
            assertFalse(application.isShowApplicationState());
            assertFalse(application.isShowApplicationMode());
            assertEquals("Organization Management", application.getDescription());
            assertEquals("StarAwardIcon", application.getIcon());
            assertEquals("100px", application.getWidth());
            assertEquals("200px", application.getHeight());
         }
      }

      // Verify container 3
      ComponentData c3 = rootChildren.get(2);
      assertNotNull(c3);
      assertTrue(c3 instanceof ContainerData);
      ContainerData container3 = (ContainerData) c3;
      assertEquals(container3.getTemplate(), "system:/groovy/portal/webui/container/UIContainer.gtmpl");
      assertEquals("Everyone", Utils.join(";", container3.getAccessPermissions()));
      assertNull(container3.getFactoryId());
      {
         // Verify site map application
         assertNotNull(container3.getChildren());
         assertEquals(1, container3.getChildren().size());
         ComponentData sitemapcomponent = container3.getChildren().get(0);
         assertTrue(sitemapcomponent instanceof ApplicationData);
         @SuppressWarnings("unchecked")
         ApplicationData<Portlet> application = (ApplicationData<Portlet>) sitemapcomponent;
         assertTrue(application.getType() == ApplicationType.PORTLET);
         ApplicationState<Portlet> state = application.getState();
         assertNotNull(state);
         assertTrue(state instanceof TransientApplicationState);
         TransientApplicationState<Portlet> tas = (TransientApplicationState<Portlet>) state;
         assertEquals("web/SiteMapPortlet", tas.getContentId());
         assertNull(tas.getContentState());

         assertEquals("Default:DefaultTheme::Vista:VistaTheme::Mac:MacTheme", application.getTheme());
         assertEquals("SiteMap", application.getTitle());
         assertEquals("*:/platform/users", Utils.join(";", application.getAccessPermissions()));
         assertTrue(application.isShowInfoBar());
         assertTrue(application.isShowApplicationState());
         assertFalse(application.isShowApplicationMode());
         assertEquals("SiteMap", application.getDescription());
         assertNull(application.getIcon());
         assertNull(application.getWidth());
         assertNull(application.getHeight());
      }
   }

   @Test
   public void testEmptyPageUnmarshalling()
   {
      PageDataMarshaller marshaller = new PageDataMarshaller();
      PageData page = marshaller.unmarshal(getClass().getResourceAsStream("/empty-page.xml"));
      assertNotNull(page);
      assertEquals("empty-page", page.getName());
      assertEquals("Empty", page.getTitle());
      assertNotNull(page.getChildren());
      assertTrue(page.getChildren().isEmpty());
   }

   @Test
   public void testPageMarshalling()
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      Portlet portlet = new Portlet();
      portlet.putPreference(new Preference("pref-1", "value-1", true));
      portlet.putPreference(new Preference("pref-2", "value-2", false));
      portlet.putPreference(new Preference("multi-value-pref", Arrays.asList("one", "two", "three"), false));

      ApplicationState<Portlet> state = new TransientApplicationState<Portlet>("app-ref/portlet-ref", portlet);
      ApplicationData<Portlet> applicationData = new ApplicationData<Portlet>(null, null,
         ApplicationType.PORTLET, state, null, "app-title", "app-icon", "app-description", false, true, false,
         "app-theme", "app-wdith", "app-height", new HashMap<String,String>(),
         Collections.singletonList("app-edit-permissions"));

      List<ComponentData> children = Collections.singletonList((ComponentData) applicationData);
      PageData expected = new PageData(null, null, "page-name", null, null, null, "Page Title", null, null, null,
         Collections.singletonList("access-permissions"), children, "", "", "edit-permission", true);

      PageDataMarshaller marshaller = new PageDataMarshaller();
      marshaller.marshal(expected, baos);

      PageData actual = marshaller.unmarshal(new ByteArrayInputStream(baos.toByteArray()));

      comparePages(expected, actual);
   }

   private void comparePages(PageData expected, PageData actual)
   {
      assertNull(actual.getStorageId());
      assertNull(actual.getStorageName());
      assertNull(actual.getId());
      assertEquals("", actual.getOwnerType());
      assertEquals("", actual.getOwnerId());
      assertEquals(expected.getName(), actual.getName());
      assertNull(actual.getIcon());
      assertNull(actual.getTemplate());
      assertNull(actual.getFactoryId());
      assertEquals(expected.getTitle(), actual.getTitle());
      assertNull(actual.getDescription());
      assertNull(actual.getWidth());
      assertNull(actual.getHeight());
      assertEquals(expected.getAccessPermissions(), actual.getAccessPermissions());

      compareComponents(expected.getChildren(), actual.getChildren());

      assertEquals("", actual.getOwnerType());
      assertEquals("", actual.getOwnerId());
      assertEquals(expected.getEditPermission(), actual.getEditPermission());
      assertEquals(expected.isShowMaxWindow(), actual.isShowMaxWindow());
   }
}
