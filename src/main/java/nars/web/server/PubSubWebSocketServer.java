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
import java.util.regex.Pattern;

/**
 *
 * @author me
 */
abstract public class PubSubWebSocketServer extends WebSocketServer {

    public static class Topic {
        public final String selector;
        public final Set<Channel> subscribers = new HashSet();
        private final Pattern p;

        //ArrayDeque<Object> history;
        public Topic(final String selector) {
            this.selector = selector;
            p = new WildcardMatcher(selector).pattern;
        }
        
        boolean matches(final String topic) {
            return p.matcher(topic).matches();        
        }
        boolean matchesAny(final String... topics) {
            for (final String t : topics)
                if (p.matcher(t).matches())
                    return true;
            return false;
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
    
    private Set<Channel> recipients = new HashSet();
    
    public synchronized int publish(final Object message, String... t) {
        //Topic t = topics.get(topic);        
        recipients.clear();
        for (Topic x : this.topics.values()) {
            if (x.matchesAny(t)) {
                recipients.addAll(x.subscribers);
            }        
        }
        if (recipients.size() == 0) return 0;
        
        
        Object messagePacket = Arrays.asList(Arrays.asList(t), message);
        for (final Channel c : recipients) {
            send(c, messagePacket);
        }
        
        return recipients.size();
    }
    
    public Topic addTopic(Topic t) {
        topics.putIfAbsent(t.selector, t);
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
    
    
    /*******************************************************************************
     * Copyright (c) 2009, 2014 Mountainminds GmbH & Co. KG and Contributors
     * All rights reserved. This program and the accompanying materials
     * are made available under the terms of the Eclipse Public License v1.0
     * which accompanies this distribution, and is available at
     * http://www.eclipse.org/legal/epl-v10.html
     *
     * Contributors:
     *    Marc R. Hoffmann - initial API and implementation
     *    
     *******************************************************************************/

    /**
     * Matches strings against <code>?</code>/<code>*</code> wildcard expressions.
     * Multiple expressions can be separated with a colon (:). In this case the
     * expression matches if at least one part matches.
     */
    public static class WildcardMatcher {

        private final Pattern pattern;

        /**
         * Creates a new matcher with the given expression.
         * 
         * @param expression
         *            wildcard expressions
         */
        public WildcardMatcher(final String expression) {
            final String[] parts = expression.split("\\:");
            final StringBuilder regex = new StringBuilder(expression.length() * 2);
            boolean next = false;
            for (final String part : parts) {
                if (next) {
                    regex.append('|');
                }
                regex.append('(').append(toRegex(part)).append(')');
                next = true;
            }
            pattern = Pattern.compile(regex.toString());
        }

        private static CharSequence toRegex(final String expression) {
            final StringBuilder regex = new StringBuilder(expression.length() * 2);
            for (final char c : expression.toCharArray()) {
                switch (c) {
                case '?':
                    regex.append(".?");
                    break;
                case '*':
                    regex.append(".*");
                    break;
                default:
                    regex.append(Pattern.quote(String.valueOf(c)));
                    break;
                }
            }
            return regex;
        }

        /**
         * Matches the given string against the expressions of this matcher.
         * 
         * @param s
         *            string to test
         * @return <code>true</code>, if the expression matches
         */
        public boolean matches(final String s) {
            return pattern.matcher(s).matches();
        }

    }

}
