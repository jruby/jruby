package org.jruby.runtime;

import junit.framework.TestCase;
import org.jruby.RubyBasicObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestDynamicScope extends TestCase {
    @Test
    public void testScopeGeneration() throws Throwable {
        for (int i = 0; i < 100; i++) {
            DynamicScope scope = (DynamicScope) DynamicScope.generate(i).invoke(null, null);

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
                if (j < DynamicScope.SPECIALIZED_SETS.size()) {
                    try {
                        String set = DynamicScope.SPECIALIZED_SETS.get(j);
                        scope.getClass().getMethod(set, IRubyObject.class).invoke(scope, RubyBasicObject.UNDEF);
                        if (j >= i) Assert.fail("expected scope of size " + i + " to raise for " + set);
                    } catch (Throwable t) {
                        if (j < i) throw t;
                    }
                    try {
                        String get = DynamicScope.SPECIALIZED_GETS.get(j);
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
