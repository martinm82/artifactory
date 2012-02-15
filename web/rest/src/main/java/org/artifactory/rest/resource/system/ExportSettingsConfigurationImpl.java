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

/**
 * Date: 10/27/11
 * Time: 2:37 PM
 *
 * @author Fred Simon
 */
public class ExportSettingsConfigurationImpl {
    public String exportPath;
    public boolean includeMetadata = true;
    public boolean createArchive;
    public boolean bypassFiltering;
    public boolean verbose;
    public boolean failOnError = true;
    public boolean failIfEmpty = true;
    public boolean m2;
    public boolean incremental;
    public boolean excludeContent;
}