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
package netention;

import java.util.Map;
import netention.core.Core;
import netention.core.NObject;
import org.boon.json.JsonParserAndMapper;
import org.boon.json.JsonParserFactory;
import org.boon.json.implementation.JsonStringDecoder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class NObjectTest {
    
    String simple1 =
            "{" +
            "i: theID," + //id
            "a: theAuthor," + //author
            "c: 0," +   //createdAt
            "m: 1" +   //modifiedAt            
            "}";
    
    @Test public void testNObjectGraph() {
        Map<String, Object> simple1JSON = Core.jsonMap(simple1);
        assertEquals(4, simple1JSON.keySet().size());
        
        Core c = new Core();
        NObject n = NObject.fromJSON(simple1);
        assertEquals("theID", n.id);
        assertEquals("theAuthor", n.author);
        assertTrue(n.hasTag("theAuthor"));
        
        c.add(n);
        assertTrue("Vertex addition", c.vertex(n.id, false)!=null);
                
        c.remove(n);        
        assertTrue("Vertex removal", c.vertex(n.id, false)==null);
        
    }
    
    
}
