/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.web.core;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.thinkaurelius.titan.core.Cardinality;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanGraphQuery;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import static java.util.stream.StreamSupport.stream;
import javafx.application.Platform;
import jnetention.p2p.Listener;
import jnetention.p2p.Network;
import org.apache.commons.math3.stat.Frequency;
import org.vertx.java.core.json.impl.Json;

/**
 * Unifies DB & P2P features
 */
public class Core extends EventEmitter {
    
    private final static String Session_MYSELF = "myself";
    
    public Network net;
    
    //https://github.com/thinkaurelius/titan/blob/c958ad2a2bafd305a33655347fef17138ee75088/titan-test/src/main/java/com/thinkaurelius/titan/graphdb/TitanIndexTest.java
    public final TitanGraph graph;

    public static class SaveEvent {
        public final NObject object;
        public SaveEvent(NObject object) { this.object = object;        }
    }
    public static class NetworkUpdateEvent {        
        
    }

    
    
    @Deprecated final Map<String,NProperty> property = new HashMap();
    @Deprecated final Map<String,NClass> nclass = new HashMap();
    
    private NObject myself;
    
    
    public Core(TitanGraph db) {
        
        this.graph = db;

//        TitanTransaction tx = graph.newTransaction();
//        tx.makePropertyKey("url").dataType(String.class).cardinality(Cardinality.SINGLE);
//        tx.commit();
        
        TitanManagement mgmt = graph.getManagementSystem();
        if (!mgmt.containsGraphIndex("uri")) {
            PropertyKey name = mgmt.makePropertyKey("uri").dataType(String.class).cardinality(Cardinality.SINGLE).make();
            TitanGraphIndex namei = mgmt.buildIndex("uri",Vertex.class).addKey(name).unique().buildCompositeIndex();
            mgmt.commit();
        }
        

        //EdgeLabel knows = makeLabel("uri");
        //mgmt.buildIndex("namev",Vertex.class).addKey(name).buildMixedIndex("root");
        //mgmt.buildIndex("namee",Edge.class).addKey(name).buildMixedIndex("root");        
        
//        if (session.get(Session_MYSELF)==null) {            
//            //first time user
//            become(newUser("Anonymous " + NObject.UUID().substring(0,4)));
//        }
        
        //default tags
        //List<NObject> c = objectStreamByTag(Tag.tag).collect(Collectors.)
        
        for (Tag sysTag : Tag.values()) {
            nclass.put(sysTag.name(), NClass.asNObject(sysTag));
            addObject(NClass.asNObject(sysTag));
        }
                
        //    map.put(1, "one");
        //    map.put(2, "two");
        //    // map.keySet() is now [1,2]
        //
        //    db.commit();  //persist changes into disk
        //
        //    map.put(3, "three");
        //    // map.keySet() is now [1,2,3]
        //    db.rollback(); //revert recent changes
        //    // map.keySet() is now [1,2]
        //
        //    db.close();
        
    }

    public void addRDF(Model rdf) {
        
        TitanTransaction t = graph.newTransaction();
        
        StmtIterator l = rdf.listStatements();
        while (l.hasNext()) {
            Statement s = l.next();
            Resource subj = s.getSubject();
            RDFNode obj = s.getObject();
            Property p = s.getPredicate();
            
            Vertex sv = vertex(t, subj.getURI(), true);
            if (obj instanceof Resource) {
                String ovu = ((Resource)obj).getURI();
                Vertex ov = vertex(t, ovu, true);
                System.out.println("  +: " + subj.getURI() + " " + p.toString() + " " + ovu );
                t.addEdge(null, sv, ov, p.toString());                
            }
            else if (obj instanceof Literal) {
                //TODO support other literal types
                String str = ((Literal)obj).getString();
                if (s!=null)
                    sv.setProperty("rdf", str);
            }
            
        }
        t.commit();
        
    }
    
    public void printGraph() {
        for (Vertex v : graph.getVertices())
            System.out.println(v.toString() + " " + v.getProperty("uri"));
        for (Edge e : graph.getEdges())
            System.out.println(e.toString() + " " + e.getLabel() + " " + e.getPropertyKeys());        
    }
    
    
    public Vertex vertex(String uri, boolean createIfNonExist) {
        TitanTransaction t = graph.newTransaction();
        Vertex r = vertex(t, uri, createIfNonExist);
        t.commit();
        return r;
    }
    
