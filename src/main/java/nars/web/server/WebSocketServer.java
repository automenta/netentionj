/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package nars.web.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import io.netty.handler.codec.http.HttpMethod;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.boon.json.JsonParserAndMapper;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;

/**
 * A HTTP server which serves Web Socket requests at:
 *
 * http://localhost:8080/websocket
 *
 * Open your browser at http://localhost:8080/, then the demo page will be loaded and a Web Socket connection will be
 * made automatically.
 *
 * This server illustrates support for the different web socket specification versions and will work with:
 *
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Firefox 11+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * </ul>
 */

@Sharable abstract public class WebSocketServer extends SimpleChannelInboundHandler<Object> implements Runnable {

    static final boolean SSL = false; //System.getProperty("ssl") != null;
    private final JsonSerializer jsonSerializer;
    private final JsonParserAndMapper jsonParser;
    
    static {
        System.setProperty ( "java.version", "1.8" );
    }
    private Channel ch;
    private final NioEventLoopGroup workerGroup;
    private final NioEventLoopGroup bossGroup;
    private Thread mainThread;

    public WebSocketServer(String host, int PORT) throws Exception {
        // Configure SSL.
        //final SslContext sslCtx;
        /*
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
        } else {
            sslCtx = null;
        }
                */
        
        
        JsonParserFactory jsonParserFactory = new JsonParserFactory();
        jsonParser = jsonParserFactory.create();
        /*
                .useFieldsFirst()//useFieldsOnly().usePropertiesFirst().usePropertyOnly() //one of these
                //.plistStyle() //allow parsing of ASCII PList style files
                .lax() //allow loose parsing of JSON like JSON Smart
                //.strict() //opposite of lax
                .setCharset( StandardCharsets.UTF_8 ) //Set the standard charset, defaults to UTF_8
                //.setChop( true ) //chops up buffer overlay buffer (more discussion of this later)
                //.setLazyChop( true ) //similar to chop but only does it after map.get               
                ;
        */
        

        JsonSerializerFactory jsonSerializerFactory = new JsonSerializerFactory()
                .useFieldsFirst()//.useFieldsOnly().usePropertiesFirst().usePropertyOnly() //one of these
                //.addPropertySerializer(  )  customize property output
                //.addTypeSerializer(  )      customize type output
                .useJsonFormatForDates() //use json dates
                //.addFilter(  )   add a property filter to exclude properties
                .includeEmpty().includeNulls().includeDefaultValues() //override defaults
                //.handleComplexBackReference() //uses identity map to track complex back reference and avoid them
                //.setHandleSimpleBackReference( true ) //looks for simple back reference for parent
                .setCacheInstances( true ) //turns on caching for immutable objects
                ;
        jsonSerializer = jsonSerializerFactory.create();
        

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        SSLEngine ssl = SSLContext.getDefault().createSSLEngine();        
        ssl.setUseClientMode(true);
        
        
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.INFO))
         //.handler(new SslHandler(ssl))
         .childHandler(new WebSocketServerInitializer(this));

        ch = b.bind(host, PORT).sync().channel();

        System.err.println("Open your web browser and navigate to " +
                (SSL? "https" : "http") + "://" + host + ':' + PORT + '/');

    }
    
    public void start() {
        try {
            ChannelFuture cf = ch.closeFuture();
            mainThread = new Thread(this);
            mainThread.start();
            cf.sync();
        } catch (InterruptedException ex) {
            Logger.getLogger(WebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();;
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        
    }
            
    abstract public void receive(Channel channel, Object message);

            
    public void send(final Channel c, final Object message) {
        String serialized = jsonSerializer.serialize(message).toString();
        c.writeAndFlush(new TextWebSocketFrame(serialized));
    }
    
    
    private static final String WEBSOCKET_PATH = "/websocket";
    private static final String staticRoot = "client";
    
    private WebSocketServerHandshaker handshaker;

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        // Send the demo page and favicon.ico
        final String uri = req.getUri();
        
        if (!uri.startsWith("/websocket") && req.getMethod() == HttpMethod.GET) {

            try {
            if ("/".equals(uri)) {
                if (HttpStaticFileServerHandler.messageReceived(staticRoot, ctx, req, "/index.html")) {
                    return;
                }
            }

            if ("/favicon.ico".equals(uri)) {
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
                sendHttpResponse(ctx, req, res);
                return;
            }
            
                if (HttpStaticFileServerHandler.messageReceived(staticRoot, ctx, req, req.getUri())) {
                    return;
                }
            } catch (Exception ex) {
                Logger.getLogger(WebSocketServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        else  {
            // Handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, true);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
            } else {
                final Channel channel = ctx.channel();
                final ChannelFuture hf = handshaker.handshake(channel, req);
                hf.addListener(new GenericFutureListener<Future<? super Void>>() {

                    @Override
                    public void operationComplete(Future<? super Void> f) throws Exception {
                        if (hf.isSuccess()) {
                            connected(channel);
                        }
                    }                   
                });
            }
        }
        
    }
    
    

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            disconnected(ctx.channel());
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(/*String.format("%s frame types not supported", frame.getClass().getName())*/);
        }

        
        // Send the uppercase string back.
        String request = ((TextWebSocketFrame) frame).text().trim();
        if (request.startsWith("{") || request.startsWith("[")) {
            try {
                Object o = jsonParser.parse(request);
                receive(ctx.channel(), o);
                return;
            }
            catch (Throwable e) {            }
        }
        receive(ctx.channel(), request);
        
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HOST) + WEBSOCKET_PATH;
        if (WebSocketServer.SSL) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }

    protected void connected(Channel channel) {
    }

    protected void disconnected(Channel channel) {
    }
    
}
