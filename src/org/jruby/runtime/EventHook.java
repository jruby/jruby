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
 */
public interface EventHook {
    public static final int RUBY_EVENT_LINE = 0;
    public static final int RUBY_EVENT_CLASS = 1;
    public static final int RUBY_EVENT_END = 2;
    public static final int RUBY_EVENT_CALL = 3;
    public static final int RUBY_EVENT_RETURN = 4;
    public static final int RUBY_EVENT_C_CALL = 5;
    public static final int RUBY_EVENT_C_RETURN = 6;
    public static final int RUBY_EVENT_RAISE = 7;
    
    public static final int RUBY_EVENT_NONE_FLAG = 0;
    public static final int RUBY_EVENT_LINE_FLAG = 1 << RUBY_EVENT_LINE;
    public static final int RUBY_EVENT_CLASS_FLAG = 1 << RUBY_EVENT_CLASS;
    public static final int RUBY_EVENT_END_FLAG = 1 << RUBY_EVENT_END;
    public static final int RUBY_EVENT_CALL_FLAG = 1 << RUBY_EVENT_CALL;
    public static final int RUBY_EVENT_RETURN_FLAG = 1 << RUBY_EVENT_RETURN;
    public static final int RUBY_EVENT_C_CALL_FLAG = 1 << RUBY_EVENT_C_CALL;
    public static final int RUBY_EVENT_C_RETURN_FLAG = 1 << RUBY_EVENT_C_RETURN;
    public static final int RUBY_EVENT_RAISE_FLAG = 1 << RUBY_EVENT_RAISE;
    public static final int RUBY_EVENT_ALL_FLAG = 0xff;
    
    public static final String[] EVENT_NAMES = {"line", "class", "end", "call", "return", "c-call", "c-return", "raise"};
    
    public void event(ThreadContext context, int event, String file, int line, String name, IRubyObject type);
}
