package nars.web;

import io.netty.channel.Channel;
import java.util.HashMap;
import java.util.Map;
import nars.core.NAR;
import nars.core.build.DefaultNARBuilder;
import nars.io.Output;
import nars.web.server.PubSubWebSocketServer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author me
 */
public class NARServer extends PubSubWebSocketServer {

    public final Map<Channel, NAR> nars = new HashMap();
    private long minClientNARCyclePeriodMS = 1000;
    
    
    public NARServer() throws Exception {
        super("localhost", 8080);
        
    }

    public NAR newClientNAR() {
        NAR n = new DefaultNARBuilder().build();
        return n;
    }
    
    @Override
    protected void connected(final Channel channel) {
        NAR n = newClientNAR();
        nars.put(channel, n);
        n.addOutput(new Output() {
            @Override public void output(final Class type, final Object o) {                
                send(channel, o.toString());
            }            
        });
        n.addInput("<a --> b>.");
        n.addInput("<b --> c>.");
        n.addInput("<c <-> a>.");
        n.start(minClientNARCyclePeriodMS);
    }

    @Override
    protected void disconnected(final Channel channel) {
        NAR n = nars.get(channel);
        if (n!=null) {
            n.stop();
            nars.remove(channel);
        }
    }
    
    

    @Override
    public void run() {
    
    }
    
    
    
    
    public static void main(String[] args) throws Exception {
        new NARServer();
    }
    
}
