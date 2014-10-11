/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import com.google.common.collect.LinkedHashMultimap;
import com.tinkerpop.blueprints.Vertex;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author me
 */
public class NObject /*extends Value*/ implements Serializable, Comparable {
        
    public String id;
    public long createdAt;
    public long modifiedAt;
    //public long modifiedAt;
    public String name;
    public String author;
    
    /** set of tags: transient because it is a cache, since it can be derived from other present data */
    public transient Map<String,Double> tags; 
    
    @Deprecated private String subject;
    
    
    public static NObject fromVertex(Vertex v) {
        //TODO
        return null;
    }
    public void toVertex(Vertex v) {
        //TODO
    }
    
    public static NObject fromJSON(String json) {
        Map<String, Object> j = Core.jsonMap(json);
        
        String id = j.containsKey("i") ? j.get("i").toString() : uuid();
        String name = j.containsKey("n") ? j.get("n").toString() : null;
        
        
        NObject n = new NObject(id, name);
        
        //AUTHOR
        if (j.containsKey("a"))
            n.setAuthor( j.get("a").toString() );

        //CREATED AT
        if (j.containsKey("c")) {
            //TODO parse hex or base64 long's
            n.createdAt = Long.parseLong(j.get("c").toString());
        }
        //MODIFIED AT
        if (j.containsKey("m")) {
            //TODO parse hex or base64 long's
            n.modifiedAt = Long.parseLong(j.get("m").toString());
        }

        
        return n;
    }
    public String toJSON() {
        //TODO
        return "";
    }

    
    public NObject() {
        this("");
    }
    
    public NObject(String name) {
        this(uuid(), name);
    }
    
    public NObject(String id, String name) {        
        this.name = name;
        this.id = id;
        this.createdAt = System.currentTimeMillis();
        this.tags = new HashMap();
    }
    

    public void tag(Tag tag) {
        add(tag.toString());
    }
    
    public void add(String tag) {
        NObject.this.tag(tag, 1.0);
    }
    
    public void tag(String tag, double strength) {
        if (strength > 0)
            tags.put(tag, strength);
        else
            tags.remove(tag);
    }

    
    @Override
    public int compareTo(final Object o) {
        if (o instanceof NObject) {
            NObject n = (NObject)o;
            return id.compareTo(n.id);
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return id.hashCode(); 
    }
    
    public boolean hasTag(final String t) {
        return tags.containsKey(t);        
    }

    @JsonIgnore
    public Map<String,Double> getTags() {
        return tags;
    }
    
//    public Iterator<String> iterateTags(boolean includeObjectValues) {
//        Iterator<String> i = value.keySet().iterator();
//        if (includeObjectValues) {
//            i = concat(i, filter(transform(value.entries().iterator(), new Function<Map.Entry<String,Object>, String>() {
//
//                @Override
//                public String apply(Map.Entry<String, Object> e) {
//                    Object v = e.getValue();
//                    if (v instanceof Ref)
//                        return ((Ref)v).object;
//                    return null;
//                }
//
//            }), Predicates.notNull()));
//        }
//        return i;
//    }
    
//    @JsonIgnore
//    public Set<String> getTags(final Predicate<String> p) {
//        Set<String> s = new HashSet();
//        for (Map.Entry<String, Object> v : value.entries()) {
//            if (v.getValue() instanceof Double) {
//                if (p.apply(v.getKey()))
//                    s.add(v.getKey());
//            }
//        }
//        return s;        
//    }    

    @Override
    public String toString() {
        if ((name!=null) && (name.length() > 0))
            return name;
        return id;
    }
    
    public String toStringDetailed() {
        return id + "," + name + "," + author + "," + subject + "," + new Date(createdAt).toString() + "=" + tags;
    }


    @JsonIgnore
    public boolean isClass() {
        return hasTag(Tag.tag.toString());
    }
    @JsonIgnore
    public boolean isProperty() {
        return hasTag(Tag.property.toString());
    }    

    
    
    public static String uuid() {        
        return Core.uuid();
        
//        long a = (long)(Math.random() * Long.MAX_VALUE);
//        long b = (long)(Math.random() * Long.MAX_VALUE);
//        
//        return Base64.getEncoder().encodeToString( Longs.toByteArray(a) ).substring(0, 11) 
//                + Base64.getEncoder().encodeToString( Longs.toByteArray(b) ).substring(0, 11);
    }

    public void setAuthor(String newAuthorID) {
        if (author!=null)
            tags.remove(author);
        
        this.author = newAuthorID;
        
        NObject.this.tag(this.author, 1.0);
    }


    @Deprecated public void setSubject(String id) {
        subject = id;
    }

    void value(String predicate, Object value) {
        
        if (value==null) {
            tags.remove(predicate);
            return;
        }
        
        tag(predicate, 1.0);
        
        //TODO: store value        
    }
    
    void edge(String subject, String predicate, Object value) {
    }

    public Object value(String id) {
        return null;
    }
    
}
