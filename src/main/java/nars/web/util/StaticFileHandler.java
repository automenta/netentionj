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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.impl.MimeMapping;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

/**
 *
 * We use a different sort of caching from most other web servers - the sha1
 * hash of the file contents is used as an ETAG. Most other servers will use a
 * hash of the last modified time and file size. This is a problem in a multi
 * server environment as files on different servers may have different modified
 * dates based on server deployment times, etc. By using the sha1 we ensure that
 * files with the same ETAG will be cached across all servers.
 *
 * Calculating the SHA1 is expensive, so we do it in-line with transmitting the
 * file, and we cache the result and only re-check it when the file modification
 * date we have cached changes. https://gist.github.com/Ryan-ZA/8375100
 */
public class StaticFileHandler implements Handler<HttpServerRequest> {

    static final Logger logger = Logger.getLogger(StaticFileHandler.class.toString());
    private static final String DEFAULT_FILE = "index.html";

    final Path staticPath;
    final String staticPathStr;
    final Vertx vertx;

    private static final Map<String, FileCacheInfo> cacheMap = new ConcurrentHashMap<>();

    public StaticFileHandler(Vertx vertx, String staticPathStr) {
        this.vertx = vertx;
        this.staticPathStr = staticPathStr;        

        staticPath = FileSystems.getDefault().getPath(staticPathStr).normalize();
    }

    public void handle(final HttpServerRequest request) {
        if (!"GET".equals(request.method())) {
            sendNotFound(request);
            return;
        }

        Path requestPath = FileSystems.getDefault().getPath(staticPathStr, request.path()).normalize();
        // Ensure path request is inside statics path
        if (!requestPath.startsWith(staticPath)) {
            logger.info("Attempt to access outside of path");
            sendNotFound(request);
            return;
        }

        handleRequestString(request, requestPath.toString());
    }

    private void handleRequestString(final HttpServerRequest request, final String requestStr) {
        final FileSystem fileSystem = vertx.fileSystem();
        fileSystem.exists(requestStr, new Handler<AsyncResult<Boolean>>() {

            @Override
            public void handle(AsyncResult<Boolean> exists) {
                if (!exists.result()) {
                    sendNotFound(request);
                    return;
                }

                fileSystem.props(requestStr, new Handler<AsyncResult<FileProps>>() {

                    @Override
                    public void handle(AsyncResult<FileProps> event) {
                        FileProps props = event.result();
                        testFileAndSend(request, requestStr, props);
                    }
                });
            }
        });
    }

    private void testFileAndSend(final HttpServerRequest request, final String requestStr, FileProps props) {
        if (props.isDirectory()) {
            handleRequestString(request, requestStr + "/" + DEFAULT_FILE);
            return;
        }
        if (!props.isRegularFile()) {
            sendNotFound(request);
            return;
        }

        FileCacheInfo cacheInfo = cacheMap.get(requestStr);
        String etag = request.headers().get("If-None-Match");

        if (cacheInfo != null && cacheInfo.lastModifiedTime == props.lastModifiedTime().getTime()) {
            // Last modified time has not changed for this file, we don't need to recalculate the sha1 of the contents
            if (etag != null && etag.equals(cacheInfo.etagsha1)) {
                sendNotChanged(request);
            } else {
                sendFile(request, requestStr, cacheInfo);
            }
        } else {
            // Last modified time has changed - we need to send the file and also calculate sha1 of the contents
            sendFileAndCache(request, requestStr, props);
        }
    }

    private void sendFileAndCache(final HttpServerRequest request, final String requestStr, final FileProps props) {
        request.response().putHeader("Content-Length", Long.toString(props.size()));
        int li = requestStr.lastIndexOf('.');
        if (li != -1 && li != requestStr.length() - 1) {
            String ext = requestStr.substring(li + 1, requestStr.length());
            String contentType = MimeMapping.getMimeTypeForExtension(ext);
            if (contentType != null) {
                request.response().putHeader("Content-Type", contentType);
            }
        }

        vertx.fileSystem().open(requestStr, null, true, false, false, new Handler<AsyncResult<AsyncFile>>() {

            @Override
            public void handle(AsyncResult<AsyncFile> event) {
                final AsyncFile asyncFile = event.result();
                final Sha1PumpToHttp pump = new Sha1PumpToHttp(asyncFile, request.response());

                asyncFile.endHandler(new Handler<Void>() {

                    @Override
                    public void handle(Void event) {
                        FileCacheInfo fileCacheInfo = new FileCacheInfo(props.lastModifiedTime().getTime(), pump.getSHA1Hash());
                        cacheMap.put(requestStr, fileCacheInfo);
                        asyncFile.close();

                        // Unfortunately we can't send the new ETAG to this request as the ETAG must be sent in the header, but the next request will get it.
                        request.response().end();
                    }
                });

                pump.start();
            }
        });
    }

    private void sendFile(HttpServerRequest request, String requestStr, FileCacheInfo cacheInfo) {
        request.response().putHeader("ETag", cacheInfo.etagsha1);
        request.response().sendFile(requestStr);
    }

    private void sendNotFound(HttpServerRequest request) {
        request.response().setStatusCode(404).end("Not found");
    }

    private void sendNotChanged(HttpServerRequest request) {
        request.response().setStatusCode(304).end();
    }

    private static class FileCacheInfo {

        final long lastModifiedTime;
        final String etagsha1;

        public FileCacheInfo(long lastModifiedTime, String etagsha1) {
            this.lastModifiedTime = lastModifiedTime;
            this.etagsha1 = etagsha1;
        }

    }

    /**
     * A copy of Pump that also creates an SHA1 hash of the stream as it passes
     * through. Can be fetched once all data has been pushed through with
     * getSHA1Hash()
     */
    public static class Sha1PumpToHttp {

        private final ReadStream<?> readStream;
        private final WriteStream<?> writeStream;
        private int pumped;
        private MessageDigest md;

        /**
         * Start the Pump. The Pump can be started and stopped multiple times.
         */
        public Sha1PumpToHttp start() {
            readStream.dataHandler(dataHandler);
            return this;
        }

        /**
         * Stop the Pump. The Pump can be started and stopped multiple times.
         */
        public Sha1PumpToHttp stop() {
            writeStream.drainHandler(null);
            readStream.dataHandler(null);
            return this;
        }

        /**
         * Return the total number of bytes pumped by this pump.
         */
        public int bytesPumped() {
            return pumped;
        }

        /**
         * Return a hex string of the sha1 hash of the data that passed through
         */
        public String getSHA1Hash() {
            return convertToHex(md.digest());
        }

        private String convertToHex(byte[] data) {
            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                String h = Integer.toHexString(0xFF & data[i]);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        }

        private final Handler<Void> drainHandler = new Handler<Void>() {
            @Override
            public void handle(Void v) {
                readStream.resume();
            }
        };

        private final Handler<Buffer> dataHandler = new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                md.update(buffer.getBytes());
                writeStream.write(buffer);
                pumped += buffer.length();
                if (writeStream.writeQueueFull()) {
                    readStream.pause();
                    writeStream.drainHandler(drainHandler);
                }
            }
        };

        public Sha1PumpToHttp(ReadStream<?> rs, WriteStream<?> ws) {
            readStream = rs;
            writeStream = ws;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