    public Vertex vertex(TitanTransaction t, String uri, boolean createIfNonExist) {
        //System.out.println("indexed keys: " + graph.getIndexedKeys(String.class));
        //Iterable<Vertex> ee = graph.getVertices("uri",uri);
        //TitanIndexQuery iq = graph.indexQuery("uri", uri).limit(1);                
        TitanGraphQuery g = t.query().has("uri", uri).limit(1);
        for (Object v : g.vertices()) {
            if (v!=null)
                return (Vertex)v;
        }
        if (createIfNonExist)  {
            Vertex v = t.addVertex(null);
            v.setProperty("uri", uri);
            return v;            
        }
        return null;
    }
    
    public void addObject(NObject n) {
        TitanTransaction t = graph.newTransaction();
        Vertex v = vertex(t, n.id, true);
        if (n instanceof NClass) {
            NClass nc = (NClass)n;
            for (String s : nc.getSuperTags()) {
                Vertex p = vertex(t, s, true);
                t.addEdge(null, v, p, "-->");
            }
        }
        t.commit();
    }
    
    /** eigenvector centrality */
    public Map<Vertex, Number> centrality(final TitanTransaction t, final int iterations, Vertex start) {
        Map<Vertex, Number> map = new HashMap();
        
        new GremlinPipeline<Vertex, Vertex>(t.getVertices()).start(start).as("x").both().groupCount(map).loop("x", 
                new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {

                    int c = 0;
                    @Override
                    public Boolean compute(LoopPipe.LoopBundle<Vertex> a) {                      
                        return (c++ < iterations);
                    }
        }).iterate();
        
        return map;

        //out(label);
        /*
        m = [:]; c = 0;
        g.V.as('x').out.groupCount(m).loop('x'){c++ < 1000}
            m.sort{-it.value}
        */
    }
    
    public Core online(int listenPort) throws IOException, UnknownHostException, SocketException, InterruptedException {
        net = new Network(listenPort);
        net.listen("obj.", new Listener() {
            @Override public void handleMessage(String topic, String message) {
                System.err.println("recv: " + message);
            }            
        });
        
        
        
        
        //net.getConfiguration().setBehindFirewall(true);                
        
        System.out.println("Server started listening to ");
	System.out.println("Accessible to outside networks at ");
        
        
        return this;
    }
    
    protected void broadcastSelf() {
        if (myself!=null)
            Platform.runLater(new Runnable() {
                @Override public void run() {
                        broadcast(myself);
                }                    
            });        
    }
    
    public void connect(String host, int port) throws UnknownHostException {
        net.connect(host, port);
    }

    
    /*public Core offline() {
        
        return this;
    }*/

    public Iterable<NObject> netValues() {
        return Collections.EMPTY_LIST;
//        
//        //dht.storageLayer().checkTimeout();
//        return Iterables.filter(Iterables.transform(dht.storageLayer().get().values(), 
//            new Function<Data,NObject>() {
//                @Override public NObject apply(final Data f) {
//                    try {
//                        final Object o = f.object();
//                        if (o instanceof NObject) {
//                            NObject n = (NObject)o;
//                            
//                            if (data.containsKey(n.id))
//                                return null;                                
//                            
//                            /*System.out.println("net value: " + f.object() + " " + f.object().getClass() + " " + data.containsKey(n.id));*/
//                            return n;
//                        }
//                        /*else {
//                            System.out.println("p: " + o + " " + o.getClass());
//                        }*/
//                        /*else if (o instanceof String) {
//                            Object p = dht.get(Number160.createHash((String)o));
//                            System.out.println("p: " + p + " " + p.getClass());
//                        }*/
//                        return null;
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                        return null;
//                    }
//                }                
//        }), Predicates.notNull());        
    }
    
//    public Stream<NObject> objectStream() {
////        if (net!=null) {
////            //return Stream.concat(data.values().stream(), netValues());
////            return data.values().stream();
////        }
//        
//        return data.values().stream();
//    }
    
