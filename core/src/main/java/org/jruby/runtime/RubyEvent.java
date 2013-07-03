/*
 * RubyEvent.java
 * 
 * Created on August 8, 2008
 * 
 */
package org.jruby.runtime;

public enum RubyEvent {
    LINE     ("line", 1),
    CLASS    ("class", 1),
    END      ("end", 1),
    CALL     ("call", 1),
    RETURN   ("return", 1),
    C_CALL   ("c-call", 1),
    C_RETURN ("c-return", 1),
    B_CALL   ("b-call", 1),
    B_RETURN ("b-return", 1),
    THREAD_BEGIN   ("thread-begin", 1),
    THREAD_END ("thread-end", 1),
    RAISE    ("raise", 1),
    COVERAGE ("coverage", 1);

    private final String event_name;
    private final int line_number_offset;

    RubyEvent(String event_name, int line_number_offset){
        this.event_name = event_name;
        this.line_number_offset = line_number_offset;
    }
	
    public int getLineNumberOffset(){
        return line_number_offset;
    }
	
    public String getName(){
        return event_name;
    }
}

