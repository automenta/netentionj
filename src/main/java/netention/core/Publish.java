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

package netention.core;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import java.util.List;
import java.util.Map;
import netention.web.Bus;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class Publish implements Handler<Message> {
    private final Core core;
    private final EventBus bus;

    public Publish(Core c, EventBus b) {
        
        this.core = c;
        this.bus = b;
        b.registerHandler(Bus.PUBLISH, this);
    }

    
    @Override public void handle(Message e) {
        switch (e.address()) {
            case Bus.PUBLISH:
                try {
                    System.out.println("Publish: "+ e.address() + "|" + e.replyAddress() + "|" + e.body());
                    publish(e.body().toString());
                }
                catch (Exception ex) {
                    System.err.println("Unable to decode: " + ex);
                    ex.printStackTrace();
                }
                break;
        }
        
    }
    
    public boolean publish(String nobjectJSON) {
        
  
        //TODO authorize
        
        NObject incoming = NObject.fromJSON(nobjectJSON);
        core.add(incoming);                
        
        //bus.publish(Bus.INTEREST, incoming.id);
        return true;
        
//        String id = (String) m.get("i");
//        String author = (String) m.get("a");
        
//        //Create Edge
//        if ((pred = m.get("predicate"))!=null) {
//            
//            long createdAt = (long) m.get("createdAt");            
//            String subj = (String)m.get("subject");
//            String obj = (String)m.get("object");
//            if ((subj!=null) && (obj!=null)) {
//                if (pred instanceof List) {
//                    List<String> l = (List)pred;
//                    
//                    Vertex sv = core.vertex(subj, true);
//                    sv.setProperty("type", "user");
//                    
//                    Vertex ov = core.vertex(obj, true);                    
//                    
//                    for (String p : l) {                        
//                        Edge e = core.uniqueEdge(sv, ov, p);
//                        e.setProperty("author", author);
//                        e.setProperty("createdAt", createdAt);
//                    }
//                    
//                    core.commit();
//
//                }
//            }
//        }
//        //Add/Set NObject
//        else {
//            //set vertex value
//            ///System.out.println("Setting vertex: " + m);
//            
//            
//            Vertex sv = core.vertex(id, true);
//            
//            final String[] rootFields =  { "name", "geolocation", "createdAt", "modifiedAt", "author", "scope" };
//            for (String r : rootFields) {
//                Object v = m.get(r);
//                if (v!=null) {                    
//                    if (v instanceof Number) {
//                        
//                    }
//                    else if (v instanceof String) {
//                        
//                    }
//                    //other primitive types supported by the graph database
//                    else {
//                        v = v.toString();
//                    }
//                    sv.setProperty(r, v);
//                }
//            }
//            //TODO remove existing non-specified edges
//            
//            Object value = m.get("value");
//            if ((value!=null) && (value instanceof List)) {
//                List valList = (List)value;
//                for (Map<String,Object> l : (List<Map<String,Object>>)valList) {
//                    
//                    Object tagID = l.get("id");
//
//                    if (tagID!=null) {
//                        //create inherit edge                    
//                        core.uniqueEdge(sv, core.vertex(tagID.toString(), true), "is");
//                    }
//                }
//            }
//            
//            core.commit();
//            
//        }
        
    }
}
