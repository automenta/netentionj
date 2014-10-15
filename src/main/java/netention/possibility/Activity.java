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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        public final Map<String, Double> context;
        int maxResults = 32;
        final int iterations = 256;

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
            
            Map<String, Double> c = context;
            
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
                int num = 0;
                List<Vertex> sorted = new ArrayList(m.keySet());
                Collections.sort(sorted, new Comparator<Vertex>() {                    
                    @Override public int compare(Vertex a, Vertex b) {
                        return Double.compare( m.get(b).doubleValue(), m.get(a).doubleValue() );
                    }
                });
                                        
                for (Vertex k : sorted) {
                    String u = k.getProperty("i");
 
                    if (u!=null) {
                        if (!excludeProperty.contains(u)) {
                            double vv = m.get(k).doubleValue();
                            double pv = vv/total;
                            c.put(u, (pv*100.0));
                            if (num++ > maxResults) {
                                //TODO sort because this will truncate results too early
                                break;
                            }
                        }
                    }
                }
            }
            
        }
        
    }
    
    
    @Override
    public void handle(Message e) {
        String id = e.body().toString();       
        
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