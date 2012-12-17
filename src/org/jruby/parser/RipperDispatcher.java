/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.parser;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class RipperDispatcher {
    private ParserSupport19 support;
    
    public RipperDispatcher(ParserSupport19 support) {
        this.support = support;
    }
    
    public IRubyObject dispatch(String method_name) {
        return null;
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1) {
        return null;
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2) {
        return null;
    }
}
