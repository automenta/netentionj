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

import com.tinkerpop.blueprints.Direction;
import java.util.Map;
import netention.core.Core;
import netention.core.NObject;
import netention.util.NOntology;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author me
 */
public class NObjectTest {
    
    String oSimple =
        "{" +
            "i: theID," + //id
            "a: theAuthor," + //author
            "c: 0," +   //createdAt
            "m: 1" +   //modifiedAt            
        "}";
    
    @Test public void testNObjectGraph() {
        Map<String, Object> simple1JSON = Core.jsonMap(oSimple);
        assertEquals(4, simple1JSON.keySet().size());
        
        Core c = new Core();
        NObject n = NObject.fromJSON(oSimple);
        assertEquals("theID", n.id);
        assertEquals("theAuthor", n.author);
        assertTrue(n.hasTag("theAuthor"));
        
        c.add(n);
        
        assertTrue("Vertex addition", c.vertex(n.id)!=null);
        assertTrue("Author addition", c.vertex(n.author)!=null);
        assertTrue("Vertex -> Author tag edge", c.vertex(n.author).getEdges(Direction.IN,"tag").iterator().hasNext());
        assertTrue("Author -> Vertex author edge", c.vertex(n.author).getEdges(Direction.OUT,"author").iterator().hasNext());
        assertTrue("Edge inexistence", !c.vertex(n.author).getEdges(Direction.OUT,"non_existing").iterator().hasNext());                
        assertEquals(2, c.vertexCount());
        c.remove(n);        
        
        assertTrue("Vertex removal", c.vertex(n.id)==null);        
    }
    
    
    String oValue =
        "{" +
            "i: theID," +
            "a: theAuthor," +
            "c: 0," +
            "m: 1," +
            "v: { html: 'description', Geometry: 1.0, length: 0.5 }" +
        "}";
    
    @Test public void testNObjectValue() throws Exception {
        Core c = new Core();
        new NOntology(c);
        
        
        assertTrue( c.vertex("Geometry") != null);
        assertTrue( c.object("Geometry").isClass() );
        
        
        NObject n = NObject.fromJSON(oValue);        
        System.out.println(n.toStringDetailed());
        
        assertTrue(n.getTags().containsKey("Geometry"));
        assertTrue(n.getTags().containsKey("length"));
        assertTrue(!Core.isPrimitive("Geometry"));
        assertTrue(Core.isPrimitive("html"));
        
        assertTrue("do not consider primitive as a tag", !n.getTags().containsKey("html"));
    }
}
