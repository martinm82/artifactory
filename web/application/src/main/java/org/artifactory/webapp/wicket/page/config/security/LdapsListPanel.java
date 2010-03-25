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

package org.artifactory.webapp.wicket.page.config.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;

import java.util.Collections;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public class LdapsListPanel extends ModalListPanel<LdapSetting> {

    @SpringBean
    private CentralConfigService centralConfigService;

    private MutableCentralConfigDescriptor mutableDescriptor;

    public LdapsListPanel(String id) {
        super(id);
        setOutputMarkupId(true);
        mutableDescriptor = centralConfigService.getMutableDescriptor();
    }

    @Override
    public String getTitle() {
        return "LDAP Servers";
    }

    @Override
    protected List<LdapSetting> getList() {
        SecurityDescriptor security = mutableDescriptor.getSecurity();
        List<LdapSetting> ldaps = security.getLdapSettings();
        if (ldaps != null) {
            return ldaps;
        }
        return Collections.emptyList();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("LDAP Key"), "key", "key"));
        columns.add(new PropertyColumn(new Model("URL"), "ldapUrl", "ldapUrl"));
        columns.add(new BooleanColumn(new Model("Enabled"), "enabled", "enabled"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new LdapCreateUpdatePanel(CreateUpdateAction.CREATE, new LdapSetting(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(LdapSetting ldapSetting) {
        return new LdapCreateUpdatePanel(CreateUpdateAction.UPDATE, ldapSetting, this);
    }

    @Override
    protected String getDeleteConfirmationText(LdapSetting ldapSetting) {
        return "Are you sure you wish to delete the ldap " + ldapSetting.getKey() + "?";
    }

    @Override
    protected void deleteItem(LdapSetting ldapSetting, AjaxRequestTarget target) {
        SecurityDescriptor securityDescriptor = mutableDescriptor.getSecurity();
        securityDescriptor.removeLdap(ldapSetting.getKey());
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
    }

    MutableCentralConfigDescriptor getMutableDescriptor() {
        return mutableDescriptor;
    }

    void setMutableDescriptor(MutableCentralConfigDescriptor mutableDescriptor) {
        this.mutableDescriptor = mutableDescriptor;
    }
}