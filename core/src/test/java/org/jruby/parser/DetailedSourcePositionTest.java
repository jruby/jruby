/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.parser;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

public class DetailedSourcePositionTest extends TestCase {

    public void testSingleLineFixnum() {
        final SimpleSourcePosition position = detailedSource(find(parse("  14  "), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        // assertEquals(2, position.getOffset());
        // assertEquals(2, position.getLength());
    }

    public void testMultiLineFixnum() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\n14\nfalse\n"), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(2, position.getLength());
    }

    public void testSingleLineAssignment() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nx = 14\nfalse\n"), LocalAsgnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(6, position.getLength());
    }

    public void testMultiLineAssignment() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nx = \n14\nfalse\n"), LocalAsgnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(7, position.getLength());
    }

    public void testSingleLineIf() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nif true; false else true end\nfalse\n"), IfNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(29, position.getLength());
    }

    public void testMultiLineIf() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nif true\n  false\nelse\n  true\nend\nfalse\n"), IfNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(31, position.getLength());
    }

    public void testSingleLineDef() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\ndef foo; true end\nfalse\n"), DefnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(18, position.getLength());
    }

    public void testMultiLineDef() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\ndef foo\n  true\nend\nfalse\n"), DefnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(18, position.getLength());
    }

    public void testSingleLineCall() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nFoo.bar(true, false)\nfalse\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(21, position.getLength());
    }

    public void testMultiLineCall() {
        final SimpleSourcePosition position = detailedSource(find(parse("true\nFoo.bar(\n  true,\n  false\n)\nfalse\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        // assertEquals(5, position.getOffset());
        // assertEquals(28, position.getLength());
    }

    // This is the test case which motivated the need for the new detailed source position implementation

    public void testRegresion1() {
        final SimpleSourcePosition position = detailedSource(find(parse("p 42\n\n3.hello\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(2, position.getLine());
        // assertEquals(6, position.getOffset());
        // assertEquals(7, position.getLength());
    }

    private class FoundException extends RuntimeException {

        private final Node node;

        public FoundException(Node node) {
            this.node = node;
        }

        public Node getNode() {
            return node;
        }

    }

    private SimpleSourcePosition detailedSource(Node node) {
        final ISourcePosition sourcePosition = node.getPosition();
        assertTrue(sourcePosition instanceof SimpleSourcePosition);
        return (SimpleSourcePosition) sourcePosition;
    }

    private <T extends Node> T find(RootNode root, final Class<T> find) {
        try {
            root.accept(new AbstractNodeVisitor<Void>() {

                @Override
                protected Void defaultVisit(Node node) {
                    if (find.isAssignableFrom(node.getClass())) {
                        throw new FoundException(node);
                    }

                    visitChildren(node);

                    return null;
                }

            });
        } catch (FoundException e) {
            return (T) e.getNode();
        }

        fail();
        return null;
    }

    private RootNode parse(String source) {
        final Ruby ruby = Ruby.newInstance();
        final ParserConfiguration parserConfiguration = new org.jruby.parser.ParserConfiguration(ruby, 0, false, true, true);
        final StaticScope staticScope = ruby.getStaticScopeFactory().newLocalScope(null);
        final Parser parser = new org.jruby.parser.Parser(ruby);
        return (RootNode) parser.parse("test", source.getBytes(), new ManyVarsDynamicScope(staticScope), parserConfiguration);
    }

}
