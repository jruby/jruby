package org.jruby.runtime;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.jruby.RubyBasicObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.junit.Assert;
import org.junit.Test;

public class TestDynamicScope extends TestCase {
    @Test
    public void testMultithreadedScopeGeneration() throws Throwable {
        final ArrayList<MethodHandle> result = new ArrayList<>();
        final int numberOfThreads = 10;

        Thread[] threads = new Thread[numberOfThreads];
        for(int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(new Runnable(){
                public void run() {
                    MethodHandle dynamicScope = DynamicScopeGenerator.generate(10);
                    synchronized(result) {
                        result.add(dynamicScope);
                    }
                }
            });
            threads[i].start();
        }

        for(int i = 0; i < numberOfThreads; i++) {
            threads[i].join();
        }

        Assert.assertEquals(numberOfThreads, result.size());
        for (int i = 0; i < result.size() - 1; ++i) {
            Assert.assertEquals(result.get(i), result.get(i+1));
        }
    }

    @Test
    public void testScopeGeneration() throws Throwable {
        for (int i = 0; i < 100; i++) {
            DynamicScope scope = (DynamicScope) DynamicScopeGenerator.generate(i).invoke(null, null);

            for (int j = 0; j < 100; j++) {
                try {
                    scope.setValueVoid(RubyBasicObject.NEVER, j, 0);
                    if (j >= i) Assert.fail("expected scope of size " + i + " to raise for setValueVoid of index " + j);
                } catch (Throwable t) {
                    if (j < i) throw t;
                }
                try {
                    Assert.assertEquals(RubyBasicObject.NEVER, scope.getValue(j, 0));
                    if (j >= i) Assert.fail("expected scope of size " + i + " to raise for getValue of index " + j);
                } catch (Throwable t) {
                    if (j < i) throw t;
                }
                try {
                    scope.setValueDepthZeroVoid(RubyBasicObject.NEVER, j);
                    if (j >= i) Assert.fail("expected scope of size " + i + " to raise for setValueDepthZeroVoid of index " + j);
                } catch (Throwable t) {
                    if (j < i) throw t;
                }
                try {
                    Assert.assertEquals(RubyBasicObject.NEVER, scope.getValueDepthZero(j));
                    if (j >= i) Assert.fail("expected scope of size " + i + " to raise for getValueDepthZero of index " + j);
                } catch (Throwable t) {
                    if (j < i) throw t;
                }
                if (j < DynamicScopeGenerator.SPECIALIZED_SETS.size()) {
                    try {
                        String set = DynamicScopeGenerator.SPECIALIZED_SETS.get(j);
                        scope.getClass().getMethod(set, IRubyObject.class).invoke(scope, RubyBasicObject.UNDEF);
                        if (j >= i) Assert.fail("expected scope of size " + i + " to raise for " + set);
                    } catch (Throwable t) {
                        if (j < i) throw t;
                    }
                    try {
                        String get = DynamicScopeGenerator.SPECIALIZED_GETS.get(j);
                        Assert.assertEquals(RubyBasicObject.UNDEF, scope.getClass().getMethod(get).invoke(scope));
                        if (j >= i) Assert.fail("expected scope of size " + i + " to raise for " + get);
                    } catch (Throwable t) {
                        if (j < i) throw t;
                    }
                }
            }
        }
    }
}
