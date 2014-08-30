/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web.example;

import io.netty.channel.Channel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nars.web.server.PubSubWebSocketServer;


/**
 *
 * @author me
 */
public class SimpleServer extends PubSubWebSocketServer {

    public SimpleServer() throws Exception {
        super("localhost", 8080);
    }
    
    @Override
    public void receive(Channel channel, Object message) {
        super.receive(channel, message);
        //System.out.println("recv: " +  channel.id() + " " +  message + " " + message.getClass());
    }

    @Override
    public void run() {
        
        addTopic(new PubSubWebSocketServer.Topic("a"));
        
        while (true) {
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            publish("abc", "a");
        }
    }
    
    
 
    
    public static void main(String[] args) throws Exception {
        //Logger.getLogger("io.netty").setLevel(Level.SEVERE);
        
        new SimpleServer().start();
        
        
    }
}
