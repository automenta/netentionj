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

/**
 * Event Bus channels
 */
final public class Bus {
    /** status of interactive sessions */
    public final static String SESSION = "session";
    
    /** declared interest in a topic, passively requests for more information */
    public final static String INTEREST = "interest";
    public final static String INTEREST_WIKIPEDIA = "interest.wikipedia";

    /** permanent knowledge to share */
    public final static String PUBLISH = "publish";
 
    /** transient knowledge to share */
    public final static String SAY = "say";
}
