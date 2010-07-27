/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.panel.tabbed;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.common.wicket.util.CookieUtils;
import org.artifactory.webapp.wicket.panel.tabbed.tab.DisabledBaseTab;

import java.util.List;

/**
 * A StyledTabbedPanel that remembers the last selected tab.
 *
 * @author Yossi Shaul
 */
public class PersistentTabbedPanel extends StyledTabbedPanel {
    private static final String COOKIE_NAME = "browse-last-tab";
    private static final int UNSET = -1;

    private int lastTabIndex;

    public PersistentTabbedPanel(String id, List<ITab> tabs) {
        super(id, tabs);

        lastTabIndex = getLastTabIndex();
    }

    @Override
    protected void onBeforeRender() {
        selectLastTab();
        super.onBeforeRender();
    }

    /**
     * Re-select last selected tab. To be called before render becuase some tabs are loaded lazily (calling
     * selectLastTab from c'tor might cause NPE).
     */
    private void selectLastTab() {
        if (lastTabIndex != UNSET) {
            setSelectedTab(lastTabIndex);
            lastTabIndex = UNSET;
        }
    }

    /**
     * Return last tab index as stored in cookie.
     */
    @SuppressWarnings({"unchecked"})
    private int getLastTabIndex() {
        List<ITab> tabs = getTabs();

        /**
         * If a parameter of a tab title to select was included in the request, give it priority over the
         * last-selected-tab cookie
         */
        String defaultSelectionTabTitle = getDefaultSelectionTabTitle();
        int indexToReturn = getTabIndexByTitle(defaultSelectionTabTitle, tabs);
        if (indexToReturn != -1) {
            CookieUtils.setCookie(COOKIE_NAME, defaultSelectionTabTitle);
            return indexToReturn;
        }

        indexToReturn = getTabIndexByTitle(CookieUtils.getCookie(COOKIE_NAME), tabs);
        if (indexToReturn != -1) {
            return indexToReturn;
        }

        return UNSET;
    }

    @Override
    protected void onAjaxUpdate(AjaxRequestTarget target) {
        super.onAjaxUpdate(target);

        // store last tab name in a cookie
        ITab tab = (ITab) getTabs().get(getSelectedTab());
        CookieUtils.setCookie(COOKIE_NAME, tab.getTitle().getObject().toString());
    }

    /**
     * Retrieves the default selection tab title from the web request.
     *
     * @return Default selection tab title. May be null if not included in request
     */
    private String getDefaultSelectionTabTitle() {
        WebRequest request = (WebRequest) RequestCycle.get().getRequest();
        return request.getParameter("selectTab");
    }

    private int getTabIndexByTitle(String tabTitle, List<ITab> tabs) {
        if (StringUtils.isNotBlank(tabTitle)) {
            for (int i = 0; i < tabs.size(); i++) {
                ITab tab = tabs.get(i);
                String tabName = tab.getTitle().getObject().toString();
                if (tabName.equals(tabTitle) && !(tab instanceof DisabledBaseTab)) {
                    return i;
                }
            }
        }

        return UNSET;
    }
}