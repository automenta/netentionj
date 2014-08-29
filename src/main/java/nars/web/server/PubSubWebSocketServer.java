/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author me
 */
public class PubSubWebSocketServer extends WebSocketServer {

    public static class Topic {
        public final String id;
        public final Set<Channel> subscribers = new HashSet();

        //ArrayDeque<Object> history;
        public Topic(String id) {
            this.id = id;
        }
                
    }
    
    public final Map<String, Topic> topics = new HashMap();
    
    
    public PubSubWebSocketServer(String host, int port) throws Exception {
        super(host, port);
        
    }

    
    @Override
    public void receive(Channel channel, Object message) {
        if (message instanceof List) {
            List lvm = (List)message;
            if (lvm.size() >= 2) {
                Object o = lvm.get(0);
                if (o instanceof String) {
                    switch ((String)o) {
                        case "sub":
                            subscribe(channel, lvm.get(1).toString());
                            break;
                        case "unsub":
                            unsubscribe(channel, lvm.get(1).toString());
                            break;                    
                    }
                }
            }
        }
    }
    
    public void publish(final String topic, final Object message) {
        Topic t = topics.get(topic);        
        
        if (t!=null) {
            for (final Channel c : t.subscribers)
                publish(c, Arrays.asList(topic, message));
        }
    }
    
    public Topic addTopic(Topic t) {
        topics.putIfAbsent(t.id, t);
        return t;
    }
    
    protected void subscribe(Channel c, String topic) {
        Topic t = topics.get(topic);
        if (t!=null)
            t.subscribers.add(c);
    }
    protected void unsubscribe(Channel c, String topic) {
        Topic t = topics.get(topic);
        if (t!=null)
            t.subscribers.remove(c);        
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.disconnect(ctx, promise);
        Channel c = ctx.channel();
        for (Topic t : topics.values())
            t.subscribers.remove(c);            
    }
    
    
    
    
}
