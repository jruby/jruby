/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ripper;

import org.jruby.parser.ParserSupport19;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class RipperSupport extends ParserSupport19 {
    
    @Override
    public IRubyObject dispatch(String method_name) {
        return null;
    }
    
    @Override
    public IRubyObject dispatch(String method_name, IRubyObject arg1) {
        return null;
    }
    
    @Override
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2) {
        return null;
    }
    
    @Override
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return null;
    }
    
    @Override
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        return null;
    }
    
    public RipperParserResult getRipperResult() {
        return null;
    }
}
