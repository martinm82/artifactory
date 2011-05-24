/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.panel.tabbed.StyledTabbedPanel;
import org.artifactory.webapp.wicket.panel.tabbed.SubmittingTabbedPanel;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.ReservedPathPrefixValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

import java.util.List;

/**
 * Base panel for repositories configuration.
 *
 * @author Yossi Shaul
 */
public abstract class RepoConfigCreateUpdatePanel<E extends RepoDescriptor> extends CreateUpdatePanel<E> {

    @SpringBean
    protected AddonsManager addons;

    @SpringBean
    protected CentralConfigService centralConfigService;

    @SpringBean
    protected RepositoryService repositoryService;

    protected final CachingDescriptorHelper cachingDescriptorHelper;

    @WicketProperty
    protected String key;

    protected RepoConfigCreateUpdatePanel(CreateUpdateAction action, E repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor);
        this.cachingDescriptorHelper = cachingDescriptorHelper;
        form.setOutputMarkupId(true);
        add(new CssClass("repo-config"));
        setWidth(650);
        TitledBorder repoConfigBorder = new TitledBorder("repoConfigBorder");
        TextField<String> repoKeyField = new TextField<String>("key", new PropertyModel<String>(this, "key"));
        boolean create = isCreate();
        repoKeyField.setEnabled(create);// don't allow key update
        if (create) {
            repoKeyField.add(new JcrNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new XsdNCNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new UniqueXmlIdValidator(cachingDescriptorHelper.getModelMutableDescriptor()));
            repoKeyField.add(new ReservedPathPrefixValidator());
        } else {
            repoKeyField.setModelObject(repoDescriptor.getKey());
        }

        repoConfigBorder.add(repoKeyField);
        repoConfigBorder.add(new SchemaHelpBubble("key.help"));

        StyledTabbedPanel repoConfigTabbedPanel =
                new SubmittingTabbedPanel("repoConfigTabbedPanel", getConfigurationTabs()) {
                    @Override
                    protected void onAjaxUpdate(AjaxRequestTarget target) {
                        super.onAjaxUpdate(target);
                        ModalHandler.resizeCurrent(target);
                        ModalHandler.centerCurrent(target);
                    }
                };
        repoConfigTabbedPanel.setOutputMarkupId(true);
        repoConfigBorder.add(repoConfigTabbedPanel);

        form.add(repoConfigBorder);

        // Cancel button
        form.add(getCloseLink());

        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        add(form);
    }

    protected abstract List<ITab> getConfigurationTabs();

    protected ModalCloseLink getCloseLink() {
        return new ModalCloseLink("cancel");
    }

    public abstract void addAndSaveDescriptor(E repoDescriptor);

    public abstract void saveEditDescriptor(E repoDescriptor);

    @SuppressWarnings({"unchecked"})
    protected E getRepoDescriptor() {
        return (E) form.getDefaultModelObject();
    }

    private TitledAjaxSubmitLink createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                E repoDescriptor = getRepoDescriptor();
                if (StringUtils.isBlank(key)) {
                    error("Please enter the repository key.");
                    AjaxUtils.refreshFeedback(target);
                    return;
                }
                if (!validate(repoDescriptor)) {
                    AjaxUtils.refreshFeedback();
                    return;
                }
                if (isCreate()) {
                    addAndSaveDescriptor(repoDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully created.");
                } else {
                    saveEditDescriptor(repoDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully updated.");
                }

                ((RepositoryConfigPage) getPage()).refresh(target);
                AjaxUtils.refreshFeedback(target);
                close(target);
            }
        };
    }

    protected abstract boolean validate(E repoDescriptor);

    protected CachingDescriptorHelper getCachingDescriptorHelper() {
        return cachingDescriptorHelper;
    }

    @Override
    public void onClose(AjaxRequestTarget target) {
        if (!isCreate()) {
            // if not create, reload the repo from the latest config to handle cancel actions
            getCachingDescriptorHelper().reloadRepository(getRepoDescriptor().getKey());
        }
    }
}
