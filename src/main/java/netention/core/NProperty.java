/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package netention.core;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author me
 */
public class NProperty extends NClass {
    
    public final List<String> domain;

    public NProperty(String id, String label, String extend) {
        super(id, label, extend);
        this.domain = new ArrayList();
    }
    
    public NProperty(String id, String label, List<String> domain, List<String> range) {
        super(id, label, range);
        this.domain = domain;
    }

    protected void addDefaultTags() {
        //add(Tag.property.toString(), 1.0);        
    }
    
    
}
