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

package nars.web.core;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Vertex;
import java.util.HashMap;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class ContextualizeInterest implements Handler<Message> {
    private final Core core;
    private final EventBus bus;

    public ContextualizeInterest(Core c, EventBus b) {
        
        this.core = c;
        this.bus = b;
        b.registerHandler("interest", this);
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
        
        
        TitanTransaction t = core.graph.newTransaction();
        Vertex v = core.vertex(t, id, false);
        
        if (v!=null) {       
            final double threshold = 0.01;
            
            
            Map<Vertex, Number> m = core.centrality(t, 1000, v);
            
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
                    String u = n.getKey().getProperty("uri");
                    double vv = n.getValue().doubleValue();
                    double pv = vv/total;
                    if (pv >= threshold)
                        c.put(u, (int)(pv*100.0));
                }
            }
            System.out.println(c);
            bus.publish("public", Json.encode(new ContextGraph(id, c)));
            
        }

        t.commit();
        
    }
    
    
}
