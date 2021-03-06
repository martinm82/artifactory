/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.rest.resource.system;


import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.storage.StorageService;
import org.artifactory.backup.InternalBackupService;
import org.artifactory.info.InfoWriter;
import org.artifactory.storage.binstore.service.InternalBinaryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:11:53 PM
 */
@Path(SystemRestConstants.PATH_ROOT)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SystemResource {

    @Context
    HttpServletResponse httpResponse;

    @Context
    private HttpServletRequest httpServletRequest;

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    RepositoryService repoService;

    @Autowired
    SecurityService securityService;

    @Autowired
    StorageService storageService;

    @Autowired
    InternalBinaryStore binaryStore;

    @Autowired
    InternalBackupService backupService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSystemInfo() throws Exception {
        return InfoWriter.getInfoString();
    }

    @Path(SystemRestConstants.PATH_CONFIGURATION)
    public ConfigResource getConfigResource() {
        return new ConfigResource(centralConfigService, httpServletRequest);
    }

    @Path(SystemRestConstants.PATH_SECURITY)
    public SecurityResource getSecurityResource() {
        return new SecurityResource(securityService, centralConfigService, httpServletRequest);
    }

    @Path(SystemRestConstants.PATH_STORAGE)
    public StorageResource getStorageResource() {
        return new StorageResource(storageService, backupService, binaryStore, httpResponse);
    }

}
