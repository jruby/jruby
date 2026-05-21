package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.java.proxies.ConcreteJavaProxy.StaticJCreateMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercises the split-constructor strategy detection in {@link ExitableInterpreterContext}.
 *
 * <p>Each test defines a Ruby subclass of {@code java.util.ArrayList} (which has multiple
 * constructors) via {@code evalScriptlet}, triggers reification by constructing an instance,
 * then inspects the strategy flags on the Java side.
 */
public class TestSplitSuperStrategy {

    private static Ruby runtime;

    @BeforeClass
    public static void setUp() {
        runtime = Ruby.newInstance();
    }

    @AfterClass
    public static void tearDown() {
        if (runtime != null) runtime.tearDown();
        runtime = null;
    }

    /** Resolves the ExitableInterpreterContext for the given Ruby class name's initialize method. */
    private ExitableInterpreterContext unwrapAfterReify(String className) {
        RubyClass rubyClass = (RubyClass) runtime.evalScriptlet(className);
        DynamicMethod dm = rubyClass.searchMethod("initialize");
        if (dm == null) return null;

        DynamicMethod real = dm.getRealMethod();
        // unwrap StaticJCreateMethod wrapper installed after reification
        if (real instanceof StaticJCreateMethod) {
            real = ((StaticJCreateMethod) real).oldInit;
            if (real == null) return null;
        }
        real = real.getRealMethod();
        if (real instanceof AbstractIRMethod) {
            return ((AbstractIRMethod) real).getJavaConstructorContext();
        }
        return null;
    }

    // --- directSuperNoArgs ---

    @Test
    public void directSuperNoArgs() {
        runtime.evalScriptlet(
                "class TestDirectSuperNoArgs < java.util.ArrayList\n" +
                "  def initialize; super(); end\n" +
                "end\n" +
                "TestDirectSuperNoArgs.new\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestDirectSuperNoArgs");

        assertTrue(eic.directSuperNoArgs());
        assertTrue(eic.exitsAtReturn());
    }

    // --- directSuperAllArgs (rest-only splat) ---

    @Test
    public void directSuperAllArgsSplat() {
        runtime.evalScriptlet(
                "class TestDirectSuperAllArgsSplat < java.util.ArrayList\n" +
                "  def initialize(*args); super(*args); end\n" +
                "end\n" +
                "TestDirectSuperAllArgsSplat.new(10)\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestDirectSuperAllArgsSplat");

        assertTrue(eic.directSuperAllArgs());
        assertFalse(eic.directSuperNoArgs());
        assertEquals(-1, eic.directSuperRequiredArgs());
    }

    // --- directSuperAllArgs (bare super with first, *rest) ---

    @Test
    public void directSuperAllZSuperArgs() {
        runtime.evalScriptlet(
                "class TestDirectSuperZSuper < java.util.ArrayList\n" +
                "  def initialize(first, *rest); super; end\n" +
                "end\n" +
                "TestDirectSuperZSuper.new(10)\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestDirectSuperZSuper");

        assertTrue(eic.directSuperAllArgs());
    }

    // --- directSuperRequiredArgs ---

    @Test
    public void directSuperRequiredArgsOneArg() {
        runtime.evalScriptlet(
                "class TestDirectSuperReqOne < java.util.ArrayList\n" +
                "  def initialize(a); super(a); end\n" +
                "end\n" +
                "TestDirectSuperReqOne.new(10)\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestDirectSuperReqOne");

        assertEquals(1, eic.directSuperRequiredArgs());
        assertFalse(eic.directSuperAllArgs());
    }

    @Test
    public void directSuperRequiredArgsRejectedOnReassignment() {
        runtime.evalScriptlet(
                "class TestDirectSuperReqReassign < java.util.ArrayList\n" +
                "  def initialize(n)\n" +
                "    n = 99\n" +
                "    super(n)\n" +
                "  end\n" +
                "end\n" +
                "TestDirectSuperReqReassign.new(1)\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestDirectSuperReqReassign");

        assertEquals(-1, eic.directSuperRequiredArgs());
    }

