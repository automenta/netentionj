/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.boon.core.value.ValueMap;
import org.vertx.java.core.json.impl.Json;

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
    
    private LinkedHashMap<String,Object> values = new LinkedHashMap();

    /** set of tags: transient because it is a cache, since it can be derived from other present data */
    private transient Map<String,Double> tags; 
    
    
    @Deprecated private String subject;
    
    
    public static NObject fromVertex(Vertex v) {
        //TODO        
        String id = (String)v.getProperty("i");
        String name = (String)v.getProperty("n");
        
        
        NObject n = new NObject(id, name);

        n.setAuthor((String)v.getProperty("a"));
        
        Long cAt = (Long)v.getProperty("createdAt");
        if (cAt!=null) n.createdAt = cAt.longValue();
        
        Long mAt = (Long)v.getProperty("modifiedAt");
        if (mAt!=null) n.modifiedAt = mAt.longValue();
        else n.modifiedAt = n.createdAt;
        
        //String valueJSON = (String)v.getProperty("v");
        Map va = (Map)v.getProperty("v");
        if (va!=null) {
            //List va = Core.jsonList(valueJSON);
            for (Object property : va.keySet()) {                            
                Object pv = va.get(property);
                n.set((String)property, pv);
            }
        }
        
        return n;
    }
    
    public void toVertex(Core c, Vertex v) {
                
        
        if (name!=null) v.setProperty("n", name);
        if (author!=null) v.setProperty("a", author);
        v.setProperty("createdAt", new Long(createdAt));
        v.setProperty("modifiedAt", new Long(modifiedAt));
        
        
        
        //TODO createdAt, modifiedAt, ..
        
        if ((author!=null) && (author!=id)) {
            c.uniqueEdge(c.vertex(author, true), v, "author", id);
        }
        

        //TODO use entryset
        for (String property : values.keySet()) {
            Object val = values.get(property);
            
            if (property.equals("g")) {
                //graph edges
                
                
                for (Object e: (List)val) {                    
                            
                    List triple = Core.jsonList(e.toString());
                    
                    Collection<String> subjs = getTripleComponent(triple.get(0));
                    Collection<String> preds = getTripleComponent(triple.get(1));
                    Collection<String> objs = getTripleComponent(triple.get(2));
                    
                    double strength;
                    if ((triple.size() > 3) && (triple.get(3) instanceof Number))
                        strength = ((Number)triple.get(3)).doubleValue();
                    else
                        strength = 1;
                    
                    //check how many edges will be created and disallow if > limit
                    
                    for (String s : subjs)
                        for (String p : preds)
                            for (String o : objs) {
                                Edge edge = c.uniqueEdge(c.vertex(s, true), c.vertex(o, true), p);
                                edge.setProperty("i", v.getProperty("i"));
                                edge.setProperty("s", strength);
                                
                                tags.put(s, strength);
                                tags.put(o, strength);
                                tags.put(p, strength);
                            }
                                
                }                
            }
            
        }

        //TODO use tag weights
        //TODO use entryset
        for (String t : tags.keySet()) {
            Vertex p = c.vertex(t, true);
            Edge e = c.uniqueEdge(p, v, "tag", id);
            double strength = tags.get(t);
            if (strength!=1.0)
                e.setProperty("strength", tags.get(t));
        }

        
        v.setProperty("v", values);
    }
    
    private List<String> getTripleComponent(Object x) {
        List<String> ls;
        if (x instanceof String) {
            String v = (String)x;
            if (v.equals("_")) v = id;
            ls = Collections.singletonList(v);
        }
        else if (x instanceof List) {
            ls = new ArrayList(((List)x).size());
            for (Object o : (List)x) {
                String v = (String)o;
                if (v.equals("_")) v = id;
                ls.add(v);
            }
        }
        else
            ls = Collections.emptyList();
        return ls;
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
            if (n.modifiedAt == 0)
                n.modifiedAt = n.createdAt;
        }
        else {
            n.modifiedAt = n.createdAt;
        }

        if (j.containsKey("v")) {
            
            Object vv = j.get("v");
            if (vv instanceof ValueMap) {
                ValueMap vl = (ValueMap)vv;                
                for (Object o : vl.keySet()) {
                    n.set((String)o, vl.get(o));
                }
            }
            else {
                //TODO allow both lists and maps ?
            }            
        }
        
        return n;
    }
    
    
    public Map<String, Object> toJSONMap() {
        Map<String,Object> h = new HashMap();
        h.put("i", id);
        if (name!=null) h.put("n", name);
        if (author!=null) h.put("a", author);
        
        if (this instanceof NProperty) {
            h.put("extend", ((NProperty)this).extend );
        }
        else if (this instanceof NClass) {
            h.put("extend", ((NClass)this).extend);
        }
        else {
            if (values.size() > 0)
                h.put("v", values);
            h.put("c", createdAt);
            if ((modifiedAt!=createdAt) && (modifiedAt!=0))
                h.put("m", modifiedAt);
        }
        return h;        
    }
    
    public String toJSON() {                
        return Json.encode(toJSONMap());
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
        return id + "," + name + "," + author + "," + subject + "," + new Date(createdAt).toString() + ", tags=" + tags + ", values=" + values;
    }


    @JsonIgnore
    public boolean isClass() {
        return hasTag(Tag.nclass.toString());
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
        if (this.author!=null)
            tags.remove(author);
        
        this.author = newAuthorID;
    
        if (this.author!=null)
            tags.put(this.author, 1.0);
    }


    @Deprecated public void setSubject(String id) {
        subject = id;
    }

    void set(final String predicate, final Object value) {        
        if (value==null) {
            tags.remove(predicate);
            return;
        }
        
        if (!Core.isPrimitive(predicate))
            tags.put(predicate, 1.0);
        
        values.put(predicate, value);
    }
    
    /*void edge(String subject, String predicate, Object value) {
    }*/
    
    public Object get(String id) {
        return values.get(id);
    }

    public void set(Tag tag) {
        set(tag.toString());
    }
    
    public void set(String tag) {
        set(tag, 1.0);
    }

    protected static boolean e(String a, String b) {
        if ((a == null) && (b == null)) return true;
        if (a == null) return false;
        if (b == null) return false;
        return (a.equals(b));
    }
    
    @Override
    public boolean equals(Object n) {
        
        if (!(n instanceof NObject)) return false;
        
        NObject x = (NObject)n;
        
        if (!e(id, x.id))
            return false;        
        if (!e(name, x.name))
            return false;
        if (!e(author, x.author))
            return false;
        if (x.createdAt != createdAt)
            return false;
        if (x.modifiedAt != modifiedAt)
            return false;

        //TODO compare values
        if (!x.values.equals(values))
            return false;
        
        return true;
    }

    

    
    
}
