/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.vertx.java.core.json.impl.Json;

/**
 * Webserver configuration options
 */
public class Options {

    /** name of this server, used for <title> */
    public String name;
    
    /** host to bind to, usually localhost */
    public String host;
    
    /** port to bind to, usually port 80 or 8080 for development */
    public int port;
        
    public boolean allowSearchEngines;

    public boolean compressHTTP;
    
    public boolean cacheStaticFiles;
    
    public String connection; //"static" or "websocket"
    
    public int threads;
    
    public String databasePath;
    
    public static Options newDefault() {
        Options s = new Options();
        s.name = "Netention";
        s.host = "localhost";
        s.port = 8080;
        s.connection = "static";
        s.allowSearchEngines = false;
        s.compressHTTP = false;
        s.cacheStaticFiles = false;
        s.threads = 8;
        //s.databasePath = "/tmp/graph";
        return s;
    }

    static Options load(String optionsPath)  {                        
        try {
            return WebServer.jsonLoad(optionsPath, Options.class);
        }
        catch (FileNotFoundException e) {
            System.err.println(optionsPath + " not found, creating default Options at that path");
            Options o = newDefault();            
            String c = Json.encodePrettily(o);
            
            try {
                new PrintStream(new File(optionsPath)).append(c).append('\n');
            } catch (FileNotFoundException ex) {
                System.err.println(optionsPath + " could not be written to");
            }
            
            return o;
        }
    }

         

}
