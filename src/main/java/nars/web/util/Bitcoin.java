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
package nars.web.util;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.InventoryItem;
import com.google.bitcoin.core.InventoryItem.Type;
import com.google.bitcoin.core.InventoryMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.net.NioServer;
import com.google.bitcoin.net.StreamParser;
import com.google.bitcoin.net.StreamParserFactory;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.utils.BriefLogFormatter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class Bitcoin extends PeerGroup implements Runnable, PeerEventListener {
    

    private final HashMap<Peer, String> reverseDnsLookups = new HashMap<>();
    private final EventBus bus;
    private final HashMap<Sha256Hash, String> urlHash = new HashMap<>();

    static {
        BriefLogFormatter.init();
    }
    private final NioServer server;
    private final NetworkParameters param;
    
//    public static void main(String[] args) throws Exception {
//        BriefLogFormatter.init();
//
//        Bitcoin b = new Bitcoin();
//        
//        b.peerGroup.connectTo(InetSocketAddress.createUnresolved("localhost", 10001));
//        
//        Thread.sleep(6000);
//        
//        //b.publish(b.newInventory(Collections.singleton("dbpedia.org/resource/X")));
//        
//    }

    public Bitcoin(NetworkParameters params, BlockChain bc, EventBus b) throws Exception {
        super(params, bc);
        
        this.bus = b;
        this.param = params;
        
        //peerGroup.setUserAgent("NetentionJ", "0.1");
        setUserAgent("Satoshi", "0.9.1");
        setMaxConnections(4);
        //peerGroup.addPeerDiscovery(new DnsDiscovery(params));        
        

        
        //start server:        
        server = new NioServer(new StreamParserFactory() {
            @Nullable
            @Override
            public StreamParser getNewParser(InetAddress inetAddress, int port) {                
                Peer p = new Peer(params, bc, new PeerAddress(inetAddress, port), "NetentionJ", "0.1");
                try {
                    handleNewPeer(p);
                }
                catch (Exception e) { 
                }
                return p;
            }
        }, new InetSocketAddress("127.0.0.1", 8333));
        server.startAsync();
        
            
//            
//        try {
//            peerGroup.addAddress(new PeerAddress(InetAddress.getByName("54.84.209.171"), 8333));
//            peerGroup.connectTo(new InetSocketAddress("54.84.209.171", 8333));
//            
//        } catch (Exception ex) {
//            Logger.getLogger(Bitcoin.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println("Connected");
            
        
        //peerGroup.addWallet(wallet);
        
        addEventListener(this);
                
        System.out.println(this + " running");

        
        startAsync();
        
        new Thread(this).start();
    }


    @Override
    public void onTransaction(Peer peer, Transaction t) {
        //System.out.println("Transaction: " + t);
        //bus.publish("public", t.toString());
    }

    @Override
    public Message onPreMessageReceived(Peer peer, Message m) {
        System.out.println("Message: " + peer + " " + m);
        bus.publish("public", m.toString());
        return m;
    }

    @Override
    public void onPeerConnected(final Peer peer, int peerCount) {
        //refreshUI();
        //System.out.println("Peer connect: " + peer + " " + peer.toString());
        //bus.publish("public", peer.toString());
        //lookupReverseDNS(peer);
    }

    @Override
    public void onPeerDisconnected(final Peer peer, int peerCount) {
        //refreshUI();
        synchronized (reverseDnsLookups) {
            reverseDnsLookups.remove(peer);
        }
    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, int i) {
    }

    @Override
    public void onChainDownloadStarted(Peer peer, int i) {
    }

    @Override
    public List<Message> getData(Peer peer, GetDataMessage gdm) {
        //System.out.println("getData: " + peer + " " + gdm);
        bus.publish("public", Json.encode(gdm));
        for (InventoryItem i : gdm.getItems()) {
            if (i.type == Type.Transaction) {
                String u = urlHash.get(i.hash);
                if (u!=null) {
                    Transaction t = new Transaction(param);
                    try {
                        t.addOutput(new BigInteger("0"),
                                new ScriptBuilder().data( "__".getBytes("UTF8") ).build() );
                    } catch (UnsupportedEncodingException ex) {
                    }
                    t.setParent(gdm);                            
                }                                
            }
        }
        return null;

    }
    

    private void lookupReverseDNS(final Peer peer) {
        new Thread() {
            @Override
            public void run() {
                // This can take a looooong time.
                String reverseDns = peer.getAddress().getAddr().getCanonicalHostName();
                synchronized (reverseDnsLookups) {
                    reverseDnsLookups.put(peer, reverseDns);
                }                
            }
        }.start();
    }
    public GetDataMessage newGetData(Iterable<String> uris) {
        GetDataMessage im = new GetDataMessage(param);
        
        for (String u : uris) {
            try {
                im.addTransaction(Sha256Hash.create(u.getBytes("UTF8")));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Bitcoin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return im;
    }
    
    public InventoryMessage newInventory(Iterable<String> uris) {
        InventoryMessage im = new InventoryMessage(param);
        
        for (String u : uris) {
            try {
                byte[] uriBytes = u.getBytes("UTF8");
                Sha256Hash sha = Sha256Hash.create(uriBytes);
                im.addItem(new InventoryItem(Type.Transaction, sha));
                
                urlHash.put(sha, u);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Bitcoin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return im;
    }

    public void publish(Message m) {
        List<Peer> connected = getConnectedPeers();
        for (Peer p : connected) {
            System.out.println("publish: " + m + " to " + p);
            p.sendMessage(m);
        }
    }
    
    @Override
    public void run() {
        
        List<String> urls = Arrays.asList("netention", "dbpedia.org/resource/Thing");
        
        while (true) {
            
            publish( newInventory( urls ));
            publish( newGetData( urls ));
            try { Thread.sleep(30*1000); } catch (InterruptedException ex) {            }
        }
    }
}
