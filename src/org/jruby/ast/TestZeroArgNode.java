package org.jruby.ast;

import java.util.Collections;

import junit.framework.TestCase;

public class TestZeroArgNode extends TestCase {
    public void testChildNodes() throws Exception {
        ZeroArgNode node = new ZeroArgNode(null);
        assertEquals(Collections.EMPTY_LIST, node.childNodes());
    }

}
