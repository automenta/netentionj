/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.web.core;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import jnetention.p2p.Listener;
import jnetention.p2p.Network;
import org.apache.commons.math3.stat.Frequency;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.vertx.java.core.json.impl.Json;

/**
 * Unifies DB & P2P features
 */
public class Core extends EventEmitter {
    
    private final static String Session_MYSELF = "myself";
    private final BTreeMap<Object, Object> session;
    public Network net;


    public static class SaveEvent {
        public final NObject object;
        public SaveEvent(NObject object) { this.object = object;        }
    }
    public static class NetworkUpdateEvent {        
        
    }

    
    //DATABASE
    public final BTreeMap<String, NObject> data;
    public final DB db;
    
    final Map<String,NProperty> property = new HashMap();
    final Map<String,NClass> nclass = new HashMap();
    
    private NObject myself;

    /** in-memory Database */
    public Core() {
        this(DBMaker.newMemoryDirectDB().make());        
    }
    
    /** file Database */
    public Core(String filePath) {
        this(DBMaker.newFileDB(new File(filePath))
                .closeOnJvmShutdown()
                .transactionDisable()
                //.encryptionEnable("password")
                .make());
    }
    
    public Core(DB db) {
        
        this.db = db;
        // open existing an collection (or create new)
        data = db.getTreeMap("objects");
        session = db.getTreeMap("session");

        if (session.get(Session_MYSELF)==null) {            
            //first time user
            become(newUser("Anonymous " + NObject.UUID().substring(0,4)));
        }
        
        //default tags
        List<NObject> c = Lists.newArrayList(tagged(Tag.tag));
        
        for (Tag sysTag : Tag.values())
            nclass.put(sysTag.name(), NClass.asNObject(sysTag));
        
        
        
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
    
    public Iterable<NObject> allValues() {
        if (net!=null) {
            return Iterables.concat(data.values(), netValues());
        }
        else {
            return data.values();
        }
    }
    
    public Iterable<NObject> tagged(final String tagID) {
        return Iterables.filter(allValues(), new Predicate<NObject>(){
            @Override public boolean apply(final NObject o) {
                return o.hasTag(tagID);
            }            
        });        
    }    
    public Iterable<NObject> tagged(final String tagID, final String author) {
        return Iterables.filter(allValues(), new Predicate<NObject>(){
            @Override public boolean apply(final NObject o) {
                if (author!=null)
                    if (!author.equals(o.author))
                        return false;
                return o.hasTag(tagID);
            }            
        });        
    }
    public Iterable<NObject> tagged(final Tag t) {
        return tagged(t.name());
    }
    
    public List<NObject> getUsers() {        
        return Lists.newArrayList(tagged(Tag.User));
    }
    
    public List<NObject> getSubjects() {        
        //TODO list all possible subjects, not just users
        return getUsers();
    }
    
    
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
    
    public void become(NObject user) {
        //System.out.println("Become: " + user);
        myself = user;
        session.put(Session_MYSELF, user.id);
    }

    
    public void remove(String nobjectID) {
        data.remove(nobjectID);
    }
    
    public void remove(NObject x) {
        remove(x.id);        
    }


    
   

    
    /** save nobject to database */
    public void save(NObject x) {
        NObject removed = data.put(x.id, x);        
        index(removed, x);
        
        //emit(SaveEvent.class, x);
    }
    
    /** batch save nobject to database */    
    public void save(Iterable<NObject> y) {
        for (NObject x : y) {
            NObject removed = data.put(x.id, x);
            index(removed, x);
        }            
        //emit(SaveEvent.class, null);
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
                    
                    if (getTag(superclass)==null) {
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
    

    public Object getTag(String tagID) {
        NObject tag = data.get(tagID);
        if (tag!=null && tag.isClass())
            return tag;
        return null;
    }

    public Iterable<NObject> getTagRoots() {
        return Iterables.filter(allValues(), new Predicate<NObject>() {
            @Override public boolean apply(NObject t) {
                try {
                    NClass tag = (NClass)t;
                    return tag.getSuperTags().isEmpty();
                }
                catch (Exception e) { }
                return false;
            }            
        });
    }

    public String getOntologyJSON() {
        Map<String,Object> o = new HashMap();
        o.put("property", property.values());
        o.put("class", nclass.values());
        return Json.encode(o);
    }

    
}
