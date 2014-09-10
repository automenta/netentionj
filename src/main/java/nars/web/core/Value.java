/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.web.core;

import com.google.common.collect.LinkedHashMultimap;
import java.io.Serializable;

/**
 *
 * @author me
 */
public class Value implements Serializable {
    
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
    
}
