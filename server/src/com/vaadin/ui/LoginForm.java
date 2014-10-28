/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.loginform.LoginFormConstants;
import com.vaadin.shared.ui.loginform.LoginFormRpc;
import com.vaadin.shared.ui.loginform.LoginFormState;

/**
 * Login form with auto-completion and auto-fill for all major browsers. You can
 * derive from this class and implement the
 * {@link #createContent(com.vaadin.ui.TextField, com.vaadin.ui.PasswordField, com.vaadin.ui.Button)}
 * method to build the layout using the text fields and login button that are
 * passed to that method. The supplied components are specially treated so that
 * they work with password managers.
 * <p>
 * If you need to change the URL as part of the login procedure, call
 * {@link #setLoginMode(LoginMode)} with the argument {@link LoginMode#DEFERRED}
 * in your implementation of
 * {@link #createContent(com.vaadin.ui.TextField, com.vaadin.ui.PasswordField, com.vaadin.ui.Button)
 * createContent}.
 * <p>
 * To customize the fields or to replace them with your own implementations, you
 * can override {@link #createUserNameField()}, {@link #createPasswordField()}
 * and {@link #createLoginButton()}. These methods are called automatically and
 * cannot be called by your code. Captions can be reset by overriding
 * {@link #getUserNameFieldCaption()}, {@link #getPasswordFieldCaption()} and
 * {@link #getLoginButtonCaption()}.
 */
public class LoginForm extends AbstractSingleComponentContainer {

    /**
     * Determines the way in which the login form reacts to a login on the
     * server side. The login mode is set by calling
     * {@link LoginForm#setLoginMode(LoginMode)}.
     */
    public static enum LoginMode {
        /**
         * Direct mode means that {@link LoginForm#login(String, String)} will
         * be called as soon as the user clicks on the login button or presses
         * the enter key in the user name or password text fields. In direct
         * mode, you cannot change the URL in the
         * {@link LoginForm#login(String, String)} method, otherwise the
         * password manager will not be triggered.
         * <p/>
         * This is the default mode for a new login form instance.
         */
        DIRECT,

        /**
         * Deferred mode means that {@link LoginForm#login(String, String)} will
         * be called after the dummy form submission that triggers the password
         * manager has completed. In deferred mode, it is possible to change the
         * URL in the {@link LoginForm#login(String, String)} method. The
         * drawbacks with resepect to deferred mode are the following:
         * <ul>
         * <li>There will be a slight UI lag between the user action and the UI
         * change</li>
         * <li>Any UI change resulting from the login is not a direct
         * consequence of the user input. If you use Vaadin TestBench, you have
         * to add your own code to wait for any UI changes.</li>
         * </ul>
         */
        DEFERRED;
    }

    /**
     * This event is sent when login form is submitted.
     */
    public static class LoginEvent extends Event {

        private Map<String, String> params;

        private LoginEvent(Component source, Map<String, String> params) {
            super(source);
            this.params = params;
        }

        /**
         * Access method to form values by field names.
         * 
         * @param name
         * @return value in given field
         */
        public String getLoginParameter(String name) {
            if (params.containsKey(name)) {
                return params.get(name);
            } else {
                return null;
            }
        }
    }

    /**
     * Login listener is a class capable to listen LoginEvents sent from
     * LoginBox
     */
    public interface LoginListener extends Serializable {
        /**
         * This method is fired on each login form post.
         * 
         * @param event
         *            Login event
         */
        public void onLogin(LoginEvent event);
    }

    static {
        try {
            ON_LOGIN_METHOD = LoginListener.class.getDeclaredMethod("onLogin",
                    new Class[] { LoginEvent.class });
        } catch (final java.lang.NoSuchMethodException e) {
            // This should never happen
            throw new java.lang.RuntimeException(
                    "Internal error finding methods in LoginForm");
        }
    }

    private static final Method ON_LOGIN_METHOD;

    private boolean initialized;
    private LoginMode loginMode = LoginMode.DIRECT;

    private String usernameCaption = "Username";
    private String passwordCaption = "Password";
    private String loginButtonCaption = "Login";

    /**
     * Returns the {@link LoginMode} for this login form. The default is
     * {@link LoginMode#DIRECT}.
     * 
     * @return the login mode
     */
    public LoginMode getLoginMode() {
        return loginMode;
    }

    /**
     * Set the {@link LoginMode} for this login form. The default is
     * {@link LoginMode#DIRECT}
     * 
     * @param loginMode
     *            the login mode
     */
    public void setLoginMode(LoginMode loginMode) {
        this.loginMode = loginMode;
    }

    /**
     * Customize the user name field. Only for overriding, do not call.
     * 
     * @return the user name field
     */
    protected TextField createUserNameField() {
        checkInitialized();
        TextField field = new TextField(usernameCaption);
        field.focus();
        return field;
    }

    public String getUsernameCaption() {
        return usernameCaption;
    }

