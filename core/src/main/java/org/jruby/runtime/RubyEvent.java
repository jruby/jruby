/*
 * RubyEvent.java
 * 
 * Created on August 8, 2008
 * 
 */
package org.jruby.runtime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum RubyEvent {
    LINE     ("line"),
    CLASS    ("class", false),
    END      ("end", false),
    CALL     ("call"),
    RETURN   ("return"),
    C_CALL   ("c-call"),
    C_RETURN ("c-return"),
    B_CALL   ("b-call"),
    B_RETURN ("b-return"),
    THREAD_BEGIN   ("thread-begin"),
    THREAD_END ("thread-end"),
    RAISE    ("raise"),
    COVERAGE ("coverage"),
    // A_CALL is CALL + B_CALL + C_CALL
    A_CALL   ("a-call"),
    // A_RETURN is RETURN + B_RETURN + C_RETURN
    A_RETURN ("a-return");

    public static Set<RubyEvent> ALL_EVENTS = Collections.synchronizedSet(EnumSet.allOf(RubyEvent.class));

    private final String event_name;
    private final boolean requiresDebug;

    private static final Map<String, RubyEvent> fromName = new HashMap<>();
    static {
        for (RubyEvent event : RubyEvent.values()) {
            fromName.put(event.getName(), event);
        }
    }

    RubyEvent(String event_name){
        this(event_name, true);
    }

    RubyEvent(String event_name, boolean requiresDebug){
        this.event_name = event_name;
        this.requiresDebug = requiresDebug;
    }
	
    public int getLineNumberOffset(){
        return 1;
    }
	
    public String getName(){
        return event_name;
    }

    public static RubyEvent fromOrdinal(int value) {
        return value < 0 || value >= values().length ? null : values()[value];
    }

    public static RubyEvent fromName(String name) {
        return fromName.get(name);
    }

    public boolean requiresDebug() {
        return requiresDebug;
    }
}

