/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test.bench;

/**
 *
 * @author headius
 */
public class BenchLongFibRecursive {
    public static void main(String[] args) {
        long times = 5;
        long n = 30;
        if (args.length >= 1) {
            times = Long.parseLong(args[0]);
            if (args.length >= 2) {
                n = Long.parseLong(args[1]);
            }
        }
        
        // bench straight-up long fib(30)
        for (long j = 0; j < times; j++) {
            benchPrimitiveLongFib(n);
        }
        
        // bench Long fib
        for (long j = 0; j < times; j++) {
            benchBoxedLongFib(n);
        }
        
        // bench fully-boxed fib
        for (long j = 0; j < times; j++) {
            benchFullyBoxedFib(n);
        }
    }
    
    public static void benchPrimitiveLongFib(long n) {
        long start = System.currentTimeMillis();
        long result = longFib(n);
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for longFib(" + n + ") = " + result);
    }
    
    public static void benchBoxedLongFib(long n) {
        long start = System.currentTimeMillis();
        Long result = boxedLongFib(Long.valueOf(n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for boxedLongFib(" + n + ") = " + result);
    }
    
    public static void benchFullyBoxedFib(long n) {
        long start = System.currentTimeMillis();
        BoxedLong result = fullyBoxedFib(new BoxedLong(n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for fullyBoxedFib(" + n + ") = " + result);
    }
    
    public static long longFib(long n) {
        if (n < 2) {
            return n;
        } else {
            return longFib(n - 2) + longFib(n - 1);
        }
    }
    
    public static final Long ONE = Long.valueOf(1);
    public static final Long TWO = Long.valueOf(2);
    
    public static Long boxedLongFib(Long n) {
        if (n.longValue() < TWO.longValue()) {
            return n;
        } else {
            return boxedLongFib(Long.valueOf(n.longValue() - TWO.longValue())) +
                    boxedLongFib(Long.valueOf(n.longValue() - ONE.longValue()));
        }
    }
    
    public static class BoxedLong {
        long value;
        
        public BoxedLong(long value) {
            this.value = value;
        }
        
        public BoxedLong plus(BoxedLong other) {
            return new BoxedLong(value + other.value);
        }
        
        public BoxedLong minus(BoxedLong other) {
            return new BoxedLong(value - other.value);
        }
        
        public boolean lt(BoxedLong other) {
            return value < other.value;
        }
        
        public String toString() {
            return String.valueOf(value);
        }
    }
    
    public static BoxedLong[] BOXED_CACHE = {
        new BoxedLong(0),
        new BoxedLong(1),
        new BoxedLong(2)
    };
    
    public static BoxedLong fullyBoxedFib(BoxedLong n) {
        if (n.lt(BOXED_CACHE[2])) {
            return n;
        } else {
            return fullyBoxedFib(n.minus(BOXED_CACHE[2])).plus(fullyBoxedFib(n.minus(BOXED_CACHE[1])));
        }
    }
}
