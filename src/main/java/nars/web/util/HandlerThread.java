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

import java.util.concurrent.ExecutorService;
import org.vertx.java.core.Handler;

/**
 *
 * @author me
 */
abstract public class HandlerThread<X> implements Handler<X> {
    private final ExecutorService executor;

    public HandlerThread(ExecutorService t) {
        super();
        this.executor = t;
    }

    
    @Override
    public void handle(final X e) {
        executor.execute(new Runnable() {
            @Override public void run() {
                HandlerThread.this.run(e);
            }            
        });
    }

    abstract public void run(X e);
        
}
