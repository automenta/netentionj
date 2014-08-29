/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web;

import io.netty.channel.Channel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nars.web.server.PubSubWebSocketServer;


/**
 *
 * @author me
 */
public class Main implements Runnable {
    private final PubSubWebSocketServer server;

    public Main() throws Exception {
        
        new Thread(this).start();
        
        server = new PubSubWebSocketServer("localhost", 8080) {
            
            @Override
            public void receive(Channel channel, Object message) {
                super.receive(channel, message);
                System.out.println("recv: " +  channel.id() + " " +  message + " " + message.getClass());
            }
          
        };
        server.start();
    }

    @Override
    public void run() {
        
        
        while (true) {
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (server == null) continue;
            
            if (!server.topics.containsKey("a"))
                server.addTopic(new PubSubWebSocketServer.Topic("a"));
            
            server.publish("a", "abc");
            
        }
    }
    
    
 
    
    public static void main(String[] args) throws Exception {
        Logger.getLogger("io.netty").setLevel(Level.SEVERE);
        
        new Main();
        
        
    }
}
