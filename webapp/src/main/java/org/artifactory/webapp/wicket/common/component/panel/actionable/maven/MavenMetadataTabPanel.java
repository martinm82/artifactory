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
package org.artifactory.webapp.wicket.common.component.panel.actionable.maven;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.border.fieldset.FieldSetBorder;

/**
 * This tab will be displayed when a directory with attached maven metadata is selected from the browse tree.
 *
 * @author Yoavl
 */
public class MavenMetadataTabPanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    private FolderActionableItem repoItem;

    public MavenMetadataTabPanel(String id, FolderActionableItem repoItem) {
        super(id);
        this.repoItem = repoItem;
        addMetadataContent();
    }

    public void addMetadataContent() {
        FieldSetBorder border = new FieldSetBorder("metadataBorder");
        add(border);
        RepoPath repoPath = repoItem.getCanonicalPath();
        String content = repoService.getXmlMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME);
        TextContentPanel contentPanel = new TextContentPanel("metadataContent");
        contentPanel.setContent(content);
        border.add(contentPanel);
    }
}