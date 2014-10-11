/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.tag;

/**
 * Represents a tag as a string (ID, URL, URI, IRI, etc...) which must be matched exactly.
 * This is faster to match than WildcardTag so should be used when possible.
 * Supports unicode.
 */
public class URITag extends AbstractTag {
    public final String id;

    public URITag(String id) {
        this.id = id;
    }
    
    @Override
    public boolean matchesAny(String... topicIDs) {
        for (final String t : topicIDs) {
            if (id.equals(t)) {
                return true;
            }
        }
        return false;        
    }
    
}
