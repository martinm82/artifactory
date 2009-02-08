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
package org.artifactory.repo.webdav;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.AccessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * User: freds Date: Jul 27, 2008 Time: 9:27:12 PM
 */
@Service
public class WebdavServiceImpl implements WebdavService {
    private static final Logger log = LoggerFactory.getLogger(WebdavServiceImpl.class);

    /**
     * Default depth is infite.
     */
    private static final int INFINITY = 3;// To limit tree browsing a bit

    /**
     * PROPFIND - Specify a property mask.
     */
    private static final int FIND_BY_PROPERTY = 0;

    /**
     * PROPFIND - Display all properties.
     */
    private static final int FIND_ALL_PROP = 1;

    /**
     * PROPFIND - Return property names.
     */
    private static final int FIND_PROPERTY_NAMES = 2;

    /**
     * Default namespace.
     */
    protected static final String DEFAULT_NAMESPACE = "DAV:";

    /**
     * Simple date format for the creation date ISO representation (partial).
     */
    protected static final SimpleDateFormat creationDateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        //GMT timezone - all HTTP dates are on GMT
        creationDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repositoryService;

    public void handlePropfind(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException {
        // Retrieve the resources
        RepoPath repoPath = request.getRepoPath();
        String path = repoPath.getPath();
        int depth = INFINITY;
        String depthStr = request.getHeader("Depth");
        if (depthStr != null) {
            if ("0".equals(depthStr)) {
                depth = 0;
            } else if (depthStr.equals("1")) {
                depth = 1;
            } else if (depthStr.equals("infinity")) {
                depth = INFINITY;
            }
        }
        List<String> properties = null;
        int propertyFindType = FIND_ALL_PROP;
        Node propNode = null;
        //get propertyNode and type
        if (request.getContentLength() != 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            try {
                Document document = documentBuilder.parse(
                        new InputSource(request.getInputStream()));
                // Get the root element of the document
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();

                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            if (currentNode.getNodeName().endsWith("prop")) {
                                propertyFindType = FIND_BY_PROPERTY;
                                propNode = currentNode;
                            }
                            if (currentNode.getNodeName().endsWith("propname")) {
                                propertyFindType = FIND_PROPERTY_NAMES;
                            }
                            if (currentNode.getNodeName().endsWith("allprop")) {
                                propertyFindType = FIND_ALL_PROP;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Webdav propfind failed.", e);
            }
        }

        if (propertyFindType == FIND_BY_PROPERTY) {
            properties = getPropertiesFromXml(propNode);
        }

        response.setStatus(WebdavStatus.SC_MULTI_STATUS);
        response.setContentType("text/xml; charset=UTF-8");

        // Create multistatus object
        //Writer writer = new StringWriter();
        Writer writer = response.getWriter();
        XmlWriter generatedXml = new XmlWriter(writer);
        generatedXml.writeXMLHeader();
        generatedXml.writeElement(null, "multistatus xmlns=\"" + DEFAULT_NAMESPACE + "\"",
                XmlWriter.OPENING);
        if (depth > 0) {
            recursiveParseProperties(request, response, generatedXml, path,
                    propertyFindType, properties, depth);
        } else {
            parseProperties(request, response, generatedXml, path,
                    propertyFindType, properties);
        }
        generatedXml.writeElement(null, "multistatus", XmlWriter.CLOSING);
        generatedXml.sendData();
        response.flush();
    }

    public void handleMkcol(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        final LocalRepo repo = getLocalRepo(request, response);
        //Return 405 if folder exists
        if (repo.itemExists(repoPath.getPath())) {
            response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
            return;
        }
        //Check that we are allowed to write
        checkCanDeploy(repoPath, response);
        JcrFolder targetFolder = repo.getLockedJcrFolder(repoPath, true);
        targetFolder.mkdirs();
        // Return 201 when element created
        response.setStatus(HttpStatus.SC_CREATED);
    }

    public void handleDelete(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        //Check that we are allowed to write
        checkCanDeploy(repoPath, response);
        final LocalRepo repo = getLocalRepo(request, response);
        repo.undeploy(repoPath);
        response.sendOk();
    }

    public void handleOptions(ArtifactoryResponse response) throws IOException {
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", "OPTIONS, MKCOL, PUT, GET, HEAD, POST, DELETE, PROPFIND");
        response.setHeader("MS-Author-Via", "DAV");
        response.sendOk();
    }

    private void checkCanDeploy(RepoPath repoPath,
            ArtifactoryResponse response) throws IOException {
        boolean canDeploy = authService.canDeploy(repoPath);
        if (!canDeploy) {
            String msg = "Deploy to '" + repoPath + "' not allowed.";
            response.sendError(HttpStatus.SC_FORBIDDEN, msg, log);
            AccessLogger.deployDenied(repoPath);
            throw new RuntimeException(msg);
        }
    }

    /**
     * resourcetype Return JAXP document builder instance.
     *
     * @return
     * @throws javax.servlet.ServletException
     */
    private DocumentBuilder getDocumentBuilder() throws IOException {
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("JAXP document builder creation failed");
        }
        return documentBuilder;
    }

    private List<String> getPropertiesFromXml(Node propNode) {
        List<String> properties;
        properties = new ArrayList<String>();
        NodeList childList = propNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node currentNode = childList.item(i);
            switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    String nodeName = currentNode.getNodeName();
                    String propertyName;
                    if (nodeName.indexOf(':') != -1) {
                        propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
                    } else {
                        propertyName = nodeName;
                    }
                    // href is a live property which is handled differently
                    properties.add(propertyName);
                    break;
            }
        }
        return properties;
    }

    /**
     * Propfind helper method.
     *
     * @param generatedXml           XML response to the Propfind request
     * @param path                   Path of the current resource
     * @param type                   Propfind type
     * @param propertiesList<String> If the propfind type is find properties by name, then this List<String> contains
     *                               those properties
     */
    private void parseProperties(ArtifactoryRequest request,
            ArtifactoryResponse response,
            XmlWriter generatedXml, final String path,
            int type, List<String> propertiesList) throws IOException {
        JcrFsItem item = getJcrFsItem(request, response, path);
        if (item == null) {
            log.warn("Item '" + path + "' not found.");
            return;
        }
        String creationDate = getIsoCreationDate(item.getCreated());
        boolean isFolder = item.isDirectory();
        String lastModified =
                getIsoCreationDate(isFolder ? 0 : ((JcrFile) item).getLastModified());
        String resourceLength = isFolder ? "0" : (((JcrFile) item).getSize() + "");

        generatedXml.writeElement(null, "response", XmlWriter.OPENING);
        String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                WebdavStatus.getStatusText(WebdavStatus.SC_OK);

        //Generating href element
        generatedXml.writeElement(null, "href", XmlWriter.OPENING);
        String origPath = request.getPath();
        String uri = request.getUri();
        String hrefBase = uri;
        if (origPath.length() > 0) {
            int idx = uri.lastIndexOf(origPath);
            if (idx > 0) {
                //When called recursively avoid concatenating the original path on top of itself
                hrefBase = uri.substring(0, idx);
            }
        }
        String href = hrefBase + path;
        /*if (isFolder && !href.endsWith("/")) {
            href += "/";
        }*/

        //String encodedHref = encoder.encode(href);
        generatedXml.writeText(href);
        generatedXml.writeElement(null, "href", XmlWriter.CLOSING);

        String resourceName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            resourceName = resourceName.substring(lastSlash + 1);
        }

        switch (type) {
            case FIND_ALL_PROP:
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);

                generatedXml.writeProperty(null, "creationdate", creationDate);
                generatedXml.writeElement(null, "displayname", XmlWriter.OPENING);
                generatedXml.writeData(resourceName);
                generatedXml.writeElement(null, "displayname", XmlWriter.CLOSING);
                if (!isFolder) {
                    generatedXml.writeProperty(null, "getlastmodified", lastModified);
                    generatedXml.writeProperty(null, "getcontentlength", resourceLength);

                    ContentType ct = NamingUtils.getContentType(path);
                    if (ct != null) {
                        generatedXml.writeProperty(null, "getcontenttype", ct.getMimeType());
                    }
                    generatedXml.writeProperty(
                            null, "getetag", getEtag(resourceLength, lastModified));
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                } else {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                }
                generatedXml.writeProperty(null, "source", "");
                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_PROPERTY_NAMES:
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                generatedXml.writeElement(null, "creationdate", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "displayname", XmlWriter.NO_CONTENT);
                if (!isFolder) {
                    generatedXml.writeElement(null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getcontentlength", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getcontenttype", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getetag", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getlastmodified", XmlWriter.NO_CONTENT);
                }
                generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "source", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "lockdiscovery", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_BY_PROPERTY:
                //noinspection MismatchedQueryAndUpdateOfCollection
                List<String> propertiesNotFound = new ArrayList<String>();
                // Parse the list of properties
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                for (String property : propertiesList) {
                    if (property.equals("creationdate")) {
                        generatedXml.writeProperty(null, "creationdate", creationDate);
                    } else if (property.equals("displayname")) {
                        generatedXml.writeElement(null, "displayname", XmlWriter.OPENING);
                        generatedXml.writeData(resourceName);
                        generatedXml.writeElement(null, "displayname", XmlWriter.CLOSING);
                    } else if (property.equals("getcontentlanguage")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeElement(
                                    null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                        }
                    } else if (property.equals("getcontentlength")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontentlength", resourceLength);
                        }
                    } else if (property.equals("getcontenttype")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontenttype",
                                    NamingUtils.getMimeTypeByPathAsString(path));
                        }
                    } else if (property.equals("getetag")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getetag",
                                    getEtag(resourceLength, lastModified));
                        }
                    } else if (property.equals("getlastmodified")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getlastmodified", lastModified);
                        }
                    } else if (property.equals("source")) {
                        generatedXml.writeProperty(null, "source", "");
                    } else {
                        propertiesNotFound.add(property);
                    }
                }
                //Always include resource type
                if (isFolder) {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                } else {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                }

                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);

                // TODO: [by fsi] Find out what this is for?
                /*
                if (propertiesNotFound.size() > 0) {
                    status = "HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " " +
                            WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND);
                    generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                    for (String propertyNotFound : propertiesNotFound) {
                        generatedXml.writeElement(null, propertyNotFound, XmlWriter.NO_CONTENT);
                    }
                    generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                    generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                    generatedXml.writeText(status);
                    generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                    generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);

                }
                */
                break;

        }
        generatedXml.writeElement(null, "response", XmlWriter.CLOSING);
    }

    /**
     * goes recursive through all folders. used by propfind
     */
    private void recursiveParseProperties(ArtifactoryRequest request,
            ArtifactoryResponse response,
            XmlWriter generatedXml, String currentPath,
            int propertyFindType, List<String> properties, int depth) throws IOException {
        parseProperties(request, response, generatedXml,
                currentPath, propertyFindType, properties);
        JcrFsItem item = getJcrFsItem(request, response, currentPath);
        if (item == null) {
            log.warn("Folder '" + currentPath + "' not found.");
            return;
        }
        if (depth > 0 && item.isDirectory()) {
            JcrFolder folder = (JcrFolder) item;
            List<JcrFsItem> children = folder.getItems();
            for (JcrFsItem child : children) {
                String newPath = child.getRelativePath();
                recursiveParseProperties(
                        request, response, generatedXml, newPath,
                        propertyFindType, properties, depth - 1);
            }
        }
    }

    private JcrFsItem getJcrFsItem(ArtifactoryRequest request,
            ArtifactoryResponse response, String path) throws IOException {
        final LocalRepo repo = getLocalRepo(request, response);
        String repoKey = request.getRepoKey();
        RepoPath repoPath = new RepoPath(repoKey, path);
        StatusHolder status = repo.allowsDownload(repoPath);
        //Check that we are allowed to read
        if (status.isError()) {
            String msg = status.getStatusMsg();
            response.sendError(status.getStatusCode(), msg, log);
            throw new RuntimeException(msg);
        }
        JcrFsItem item = repo.getJcrFsItem(repoPath);
        return item;
    }

    private LocalRepo getLocalRepo(ArtifactoryRequest request,
            ArtifactoryResponse response) throws IOException {
        String repoKey = request.getRepoKey();
        final LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            String msg = "Could not find repo '" + repoKey + "'.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            throw new RuntimeException(msg);
        }
        return repo;
    }

    /**
     * Get the ETag associated with a file.
     */
    private static String getEtag(String resourceLength, String lastModified)
            throws IOException {
        return "W/\"" + resourceLength + "-" + lastModified + "\"";
    }

    /**
     * Get creation date in ISO format.
     */
    private static synchronized String getIsoCreationDate(long creationDate) {
        StringBuffer creationDateValue =
                new StringBuffer(creationDateFormat.format(new Date(creationDate)));
        return creationDateValue.toString();
    }

}