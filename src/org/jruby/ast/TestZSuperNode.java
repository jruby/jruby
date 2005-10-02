package org.jruby.ast;

import java.util.Collections;

import junit.framework.TestCase;

public class TestZSuperNode extends TestCase {

    public void testChildNodes() {
        ZSuperNode superNode = new ZSuperNode(null);
        assertEquals(Collections.EMPTY_LIST, superNode.childNodes());
    }
}
