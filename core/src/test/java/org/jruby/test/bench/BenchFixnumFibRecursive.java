/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test.bench;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;

/**
 *
 * @author headius
 */
public class BenchFixnumFibRecursive {
    private static Ruby ruby = Ruby.newInstance();
    
    public static void main(String[] args) {
        int times = 5;
        int n = 35;
        if (args.length >= 1) {
            times = Integer.parseInt(args[0]);
            if (args.length >= 2) {
                n = Integer.parseInt(args[1]);
            }
        }
        
        // bench fixnum-boxed fib
        for (int j = 0; j < times; j++) {
            benchFixnumFib(n);
        }
    }
    
    public static void benchFixnumFib(int n) {
        long start = System.currentTimeMillis();
        ThreadContext context = ruby.getCurrentContext();
        RubyFixnum result = boxedFib(context, RubyFixnum.newFixnum(ruby, n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for boxedFib(" + n + ") = " + result);
    }
    
    public static RubyFixnum[] FIXNUM_CACHE = {
        RubyFixnum.newFixnum(ruby, 0),
        RubyFixnum.newFixnum(ruby, 1),
        RubyFixnum.newFixnum(ruby, 2)
    };
    
    public static RubyFixnum boxedFib(ThreadContext context, RubyFixnum n) {
        if (n.op_lt(context, 2).isTrue()) {
            return n;
        } else {
            return (RubyFixnum)boxedFib(
                    context,
                    (RubyFixnum)n.op_minus(
                        context,
                        2)).op_plus(
                            context,
                            boxedFib(
                                context,
                                (RubyFixnum)n.op_minus(context, 1)));
        }
    }
}
