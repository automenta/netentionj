/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web.tag;

import io.netty.channel.Channel;
import java.util.HashSet;
import java.util.Set;

/** A tag represents a concept, topic, or channel which data streams through to subscribers.
 *  Essentially it is a selector for applicable tag ID's (URI strings) that decide the relevance
 *  of an input message (the matches...() methods).
 */
public abstract class AbstractTag {
    public final Set<Channel> subscribers = new HashSet();

    public boolean matches(String tagID) {
        return matchesAny(tagID);
    }

    public abstract boolean matchesAny(final String... topicIDs);

    public Set<Channel> getSubscribers() {
        return subscribers;
    }

    public boolean addSubscriber(Channel c) {
        return subscribers.add(c);
    }

    public boolean removeSubscriber(Channel c) {
        return subscribers.remove(c);
    }
    
    //TODO getPrefix(), which can be used to build a Trie index of some type.  Not possible in all cases, for example, if a WildcardTag specifies a wildcard at the beginning of the string.
}
