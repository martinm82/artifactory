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

package org.artifactory.webapp.wicket.service.authentication;

import org.apache.wicket.Page;
import org.apache.wicket.markup.MarkupStream;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;

/**
 * @author Yoav Aharoni
 */
public class LogoutService extends Page {

    @Override
    protected void onRender(MarkupStream markupStream) {
        ArtifactoryWebSession.get().signOut();
    }
}