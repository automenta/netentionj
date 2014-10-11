/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package netention.util;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import netention.core.Core;
import netention.core.NClass;
import netention.core.NObject;
import netention.core.NProperty;
import netention.core.Tag;
import org.boon.json.JsonFactory;

/**
 * Load ontology Netention from JSON
 */
public class NOntology {
    
    //temporary fix for boon
    static { System.setProperty ( "java.version", "1.8" ); }

             
    public NOntology(Core c) throws Exception {
        ArrayList<NObject> toAdd = new ArrayList();

        Map<String,List<Object>> x = (Map)JsonFactory.fromJson(new FileReader(new File("data/ontology.json")));
        
        for (Tag sysTag : Tag.values()) {            
            toAdd.add(NClass.asNObject(sysTag));
        }
        
        for (Object property : x.get("property")) {
            Map<String,Object> p = (Map)property;
            NProperty np = new NProperty(p.get("id").toString(), p.get("name").toString(), p.get("extend").toString());
            toAdd.add(np);
        }
        for (Object clazz : x.get("class")) {
            Map<String,Object> p = (Map)clazz;
            NClass np = new NClass(p.get("id").toString(), p.get("name").toString(), (List<String>)p.get("extend"));            
            toAdd.add(np);
        }
        
        c.addObjects(toAdd);
    }
    
    
}
