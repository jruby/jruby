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
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.lexer.yacc.DetailedSourcePosition;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.cli.Options;

public class DetailedSourcePositionTest extends TestCase {

    @Override
    public void setUp() {
        Options.PARSER_DETAILED_SOURCE_POSITIONS.force(Boolean.toString(true));
    }

    @Override
    public void tearDown() {
        Options.PARSER_DETAILED_SOURCE_POSITIONS.unforce();
    }

    public void testWholeFile() {
        final DetailedSourcePosition position = detailedSource(find(parse("14"), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        assertEquals(0, position.getOffset());
        //assertEquals(2, position.getLength());
    }

    public void testAtStartOfFile() {
        final DetailedSourcePosition position = detailedSource(find(parse("14  "), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        assertEquals(0, position.getOffset());
        //assertEquals(2, position.getLength());
    }

    public void testBlockNodeWrappingBegin() {
        final DetailedSourcePosition position = detailedSource(find(parse("BEGIN { p 'yo' };\n"), BlockNode.class));
        assertEquals("test", position.getFile());

        // Since the BlockNode is just a wrapper, it should have the same position as the PreExe19Node representing BEGIN.
        assertEquals(0, position.getLine());
        assertEquals(0, position.getOffset());
        assertEquals(5, position.getLength());
    }

    public void testAtEndOfFile() {
        final DetailedSourcePosition position = detailedSource(find(parse("  14"), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        assertEquals(2, position.getOffset());
        //assertEquals(2, position.getLength());
    }

    public void testInMiddleOfFile() {
        final DetailedSourcePosition position = detailedSource(find(parse("  14  "), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        assertEquals(2, position.getOffset());
        //assertEquals(2, position.getLength());
    }

    public void testMultiLineFixnum() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\n14\nfalse\n"), FixnumNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(5, position.getOffset());
        // assertEquals(2, position.getLength());
    }

    public void testSingleLineAssignment() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nx = 14\nfalse\n"), LocalAsgnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(2, position.getLine());
        assertEquals(7, position.getOffset()); // we would like this to be 5, but 7 is a good start
        // assertEquals(6, position.getLength());
    }

    public void testMultiLineAssignment() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nx = \n14\nfalse\n"), LocalAsgnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(3, position.getLine()); // we would say this is wrong - should be 1 - but we're interested in the offset here
        assertEquals(7, position.getOffset()); // we would like this to be 5, but 7 is a good start
        // assertEquals(7, position.getLength());
    }

    public void testSingleLineIf() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nif true; false else true end\nfalse\n"), IfNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(5, position.getOffset());
        // assertEquals(29, position.getLength());
    }

    public void testMultiLineIf() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nif true\n  false\nelse\n  true\nend\nfalse\n"), IfNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(5, position.getOffset());
        // assertEquals(31, position.getLength());
    }

    public void testSingleLineDef() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\ndef foo; true end\nfalse\n"), DefnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(5, position.getOffset());
        // assertEquals(18, position.getLength());
    }

    public void testMultiLineDef() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\ndef foo\n  true\nend\nfalse\n"), DefnNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(5, position.getOffset());
        // assertEquals(18, position.getLength());
    }

    public void testSingleLineCall() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nFoo.bar(true, false)\nfalse\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(8, position.getOffset()); // we would like this to be 5, but 8 is a good start
        // assertEquals(21, position.getLength());
    }

    public void testMultiLineCall() {
        final DetailedSourcePosition position = detailedSource(find(parse("true\nFoo.bar(\n  true,\n  false\n)\nfalse\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(1, position.getLine());
        assertEquals(8, position.getOffset()); // we would like this to be 5, but 8 is a good start
        // assertEquals(28, position.getLength());
    }

    // This is the test case which motivated the need for the new detailed source position implementation

    public void testRegression1() {
        final DetailedSourcePosition position = detailedSource(find(parse("p 42\n\n3.hello\n"), CallNode.class));
        assertEquals("test", position.getFile());
        assertEquals(2, position.getLine());
        assertEquals(6, position.getOffset());
        // assertEquals(7, position.getLength());
    }

    // Found during testing

    public void testRegression2() {
        final DetailedSourcePosition position = detailedSource(find(parse("__FILE__"), FileNode.class));
        assertEquals("test", position.getFile());
        assertEquals(0, position.getLine());
        assertEquals(8, position.getOffset()); // should be 0 - this is the central problem with the parser at the moment - asks the lexer for position after the token's parsed
        // assertEquals(8, position.getLength());
    }

    public void testSyntaxError() {
        try {
            parse("3.to_i(\n");
        } catch (org.jruby.exceptions.RaiseException e) {
            final String syntaxErrorMessage = e.getException().message.asJavaString();

            // There's no easy way to get at the source position information, but it is embedded in the syntax error message.
            assertEquals("test:1: syntax error, unexpected end-of-file\n", syntaxErrorMessage);
        }
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

    private DetailedSourcePosition detailedSource(Node node) {
        final ISourcePosition sourcePosition = node.getPosition();
        assertTrue(sourcePosition instanceof DetailedSourcePosition);
        return (DetailedSourcePosition) sourcePosition;
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
        final RubyInstanceConfig instanceConfiguration = new RubyInstanceConfig();
        instanceConfiguration.setDisableGems(true);
        final Ruby ruby = Ruby.newInstance(instanceConfiguration);
        final ParserConfiguration parserConfiguration = new org.jruby.parser.ParserConfiguration(ruby, 0, false, true, true);
        final StaticScope staticScope = ruby.getStaticScopeFactory().newLocalScope(null);
        final Parser parser = new org.jruby.parser.Parser(ruby);
        return (RootNode) parser.parse("test", source.getBytes(), new ManyVarsDynamicScope(staticScope), parserConfiguration);
    }

}
