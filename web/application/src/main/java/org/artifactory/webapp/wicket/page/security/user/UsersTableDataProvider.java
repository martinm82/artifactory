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

package org.artifactory.webapp.wicket.page.security.user;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Data provider for the users table.
 *
 * @author Yossi Shaul
 */
class UsersTableDataProvider extends SortableDataProvider {
    private List<UserModel> users;

    private UserGroupService userGroupService;

    private SortParam previousSort;
    private UsersFilterPanel usersFilterPanel;

    UsersTableDataProvider(UsersFilterPanel usersFilterPanel, UserGroupService userGroupService) {
        this.usersFilterPanel = usersFilterPanel;
        this.userGroupService = userGroupService;
        //Set default sort
        setSort("username", true);
        previousSort = getSort();
        recalcUsersList();
    }

    public List<UserModel> getUsers() {
        return users;
    }

    public Iterator iterator(int first, int count) {
        if (!previousSort.equals(getSort())) {
            sortUsers();
        }
        List<UserModel> usersSubList = users.subList(first, first + count);
        return usersSubList.iterator();
    }

    public int size() {
        return users.size();
    }

    public IModel model(Object object) {
        return new Model((UserModel) object);
    }

    public void recalcUsersList() {
        users = getFilteredUsers();
        sortUsers();
    }

    private void sortUsers() {
        previousSort = getSort();
        if (users != null) {
            ListPropertySorter.sort(users, getSort());
        }
    }

    private List<UserModel> getFilteredUsers() {
        // get selected users
        Set<UserModel> selectedUsers = getSelectedUsers();

        List<UserInfo> allUsers = userGroupService.getAllUsers(true);
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserInfo userInfo : allUsers) {
            //Don't list anonymous and excluded users
            if (!userInfo.isAnonymous() && includedByFilter(userInfo)) {
                UserModel userModel = new UserModel(userInfo);
                users.add(userModel);

                // persist selection
                if (selectedUsers.contains(userModel)) {
                    userModel.setSelected(true);
                }
            }
        }
        return users;
    }

    private Set<UserModel> getSelectedUsers() {
        Set<UserModel> selectedUsers = new HashSet<UserModel>();
        if (users != null) {
            for (UserModel userModel : users) {
                if (userModel.isSelected()) {
                    selectedUsers.add(userModel);
                }
            }
        }
        return selectedUsers;
    }

    /**
     * @param userInfo The user to check if to include in the table
     * @return True if the user should be included
     */
    private boolean includedByFilter(UserInfo userInfo) {
        return passesUsernameFilter(userInfo) && passesGroupNameFilter(userInfo);
    }

    private boolean passesUsernameFilter(UserInfo userInfo) {
        String usernameFilter = usersFilterPanel.getUsernameFilter();
        return (usernameFilter == null || userInfo.getUsername().contains(usernameFilter));
    }

    private boolean passesGroupNameFilter(UserInfo userInfo) {
        String groupNameFilter = usersFilterPanel.getGroupFilter();
        return (groupNameFilter == null || userInfo.isInGroup(groupNameFilter));
    }


}