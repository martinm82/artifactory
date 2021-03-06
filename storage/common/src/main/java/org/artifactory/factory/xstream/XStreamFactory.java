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

package org.artifactory.factory.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import javanet.staxutils.StaxUtilsXMLOutputFactory;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nullable;
import javax.xml.stream.XMLOutputFactory;

/**
 * @author Yoav Landman
 */
public abstract class XStreamFactory {
    private XStreamFactory() {
        // utility class
    }

    public static XStream create(Class... annotatedClassesToProcess) {
        return createXStream(null, false, annotatedClassesToProcess);
    }

    public static XStream create(@Nullable QNameMap qNameMap, Class... annotatedClassesToProcess) {
        return createXStream(qNameMap, false, annotatedClassesToProcess);
    }

    /**
     * Creates a missing field tolerating instance of XStream
     *
     * @param annotatedClassesToProcess Classes to process
     * @return XStream instance
     */
    public static XStream createMissingFieldTolerating(Class... annotatedClassesToProcess) {
        return createXStream(null, true, annotatedClassesToProcess);
    }

    /**
     * Creates an XStream instance
     *
     * @param qNameMap                  Optional map
     * @param ignoreMissingMembers      True if missing fields should be ignored
     * @param annotatedClassesToProcess Classes to process
     * @return XStream instance
     */
    private static XStream createXStream(@Nullable QNameMap qNameMap, boolean ignoreMissingMembers,
            Class... annotatedClassesToProcess) {
        XStream xstream = new ResilientXStream(ignoreMissingMembers, new PrettyStaxDriver(qNameMap));
        xstream.registerConverter(new RepoPathConverter());
        xstream.registerConverter(new PropertiesConverter());
        xstream.registerConverter(new ChecksumsInfoConverter());
        xstream.registerConverter(new UserGroupInfoConverter());
        for (Class annotatedClass : annotatedClassesToProcess) {
            xstream.processAnnotations(annotatedClass);
        }
        xstream.alias("repoPath", RepoPath.class, RepoPathImpl.class);
        return xstream;
    }

    /**
     * XStream instance that can optionally ignore missing fields
     */
    private static class ResilientXStream extends XStream {

        private boolean ignoreMissingMembers;

        private ResilientXStream(boolean ignoreMissingMembers, HierarchicalStreamDriver driver) {
            super(driver);
            this.ignoreMissingMembers = ignoreMissingMembers;
        }

        @Override
        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new MapperWrapper(next) {

                @Override
                public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                    return (!ignoreMissingMembers || (definedIn != Object.class)) ?
                            super.shouldSerializeMember(definedIn, fieldName) : false;
                }
            };
        }
    }

    /**
     * Stax driver configured to pretty print
     */
    private static class PrettyStaxDriver extends StaxDriver {
        private XMLOutputFactory outputFactory;

        private PrettyStaxDriver(QNameMap qNameMap) {
            super((qNameMap == null) ? new QNameMap() : qNameMap);
        }

        @Override
        public XMLOutputFactory getOutputFactory() {
            if (outputFactory == null) {
                //Decorate the original output factory - make it pretty print
                outputFactory = new StaxUtilsXMLOutputFactory(super.getOutputFactory()) {
                    @Override
                    public Object getProperty(String name) throws IllegalArgumentException {
                        //noinspection SimplifiableIfStatement
                        if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
                            //Avoid delegating to the parent XOF, since may result in IAE (RTFACT-2193).
                            return false;
                        }
                        return super.getProperty(name);
                    }
                };
                outputFactory.setProperty(StaxUtilsXMLOutputFactory.INDENTING, true);
            }
            return outputFactory;
        }
    }
}
