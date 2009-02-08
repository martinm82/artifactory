/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.application.sitemap.MenuNode;
import org.artifactory.webapp.wicket.application.sitemap.SiteMap;

import java.util.Collections;
import java.util.List;

@AuthorizeInstantiation(AuthorizationService.ROLE_USER)
public abstract class AuthenticatedPage extends BasePage {

    @SpringBean
    private CentralConfigService centralConfig;


    protected AuthenticatedPage() {
        add(new Label("title", getPageName()));
        add(new SubMenuPanel("sideMenuPanel", getSecondLevelMenuNodes(), getMenuPageClass()));
    }

    private List<MenuNode> getSecondLevelMenuNodes() {
        SiteMap siteMap = ArtifactoryApplication.get().getSiteMap();
        MenuNode pageNode = siteMap.getPageNode(getMenuPageClass());
        if (pageNode == null) {
            return Collections.emptyList();
        }
        MenuNode current = pageNode;
        while (!current.getParent().equals(siteMap.getRoot())) {
            current = current.getParent();
        }
        return current.getChildren();
    }
}
