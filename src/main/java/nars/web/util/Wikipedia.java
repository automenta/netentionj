/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.web.util;

import com.tinkerpop.blueprints.Vertex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import nars.web.core.Core;
import static nars.web.core.Core.u;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.impl.Json;


/**
 *
 * @author me
 */
public class Wikipedia {
    private final EventBus bus;
    private final Core core;

    public Wikipedia(ExecutorService e, Core c, EventBus b, RouteMatcher r) {
        this.bus = b;
        this.core = c;
        
        r.get("/wiki/:id/html", new WikiPage(e));
        r.get("/wiki/search/:query", new WikiSearch(e));
        
    }

    public String returnPage(Document doc, HttpServerRequest req) {
        String location = doc.location();
        if (location.contains("/"))
            location = location.substring(location.lastIndexOf("/")+1, location.length());
        
        String uri = "http://dbpedia.org/resource/" + location;
        
        

        Vertex v = core.vertex( u(uri), true);

        if ( !core.cached(v, "wikipedia") )  {
            core.cache(v, "wikipedia");

            //<link rel="canonical" href="http://en.wikipedia.org/wiki/Lysergic_acid_diethylamide" />
            Elements cs = doc.getElementsByTag("link");
            if (cs!=null) {
                for (Element e : cs) {
                    if (e.hasAttr("rel") && e.attr("rel").equals("canonical"))
                        location = e.attr("href");
                }
            }

            try {
                doc.getElementsByTag("head").remove();
                doc.getElementsByTag("script").remove();
                doc.getElementsByTag("link").remove();
                doc.getElementById("top").remove();
                doc.getElementById("siteSub").remove();
                doc.getElementById("contentSub").remove();
                doc.getElementById("jump-to-nav").remove();
                doc.getElementsByClass("IPA").remove();
                doc.getElementsByClass("search-types").remove();
                doc.getElementsByClass("mw-specialpage-summary").remove();
                doc.getElementsByClass("mw-search-top-table").remove();
            }
            catch (Exception e) {
                System.out.println(e);
            }

            removeComments(doc);
            
            //references and citations consume a lot of space
            Elements refs = doc.getElementsByClass("references");
            if (refs!=null)
                refs.remove();
            

            Map<String,Object> m = new HashMap();
            m.put("url", location);

            String metadata = Json.encode(m);
            doc.getElementById("content").prepend("<div id='_meta'>" + metadata + "</div>");

            String content = doc.getElementById("content").toString();

            Elements catlinks = doc.select(".mw-normal-catlinks li a");
            List<String> categories = new ArrayList();
            for (Element e : catlinks) {
                if (e.tag().getName().equals("a")) {
                    String c = e.attr("href");
                    c = c.substring(c.lastIndexOf('/')+1, c.length());
                    categories.add(c);
                }
            }
            
            v.setProperty("wikipedia_content", content);
            for (String s : categories) {            
                Vertex c = core.vertex("dbpedia.org/resource/" + s, true);
                core.uniqueEdge(v, c, "is");
            }
            core.commit();

            req.response().end(content);        
        }
        else {            
            System.out.println("wikipedia cached " + uri);
            String content = v.getProperty("wikipedia_content");
            core.commit();
            
            if (content != null) {
                req.response().end(content);            
            }
            else {
                req.response().end("Cache fail: " + uri);
            }
        }
        
        return uri;
    }
    
    public class WikiPage extends HandlerThread<HttpServerRequest> {      

        public WikiPage(ExecutorService t) {
            super(t);
        }

        
        @Override
        public void run(HttpServerRequest req) {
            try {
                String wikipage = req.params().get("id");
                String u = "http://en.wikipedia.org/wiki/" + wikipage;

                Document doc = Jsoup.connect(u).get();
                                
                wikipage = returnPage(doc, req);
                
                bus.publish("wikipedia", wikipage);
                
            } catch (IOException ex) {
                req.response().end(ex.toString());
            }
          }
    }
    
    public class WikiSearch extends HandlerThread<HttpServerRequest> {

        public WikiSearch(ExecutorService t) {
            super(t);
        }
            
        @Override
        public void run(HttpServerRequest req) {
            try {
                String q = req.params().get("query");                
                String u = "http://en.wikipedia.org/w/index.php?search=" + q;

                Document doc = Jsoup.connect(u).get();    
                String wikipage = returnPage(doc, req);
                
                bus.publish("wikipedia", wikipage);
                
            } catch (IOException ex) {
                req.response().end(ex.toString());
            }
          }
    }
    

     public static void removeComments(Node node) {
        // as we are removing child nodes while iterating, we cannot use a normal foreach over children,
        // or will get a concurrent list modification error.
        int i = 0;
        while (i < node.childNodes().size()) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }
     
//    public static String getPage(String u) {
//        URL url;
//        HttpURLConnection conn;
//        BufferedReader rd;
//        String line;
//        String result = "";
//        try {
//           url = new URL(u);
//           conn = (HttpURLConnection) url.openConnection();
//           conn.setRequestMethod("GET");
//           rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//           while ((line = rd.readLine()) != null) {
//              result += line;
//           }
//           rd.close();
//        } catch (IOException e) {
//           e.printStackTrace();
//        } catch (Exception e) {
//           e.printStackTrace();
//        }
//        return result;
//    }
    
}
/*
            function returnWikiPage(url, rres, redirector) {
                    http.get(url, function(res) {

                            if (res.statusCode > 300 && res.statusCode < 400 && res.headers.location) {
                                    // The location for some (most) redirects will only contain the path,  not the hostname;
                                    // detect this and add the host to the path.
                                    var u = res.headers.location;
                                    var pu = u.indexOf('/wiki/');
                                    if (pu != -1) {
                                            redirector = u.substring(pu + 6);
                                            returnWikiPage(u, rres, redirector);
                                            return;
                                    }
                            }
                            rres.writeHead(200, {'Content-Type': 'text/html'});

                            var page = '';
                            res.on("data", function(chunk) {
                                    page += chunk;
                            });
                            res.on('end', function() {
                                    var cheerio = require('cheerio');
                                    var $ = cheerio.load(page);
                                    $('script').remove();

                                    {
                                            //get the actual wikipage from the page-_____ class added to <body>
                                            var bodyclasses = $('body').attr('class').split(' ');
                                            for (var i = 0; i < bodyclasses.length; i++) {
                                                    var bc = bodyclasses[i];
                                                    if (bc.indexOf('page-') === 0) {
                                                            redirector = bc.substring(5);
                                                    }
                                            }
                                    }

                                    if (redirector)
                                            $('#content').append('<div style="display:none" class="WIKIPAGEREDIRECTOR">' + redirector + '</div>');
                                    rres.write($('#content').html() || $.html());
                                    rres.end();
                            });
                    })
            }

            express.get('/wiki/search/:query', compression, function(req, rres) {
                    var q = req.params.query;
                    returnWikiPage('http://en.wikipedia.org/w/index.php?search=' + q, rres);
            });

            express.get('/wiki/:tag/html', compression, function(req, rres) {
                    var t = req.params.tag;
                    returnWikiPage("http://en.wikipedia.org/wiki/" + t, rres);
            });
            express.get('/wiki/:tag1/:tag2/html', compression, function(req, rres) {
                    var t = req.params.tag1 + '/' + req.params.tag2;
                    returnWikiPage("http://en.wikipedia.org/wiki/" + t, rres);
            });

*/