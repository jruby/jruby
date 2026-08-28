package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.api.Create;
import org.jruby.exceptions.NoMethodError;
import org.jruby.ext.bigdecimal.BigDecimalLibrary;
import org.jruby.ext.bigdecimal.RubyBigDecimal;

public class TestRubyModule extends junit.framework.TestCase {

    Ruby runtime;

    public TestRubyModule(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        runtime.tearDown();
    }

    public void testKernelBigDecimalMethodMissingCache() throws Throwable {
        var context = runtime.getCurrentContext();

        try {
            runtime.getObject().callMethod("BigDecimal", Create.newString(context, "1.0"));
        } catch (NoMethodError nme) {
            assertTrue(nme.getMessage().contains("BigDecimal"));
        }

        new BigDecimalLibrary().load(runtime, false);

        var result = runtime.getObject().callMethod("BigDecimal", Create.newString(context, "1.0"));

        assertTrue(result instanceof RubyBigDecimal);
        assertEquals(1.0, result.convertToFloat().getValue());
    }
}
