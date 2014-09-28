package nars.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import nars.web.core.Core;
import nars.web.core.NObject;
import nars.web.core.Publish;
import nars.web.possibility.Activity;
import nars.web.util.DBPedia;
import nars.web.util.MozillaPersonaHandler;
import nars.web.util.NOntology;
import nars.web.util.Wikipedia;
import org.boon.json.JsonParserFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.mods.web.StaticFileHandler;


/**
 *
 * @author me
 */
public class WebServer  {
    
    final MustacheFactory mf = new DefaultMustacheFactory();
    final Vertx vertx;
    private final HttpServer http;        
    private final Options options;
    private final Core core;
    
    //TODO use LRU cache on lastActivity
    private final Map<String,Session> sessions = new HashMap();
    

            
    ExecutorService executor;
    
    abstract public class SessionHandler implements Handler<HttpServerRequest> {

        @Override public void handle(final HttpServerRequest req) {
            inSession(req, new Handler<Session>() {
                @Override public void handle(Session session) {
                    SessionHandler.this.handle(session, req);
                }
            });
        }        
        abstract public void handle(Session session, HttpServerRequest e);
    }
    
    public WebServer(Core c, Options o) throws Exception {
        super();

        
        this.core = c;
        this.options = o;

        this.executor = Executors.newFixedThreadPool(o.threads);
        
        
        vertx = VertxFactory.newVertx();        
        http = vertx.createHttpServer();        
    
        
        
        /*http.websocketHandler(new Handler<ServerWebSocket>() {

            @Override
            public void handle(ServerWebSocket e) {
            }
        });*/
        RouteMatcher r = new RouteMatcher()
        
        .get("/wiki", new SessionHandler() {            
            @Override public void handle(Session session, HttpServerRequest e) {
                //template(e, "client/debug.eventbus.html", getIndexPage());
                template(e, "client/wiki.html", getIndexPage(session));
            }            
        })
        .get("/", new SessionHandler() {
            @Override public void handle(Session session, HttpServerRequest e) {
                template(e, "client/netention.html", getIndexPage(session));
            }
        })
                
        .get("/auth", new SessionHandler() {            
            @Override public void handle(Session session, HttpServerRequest e) {
                template(e, "client/auth.html", session);
            }
        })
                
        .get("/client_configuration.js", new Handler<HttpServerRequest>() {                        
            @Override public void handle(HttpServerRequest e) { 
                Map<String, Object> icons = getClientIcons();
                Map<String, Object> themes = getClientThemes();
                Map<String, Object> clientOptions = getClientOptions();
                
                e.response().end(
                    "var configuration = " + Json.encode(clientOptions) + ";\n" +
                    "var themes = " + Json.encode(themes) + ";\n" +
                    "var defaultIcons = " + Json.encode(icons) + ";\n"                        
                );
            }
            
        })
                
        .get("/ontology.json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest e) {                
                e.response().end(core.getOntologyJSON());
        }})
        .get("/object/tag/:tag/json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest req) {
                String tag = req.params().get("tag");                
                req.response().end( 
                    Json.encode(core.objectStreamByTag(tag).map(v -> core.getObject(v)).collect(toList())
                ));
                core.commit();
        }})
        .get("/object/author/:author/json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest req) {
                String author = req.params().get("author");
                req.response().end( 
                    Json.encode(core.objectStreamByAuthor(author).map(v -> core.getObject(v)).collect(toList())
                ));
                core.commit();
        }})
        .get("/object/latest/:num/json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest req) {
                //TODO
                int num = Integer.valueOf(req.params().get("num"));
                req.response().end( 
                    Json.encode( new Object[0] )
                );
                core.commit();
        }})
        .get("/object/:id/json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest req) {
                String id = req.params().get("id");
                
                req.response().end( 
                        Json.encode(
                                core.getObject(id)
                        ) 
                );
        }})
                
        //TODO Realm parameter: https://github.com/mozilla/persona/pull/3854
        .post("/login/persona", new MozillaPersonaHandler(options.host, options.port, this))
        .post("/logout", new Handler<HttpServerRequest>() {

            @Override public void handle(HttpServerRequest e) {
                //TODO erase session on server, and the session field in client's cookie
                e.response().end("logged out.");
            }
        })
        
        .noMatch(new StaticFileHandler(vertx, "client/", "index.html", options.compressHTTP, options.cacheStaticFiles));
        

        new DBPedia(executor, core, vertx.eventBus());
        new Wikipedia(executor, core, vertx.eventBus(), r);
        new Activity(c, vertx.eventBus());
        new Publish(c, vertx.eventBus());
        //new IRC(vertx.eventBus());
        
        

        
        http.requestHandler(r);        

        
        initWebSockets();
        
        
        http.listen(options.port);
        
        System.out.println("listening on port " + options.port);
        
