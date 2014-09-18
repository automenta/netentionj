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
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.InventoryItem;
import com.google.bitcoin.core.InventoryItem.Type;
import com.google.bitcoin.core.InventoryMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.utils.BriefLogFormatter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.impl.Json;

/**
 *
 * @author me
 */
public class Bitcoin implements Runnable {
    
    public final NetworkParameters params;
    public final PeerGroup peerGroup;

    private final HashMap<Peer, String> reverseDnsLookups = new HashMap<>();
    private final EventBus bus;
    private final HashMap<Sha256Hash, String> urlHash = new HashMap<>();

    static {
        BriefLogFormatter.init();
    }
    
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

    public Bitcoin(EventBus b) throws BlockStoreException {
        this.bus = b;
        
        params = MainNetParams.get();
        //params = TestNet3Params.get();
        //params = TestNet2Params.get();

        Wallet wallet = new Wallet(params);
        wallet.addKey(new ECKey());
        wallet.addEventListener(new WalletEventListener() {

            @Override
            public void onCoinsReceived(Wallet wallet, Transaction t, BigInteger bi, BigInteger bi1) {
                System.out.println("Coins received: " + t);
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction t, BigInteger bi, BigInteger bi1) {
            }

            @Override
            public void onReorganize(Wallet wallet) {
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction t) {
            }

            @Override
            public void onWalletChanged(Wallet wallet) {
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> list) {
            }

            @Override
            public void onScriptsAdded(Wallet wallet, List<Script> list) {
                System.out.println("Added scripts: " + list);
            }
        });
        
        //BlockChain chain = new BlockChain(params, wallet, new MemoryBlockStore(params));


        peerGroup = new PeerGroup(params, null);
        //peerGroup.setUserAgent("NetentionJ", "0.1");
        peerGroup.setUserAgent("Satoshi", "0.9.1");
        peerGroup.setMaxConnections(4);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));        
        //peerGroup.addWallet(wallet);
        
        peerGroup.addEventListener(new PeerEventListener() {

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
                            Transaction t = new Transaction(params);
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
        });
        
        System.out.println(wallet);
        System.out.println(peerGroup + " running");

        
        peerGroup.startAsync();
        
        new Thread(this).start();
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
    
    public InventoryMessage newInventory(Iterable<String> uris) {
        InventoryMessage im = new InventoryMessage(params);
        
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
        List<Peer> connected = peerGroup.getConnectedPeers();
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
            
            try { Thread.sleep(30*1000); } catch (InterruptedException ex) {            }
        }
    }
}
