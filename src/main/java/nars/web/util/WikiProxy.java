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

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author me
 */
public class WikiProxy implements Handler<HttpServerRequest> {
    private final HttpClient client;
    
    //final HttpClient client = vertx.createHttpClient().setHost("localhost").setPort(8282);

    public WikiProxy(HttpClient client) {
        super();
        this.client = client;
    }

    
    
    @Override
    public void handle(final HttpServerRequest req) {
        System.out.println("Proxying request: " + req.uri());
        final HttpClientRequest cReq = client.request(req.method(), req.uri(), new Handler<HttpClientResponse>() {
            public void handle(HttpClientResponse cRes) {
                System.out.println("Proxying response: " + cRes.statusCode());
                req.response().setStatusCode(cRes.statusCode());
                req.response().headers().set(cRes.headers());
                req.response().setChunked(true);
                cRes.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        System.out.println("Proxying response body:" + data);
                        req.response().write(data);
                    }
                });
                cRes.endHandler(new VoidHandler() {
                    public void handle() {
                        req.response().end();
                    }
                });
            }
        });
        cReq.headers().set(req.headers());
        cReq.setChunked(true);
        req.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                System.out.println("Proxying request body:" + data);
                cReq.write(data);
            }
        });
        req.endHandler(new VoidHandler() {
            public void handle() {
                System.out.println("end of the request");
                cReq.end();
            }
        });
    }

}
