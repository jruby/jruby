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
    C_CALL   ("c_call"),
    C_RETURN ("c_return"),
    B_CALL   ("b_call"),
    B_RETURN ("b_return"),
    THREAD_BEGIN   ("thread_begin", false),
    THREAD_END ("thread_end", false),
    RAISE    ("raise", false),
    COVERAGE ("coverage"),
    // A_CALL is CALL + B_CALL + C_CALL
    A_CALL   ("a_call"),
    // A_RETURN is RETURN + B_RETURN + C_RETURN
    A_RETURN ("a_return"),
    RESCUE ("rescue", false);

    public static final Set<RubyEvent> ALL_EVENTS = Collections.synchronizedSet(EnumSet.allOf(RubyEvent.class));
    public static final EnumSet ALL_EVENTS_ENUMSET = EnumSet.copyOf(RubyEvent.ALL_EVENTS);

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

    @Deprecated(since = "9.2.9.0")
    public int getLineNumberOffset(){
        return 0;
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

