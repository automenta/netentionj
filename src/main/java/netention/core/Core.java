/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package netention.core;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static java.util.stream.StreamSupport.stream;
import javafx.application.Platform;
import netention.p2p.Listener;
import netention.p2p.Network;
import org.apache.commons.math3.stat.Frequency;
import org.boon.json.JsonParserAndMapper;
import org.boon.json.JsonParserFactory;
import org.vertx.java.core.json.impl.Json;

/**
 * Unifies DB & P2P features
 */
public class Core extends EventEmitter {

    final static JsonParserAndMapper defaultJSONParser = new JsonParserFactory().createLaxParser();    

    static final Pattern primitiveRegEx = Pattern.compile("/^()$/");

    public static boolean isPrimitive(final String s) {
        switch (s) {
            case "class":
            case "property":
            case "g":
            case "boolean":
            case "text":
            case "html":
            case "integer":
            case "real":
            case "url":
            case "object":
            case "spacepoint":
            case "timepoint":
            case "timerange":
            case "sketch":
            case "markdown":
            case "image":
            case "tagcloud":
            case "chat":                
                return true;
            default:
                return false;
        }
    }

    public static TransactionalGraph newMemoryGraph() {
        return new OrientGraph("memory:test");
    }

    /**
     * normalize a URL by removing http://
     */
    public static String u(final String url) {
        if (url.startsWith("http://")) {
            return url.substring(7);
        }
        return url;
    }

    public static Frequency tokenBag(String x, int minLength, int maxTokenLength) {
        String[] tokens = tokenize(x);
        Frequency f = new Frequency();
        for (String t : tokens) {
            if (t == null) {
                continue;
            }
            if (t.length() < minLength) {
                continue;
            }
            if (t.length() > maxTokenLength) {
                continue;
            }
            t = t.toLowerCase();
            f.addValue(t);
        }
        return f;
    }

    public static String[] tokenize(String value) {
        String v = value.replaceAll(",", " \uFFEB ").
                replaceAll("\\.", " \uFFED").
                replaceAll("\\!", " \uFFED"). //TODO alternate char
                replaceAll("\\?", " \uFFED") //TODO alternate char
                ;
        return v.split(" ");
    }

    public static String uuid() {
        //Mongo _id = 12 bytes (BSON) = Math.pow(2, 12*8) = 7.922816251426434e+28 permutations
        //UUID = 128 bit = Math.pow(2, 128) = 3.402823669209385e+38 permutations
        
        //RFC 2396 - Allowed characters in a URI - http://www.ietf.org/rfc/rfc2396.txt
        //		removing all that would confuse jquery
        //var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz-_.!~*\'()";
        //var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz-_";
        
        //TODO recalculate this
        //70 possible chars
        //	21 chars = 5.58545864083284e+38 ( > UUID) permutations
        //		if we allow author+objectID >= 21 then we can guarantee approximate sparseness as UUID spec
        //			so we should choose 11 character Nobject UUID length
        
        //TODO recalculate, removed the '-' which affects some query selectors if - is first
        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz_";
        final int numDiffChars = chars.length();
        
        final int string_length = 11;
        StringBuilder sb = new StringBuilder(string_length);
        for (int i = 0; i < string_length; i++) {
            int p = (int)Math.floor( Math.random() * numDiffChars );
            char c = chars.charAt(p);
            sb.append(c);
        }
        return sb.toString();
    }

    public static Map<String, Object> jsonMap(final String json) {        
        return defaultJSONParser.parseMap(json);        
    }
    public static List jsonList(final String json) {        
        return defaultJSONParser.parseList(Object.class, json);
    }

    public Network net;

    //https://github.com/thinkaurelius/titan/blob/c958ad2a2bafd305a33655347fef17138ee75088/titan-test/src/main/java/com/thinkaurelius/titan/graphdb/TitanIndexTest.java
    //public final TitanGraph graph;
    TransactionalGraph graph;


    final Map<String, NProperty> property = new HashMap();
    final Map<String, NClass> nclass = new HashMap();

    
    /** new Memory-only Core */
    public Core() {
        this(newMemoryGraph());
    }

