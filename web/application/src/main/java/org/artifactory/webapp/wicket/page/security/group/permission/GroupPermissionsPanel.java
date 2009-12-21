package org.artifactory.webapp.wicket.page.security.group.permission;

import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;
import org.artifactory.webapp.wicket.page.security.acl.PermissionsRow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class GroupPermissionsPanel extends BaseModalPanel {

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AclService aclService;

    @SpringBean
    private AuthorizationService authorizationService;

    private GroupInfo groupInfo;

    public GroupPermissionsPanel(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
        setWidth(500);
        setTitle(String.format("%s's Permission Targets", groupInfo.getGroupName()));

        TitledBorder border = new TitledBorder("border");
        add(border);
        border.add(addTable());
    }

    private SortableTable addTable() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new Model("Permission Target"), "permissionTarget.name") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                cellItem.add(new LinkPanel(componentId, rowModel));
            }
        });

        columns.add(new BooleanColumn(new Model("Admin"), "admin", "admin"));
        columns.add(new BooleanColumn(new Model("Delete"), "delete", "delete"));
        columns.add(new BooleanColumn(new Model("Deploy"), "deploy", "deploy"));
        columns.add(new BooleanColumn(new Model("Annotate"), "annotate", "annotate"));
        columns.add(new BooleanColumn(new Model("Read"), "read", "read"));
        PermissionsTabTableDataProvider dataProvider = new PermissionsTabTableDataProvider(groupInfo);
        return new SortableTable("userPermissionsTable", columns, dataProvider, 10);
    }

    class PermissionsTabTableDataProvider extends SortableDataProvider {
        private final GroupInfo groupInfo;
        private List<PermissionsRow> groupPermissions;

        PermissionsTabTableDataProvider(GroupInfo groupInfo) {
            setSort("permissionTarget.name", true);
            this.groupInfo = groupInfo;
            loadData();
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(groupPermissions, getSort());
            List<PermissionsRow> list = groupPermissions.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return groupPermissions.size();
        }

        public IModel model(Object object) {
            return new Model((PermissionsRow) object);
        }

        private void loadData() {
            groupPermissions = new ArrayList<PermissionsRow>();
            List<AclInfo> acls = aclService.getAllAcls();
            for (AclInfo acl : acls) {
                PermissionsRow permissionRow = createPermissionRow(acl);
                addIfHasPermissions(permissionRow, groupPermissions);
            }
        }

        private PermissionsRow createPermissionRow(AclInfo acl) {
            PermissionTargetInfo target = acl.getPermissionTarget();
            Set<AceInfo> infos = acl.getAces();
            AceInfo targetAce = getTargetAce(infos);
            PermissionsRow permissionsRow = new PermissionsRow(target);
            if (targetAce != null) {
                permissionsRow.setRead(targetAce.canRead());
                permissionsRow.setAnnotate(targetAce.canAnnotate());
                permissionsRow.setDeploy(targetAce.canDeploy());
                permissionsRow.setDelete(targetAce.canDelete());
                permissionsRow.setAdmin(targetAce.canAdmin());
            }
            return permissionsRow;
        }

        private AceInfo getTargetAce(Set<AceInfo> infos) {
            for (AceInfo info : infos) {
                if (info.getPrincipal().equals(this.groupInfo.getGroupName())) {
                    return info;
                }
            }
            return null;
        }

        private void addIfHasPermissions(PermissionsRow permissionRow, List<PermissionsRow> userPermissions) {
            if (permissionRow.hasPermissions()) {
                // only add users/groups who have some permission
                userPermissions.add(permissionRow);
            }
        }

    }

    private class LinkPanel extends Panel {
        private LinkPanel(String id, IModel model) {
            super(id, model);
            PermissionsRow permRow = (PermissionsRow) model.getObject();
            final PermissionTargetInfo permissionTarget = permRow.getPermissionTarget();
            Link link = new Link("link") {
                @Override
                public void onClick() {
                    setResponsePage(new AclsPage(permissionTarget));
                }
            };
            add(link);
            link.add(new Label("label", permissionTarget.getName()));
        }
    }
}
