/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.tag;

import io.netty.channel.Channel;
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
abstract public class PubSubWebSocketServer  {

    
    
    public final Map<String, AbstractTag> tags = new HashMap();
    
    
    public PubSubWebSocketServer(String host, int port) throws Exception {
        //super(host, port);
        
    }

        
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
    
    private Set<Channel> recipients = new HashSet();
    
    public synchronized int publish(final Object message, String... t) {
        //Topic t = topics.get(topic);        
        recipients.clear();
        for (AbstractTag x : this.tags.values()) {
            if (x.matchesAny(t)) {
                recipients.addAll(x.getSubscribers());
            }        
        }
        if (recipients.size() == 0) return 0;
        
        
        Object messagePacket = Arrays.asList(Arrays.asList(t), message);
        for (final Channel c : recipients) {
            send(c, messagePacket);
        }
        
        return recipients.size();
    }
    
    abstract public void send(Channel c, Object message);
    
    public WildcardTag addTopic(WildcardTag t) {
        tags.putIfAbsent(t.selector, t);
        return t;
    }
    
    protected void subscribe(Channel c, String topic) {
        AbstractTag t = tags.get(topic);
        if (t!=null)
            t.subscribers.add(c);
    }
    protected void unsubscribe(Channel c, String topic) {
        AbstractTag t = tags.get(topic);
        if (t!=null)
            t.removeSubscriber(c);        
    }

//    @Override
//    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        super.disconnect(ctx, promise);
//        Channel c = ctx.channel();
//        for (AbstractTag t : tags.values())
//            t.removeSubscriber(c);            
//    }
    

}
