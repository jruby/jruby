/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test.bench;

/**
 *
 * @author headius
 */
public class BenchIntFibRecursive {
    public static void main(String[] args) {
        int times = 5;
        int n = 30;
        if (args.length >= 1) {
            times = Integer.parseInt(args[0]);
            if (args.length >= 2) {
                n = Integer.parseInt(args[1]);
            }
        }
        
        // bench straight-up integer fib(30)
        for (int j = 0; j < times; j++) {
            benchIntFib(n);
        }
        
        // bench Integer fib
        for (int j = 0; j < times; j++) {
            benchIntegerFib(n);
        }
        
        // bench fully-boxed fib
        for (int j = 0; j < times; j++) {
            benchBoxedFib(n);
        }
    }
    
    public static void benchIntFib(int n) {
        long start = System.currentTimeMillis();
        int result = intFib(n);
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for intFib(" + n + ") = " + result);
    }
    
    public static void benchIntegerFib(int n) {
        long start = System.currentTimeMillis();
        Integer result = integerFib(Integer.valueOf(n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for integerFib(" + n + ") = " + result);
    }
    
    public static void benchBoxedFib(int n) {
        long start = System.currentTimeMillis();
        BoxedInt result = boxedFib(new BoxedInt(n));
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms for boxedFib(" + n + ") = " + result);
    }
    
    public static int intFib(int n) {
        if (n < 2) {
            return n;
        } else {
            return intFib(n - 2) + intFib(n - 1);
        }
    }
    
    public static final Integer ONE = Integer.valueOf(1);
    public static final Integer TWO = Integer.valueOf(2);
    
    public static Integer integerFib(Integer n) {
        if (n.intValue() < TWO.intValue()) {
            return n;
        } else {
            return integerFib(Integer.valueOf(n.intValue() - TWO.intValue())) +
                    integerFib(Integer.valueOf(n.intValue() - ONE.intValue()));
        }
    }
    
    public static class BoxedInt {
        int value;
        
        public BoxedInt(int value) {
            this.value = value;
        }
        
        public BoxedInt plus(BoxedInt other) {
            return new BoxedInt(value + other.value);
        }
        
        public BoxedInt minus(BoxedInt other) {
            return new BoxedInt(value - other.value);
        }
        
        public boolean lt(BoxedInt other) {
            return value < other.value;
        }
        
        public String toString() {
            return String.valueOf(value);
        }
    }
    
    public static BoxedInt[] BOXED_CACHE = {
        new BoxedInt(0),
        new BoxedInt(1),
        new BoxedInt(2)
    };
    
    public static BoxedInt boxedFib(BoxedInt n) {
        if (n.lt(BOXED_CACHE[2])) {
            return n;
        } else {
            return boxedFib(n.minus(BOXED_CACHE[2])).plus(boxedFib(n.minus(BOXED_CACHE[1])));
        }
    }
}