    // --- terminalLiteralSuper ---

    @Test
    public void terminalLiteralSuperInteger() {
        runtime.evalScriptlet(
                "class TestTermLitInt < java.util.ArrayList\n" +
                "  def initialize; super(7); end\n" +
                "end\n" +
                "TestTermLitInt.new\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestTermLitInt");

        assertTrue(eic.isTerminalLiteralSuper());
    }

    @Test
    public void terminalLiteralSuperRejectsMutableString() {
        runtime.evalScriptlet(
                "class TestTermLitMutStrActual < java.util.ArrayList\n" +
                "  def initialize; super(\"x\"); end\n" +
                "end\n");
        runtime.evalScriptlet("TestTermLitMutStrActual.new rescue nil\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestTermLitMutStrActual");

        assertFalse(eic.isTerminalLiteralSuper());
    }

    @Test
    public void terminalLiteralSuperFrozenString() {
        runtime.evalScriptlet(
                "class TestTermLitFrzStrActual < java.util.ArrayList\n" +
                "  def initialize; super(\"x\".freeze); end\n" +
                "end\n");
        runtime.evalScriptlet("TestTermLitFrzStrActual.new rescue nil\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestTermLitFrzStrActual");

        assertTrue("frozen String should be terminal literal", eic.isTerminalLiteralSuper());
    }

    @Test
    public void terminalLiteralSuperRejectsMethodCall() {
        runtime.evalScriptlet(
                "class TestTermLitMethodCall < java.util.ArrayList\n" +
                "  def initialize(s); super(s.to_i); end\n" +
                "end\n" +
                "TestTermLitMethodCall.new('5')\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestTermLitMethodCall");

        assertFalse(eic.isTerminalLiteralSuper());
    }

    // --- arbitrary pre-super (falls back to split interpreter) ---

    @Test
    public void arbitraryPreSuperRejectsAllDirectStrategies() {
        runtime.evalScriptlet(
                "class TestArbitraryPreSuper < java.util.ArrayList\n" +
                "  def initialize(opts)\n" +
                "    n = opts[:n] + 1\n" +
                "    super(n)\n" +
                "  end\n" +
                "end\n" +
                "TestArbitraryPreSuper.new(n: 5)\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestArbitraryPreSuper");

        assertFalse(eic.directSuperNoArgs());
        assertFalse(eic.directSuperAllArgs());
        assertEquals(-1, eic.directSuperRequiredArgs());
        assertFalse(eic.isTerminalLiteralSuper());
    }

    // --- post-super continuation ---

    @Test
    public void postSuperContinuationNotExitAtReturn() {
        runtime.evalScriptlet(
                "class TestPostSuperContinuation < java.util.ArrayList\n" +
                "  attr_reader :marker\n" +
                "  def initialize(n)\n" +
                "    super(n)\n" +
                "    @marker = 'after'\n" +
                "  end\n" +
                "end\n");
        Object obj = runtime.evalScriptlet("TestPostSuperContinuation.new(5)").toJava(Object.class);
        assertNotNull(obj);

        ExitableInterpreterContext eic = unwrapAfterReify("TestPostSuperContinuation");

        assertFalse("post-super code means exitsAtReturn should be false", eic.exitsAtReturn());

        // Verify post-super code ran
        Object marker = runtime.evalScriptlet("TestPostSuperContinuation.new(5).marker").toJava(Object.class);
        assertEquals("after", marker);
    }

    // --- super with explicit block rejects terminal literal ---

    @Test
    public void superWithBlockRejectsTerminalLiteral() {
        runtime.evalScriptlet(
                "class TestSuperWithBlock < java.util.ArrayList\n" +
                "  def initialize\n" +
                "    super(7) { :never_called }\n" +
                "  end\n" +
                "end\n" +
                "TestSuperWithBlock.new\n");

        ExitableInterpreterContext eic = unwrapAfterReify("TestSuperWithBlock");

        assertFalse("block-bearing super should reject terminal literal", eic.isTerminalLiteralSuper());
    }
}