    public Core(TransactionalGraph db) {
        
        this.graph = db;
        
        ensureIndex("i", Vertex.class, new Parameter("type", "UNIQUE"));
        ensureIndex("modifiedAt", Vertex.class, new Parameter("class", "Long"));
        ensureIndex("i", Edge.class, new Parameter("class", "String"));
        
//
//                TitanManagement mgmt = graph.getManagementSystem();
//        if (!mgmt.containsGraphIndex("i")) {
//            PropertyKey name = mgmt.makePropertyKey("i").dataType(String.class).cardinality(Cardinality.SINGLE).make();
//            TitanGraphIndex namei = mgmt.buildIndex("i",Vertex.class).addKey(name).unique().buildCompositeIndex();
//            mgmt.commit();
//        }        
    }

    public Map<String, Object> getObject(final Vertex v) {
        return getObject(v, Collections.EMPTY_SET);
    }

    public Map<String, Object> getObject(final Vertex v, Set<String> propertyExclude) {
        return object(v.getProperty("i")).toJSONMap();
    }
    
    public Map<String, Object> getObject2(final Vertex v, Set<String> propertyExclude) {

        Map<String, Object> r = new HashMap();

        for (String s : v.getPropertyKeys()) {
            if (!propertyExclude.contains(s)) {
                r.put(s, v.getProperty(s));
            }
        }

        Iterable<Edge> outs = v.getEdges(Direction.OUT);
        Map<String, List<String>> outMap = new HashMap();
        for (Edge e : outs) {
            String edge = e.getLabel();
            String uri = e.getVertex(Direction.IN).getProperty("i");
            List<String> uris = outMap.get(edge);
            if (uris == null) {
                uris = new ArrayList();
                outMap.put(edge, uris);
            }
            uris.add(uri);
        }
        if (outMap.size() > 0) {
            r.put("out", outMap);
        }

        Iterable<Edge> ins = v.getEdges(Direction.IN);
        Map<String, List<String>> inMap = new HashMap();
        for (Edge e : ins) {
            String edge = e.getLabel();
            String uri = e.getVertex(Direction.OUT).getProperty("i");
            List<String> uris = inMap.get(edge);
            if (uris == null) {
                uris = new ArrayList();
                inMap.put(edge, uris);
            }
            uris.add(uri);
        }
        if (inMap.size() > 0) {
            r.put("in", inMap);
        }

        return r;
    }

    public Map<String, Object> getObject(final String objId) {

        Vertex v = vertex(objId, false);

        if (v == null) {
            Map<String, Object> h = new HashMap();
            h.put("error", objId + " not found");
            return h;
        }
        Map<String, Object> r = getObject(v);
        graph.commit();

        return r;
    }

    public void cache(Vertex v, String type) {
        v.setProperty(type + "_modifiedAt", System.currentTimeMillis());
    }

    public boolean cached(Vertex v, String type) {
        long maxCachedTime = 7 * 24 * 60 * 60 * 1000; //1 week
        long now = System.currentTimeMillis();
        Object l = v.getProperty(type + "_modifiedAt");
        if (l == null) {
            return false;
        }

        long then = (long) l;
        if (now - then < maxCachedTime) {
            return true;
        }
        return false;
    }

    public Map<String, Object> vertexProperties(Vertex v) {
        Map<String, Object> m = new HashMap();
        for (String s : v.getPropertyKeys()) {
            m.put(s, v.getProperty(s));
        }
        return m;
    }

    public synchronized void commit() {
        graph.commit();
    }
    
    protected void ensureIndex(String property, Class c, Parameter p) {        
        if (!((KeyIndexableGraph) graph).getIndexedKeys(c).contains(property)) {
            ((KeyIndexableGraph) graph).createKeyIndex(property, c, p);
        }
    }
    
