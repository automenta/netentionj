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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.vertx.java.core.Handler;
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

    public Wikipedia(EventBus b, RouteMatcher r) {
        this.bus = b;
        
        
        r.get("/wiki/:id/html", new WikiPage());
        r.get("/wiki/search/:query", new WikiSearch());
        
    }

    public String returnPage(Document doc, HttpServerRequest req) {
        String location = doc.location();
        
        
        //<link rel="canonical" href="http://en.wikipedia.org/wiki/Lysergic_acid_diethylamide" />
        Elements cs = doc.getElementsByTag("link");
        if (cs!=null) {
            for (Element e : cs) {
                if (e.hasAttr("rel") && e.attr("rel").equals("canonical"))
                    location = e.attr("href");
            }
        }
                
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
        
        Map<String,Object> m = new HashMap();
        m.put("url", location);
        
        String metadata = Json.encode(m);
        doc.getElementById("content").prepend("<div id='_meta'>" + metadata + "</div>");
        
        String content = doc.getElementById("content").toString();

        req.response().end(content);        
        
        return location;
    }
    
    public class WikiPage implements Handler<HttpServerRequest> {      
    
        @Override
        public void handle(final HttpServerRequest req) {
            try {
                String wikipage = req.params().get("id");
                String u = "http://en.wikipedia.org/wiki/" + wikipage;

                Document doc = Jsoup.connect(u).get();
                                
                String wikipagae = returnPage(doc, req);
                
                bus.publish("wikipedia", wikipage);
                
            } catch (IOException ex) {
                req.response().end(ex.toString());
            }
          }
    }
    public class WikiSearch implements Handler<HttpServerRequest> {      
    
        @Override
        public void handle(final HttpServerRequest req) {
            try {
                String q = req.params().get("query");                
                String u = "http://en.wikipedia.org/w/index.php?search=" + q;

                Document doc = Jsoup.connect(u).get();    
                returnPage(doc, req);
                
            } catch (IOException ex) {
                req.response().end(ex.toString());
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