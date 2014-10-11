/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import com.google.common.base.Predicate;
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
    
    public Set<String> extend = new HashSet();
    public String description;

    protected NClass(String id) {
        this(id, id);
    }
    
    protected NClass(String id, String name) {
        this(id, name, (String[])null);
    }

    public NClass(String id, String name, String... extend) {
        super(id, name);
        

        addDefaultTags();
        
        if (extend!=null) {
            for (String e : extend)
                this.extend.add(e);
        }
        
    }
    
    protected void addDefaultTags() {
        //add(Tag.tag.toString(), 1.0);        
    }
        
    public NClass(String id, String name, List<String> extend) {
        this(id, name, (String)null);
        
        if (extend!=null) {
            for (String c : extend) {
                extend.add(c.trim());
                c = c.trim();
                if (c.length() == 0) continue;
                tag(c, 1.0);            
            }
        }
    }
    
    public void mergeFrom(NClass c) {
        //TODO
    }

    public Set<String> getExtend() {
        return extend;
    }

    
}
