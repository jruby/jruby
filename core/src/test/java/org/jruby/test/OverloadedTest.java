/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test;

/**
 *
 * @author headius
 */
public class OverloadedTest {
    public static void testOverloaded(Overloaded o, int loops) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < loops; i++) {
            o.getName("Charlie");
        }
        System.out.println("took: " + (System.currentTimeMillis() - time));
    }
    public static void testOverloaded2(Overloaded o, int loops) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < loops; i++) {
            o.foo("Charlie");
        }
        System.out.println("took: " + (System.currentTimeMillis() - time));
    }
}
