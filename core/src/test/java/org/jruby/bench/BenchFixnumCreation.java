/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.bench;

import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class BenchFixnumCreation {
    static RubyFixnum one;
    static RubyFixnum two;
    public static void main(String[] args) {
        Ruby ruby = Ruby.newInstance();
        
        two = RubyFixnum.newFixnum(ruby, 2);
        one = RubyFixnum.newFixnum(ruby, 1);
        
        for (int i = 0; i < 20; i++) {
            benchFixnumCreation(ruby);
        }
        
        for (int i = 0; i < 20; i++) {
            benchStaticFib(ruby);
        }
    }
    
    public static void benchFixnumCreation(Ruby ruby) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            RubyFixnum fixnum = RubyFixnum.newFixnum(ruby, time);
            if (i % 10000 == 0) System.out.print(".");
        }
        System.out.println("\n" + (System.currentTimeMillis() - time));
    }
    
    public static void benchStaticFib(Ruby ruby) {
        ThreadContext context = ruby.getCurrentContext();
        long time = System.currentTimeMillis();
        System.out.println(fib(ruby, context, RubyFixnum.newFixnum(ruby, 30)));
        System.out.println("\n" + (System.currentTimeMillis() - time));
    }
    
    public static IRubyObject fib(Ruby ruby, ThreadContext context, IRubyObject object) {
        RubyFixnum value = (RubyFixnum)object;
        if (value.op_lt(context, two).isTrue()) {
            return value;
        }
        return ((RubyFixnum)fib(ruby, context, value.op_minus(context, two))).op_plus(context, fib(ruby, context, value.op_minus(context, one)));
    }
}
