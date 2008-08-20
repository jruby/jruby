/*
 * EventHooke.java
 * 
 * Created on May 26, 2007, 3:12:11 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 * @author hooligan495
 * Changed event hook to an enum that manages a collection of event handlers.
 * There are now global event delgators for each event type.  If a component
 * is interested in being notified of an event they should register a handler 
 * with that event.
 * one of the motivations of implementing the EventHook in this way is that we 
 * needed to handle modifying line numbers to be one based (and the RETURN type 
 * ine number for ruby needs to be offset by 2).  If these rules ever change we 
 * can change them here.
 *
 */
public abstract class EventHook {    
    public void event(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type){
        eventHandler(context, event.getName(), file, line + event.getLineNumberOffset(), name, type);
    }
    
    public abstract void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type);
    public abstract boolean isInterestedInEvent(RubyEvent event);
}
