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
package nars.web.possibility;

import com.tinkerpop.blueprints.Vertex;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nars.web.Bus;
import nars.web.core.Core;
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
    Set<String> excludeProperty;
        
    public Activity(Core c, EventBus b) {

        this.core = c;
        this.bus = b;
        
        excludeProperty = new HashSet();
        excludeProperty.add("wikipedia_content");

        b.registerHandler(Bus.INTEREST, this);
        
        
    }
    
    public static class ActivityGraph {
        public final String uri;
        public final Map<String, Object> activity;

        public ActivityGraph(String uri, Map<String, Object> activity) {
            this.uri = uri;
            this.activity = activity;
        }
        
    }
    public static class ContextGraph {
        public final String uri;
        public final Map<String, Integer> context;

        public ContextGraph(String uri, Map<String, Integer> context) {
            this.uri = uri;
            this.context = context;
        }
        
    }
    
    
    @Override
    public void handle(Message e) {
        String id = e.body().toString();
        
        Vertex v = core.vertex(id, false);
        if (v!=null) {
            Map<String, Object> a = core.getObject(v, excludeProperty);
            if (a!=null) {
                bus.publish(Bus.SAY, Json.encode(new ActivityGraph(id, a)));
            }
        }
        if (v!=null) {       
            final double threshold = 0.01;
            
            
            Map<Vertex, Number> m = core.centrality(1000, v);
            
            m.remove(v); //exclude own vertex
            
            Map<String, Integer> c = new HashMap();
            
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
                        double vv = n.getValue().doubleValue();
                        double pv = vv/total;
                        if (pv >= threshold)
                            c.put(u, (int)(pv*100.0));
                    }
                }
            }
            if (c.size() > 0)
                bus.publish(Bus.SAY, Json.encode(new ContextGraph(id, c)));            
        }
        else {
            System.err.println("unknown context: " + id);
        }
        
        core.commit();
        
        
    }
    

    
    
}