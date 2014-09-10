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

package nars.web.core;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;

/**
 *
 * @author me
 */
public class UserActivity implements Handler<Message> {
    private final Core core;
    private final EventBus bus;

    public UserActivity(Core c, EventBus b) {
        
        this.core = c;
        this.bus = b;
        b.registerHandler("publish", this);
    }

    
    @Override
    public void handle(Message e) {
        switch (e.address()) {
            case "publish":
                String message = e.body().toString();
                System.out.println("PUBLISH: " + message);
                break;
        }
        
    }
    
    
}
