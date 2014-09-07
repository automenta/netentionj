package nars.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nars.web.core.Core;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.impl.Json;
import org.vertx.mods.web.StaticFileHandler;


/**
 *
 * @author me
 */
public class WebServer {
    
    final MustacheFactory mf = new DefaultMustacheFactory();
    final Vertx vertx;
    private final HttpServer http;        
    public static final JsonSerializer jsonSerializer;    
    private final Options options;
    private final Core core;
    
    
    public WebServer(Core c, Options o) throws Exception {
        super();

        this.core = c;
        this.options = o;

        vertx = VertxFactory.newVertx();        
        http = vertx.createHttpServer();
        
        /*http.websocketHandler(new Handler<ServerWebSocket>() {

            @Override
            public void handle(ServerWebSocket e) {
            }
        });*/
                
        RouteMatcher r = new RouteMatcher()
        
        .get("/", new Handler<HttpServerRequest>() {            
            @Override public void handle(HttpServerRequest e) {                
                template(e, "client/index.html", options.getIndexPage());
            }            
        })
                
        .get("/client_configuration.js", new Handler<HttpServerRequest>() {                        
            @Override public void handle(HttpServerRequest e) { 
                Map<String, Object> icons = options.getClientIcons();
                Map<String, Object> themes = options.getClientThemes();
                Map<String, Object> clientOptions = options.getClientOptions();
                
                e.response().end(
                    "var configuration = " + Json.encode(clientOptions) + ";\n" +
                    "var themes = " + Json.encode(themes) + ";\n" +
                    "var icons = " + Json.encode(icons) + ";\n"
                );
            }
            
        })
                
        .get("/ontology.json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest e) {                
                e.response().end(core.getOntologyJSON());
        }})
        
                
        .noMatch(new StaticFileHandler(vertx, "client/", "index.html", options.compressHTTP, options.cacheStaticFiles));
        
        http.requestHandler(r);        
        http.listen(8080);
        
        System.in.read();
        
    }

    
    
    public static void main(String[] args) throws Exception {
        String optionsPath = args.length > 0 ? args[0] : "options.json";        
        new WebServer(new Core(), Options.load(optionsPath));
    }

    static {
         //temporar yworkaround for boon
         System.setProperty ( "java.version", "1.8" );
         
         JsonSerializerFactory jsonSerializerFactory = new JsonSerializerFactory()
                .useFieldsFirst()//.useFieldsOnly().usePropertiesFirst().usePropertyOnly() //one of these
                //.addPropertySerializer(  )  customize property output
                //.addTypeSerializer(  )      customize type output
                .useJsonFormatForDates() //use json dates
                //.addFilter(  )   add a property filter to exclude properties
                .includeEmpty().includeNulls().includeDefaultValues() //override defaults
                //.handleComplexBackReference() //uses identity map to track complex back reference and avoid them
                //.setHandleSimpleBackReference( true ) //looks for simple back reference for parent
                .setCacheInstances( true ) //turns on caching for immutable objects
                ;
        jsonSerializer = jsonSerializerFactory.create();        
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

    protected void template(final HttpServerRequest e, final String input, final Object param) {
        ByteBufOutputStream bos = new ByteBufOutputStream(Unpooled.buffer());

        Writer writer = new OutputStreamWriter(bos);        
        try {
            //TODO cache compiled mustache, if not already cached by Mustache itself
            Mustache mustache = mf.compile(new InputStreamReader(new FileInputStream(input)),"index.html");
            mustache.execute(new OutputStreamWriter(bos), param);
            writer.flush();
        } catch (Exception ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        e.response().end( new Buffer(bos.buffer()) );
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