    public Stream<Vertex> objectStreamByTag(final String tagID) {
        return stream( graph.getVertexLabel(tagID).getEdges(Direction.OUT , "class").spliterator(), false ).map( e -> e.getVertex(Direction.OUT) );
    } 
    
//    public Stream<NObject> objectStreamByTagAndAuthor(final String tagID, final String author) {
//        return objectStream().filter(o -> (o.author == author && o.hasTag(tagID)));
//    }
//    
//    public Stream<NObject> objectStreamByTag(final Tag t) {
//        return objectStreamByTag(t.name());
//    }
    
//    public Stream<NObject> userStream() {        
//        return objectStreamByTag(Tag.User);
//    }
    
//    /** list all possible subjects, not just users*/
//    public List<NObject> getSubjects() {
//        return userStream().collect(Collectors.toList());
//    }
    
    
    public NObject newUser(String name) {
        NObject n = new NObject(name);
        n.author = n.id;
        n.add(Tag.User);
        n.add(Tag.Human);
        n.add("@", new SpacePoint(40, -80));
        publish(n);
        return n;
    }
    
    /** creates a new anonymous object, but doesn't publish it yet */
    public NObject newAnonymousObject(String name) {
        NObject n = new NObject(name);
        return n;
    }
    
    /** creates a new object (with author = myself), but doesn't publish it yet */
    public NObject newObject(String name) {
        if (myself==null)
            throw new RuntimeException("Unidentified; can not create new object");
        
        NObject n = new NObject(name);                        
        n.author = myself.id;                
        return n;
    }
    
//    public void become(NObject user) {
//        //System.out.println("Become: " + user);
//        myself = user;
//        session.put(Session_MYSELF, user.id);
//    }

    
    public void remove(String nobjectID) {
//        data.remove(nobjectID);
    }
    
    public void remove(NObject x) {
        remove(x.id);        
    }


    
   

    
    /** save nobject to database */
    public void save(NObject x) {
//        NObject removed = data.put(x.id, x);        
//        index(removed, x);
//        
//        //emit(SaveEvent.class, x);
    }
    
    /** batch save nobject to database */    
    public void save(Iterable<NObject> y) {
//        for (NObject x : y) {
//            NObject removed = data.put(x.id, x);
//            index(removed, x);
//        }            
//        //emit(SaveEvent.class, null);
    }

    
    public void broadcast(NObject x) {
        broadcast(x, false);
    }
    public synchronized void broadcast(NObject x, boolean block) {
        if (net!=null) {
            System.err.println("broadcasting " + x);
            net.send("obj0", x.toStringDetailed());
//            try {
//                
//                    
//            }
//            catch (IOException e) {
//                System.err.println("publish: " + e);
//            }
        }        
    }
    
    /** save to database and publish in DHT */
    public void publish(NObject x, boolean block) {
        save(x);
    
        broadcast(x, block);
        
        
        //TODO save to geo-index
    }
    public void publish(NObject x) {
        publish(x, false);        
    }
    
    /*
    public int getNetID() {
        if (net == null)
            return -1;
        return net.
    }
    */

    public NObject getMyself() {
        return myself;
    }

    protected void index(NObject previous, NObject o) {
        if (previous!=null) {
            if (previous.isClass()) {
                
            }
        }
        
        if (o!=null) {
            
            if ((o.isClass()) || (o.isProperty())) {
                
                for (Map.Entry<String, Object> e : o.value.entries()) {
                    String superclass = e.getKey();
                    if (superclass.equals("tag"))
                        continue;
                    
                    if (nclass.get(superclass)==null) {
                        save(new NClass(superclass));
                    }
                    
                }
                
            }
            
        }
        
    }

    

    public static Frequency tokenBag(String x, int minLength, int maxTokenLength) {
        String[] tokens = tokenize(x);
        Frequency f = new Frequency();
        for (String t : tokens) {
            if (t==null) continue;
            if (t.length() < minLength) continue;
            if (t.length() > maxTokenLength) continue;
            t = t.toLowerCase();
            f.addValue(t);            
        }
        return f;
    }

    public static String[] tokenize(String value) {
            String v = value.replaceAll(","," \uFFEB ").
                        replaceAll("\\."," \uFFED").
                        replaceAll("\\!"," \uFFED").  //TODO alternate char
                        replaceAll("\\?"," \uFFED")   //TODO alternate char
                    ;
            return v.split(" ");
        }    
    

    public Stream<NClass> classStreamRoots() {
        return nclass.values().stream().filter(n -> n.getSuperTags().isEmpty());        
    }

    public String getOntologyJSON() {
        Map<String,Object> o = new HashMap();
        o.put("property", property.values());
        o.put("class", nclass.values());
        return Json.encode(o);
    }

    
}