//                
//        vertx.setPeriodic(5000, new Handler<Long>() {
//
//            @Override
//            public void handle(Long e) {
//                
//            }
//        });
        
    }

    
    
    public static void main(String[] args) throws Exception {
        String optionsPath = args.length > 0 ? args[0] : "options.json";        
        Options options = Options.load(optionsPath);

        TransactionalGraph g;
        String dbPath = options.databasePath;
        if ((dbPath!=null) && (dbPath.startsWith("orientdb:"))) {
            String orientPath = dbPath.substring("orientdb:".length());
            //"plocal:" + options.databasePath
            g = new OrientGraph(orientPath);
        }
        else {
            g = new OrientGraph("orientdb:memory:test");
            System.err.println("Using OrientDB in-memory database.  Changes will not be saved");
        }
                
        Core core = new Core((TransactionalGraph)g);
        new NOntology(core);
        //new SchemaOrg(core);
        new WebServer(core, options);
        
        
        //keep everything running in non-daemon
        new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                    }
                }
            }            
        }).start();
    }
    
    public static class IndexPage {
        public final String title;
        public final boolean allowSearchEngineIndexing;
        public final String auth;
        public final String selves;

        public IndexPage(Session session, String title, boolean allowSearchEngineIndexing) {
            this.auth = session.auth;
            this.selves = Json.encode(session.selves);
            this.title = title;
            this.allowSearchEngineIndexing = allowSearchEngineIndexing;
        }
        
    }
    
    public IndexPage getIndexPage(Session session) {
        //TODO cache the instance
        return new IndexPage(session, options.name, options.allowSearchEngines);
    }
    
    public Map<String,Object> getClientOptions() {
        try {        
            Map<String, Object> co = new HashMap(WebServer.jsonLoad("data/client.json"));
            co.put("connection", options.connection);
            return co;
        } catch (FileNotFoundException ex) {
            return new HashMap();
        }
    }
    
    Map<String, Object> getClientIcons() {
        try {        
            return WebServer.jsonLoad("data/icons.json");
        } catch (FileNotFoundException ex) {
            return new HashMap();
        }                
    }
    
    Map<String, Object> getClientThemes() {
        try {        
            return WebServer.jsonLoad("data/themes.json");
        } catch (FileNotFoundException ex) {
            return new HashMap();
        }                
    }

    

    static {
         //temporar yworkaround for boon
         System.setProperty ( "java.version", "1.8" );
         
//         JsonSerializerFactory jsonSerializerFactory = new JsonSerializerFactory()
//                .useFieldsFirst()//.useFieldsOnly().usePropertiesFirst().usePropertyOnly() //one of these
//                //.addPropertySerializer(  )  customize property output
//                //.addTypeSerializer(  )      customize type output
//                .useJsonFormatForDates() //use json dates
//                //.addFilter(  )   add a property filter to exclude properties
//                .includeEmpty().includeNulls().includeDefaultValues() //override defaults
//                //.handleComplexBackReference() //uses identity map to track complex back reference and avoid them
//                //.setHandleSimpleBackReference( true ) //looks for simple back reference for parent
//                .setCacheInstances( true ) //turns on caching for immutable objects
//                ;
        //jsonSerializer = jsonSerializerFactory.create();        
        /*
                .useFieldsFirst()//useFieldsOnly().usePropertiesFirst().usePropertyOnly() //one of these
                //.plistStyle() //allow parsing of ASCII PList style files
                .lax() //allow loose parsing of JSON like JSON Smart
                //.strict() //opposite of lax
                .setCharset( StandardCharsets.UTF_8 ) //Set the standard charset, defaults to UTF_8
                //.setChop( true ) //chops up buffer overlay buffer (more discussion of this later)
                //.setLazyChop( true ) //similar to chop but only does it after map.get               
                ;
        */
    }
    
    public static <O> O jsonLoad(String filePath, Class<? extends O> c) throws FileNotFoundException {
        return new JsonParserFactory().create().parse(c, new FileInputStream(filePath));
    }
    
    public static Map<String,Object> jsonLoad(String filePath) throws FileNotFoundException {
        return new JsonParserFactory().create().parseMap(new FileInputStream(filePath));
    }

