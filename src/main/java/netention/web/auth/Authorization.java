/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netention.web.auth;

import java.util.HashMap;
import java.util.Map;
import netention.core.Core;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author me
 */
/**
 * Basic Authentication Manager Bus Module<p>
 * Please see the busmods manual for a full description<p>
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Authorization extends BusModBase {

    private Handler<Message<JsonObject>> loginHandler;
    private Handler<Message<JsonObject>> logoutHandler;
    private Handler<Message<JsonObject>> authoriseHandler;

    protected final Map<String, String> sessions = new HashMap<>();
    protected final Map<String, LoginInfo> logins = new HashMap<>();

    private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

    private String address;
    private String userCollection;
    private String persistorAddress;
    private long sessionTimeout;

    private static final class LoginInfo {

        final long timerID;
        final String sessionID;

        private LoginInfo(long timerID, String sessionID) {
            this.timerID = timerID;
            this.sessionID = sessionID;
        }
    }

    /**
     * Start the busmod
     */
    public void start() {
        super.start();

        this.address = getOptionalStringConfig("address", "vertx.basicauthmanager");
        this.userCollection = getOptionalStringConfig("user_collection", "users");
        this.persistorAddress = getOptionalStringConfig("persistor_address", "vertx.mongopersistor");
        Number timeout = config.getNumber("session_timeout");
        if (timeout != null) {
            if (timeout instanceof Long) {
                this.sessionTimeout = (Long) timeout;
            } else if (timeout instanceof Integer) {
                this.sessionTimeout = (Integer) timeout;
            }
        } else {
            this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        }

        loginHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doLogin(message);
            }
        };
        eb.registerHandler(address + ".login", loginHandler);
        logoutHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doLogout(message);
            }
        };
        eb.registerHandler(address + ".logout", logoutHandler);
        authoriseHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doAuthorise(message);
            }
        };
        eb.registerHandler(address + ".authorise", authoriseHandler);
    }

    private void doLogin(final Message<JsonObject> message) {

        final String username = getMandatoryString("username", message);
        if (username == null) {
            return;
        }
        String password = getMandatoryString("password", message);
        if (password == null) {
            return;
        }

        JsonObject findMsg = new JsonObject().putString("action", "findone").putString("collection", userCollection);
        JsonObject matcher = new JsonObject().putString("username", username).putString("password", password);
        findMsg.putObject("matcher", matcher);

        eb.send(persistorAddress, findMsg, new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> reply) {

                if (reply.body().getString("status").equals("ok")) {
                    if (reply.body().getObject("result") != null) {

                        // Check if already logged in, if so logout of the old session
                        LoginInfo info = logins.get(username);
                        if (info != null) {
                            logout(info.sessionID);
                        }

                        // Found
                        final String sessionID = Core.uuid();
                        long timerID = vertx.setTimer(sessionTimeout, new Handler<Long>() {
                            public void handle(Long timerID) {
                                sessions.remove(sessionID);
                                logins.remove(username);
                            }
                        });
                        sessions.put(sessionID, username);
                        logins.put(username, new LoginInfo(timerID, sessionID));
                        JsonObject jsonReply = new JsonObject().putString("sessionID", sessionID);
                        sendOK(message, jsonReply);
                    } else {
                        // Not found
                        sendStatus("denied", message);
                    }
                } else {
                    logger.error("Failed to execute login query: " + reply.body().getString("message"));
                    sendError(message, "Failed to excecute login");
                }
            }
        });
    }

    protected void doLogout(final Message<JsonObject> message) {
        final String sessionID = getMandatoryString("sessionID", message);
        if (sessionID != null) {
            if (logout(sessionID)) {
                sendOK(message);
            } else {
                super.sendError(message, "Not logged in");
            }
        }
    }

    protected boolean logout(String sessionID) {
        String username = sessions.remove(sessionID);
        if (username != null) {
            LoginInfo info = logins.remove(username);
            vertx.cancelTimer(info.timerID);
            return true;
        } else {
            return false;
        }
    }

    protected void doAuthorise(Message<JsonObject> message) {
        String sessionID = getMandatoryString("sessionID", message);
        if (sessionID == null) {
            return;
        }
        String username = sessions.get(sessionID);

    // In this basic auth manager we don't do any resource specific authorisation
        // The user is always authorised if they are logged in
        if (username != null) {
            JsonObject reply = new JsonObject().putString("username", username);
            sendOK(message, reply);
        } else {
            sendStatus("denied", message);
        }
    }

}
