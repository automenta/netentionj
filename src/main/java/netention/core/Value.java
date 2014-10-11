/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.LinkedHashMultimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author me
 */
@Deprecated public class Value implements Serializable {
    
    public String id;
    protected LinkedHashMultimap<String, Object> value;
 
    public static class Ref implements Serializable {
        public final String object;
        public Ref(String object) { this.object = object;        }        
    }
    
    public static Ref object(NObject n) {
        return new Ref(n.id);
    }

    public LinkedHashMultimap<String, Object> getValue() {
        return value;
    }
    

    public <X> List<X> values(Class<X> c) {
        List<X> x = new ArrayList();
        for (Object o : value.values()) {
            if (c.isInstance(o))
                x.add((X)o);
        }
        return x;
    }

    public <X> X firstValue(Class<X> c) {
        for (Object o : value.values()) {
            if (c.isInstance(o))
                return (X)o;
        }        
        return null;        
    }
    
    @JsonIgnore
    public Map<String, Double> getTagStrengths() {
        Map<String,Double> s = new HashMap();
        for (Map.Entry<String, Object> e: value.entries()) {
            if (e.getValue() instanceof Double) {
                //TODO calculate maximum value if repeating keys?
                s.put(e.getKey(), (Double)e.getValue());
            }
        }
        return s;
    }
    
}
