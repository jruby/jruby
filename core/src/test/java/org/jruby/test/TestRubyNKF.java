package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.ext.nkf.RubyNKF;
import org.jruby.ext.nkf.Command;

public class TestRubyNKF extends TestRubyBase {
    public TestRubyNKF(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (runtime == null) {
            runtime = Ruby.newInstance();
        }
    }

    public void testOptParse() throws Exception {
        Command cmd = RubyNKF.parseOption("-j");
        assertEquals("[[opt: j longOpt: jis hasArg: false pattern: null value: null]]", cmd.toString());
        cmd = RubyNKF.parseOption("--hiragana");
        assertEquals("[[opt: h1 longOpt: hiragana hasArg: false pattern: null value: null]]", cmd.toString());
        cmd = RubyNKF.parseOption("-j --hiragana");
        assertEquals("[[opt: j longOpt: jis hasArg: false pattern: null value: null], [opt: h1 longOpt: hiragana hasArg: false pattern: null value: null]]", cmd.toString());
        cmd = RubyNKF.parseOption("-Z");
        assertEquals("[[opt: Z longOpt: null hasArg: true pattern: [0-3] value: null]]", cmd.toString());
        assertTrue(cmd.hasOption("Z"));
        cmd = RubyNKF.parseOption("-Z0");
        assertEquals("[[opt: Z longOpt: null hasArg: true pattern: [0-3] value: 0]]", cmd.toString());
        cmd = RubyNKF.parseOption("-Z1");
        assertEquals("[[opt: Z longOpt: null hasArg: true pattern: [0-3] value: 1]]", cmd.toString());
        cmd = RubyNKF.parseOption("--unix");
        assertEquals("[[opt: null longOpt: unix hasArg: false pattern: null value: null]]", cmd.toString());
        assertTrue(cmd.hasOption("unix"));
        cmd = RubyNKF.parseOption("-m");
        assertEquals("[[opt: m longOpt: null hasArg: true pattern: [BQN0] value: null]]", cmd.toString());
    }

    public void testOptParseWithArg() throws Exception {
        Command cmd = RubyNKF.parseOption("-L");
        assertEquals("[[opt: L longOpt: null hasArg: true pattern: [uwm] value: null]]", cmd.toString());
        cmd = RubyNKF.parseOption("-Lu");
        assertEquals("[[opt: L longOpt: null hasArg: true pattern: [uwm] value: u]]", cmd.toString());
        assertTrue(cmd.hasOption("L"));
        cmd = RubyNKF.parseOption("-f60-");
        assertEquals("[[opt: f longOpt: null hasArg: true pattern: [0-9]+-[0-9]* value: 60-]]", cmd.toString());
        assertTrue(cmd.hasOption("f"));
        cmd = RubyNKF.parseOption("-f60-30");
        assertEquals("[[opt: f longOpt: null hasArg: true pattern: [0-9]+-[0-9]* value: 60-30]]", cmd.toString());
        assertTrue(cmd.hasOption("f"));
        cmd = RubyNKF.parseOption("-mB");
        assertEquals("[[opt: m longOpt: null hasArg: true pattern: [BQN0] value: B]]", cmd.toString());
        assertTrue(cmd.hasOption("m"));
    }

    public void testMultiShortOptParse() throws Exception {
        Command cmd = RubyNKF.parseOption("-jSbw32");
        assertEquals("[[opt: j longOpt: jis hasArg: false pattern: null value: null], [opt: S longOpt: sjis-input hasArg: false pattern: null value: null], [opt: b longOpt: null hasArg: false pattern: null value: null], [opt: w longOpt: null hasArg: true pattern: [0-9][0-9] value: 32]]", cmd.toString());
    }

    public void testLongOptArg() throws Exception {
        Command cmd = RubyNKF.parseOption("--ic=ISO-2022-JP");
        assertEquals("[[opt: null longOpt: ic hasArg: true pattern: ic=(.*) value: ISO-2022-JP]]", cmd.toString());
        assertTrue(cmd.hasOption("ic"));
        cmd = RubyNKF.parseOption("-w16 --oc=utf-8");
        assertEquals("[[opt: w longOpt: null hasArg: true pattern: [0-9][0-9] value: 16], [opt: null longOpt: oc hasArg: true pattern: oc=(.*) value: utf-8]]", cmd.toString());
        assertTrue(cmd.hasOption("w"));
        assertEquals("16", cmd.getOptionValue("w"));
        assertTrue(cmd.hasOption("oc"));
        assertEquals("utf-8", cmd.getOptionValue("oc"));
    }
}
