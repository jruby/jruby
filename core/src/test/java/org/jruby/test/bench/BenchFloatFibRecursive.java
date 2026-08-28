/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test.bench;

import org.jruby.Ruby;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;

/**
 *
 * @author headius
 */
public class BenchFloatFibRecursive {
    private static Ruby ruby = Ruby.newInstance();

    public static void main(String[] args) {
        int times = 5;
        int n = 30;
        if (args.length >= 1) {
            times = Integer.parseInt(args[0]);
            if (args.length >= 2) {
                n = Integer.parseInt(args[1]);
            }
        }

        // bench float-boxed fib
        for (int j = 0; j < times; j++) {
            benchFloatFib(n);
        }
    }

    public static void benchFloatFib(int n) {
        long start = System.currentTimeMillis();
        ThreadContext context = ruby.getCurrentContext();
        RubyFloat result = boxedFib(context, RubyFloat.newFloat(ruby, n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for boxedFib(" + n + ") = " + result);
    }

    public static RubyFloat[] FLOAT_CACHE = {
        RubyFloat.newFloat(ruby, 0),
        RubyFloat.newFloat(ruby, 1),
        RubyFloat.newFloat(ruby, 2)
    };

    public static RubyFloat boxedFib(ThreadContext context, RubyFloat n) {
        if (n.op_lt(context, 2.0).isTrue()) {
            return n;
        } else {
            return (RubyFloat)boxedFib(
                    context,
                    (RubyFloat)n.op_minus(
                        context,
                        2.0)).op_plus(
                            context,
                            boxedFib(
                                context,
                                (RubyFloat)n.op_minus(context, 1.0)));
        }
    }
}
