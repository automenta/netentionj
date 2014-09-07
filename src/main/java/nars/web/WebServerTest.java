/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.mods.web.StaticFileHandler;


/**
 *
 * @author me
 */
public class WebServerTest {
    
    final MustacheFactory mf = new DefaultMustacheFactory();
    final Vertx vertx;
    private final HttpServer http;
    
    boolean compress = false;
    boolean cache = false;
    
    public static class IndexPage {
        public String title = "Netention";                
        public boolean allowSearchEngineIndexing = false;        
    }
    
    
    
    public WebServerTest() throws Exception {
        super();

//        PlatformManager pm = PlatformLocator.factory.createPlatformManager();
//        
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
                
                ByteBufOutputStream bos = new ByteBufOutputStream(Unpooled.buffer());
                
                
                Writer writer = new OutputStreamWriter(bos);
                Mustache mustache;
                try {
                    mustache = mf.compile(new InputStreamReader(new FileInputStream("client/index.html")),"index.html");
                    mustache.execute(new OutputStreamWriter(bos), new IndexPage());
                    writer.flush();

                } catch (FileNotFoundException ex) {
                    Logger.getLogger(WebServerTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(WebServerTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                e.response().end( new Buffer(bos.buffer()) );
            }
            
        })
                
        .get("/client_configuration.js", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest e) {                
                String x = "{\"autoLoginDefaultProfile\":true,\"initialView\":\"wall\",\"avatarMenuDisplayInitially\":true,\"focusEditDisplayStartup\":true,\"favicon\":null,\"loginLogo\":\"/theme/login-logo.png\",\"defaultAvatarIcon\":\"/theme/default-avatar.jpg\",\"wikiStartPage\":\"Life\",\"showPlanOnSelfPage\":true,\"defaultTheme\":\"_bootswatch.cerulean\",\"maxStartupObjects\":8192,\"defaultMapMode2D\":false,\"mapDefaultLocation\":[40.44,-80.0],\"ontologySearchIntervalMS\":1500,\"viewlockDefault\":false,\"viewUpdateTime\":[[150,50,250],[0,0,100]],\"views\":[\"us\",\"map\",\"browse\",\"wiki\",\"graph\",\"share\",\"forum\",\"main\",\"trends\",\"time\",\"notebook\",\"wall\",\"slides\"],\"newUserProperties\":[\"walletRipple\"],\"shareTags\":['Offer','Sell','Lend','Rent','Swap','GiveAway','Need'],\"shareCategories\":['Food','Service','Volunteer','Shelter','Tools','Health','Transport','Animal'],\"knowTags\":['Learn','Teach','Do'],\"defaultScope\":7    }";                
                e.response().end("configuration =" + x /* to JSON */);
            }
            
        })
                
        .get("/ontology.json", new Handler<HttpServerRequest>() {
            @Override public void handle(HttpServerRequest e) {                
                String x = "{}";                
                e.response().end(x /* to JSON */);
            }
            
        })                
                
        .noMatch(new StaticFileHandler(vertx, "client/", "index.html", compress, cache));
        
        http.requestHandler(r);
        
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
        
        
        http.listen(8080);
        
        System.in.read();
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
        
    }

    
    
    public static void main(String[] args) throws Exception {
        new WebServerTest();
    }
}