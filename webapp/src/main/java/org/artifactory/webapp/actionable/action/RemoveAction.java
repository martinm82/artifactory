/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPanel;

/**
 * @author yoavl
 */
public class RemoveAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "Remove";

    public RemoveAction() {
        super(ACTION_NAME, "Are you sure you wish to remove");
    }

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoPath repoPath = e.getRepoPath();
        getRepoService().undeploy(repoPath);
        removeNodePanel(e);
        Component componentToRefresh = e.getTargetComponents().getRefreshableComponent();
        if (componentToRefresh != null) {
            e.getTarget().addComponent(componentToRefresh);
        }
    }

    private void removeNodePanel(ItemEvent event) {
        //Remove panel
        WebMarkupContainer nodaPanelContainer = event.getTargetComponents().getNodePanelContainer();
        BrowseRepoPanel browseRepoPanel = (BrowseRepoPanel) nodaPanelContainer.getParent();

        //Create dummy panel
        Panel dummy = new EmptyPanel("nodePanel");
        dummy.setOutputMarkupId(true);
        browseRepoPanel.setItemDisplayPanel(dummy);
        nodaPanelContainer.replace(dummy);
        event.getTarget().addComponent(dummy);
    }
}