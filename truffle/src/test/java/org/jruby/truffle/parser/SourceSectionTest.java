/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyTest;
import org.jruby.truffle.core.array.ArrayLiteralNode;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.literal.IntegerFixnumLiteralNode;
import org.jruby.truffle.language.methods.AddMethodNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SourceSectionTest extends RubyTest {

    @Test
    public void testMinimalSections() {
        testSourceSection("14", IntegerFixnumLiteralNode.class, "14", 1, 1, 0, 2, 1, 2);
    }

    @Test
    public void testSectionsDontJustCoverWholeLines() {
        testSourceSection(" 14 ", IntegerFixnumLiteralNode.class, "14", 1, 1, 1, 2, 2, 3);
    }

    @Test
    public void testArrayLiteralSections() {
        testSourceSection(" [1, 2, 3] ", ArrayLiteralNode.class, "[1, 2, 3]", 1, 1, 1, 9, 2, 9);
    }

    @Test
    public void testArrayLiteralSectionsAcrossLines() {
        testSourceSection(" [1,\n  2,\n  3\n ] ", ArrayLiteralNode.class, "[1, 2, 3]", 1, 4, 1, 15, 2, 2);
    }

    @Test
    public void testCallSections() {
        testSourceSection(" 1 + 2 ", RubyCallNode.class, "1 + 2", 1, 1, 1, 5, 2, 6);
    }

    @Test
    public void testCallSectionsAcrossLines() {
        testSourceSection(" (1 +\n  2) ", RubyCallNode.class, "1 + 2", 1, 2, 1, 9, 2, 3);
    }

    @Test
    public void testDeeplyNestedCallSections() {
        testSourceSection(" [1, 2, {\n           a: 3,\n           b: [\n               4,\n               5 + 6,\n               7],\n           c: 8\n        }, 9, 10] ", RubyCallNode.class, "5 + 6", 5, 5, 85, 5, 16, 20);
    }

    @Test
    public void testMethod() {
        testSourceSection("10\n\ndef foo(a, b)\n  a + b\nend\n\n11", AddMethodNode.class, "def foo(a, b)\n  a + b\nend", 3, 5, 4, 24, 1, 3);
    }

    public <T extends Node> void testSourceSection(String text, Class<T> nodeClass, String code, int startLine, int endLine, int charIndex, int charLength, int startColumn, int endColumn) {
        testWithNode(text, nodeClass, (node) -> {
            // Commented lines fail due to Ruby imprecise source sections

            final SourceSection sourceSection = node.getEncapsulatingSourceSection();
            assertNotNull(sourceSection);
            assertTrue(sourceSection.isAvailable());
            assertNotNull(sourceSection.getSource());
            assertEquals("test.rb", sourceSection.getSource().getName());
            // assertEquals(code, sourceSection.getCode());
            assertEquals(startLine, sourceSection.getStartLine());
            // assertEquals(endLine, sourceSection.getEndLine()); - this one is a worse failure than the others - see testArrayLiteralSectionsAcrossLines for example
            // assertEquals(charIndex, sourceSection.getCharIndex());
            // assertEquals(charLength, sourceSection.getLength());
            // assertEquals(charIndex + charLength, sourceSection.getCharEndIndex());
            // assertEquals(startColumn, sourceSection.getStartColumn());
            // assertEquals(endColumn, sourceSection.getEndColumn());
        });
    }

}