    public void setUsernameCaption(String cap) {
        usernameCaption = cap;
    }

    /**
     * Customize the password field. Only for overriding, do not call.
     * 
     * @return the password field
     */
    protected PasswordField createPasswordField() {
        checkInitialized();
        return new PasswordField(passwordCaption);
    }

    public String getPasswordCaption() {
        return passwordCaption;
    }

    public void setPasswordCaption(String cap) {
        passwordCaption = cap;
        ;
    }

    /**
     * Customize the login button. Only for overriding, do not call.
     * 
     * @return the login button
     */
    protected Button createLoginButton() {
        checkInitialized();
        return new Button(loginButtonCaption);
    }

    public String getLoginButtonCaption() {
        return loginButtonCaption;
    }

    public void setLoginButtonCaption(String cap) {
        loginButtonCaption = cap;
    }

    @Override
    protected LoginFormState getState() {
        return (LoginFormState) super.getState();
    }

    @Override
    public void attach() {
        super.attach();
        init();
    }

    private void checkInitialized() {
        if (initialized) {
            throw new IllegalStateException(
                    "Already initialized. The create methods may not be called explicitly.");
        }
    }

    /**
     * Create the content for the login form with the supplied user name field,
     * password field and the login button. You cannot use any other text fields
     * or buttons for this purpose. To replace these components with your own
     * implementations, override {@link #createUserNameField()},
     * {@link #createPasswordField()} and {@link #createLoginButton()}. If you
     * only want to change the default captions, override
     * {@link #getUserNameFieldCaption()}, {@link #getPasswordFieldCaption()}
     * and {@link #getLoginButtonCaption()}. You do not have to use the login
     * button in your layout.
     * 
     * @param userNameField
     *            the user name text field
     * @param passwordField
     *            the password field
     * @param loginButton
     *            the login button
     * @return
     */
    protected Component createContent(TextField userNameField,
            PasswordField passwordField, Button loginButton) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.addComponent(userNameField);
        layout.addComponent(passwordField);
        layout.addComponent(loginButton);
        return layout;
    }

    private void init() {
        if (initialized) {
            return;
        }

        LoginFormState state = getState();
        state.userNameFieldConnector = createUserNameField();
        state.passwordFieldConnector = createPasswordField();
        state.loginButtonConnector = createLoginButton();

        String contextPath = VaadinService.getCurrentRequest().getContextPath();
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        state.contextPath = contextPath;

        VaadinSession.getCurrent().addRequestHandler(new RequestHandler() {
            @Override
            public boolean handleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response)
                    throws IOException {
                if (LoginFormConstants.LOGIN_URL.equals(request.getPathInfo())) {
                    response.setContentType("text/html; charset=utf-8");
                    response.setCacheTime(-1);
                    PrintWriter writer = response.getWriter();
                    writer.append("<html>Success</html>");
                    return true;
                } else {
                    return false;
                }
            }
        });

        registerRpc(new LoginFormRpc() {
            @Override
            public void submitted() {
                if (loginMode == LoginMode.DIRECT) {
                    login();
                }
            }

            @Override
            public void submitCompleted() {
                if (loginMode == LoginMode.DEFERRED) {
                    login();
                }
            }
        });

        initialized = true;

        setContent(createContent(getUserNameField(), getPasswordField(),
                getLoginButton()));
    }

    private TextField getUserNameField() {
        return (TextField) getState().userNameFieldConnector;
    }

    private PasswordField getPasswordField() {
        return (PasswordField) getState().passwordFieldConnector;
    }

    private Button getLoginButton() {
        return (Button) getState().loginButtonConnector;
    }

    /*
     * (non-Javadoc)
     * 
     * Handle the login. In deferred mode, this method is called after the dummy
     * POST request that triggers the password manager has been completed. In
     * direct mode (the default setting), it is called directly when the user
     * hits the enter key or clicks on the login button. In the latter case, you
     * cannot change the URL in the method or the password manager will not be
     * triggered.
     */
    private void login() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", getUserNameField().getValue());
        params.put("password", getPasswordField().getValue());
        LoginEvent event = new LoginEvent(LoginForm.this, params);
        fireEvent(event);
    }

    /**
     * Adds LoginListener to handle login logic
     * 
     * @param listener
     */
    public void addLoginListener(LoginListener listener) {
        addListener(LoginEvent.class, listener, ON_LOGIN_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #addLoginListener(LoginListener)}
     **/
    @Deprecated
    public void addListener(LoginListener listener) {
        addLoginListener(listener);
    }

    /**
     * Removes LoginListener
     * 
     * @param listener
     */
    public void removeLoginListener(LoginListener listener) {
        removeListener(LoginEvent.class, listener, ON_LOGIN_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeLoginListener(LoginListener)}
     **/
    @Deprecated
    public void removeListener(LoginListener listener) {
        removeLoginListener(listener);
    }

}