    public void addRDF(Model rdf, String topic) {
        topic = u(topic);

        StmtIterator l = rdf.listStatements();
        while (l.hasNext()) {
            Statement s = l.next();
            Resource subj = s.getSubject();
            RDFNode obj = s.getObject();

            Property p = s.getPredicate();
            String ps = u(p.toString());

            //certain properties
            switch (ps) {
                case "purl.org/dc/terms/subject":
                case "www.w3.org/1999/02/22-rdf-syntax-ns#type":
                case "www.w3.org/2000/01/rdf-schema#label":
                    break;
                    /*case "http://www.w3.org/2002/07/owl#sameAs":
                    if (!subj.getURI().equals(topic))
                    continue;
                    break;*/
                    /*case "http://dbpedia.org/ontology/division":
                    case "http://dbpedia.org/ontology/subdivision":
                    case "http://dbpedia.org/ontology/subdivisio":
                    if (!subj.getURI().equals(topic))
                    continue;
                    break;*/
                default:
                    //System.out.println("  -: " + ps + " " + obj.toString() + " (ignored)");
                    continue;
            }

            String usub = u(subj.getURI());
            Vertex sv = vertex(usub, true);
            if (obj instanceof Resource) {
                String ovu = u(((Resource) obj).getURI());
                Vertex ov = vertex(ovu, true);
                uniqueEdge(sv, ov, p.toString());
            } else if (obj instanceof Literal) {
                //TODO support other literal types
                String str = ((Literal) obj).getString();
                sv.setProperty("rdf", str);
            }

        }
        graph.commit();

    }

    /**
     * removes any existing edges between the two vertices, then adds it
     */
    public Edge uniqueEdge(Vertex from, Vertex to, String predicate) {
        Iterable<Edge> existing = from.getEdges(Direction.OUT, predicate);
        for (Edge e : existing) {
            if (e.getVertex(Direction.IN).equals(to)) {
                //System.out.println(predicate + " existing edge: " + e + " " + e.getLabel() + " " + e.getProperty("i"));

                //TODO set any updated properties
                return e;
            }
        }
        return addEdge(from, to, predicate);
    }

    public Edge addEdge(Vertex sv, Vertex ov, String predicate) {
        //System.out.println("  +: " + sv.toString() + " " + predicate + " " + ov.toString());
        Edge e = graph.addEdge(null, sv, ov, predicate);
        return e;
    }

    public void printGraph() {
        for (Vertex v : (Iterable<Vertex>) graph.getVertices()) {
            System.out.println(v.toString() + " " + v.getProperty("i"));
        }
        for (Edge e : (Iterable<Edge>) graph.getEdges()) {
            System.out.println(e.toString() + " " + e.getLabel() + " " + e.getPropertyKeys());
        }
    }

    public Vertex vertex(String uri, final boolean createIfNonExist) {        
        for (Object v : graph.getVertices("i", uri)) {
            if (v != null) {
                return (Vertex) v;
            }
        }
        if (createIfNonExist) {
            Vertex v = graph.addVertex(null);
            v.setProperty("i", uri);
            return v;
        }
        return null;
    }

    public void addObjects(Iterable<NObject> N) {
        removeObjects(N);
        for (final NObject n : N) {
            
            Vertex v = vertex(n.id, true);
            
            n.toVertex(this, v);

            if (n instanceof NProperty) {
                NProperty np = (NProperty) n;
                property.put(np.id, np);
                //TODO add to graph?
                continue;
            } else if (n instanceof NClass) {
                NClass nc = (NClass) n;
                nclass.put(nc.id, nc);
                for (String s : nc.getExtend()) {
                    Vertex p = vertex(s, true);
                    uniqueEdge(v, p, "is");
                }
            }
            
        }
        graph.commit();
    }
    
    public void removeObjects(Iterable<NObject> N) {
        for (final NObject n : N) {
            
        }
    }


    public void add(NObject... N) {        
        addObjects(Arrays.asList(N));
    }

    public void remove(NObject... n) {
        removeObjects(Arrays.asList(n));
    }
    
    /**
     * eigenvector centrality
     */    public Map<Vertex, Number> centrality(final int iterations, Vertex start) {
         Map<Vertex, Number> map = new HashMap();
         
         new GremlinPipeline<Vertex, Vertex>(graph.getVertices()).start(start).as("x").both().groupCount(map).loop("x",
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
             @Override
             public void handleMessage(String topic, String message) {
                 System.err.println("recv: " + message);
             }
         });
         
