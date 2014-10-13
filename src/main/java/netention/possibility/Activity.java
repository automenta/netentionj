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
package netention.possibility;

import com.tinkerpop.blueprints.Vertex;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import netention.web.Bus;
import netention.core.Core;
import netention.possibility.Activity.ActivityGraph;
import netention.possibility.Activity.ObjectCentrality;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class Activity  implements Handler<Message> {
    private final Core core;
    private final EventBus bus;
    static final Set<String> excludeProperty = new HashSet(
            Arrays.asList(
               "wikipedia_content", "Thing", "nclass", "property"
            )
    );
        
    public Activity(Core c, EventBus b) {

        this.core = c;
        this.bus = b;
        

        b.registerHandler(Bus.INTEREST, this);
        
        
    }
    
    
    //TODO rename to ObjectGraphContext
    public static class ActivityGraph {
        public final String uri;
        public final Map<String, Object> activity;

        public ActivityGraph(Core core, String uri) {
            this(core, uri, null);
        }
        
        public ActivityGraph(Core core, String uri, Vertex v) {
            this.uri = uri;
            
            if (v == null)
                v = core.vertex(uri, false);

            if (v!=null)
                this.activity = core.getObjectContext(v, excludeProperty);
            else
                this.activity = Collections.EMPTY_MAP;
        }
        
    }
    public static class ObjectCentrality {
        public final String uri;
        public final Map<String, Integer> context;
        final double threshold = 0.01;
        final int iterations = 1600;

        public ObjectCentrality(Core core, String uri) {
            this(core, uri, null);
        }
        
        public ObjectCentrality(Core core, String uri, Vertex v) {
            this.uri = uri;
            
            if (v == null)
                v = core.vertex(uri, false);
            
            if (v == null) {
                context = Collections.EMPTY_MAP;
                return;
            }
            else
                context = new HashMap();
            
            Map<Vertex, Number> m = core.centrality(iterations, v);
            
            m.remove(v); //exclude own vertex
            
            Map<String, Integer> c = context;
            
            double total = 0;
            int count = 0;
            double max = 0;
            for (Number n : m.values()) {
                double nv = n.doubleValue();
                total += nv;
                if (max < nv)
                    max = nv;
                count++;
            }
            if (max > 0) {
                for (Map.Entry<Vertex, Number> n : m.entrySet()) {
                    String u = n.getKey().getProperty("i");
 
                    if (u!=null) {
                        if (!excludeProperty.contains(u)) {
                            double vv = n.getValue().doubleValue();
                            double pv = vv/total;
                            if (pv >= threshold)
                                c.put(u, (int)(pv*100.0));
                        }
                    }
                }
            }
            
        }
        
    }
    
    
    @Override
    public void handle(Message e) {
        String id = e.body().toString();
                
        //TODO avoid calling Core.vertex() twice by passing it as a parameter to both constructors
       
        
        Map<String,Object> x = getActivity(core, id);
        if (x!=null)
            bus.publish(Bus.SAY, Json.encode(x));
        
        
    }    
    
    public static Map<String,Object> getActivity(Core core, String id) {
        Vertex v = core.vertex(id, false);
        if (v!=null) {

            ActivityGraph ag = new ActivityGraph(core, id, v);
            ObjectCentrality cg = new ObjectCentrality(core, id, v);
            
            if (ag.activity.isEmpty() && cg.context.isEmpty())
                return null;
            
            Map<String,Object> x = new HashMap();
            if (!ag.activity.isEmpty())
                x.put("activity", ag.activity);
            if (!cg.context.isEmpty())
                x.put("context", cg.context);
            return x;
        }
        return null;
    }
    
}