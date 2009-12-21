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

package org.artifactory.webapp.wicket.page.security.login.forgot;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebApplicationAddon;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.util.HttpUtils;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.reset.ResetPasswordPage;

/**
 * Displays the "Forgot Password" interface. and mails the user (if valid) with a key to the password reset page
 *
 * @author Noam Tenne
 */
public class ForgotPasswordPanel extends TitledActionPanel {


    @SpringBean
    private AddonsManager addonsManager;

    protected ForgotPasswordPanel(String id) {
        super(id);

        Form forgotForm = new Form("forgotForm");

        forgotForm.add(new Label("description",
                "Please enter your user name to receive a password reset link by email."));

        final TextField usernameTextField = new TextField("username", new Model());
        usernameTextField.setRequired(true);
        forgotForm.add(usernameTextField);

        TitledAjaxSubmitLink sendButton = new TitledAjaxSubmitLink("send", "Send It To Me", forgotForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                String username = usernameTextField.getValue();
                //Check if username is valid
                if (StringUtils.isEmpty(username)) {
                    displayError("Please specifiy a valid username");
                    return;
                }
                // if in aol mode then have to go to the dashboard to reset password
                String remoteAddress = HttpUtils.getRemoteClientAddress(
                        WicketUtils.getWebRequest().getHttpServletRequest());
                String resetPasswordPageUrl = getResetPasswordPageUrl();
                try {
                    WebApplicationAddon applicationAddon = addonsManager.addonByType(WebApplicationAddon.class);
                    String status = applicationAddon.resetPassword(username, remoteAddress, resetPasswordPageUrl);
                    Session.get().info(status);
                } catch (Exception e) {
                    displayError(e.getMessage());
                }
                setResponsePage(LoginPage.class);
            }

            /**
             * Display an error message in the feedback
             *
             * @param errorMessage The error to display
             */

            private void displayError(String errorMessage) {
                Session.get().error(errorMessage);
                setResponsePage(LoginPage.class);
            }
        };
        forgotForm.add(new DefaultButtonBehavior(sendButton));
        addButton(sendButton);

        addButton(new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new LoginPage());
            }
        });

        add(forgotForm);
    }

    /**
     * Get the bookmarkable URL of the reset password page
     *
     * @return String - URL to reset password page
     */

    private String getResetPasswordPageUrl() {
        String servletContextUrl = RequestUtils.getWicketServletContextUrl();
        CharSequence path = WicketUtils.mountPathForPage(ResetPasswordPage.class);
        String resetPageUrl = servletContextUrl + "/" + RequestUtils.WEBAPP_URL_PATH_PREFIX + "/" + path;
        return resetPageUrl;
    }

}