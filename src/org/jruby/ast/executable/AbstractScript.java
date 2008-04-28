/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class AbstractScript implements Script {
    public AbstractScript() {}
    
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return null;
    }
    
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return __file__(context, self, args, block);
    }
    
    public CallSite site0;
    public CallSite site1;
    public CallSite site2;
    public CallSite site3;
    public CallSite site4;
    public CallSite site5;
    public CallSite site6;
    public CallSite site7;
    public CallSite site8;
    public CallSite site9;
    public CallSite site10;
    public CallSite site11;
    public CallSite site12;
    public CallSite site13;
    public CallSite site14;
    public CallSite site15;
    public CallSite site16;
    public CallSite site17;
    public CallSite site18;
    public CallSite site19;
    public CallSite site20;
    public CallSite site21;
    public CallSite site22;
    public CallSite site23;
    public CallSite site24;
    public CallSite site25;
    public CallSite site26;
    public CallSite site27;
    public CallSite site28;
    public CallSite site29;
    public CallSite site30;
    public CallSite site31;
    public CallSite site32;
    public CallSite site33;
    public CallSite site34;
    public CallSite site35;
    public CallSite site36;
    public CallSite site37;
    public CallSite site38;
    public CallSite site39;
    public CallSite site40;
    public CallSite site41;
    public CallSite site42;
    public CallSite site43;
    public CallSite site44;
    public CallSite site45;
    public CallSite site46;
    public CallSite site47;
    public CallSite site48;
    public CallSite site49;
}
