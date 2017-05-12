package org.jruby.embed;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class EmbedClearBug7058Test {
    private ScriptingContainer c;
    @Before
    public void setUp() {
        c = new ScriptingContainer(LocalContextScope.THREADSAFE);
    }
    @Test
    public void testDoesNotThrowNPEAfterClear_Bug7058() {
        c.clear();
        c.runScriptlet(""); // throws NPE
    }
    @Test
    public void testClearReleasesGlobalVariable_EndlessLoopIn1_7_2() {
        c.runScriptlet("$a = 1");
        assertEquals(1L, c.getVarMap().get("$a"));
        c.clear();
        assertEquals(null, c.getVarMap().get("$a"));
    }
    @Test
    public void testClearReleasesGlobalVariable_NPEIn1_7_2() {
        c.runScriptlet("$a = 1");
        assertEquals(1L, c.runScriptlet("$a"));
        assertEquals(1L, c.getVarMap().get("$a"));
        c.clear();
        assertEquals(null, c.runScriptlet("$a"));
    }
    @Test
    public void testClearReleasesGlobalVariable_FailsIn1_6_7_NPEIn1_7_2() {
        c.runScriptlet("$a = 1");
        assertEquals(1L, c.runScriptlet("$a"));
        c.clear();
        assertEquals(null, c.runScriptlet("$a"));
    }

}
