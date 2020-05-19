package org.jruby.lexer.yacc;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

import junit.framework.TestCase;

public class StringTermTest extends TestCase {

    /**
     * see https://github.com/jruby/jruby/issues/1069
     */
    public void testGH1069() {
        final String testScriptUsingSingleQuote = "# encoding: utf-8\n'\\Ã¢â‚¬â„¢'";
        final String testScriptUsingDoubleQuote = "# encoding: utf-8\n\"\\Ã¢â‚¬â„¢\"";

        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby runtime = Ruby.newInstance(config);

        IRubyObject eval1 = runtime.evalScriptlet(testScriptUsingSingleQuote);
        assertEquals("\\Ã¢â‚¬â„¢", eval1.toJava(String.class));

        IRubyObject eval2 = runtime.evalScriptlet(testScriptUsingDoubleQuote);
        assertEquals("Ã¢â‚¬â„¢", eval2.toJava(String.class));
    }

    /**
     * see https://github.com/jruby/jruby/issues/1390
     */
    public void testGH1390() {
        final String testScriptUsingSingleQuote = "# encoding: utf-8\n'\\\\あ'";
        final String testScriptUsingDoubleQuote = "# encoding: utf-8\n\"\\\\あ\"";

        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby runtime = Ruby.newInstance(config);

        IRubyObject eval1 = runtime.evalScriptlet(testScriptUsingSingleQuote);
        assertEquals("\\あ", eval1.toJava(String.class));

        IRubyObject eval2 = runtime.evalScriptlet(testScriptUsingDoubleQuote);
        assertEquals("\\あ", eval2.toJava(String.class));
    }

}
