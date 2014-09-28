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
package nars.web;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author me
 */
public class Session {
    /** authorization key */
    public String auth;
    public List<String> selves;
    public long lastActive;
    private transient InetSocketAddress address;
    
    /** list of self objects for fast reference, to avoid repeat requests on login, etc.
     *  may contain the self data of 'selves' but also friends, etc.
     */
    //private List<NObject> initObjects; 

    /** new session with existing self */
    public Session(HttpServerRequest req, String auth, List<String> selves) {
        this.auth = auth;
        this.selves = selves;
        active(req);
    }

    /** new session with a new self */
    public Session(HttpServerRequest req, String auth) {
        this(req, auth, Arrays.asList(UUID.randomUUID().toString()));
    }

    /** new anonymous session with new auth and new self */
    public Session(HttpServerRequest req) {
        this(req, req.remoteAddress().getHostString() + "_" + UUID.randomUUID().toString(), Arrays.asList(UUID.randomUUID().toString()));
    }

    public void active(HttpServerRequest req) {
        this.lastActive = System.currentTimeMillis();
        this.address = req.remoteAddress();
    }

//    void addSelfObject(NObject u) {
//        
//    }
    
}
