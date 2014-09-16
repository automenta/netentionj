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

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.utils.BriefLogFormatter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author me
 */
public class Bitcoin {
    
    private NetworkParameters params;
    private PeerGroup peerGroup;

    private final HashMap<Peer, String> reverseDnsLookups = new HashMap<Peer, String>();

    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        new Bitcoin();
    }

    public Bitcoin() throws BlockStoreException {
        params = MainNetParams.get();
        //params = TestNet3Params.get();
        //params = TestNet2Params.get();
        
        peerGroup = new PeerGroup(params, null /* no chain */);
        peerGroup.setUserAgent("NetentionJ", "0.1");
        peerGroup.setMaxConnections(4);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        
        peerGroup.addEventListener(new AbstractPeerEventListener() {

            @Override
            public void onTransaction(Peer peer, Transaction t) {
                super.onTransaction(peer, t);
                System.out.println("Transaction: " + peer + " " + t);
            }

            @Override
            public Message onPreMessageReceived(Peer peer, Message m) {
                System.out.println("Message: " + peer + " " + m);
                return super.onPreMessageReceived(peer, m);                
            }
            
            
            
            @Override
            public void onPeerConnected(final Peer peer, int peerCount) {
                //refreshUI();
                lookupReverseDNS(peer);
            }

            @Override
            public void onPeerDisconnected(final Peer peer, int peerCount) {
                //refreshUI();
                synchronized (reverseDnsLookups) {
                    reverseDnsLookups.remove(peer);
                }
            }
        });
        
        
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
        BlockChain chain = new BlockChain(params, wallet, new MemoryBlockStore(params));
        
        peerGroup.addWallet(wallet);
        
        System.out.println(wallet);
        peerGroup.startAsync();
        
        System.out.println("Running");
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
    
    
}
