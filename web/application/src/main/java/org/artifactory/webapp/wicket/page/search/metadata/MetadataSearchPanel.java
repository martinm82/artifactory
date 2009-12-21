/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.search.metadata;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.combobox.ComboBox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.util.ComponentUtils;
import org.artifactory.common.wicket.util.CookieUtils;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.search.BaseSearchPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPanel;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableArtifactSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableMetadataSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the metadata searcher
 *
 * @author Noam Tenne
 */
public class MetadataSearchPanel<T extends MetadataSearchResult> extends BaseSearchPanel<T> {

    private MetadataSearchControls searchControls;
    private StyledCheckbox metaDataSearchCheckBox;
    private List<String> xmlTypes;
    private List<String> metaDataNames;

    public MetadataSearchPanel(final Page parent, String id) {
        super(parent, id);
    }

    @Override
    protected void addSearchComponents(Form form) {
        add(new CssClass("metadata-panel"));
        searchControls = new MetadataSearchControls();
        xmlTypes = Lists.newArrayList("*.pom", "*ivy*.xml");
        searchControls.setMetadataName(xmlTypes.get(0));
        final ComboBox typesChoices =
                new ComboBox("metadataName", new PropertyModel(searchControls, "metadataName"),
                        new PropertyModel(this, "metaDataNames"));
        typesChoices.setPersistent(true);
        typesChoices.setRequired(true);
        typesChoices.setOutputMarkupId(true);
        form.add(typesChoices);
        metaDataSearchCheckBox = new StyledCheckbox("metaDataSearch", new Model());
        metaDataSearchCheckBox.setLabel(new Model("Metadata Search"));
        metaDataSearchCheckBox.setPersistent(true);
        form.add(metaDataSearchCheckBox);
        metaDataSearchCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (metaDataSearchCheckBox.isChecked()) {
                    metaDataNames = Lists.newArrayList();
                } else {
                    metaDataNames = xmlTypes;
                }
                target.addComponent(typesChoices, typesChoices.getAjaxTargetMarkupId());
            }
        });

        adjustForMetadataLastSearch();
        form.add(new HelpBubble("metadataName.help", new ResourceModel("metadataName.help")));

        TextArea xPathTextArea = new TextArea("xPathTextArea", new PropertyModel(searchControls, "path"));
        form.add(xPathTextArea);
        xPathTextArea.setRequired(true);
        xPathTextArea.setOutputMarkupId(true);
        xPathTextArea.setPersistent(true);
        form.add(new HelpBubble("xpath.help", new ResourceModel("xpath.help")));

        TextField metadataValueField = new TextField("metadataValueField", new PropertyModel(searchControls, "value"));
        form.add(metadataValueField);
        metadataValueField.setOutputMarkupId(true);
        metadataValueField.setPersistent(true);
        form.add(new HelpBubble("metadataValue.help", new ResourceModel("metadataValue.help")));
        StyledCheckbox exactMatchCheckbox =
                new StyledCheckbox("exactMatch", new PropertyModel(searchControls, "exactMatch"));
        exactMatchCheckbox.setLabel(new Model("Exact Match"));
        exactMatchCheckbox.setPersistent(true);
        form.add(exactMatchCheckbox);
    }

    @Override
    protected MetadataSearchControls getSearchControles() {
        return searchControls;
    }

    private void adjustForMetadataLastSearch() {
        metaDataNames = xmlTypes;
        String id = metaDataSearchCheckBox.getId();
        Cookie cookie = CookieUtils.getCookieBycomponentId(id);
        if (cookie != null) {
            String value = cookie.getValue();
            if ("true".equals(value)) {
                metaDataNames = Lists.newArrayList();
            }
        }
    }

    @Override
    protected Class<? extends BaseSearchPage> getMenuPageClass() {
        return MetadataSearchPage.class;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ComponentUtils.updatePersistentFormComponents(this);
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new ActionsColumn(""));

        columns.add(new BaseSearchPanel.ArtifactNameColumn("Metadata Container", "searchResult.name"));
        columns.add(new PropertyColumn(new Model("Path"), "searchResult.relDirPath", "searchResult.relDirPath"));
        columns.add(new PropertyColumn(new Model("Repository"), "searchResult.repoKey", "searchResult.repoKey"));
    }

    @Override
    public String getSearchExpression() {
        return searchControls.getValue();
    }

    @Override
    protected SearchResults<T> searchArtifacts() {
        //noinspection unchecked
        return search(metaDataSearchCheckBox.isChecked(), searchControls);
    }

    @Override
    protected SearchResults<T> performLimitlessArtifactSearch() {
        MetadataSearchControls controlsCopy = new MetadataSearchControls(searchControls);
        controlsCopy.setLimitSearchResults(false);
        //noinspection unchecked
        return search(metaDataSearchCheckBox.isChecked(), controlsCopy);
    }

    @Override
    protected void onSearch() {
        super.onSearch();
        if (metaDataSearchCheckBox.isChecked()) {
            metaDataNames = new ArrayList<String>();
            metaDataNames.add(searchControls.getMetadataName());
        }
    }

    @Override
    protected void onNoResults() {
        String value = StringEscapeUtils.escapeHtml(searchControls.getValue());
        if (StringUtils.isEmpty(value)) {
            value = "";
        } else {
            value = " for '" + value + "'";
        }
        Session.get().warn(String.format("No artifacts found%s.", value));
    }

    @Override
    protected ActionableSearchResult<T> getActionableResult(T searchResult) {
        if (metaDataSearchCheckBox.isChecked()) {
            return new ActionableMetadataSearchResult<T>(searchResult);
        }
        return new ActionableArtifactSearchResult<T>(searchResult);
    }

    @Override
    protected boolean isLimitSearchResults() {
        return searchControls.isLimitSearchResults();
    }

    /**
     * Performs the search
     *
     * @param metaDataSearch True if should search for metadata. False if should search for xml type
     * @param controls       Search controls
     * @return List of search results
     */
    private SearchResults search(boolean metaDataSearch, MetadataSearchControls controls) {
        if (metaDataSearch) {
            return searchService.searchMetadata(controls);
        }
        return searchService.searchXmlContent(controls);
    }
}