         //net.getConfiguration().setBehindFirewall(true);
         System.out.println("Server started listening to ");
         System.out.println("Accessible to outside networks at ");

        return this;
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
     public Stream<Vertex> vertexTagStream(final String tagID) {
         Vertex v = vertex(tagID, false);
         return stream(v.getEdges(Direction.IN, "tag").spliterator(), false).map(e -> e.getVertex(Direction.OUT));
     }
     public Stream<Vertex> vertexAuthorStream(final String author) {
         Vertex v = vertex(author, false);
        if (v == null)
            return Stream.empty();
        return Stream.concat(Stream.of(v), stream(v.getEdges(Direction.OUT, "author").spliterator(), false).map(e -> e.getVertex(Direction.OUT)));
     }
     
     public Stream<Vertex> vertexNewestStream(double secondsAgo, int max) {
         long now = System.currentTimeMillis();
        long then = (long)(now - (secondsAgo * 1000.0));
        Iterable<Vertex> v = graph.query().interval("modifiedAt", then, now).limit(max).vertices();
        return stream(v.spliterator(), false);
    }
    
    public Stream<Vertex> vertexStream() {
        return stream(graph.getVertices().spliterator(), false);
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
     public NObject newUser(String id) {
        NObject n = new NObject(id, "Anonymous");
        n.author = n.id;
        n.value("User");
        n.value("Human");        
        add(n);
        return n;
    }

    /**
     * creates a new anonymous object, but doesn't publish it yet
     */
     public NObject newAnonymousObject(String name) {
         NObject n = new NObject(name);
         return n;
     }
     
     /**
      * creates a new object (with author = myself), but doesn't publish it yet
      */
     public NObject newObject(String author, String name) {
        NObject n = new NObject(name);
        n.author = author;
        return n;
    }

//    public void become(NObject user) {
//        //System.out.println("Become: " + user);
//        myself = user;
//        session.put(Session_MYSELF, user.id);
//    }
     public boolean remove(String objID) {
        Vertex v = vertex(objID, false);
        if (v!=null) {
            v.remove();
            for (Edge e : graph.getEdges("i", objID)) {
                if (e!=null)
                    e.remove();
            }

            commit();
            return true;
        }
                
        return false;
     }
     
     public boolean remove(NObject x) {
        return remove(x.id);
    }

    /**
     * save nobject to database
     */
     public void save(NObject x) {
//        NObject removed = data.put(x.id, x);        
//        index(removed, x);
//        
//        //emit(SaveEvent.class, x);
     }
     
     /**
      * batch save nobject to database
     */
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
         if (net != null) {
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
     
     /**
      * save to database and publish in DHT
     */
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

    protected void index(NObject previous, NObject o) {
        if (previous != null) {
            if (previous.isClass()) {

            }
        }

        if (o != null) {

            if ((o.isClass()) || (o.isProperty())) {

                for (String tag : o.getTags().keySet()) {
                    
                    if (tag.equals("tag")) {
                        continue;
                    }

                    if (nclass.get(tag) == null) {
                        save(new NClass(tag));
                    }

                }

            }

        }

    }

    public Stream<NClass> classStreamRoots() {
        return nclass.values().stream().filter(n -> n.getExtend().isEmpty());
    }

    public String getOntologyJSON() {
        Map<String, Object> o = new HashMap();
        o.put("property", property.values());
        o.put("class", nclass.values());
        return Json.encode(o);
    }

    public Vertex vertex(String id) {
        return vertex(id, false);
    }

    public long vertexCount() {
        return vertexStream().count();
    }

    public NObject object(String id) {
        Vertex v = vertex(id);
        if (v == null) return null;
        return NObject.fromVertex(v);
    }



    public static class SaveEvent {

        public final NObject object;

        public SaveEvent(NObject object) {
            this.object = object;
        }
    }

    public static class NetworkUpdateEvent {
    }
}
