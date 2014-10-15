/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tag = data class
 * @author me
 */
public class NClass extends NObject {

    public static NClass asNObject(final Tag sysTag) {
        //TODO cache
        return new NClass(sysTag.name(), sysTag.name());
    }
    
    @Deprecated public String description;
    transient public Set<String> extend = new HashSet();

    protected NClass(String id) {
        this(id, id);
    }
        
    public NClass(String id, String name, List<String> extend) {
        this(id, name);
        
        if (extend!=null) {
            for (String c : extend) {
                extend(c);
            }
        }
    }

    public NClass(String id, String name, String... extend) {
        super(id, name);        

        set(Tag.nclass.toString(), 1.0);        
        
        if (extend!=null) {
            for (String e : extend) {
                extend(e);
            }
        }
        
    }
    
    protected void extend(String className) {
        className = className.trim();
        if (className.length() == 0) return;
        
        this.extend.add(className.trim());
        set(className, 1.0);        
    }
    
    public void mergeFrom(NClass c) {
        //TODO
    }

    public Set<String> getExtend() {
        return extend;
    }
    
}
