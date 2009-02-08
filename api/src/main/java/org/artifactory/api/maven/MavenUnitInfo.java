/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.maven;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;
import org.artifactory.api.mime.ContentType;
import org.artifactory.util.PathUtils;

/**
 * @author freds
 * @date Oct 19, 2008
 */
@XStreamAlias(MavenUnitInfo.ROOT)
public class MavenUnitInfo implements Info {
    public static final String ROOT = "artifactory-maven-unit";

    public static final String NA = "NA";

    public static final String POM = ContentType.mavenPom.getDefaultExtension();
    public static final String JAR = ContentType.javaArchive.getDefaultExtension();
    public static final String XML = ContentType.applicationXml.getDefaultExtension();

    private String groupId;
    private String artifactId;
    private String version;

    public MavenUnitInfo(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null) {
            throw new IllegalArgumentException("Cannot create a maven unit with null groupId or ArtifactId");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        if (PathUtils.hasText(version)) {
            this.version = version;
        } else {
            this.version = NA;
        }
    }

    public MavenUnitInfo(MavenUnitInfo copy) {
        this.groupId = copy.groupId;
        this.artifactId = copy.artifactId;
        this.version = copy.version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean hasGroupId() {
        return groupId != null && !NA.equals(groupId);
    }

    public boolean hasArtifactId() {
        return artifactId != null && !NA.equals(artifactId);
    }

    public boolean hasVersion() {
        return version != null && !NA.equals(version);
    }

    public boolean isValid() {
        return groupId != null && !NA.equals(groupId) && artifactId != null && !NA.equals(artifactId);
    }

    public String getPath() {
        return addBasePath(new StringBuilder()).toString();
    }

    public StringBuilder addBasePath(StringBuilder path) {
        if (isValid()) {
            path.append(groupId.replace('.', '/')).append("/").append(artifactId);
            if (hasVersion()) {
                path.append("/").append(version);
            }
        }
        return path;
    }

    public boolean isSnapshot() {
        return MavenNaming.isVersionSnapshot(version);
    }

    public void invalidate() {
        setGroupId(NA);
        setArtifactId(NA);
        setVersion(NA);
    }

    public String getXml() {
        if (!isValid()) {
            return "<error>Invalid Maven Unit</error>";
        }
        XmlBuilder builder = addBaseTags(new XmlBuilder());
        return builder.toString();
    }

    protected XmlBuilder addBaseTags(XmlBuilder builder) {
        builder.wrapInTag(groupId, "groupId");
        builder.wrapInTag(artifactId, "artifactId");
        if (hasVersion()) {
            builder.wrapInTag(version, "version");
        }
        return builder;
    }

    protected static class XmlBuilder {
        private final StringBuilder builder = new StringBuilder();

        protected StringBuilder wrapInTag(String content, String tag) {
            return builder.append("<").append(tag).append(">").append(content).append("</").append(tag).append(">");
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MavenUnitInfo)) {
            return false;
        }
        MavenUnitInfo info = (MavenUnitInfo) o;
        return artifactId.equals(info.artifactId) && groupId.equals(info.groupId) && version.equals(info.version);
    }

    @Override
    public int hashCode() {
        int result;
        result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MavenUnitInfo{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}