//    protected void template(final HttpServerRequest e, final String input, final Object param) {
//        ByteBufOutputStream bos = new ByteBufOutputStream(Unpooled.buffer());
//
//        Writer writer = new OutputStreamWriter(bos);        
//        try {
//            //TODO cache compiled mustache, if not already cached by Mustache itself
//            Mustache mustache = mf.compile(new InputStreamReader(new FileInputStream(input)),"index.html");
//            mustache.execute(new OutputStreamWriter(bos), param);
//            writer.flush();
//        } catch (Exception ex) {
//            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        e.response().end( new Buffer(bos.buffer()) );
//    }
    protected void template(final HttpServerRequest e, final String input, final Object param) {
        try {
            //TODO cache compiled mustache, if not already cached by Mustache itself
            Mustache mustache = mf.compile(new InputStreamReader(new FileInputStream(input)),"index.html");
            
            StringWriter sw = new StringWriter(8192);            
            mustache.execute(sw, param);
            e.response().end( sw.toString() );
        } catch (Exception ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    
    /**
     * https://gist.github.com/Narigo/3386443
     */
    public void inSession(final HttpServerRequest req, final Handler<Session> handler) {
        String value = req.headers().get("Cookie");
        if (value != null) {
            Set<Cookie> cookies = CookieDecoder.decode(value);
            for (final Cookie cookie : cookies) {
                if ("sessionId".equals(cookie.getName())) {
                    Session session = sessions.get(cookie.getValue());                    
                    if (session!=null) {
                        session.active(req);
                        handler.handle(session);
                        return;
                    }
                }
            }
        }
        
        startSession(req, new Session(req), handler);
    }

    public void startSession(final HttpServerRequest req, String auth, final Handler<Session> handler) {
        String userURI = "user_" + auth;
        Vertex v = core.vertex(userURI, true);
        Map<String, Object> user = core.getObject(userURI);
        Session s;
        if (user.get("selves") == null) {
            s = new Session(req, auth);                        
            v.setProperty("createdAt", System.currentTimeMillis());
            v.setProperty("lastLoginAt", System.currentTimeMillis());            
            v.setProperty("selves", s.selves);
            
            for (final String self : s.selves) {
                NObject u = core.newUser(self);
                //s.addSelfObject(u);
            }
            
        }
        else {
            s = new Session(req, auth, (List<String>)user.get("selves"));            
            v.setProperty("lastLoginAt", System.currentTimeMillis());
//            for (String self : s.selves) {
//                s.addSelfObject( core.getObject(self) );
//            }
            handler.handle(s);
        }
        
        core.commit();
        handler.handle(s);        
    }
    
    public void startSession(final HttpServerRequest req, Session session, final Handler<Session> handler) {
        // Create a new session and use that
//        vertx.eventBus().send("session", new JsonObject().putString("action", "start"),
//                new Handler<Message<JsonObject>>() {
//                    @Override
//                    public void handle(Message<JsonObject> event) {
//                        String sessionId = event.body().getString("sessionId");

        String sessionId = UUID.randomUUID().toString();        
        DefaultCookie dc = new DefaultCookie("sessionId", sessionId);
        req.response().putHeader("Set-Cookie", dc.toString());
        sessions.put(sessionId, session);
        
        if (handler!=null)
            handler.handle(session);
        
//                    }
//                });
    }
    
    public void initWebSockets() {
        JsonObject config = new JsonObject().putString("prefix", "/eventbus");

        JsonArray noPermitted = new JsonArray();
        noPermitted.add(new JsonObject());

        SockJSServer sockets = vertx.createSockJSServer(http);
        sockets.bridge(config, noPermitted, noPermitted);        

        
//        JsonObject config2 = new JsonObject().putString("prefix", "/echo");
//        socket.installApp(config2, new Handler<SockJSSocket>() {
//            public void handle(SockJSSocket sock) {
//                
//                Pump.createPump(sock, sock).start();
//            }
//        });
        /*sockets.installApp(new JsonObject().putString("prefix", "/test"), new Default
        });*/

//        http.requestHandler(new Handler<HttpServerRequest>() {
//          public void handle(HttpServerRequest req) {
//            if (req.path().equals("/")) req.response().sendFile("sockjs/index.html"); // Serve the html
//          }
//        });


//        sockJSServer.installApp(new JsonObject().putString("prefix", "/testapp"), new Handler<SockJSSocket>() {
//          public void handle(final SockJSSocket sock) {
//            sock.dataHandler(new Handler<Buffer>() {
//              public void handle(Buffer data) {
//                sock.write(data); // Echo it back
//              }
//            });
//          }
//        });        
        
    }

}


        
//    rm.get("/details/:user/:id", new Handler<HttpServerRequest>() {
//      public void handle(HttpServerRequest req) {
//        req.response().end("User: " + req.params().get("user") + " ID: " + req.params().get("id"));
//      }
//    });
//
//    // Catch all - serve the index page
//    rm.getWithRegEx(".*", new Handler<HttpServerRequest>() {
//      public void handle(HttpServerRequest req) {
//        req.response().sendFile("route_match/index.html");
//      }
//    });        
    

//      public void handle(final HttpServerRequest req) {
//        if (req.uri().equals("/")) {
//          // Serve the index page
//          req.response().sendFile("simpleform/index.html");
//        } else if (req.uri().startsWith("/form")) {
//          req.response().setChunked(true);
//          req.expectMultiPart(true);
//          req.endHandler(new VoidHandler() {
//            protected void handle() {
//              for (Map.Entry<String, String> entry : req.formAttributes()) {
//                req.response().write("Got attr " + entry.getKey() + " : " + entry.getValue() + "\n");
//              }
//              req.response().end();
//            }
//          });
//        } else {
//          req.response().setStatusCode(404);
//          req.response().end();
//        }
//      }
        
        
        
//        JsonObject config = new JsonObject().putString("prefix", "/eventbus");
//
//        JsonArray noPermitted = new JsonArray();
//        noPermitted.add(new JsonObject());
        
        /*
        // Let through any messages sent to 'demo.orderMgr'
JsonObject inboundPermitted1 = new JsonObject().putString("address", "demo.orderMgr");
inboundPermitted.add(inboundPermitted1);

// Allow calls to the address 'demo.persistor' as long as the messages
// have an action field with value 'find' and a collection field with value
// 'albums'
JsonObject inboundPermitted2 = new JsonObject().putString("address", "demo.persistor")
    .putObject("match", new JsonObject().putString("action", "find")
                                        .putString("collection", "albums"));
inboundPermitted.add(inboundPermitted2);

// Allow through any message with a field `wibble` with value `foo`.
JsonObject inboundPermitted3 = new JsonObject().putObject("match", new JsonObject().putString("wibble", "foo"));
inboundPermitted.add(inboundPermitted3);

JsonArray outboundPermitted = new JsonArray();

// Let through any messages coming from address 'ticker.mystock'
JsonObject outboundPermitted1 = new JsonObject().putString("address", "ticker.mystock");
outboundPermitted.add(outboundPermitted1);

// Let through any messages from addresses starting with "news." (e.g. news.europe, news.usa, etc)
JsonObject outboundPermitted2 = new JsonObject().putString("address_re", "news\\..+");
outboundPermitted.add(outboundPermitted2);
        */

//        vertx.createSockJSServer(http).bridge(config, noPermitted, noPermitted);        

        
        
//        SockJSServer sockJSServer = vertx.createSockJSServer(http);
//        JsonObject config2 = new JsonObject().putString("prefix", "/echo");
//        sockJSServer.installApp(config2, new Handler<SockJSSocket>() {
//            public void handle(SockJSSocket sock) {
//                Pump.createPump(sock, sock).start();
//            }
//        });

//        http.requestHandler(new Handler<HttpServerRequest>() {
//          public void handle(HttpServerRequest req) {
//            if (req.path().equals("/")) req.response().sendFile("sockjs/index.html"); // Serve the html
//          }
//        });


//        sockJSServer.installApp(new JsonObject().putString("prefix", "/testapp"), new Handler<SockJSSocket>() {
//          public void handle(final SockJSSocket sock) {
//            sock.dataHandler(new Handler<Buffer>() {
//              public void handle(Buffer data) {
//                sock.write(data); // Echo it back
//              }
//            });
//          }
//        });        
        
//
//        // Create an echo server
//        vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
//          public void handle(final NetSocket socket) {
//            Pump.createPump(socket, socket).start();
//          }
//        }).listen(1234);
//
//        // Prevent the JVM from exiting
//        System.in.read();
