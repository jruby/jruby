/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import org.jcodings.Encoding;
import org.jruby.RubyRegexp;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.Match2CaptureNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.Node;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.SValue19Node;
import org.jruby.ast.SValueNode;
import org.jruby.ast.Splat19Node;
import org.jruby.ast.SplatNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.lexer.yacc.Token;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;

public class ParserSupport19 extends ParserSupport {
    @Override
    public AssignableNode assignable(Token lhs, Node value) {
        checkExpression(value);

        switch (lhs.getType()) {
            case Tokens.kSELF:
                throw new SyntaxException(PID.CANNOT_CHANGE_SELF, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't change the value of self");
            case Tokens.kNIL:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to nil", "nil");
            case Tokens.kTRUE:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to true", "true");
            case Tokens.kFALSE:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to false", "false");
            case Tokens.k__FILE__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __FILE__", "__FILE__");
            case Tokens.k__LINE__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __LINE__", "__LINE__");
            case Tokens.k__ENCODING__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __ENCODING__", "__ENCODING__");
            case Tokens.tIDENTIFIER:
                // ENEBO: 1.9 has CURR nodes for local/block variables.  We don't.  I believe we follow proper logic
                return currentScope.assign(lhs.getPosition(), (String) lhs.getValue(), makeNullNil(value));
            case Tokens.tCONSTANT:
                if (isInDef() || isInSingle()) {
                    throw new SyntaxException(PID.DYNAMIC_CONSTANT_ASSIGNMENT, lhs.getPosition(),
                            lexer.getCurrentLine(), "dynamic constant assignment");
                }
                return new ConstDeclNode(lhs.getPosition(), (String) lhs.getValue(), null, value);
            case Tokens.tIVAR:
                return new InstAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
            case Tokens.tCVAR:
                return new ClassVarAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
            case Tokens.tGVAR:
                return new GlobalAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
        }

        throw new SyntaxException(PID.BAD_IDENTIFIER, lhs.getPosition(), lexer.getCurrentLine(),
                "identifier " + (String) lhs.getValue() + " is not valid to set", lhs.getValue());
    }

    @Override
    public DStrNode createDStrNode(ISourcePosition position) {
        return new DStrNode(position, lexer.getEncoding());
    }

    @Override
    protected void getterIdentifierError(ISourcePosition position, String identifier) {
        throw new SyntaxException(PID.BAD_IDENTIFIER, position, "identifier " +
                identifier + " is not valid to get", identifier);
    }

    @Override
    public SplatNode newSplatNode(ISourcePosition position, Node node) {
        return new Splat19Node(position, makeNullNil(node));
    }

    @Override
    public SValueNode newSValueNode(ISourcePosition position, Node node) {
        return new SValue19Node(position, node);
    }

    private int[] allocateNamedLocals(RegexpNode regexpNode) {
        String[] names = regexpNode.loadPattern(configuration.getRuntime()).getNames();
        int length = names.length;
        int[] locals = new int[length];
        StaticScope scope = getCurrentScope();

        for (int i = 0; i < length; i++) {
            // TODO: Pass by non-local-varnamed things but make sure consistent with list we get from regexp

            int slot = scope.isDefined(names[i]);
            if (slot >= 0) {
                locals[i] = slot;
            } else {
                locals[i] = getCurrentScope().addVariableThisScope(names[i]);
            }
        }

        return locals;
    }

    private boolean is7BitASCII(ByteList value) {
        return StringSupport.codeRangeScan(value.getEncoding(), value) == StringSupport.CR_7BIT;
    }

    public void setRegexpEncoding(RegexpNode end, ByteList value) {
        RegexpOptions options = end.getOptions();
        Encoding optionsEncoding = options.setup19(configuration.getRuntime()) ;

        // Change encoding to one specified by regexp options as long as the string is compatible.
        if (optionsEncoding != null) {
            if (optionsEncoding != value.getEncoding() && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }

            value.setEncoding(optionsEncoding);
        } else if (options.isEncodingNone()) {
            if (value.getEncoding() == RubyYaccLexer.ASCII8BIT_ENCODING && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }
            value.setEncoding(RubyYaccLexer.ASCII8BIT_ENCODING);
        } else if (lexer.getEncoding() == RubyYaccLexer.USASCII_ENCODING) {
            if (!is7BitASCII(value)) {
                value.setEncoding(RubyYaccLexer.USASCII_ENCODING); // This will raise later
            } else {
                value.setEncoding(RubyYaccLexer.ASCII8BIT_ENCODING);
            }
        }
    }


    // TODO: Put somewhere more consolidated (similiar
    private char optionsEncodingChar(Encoding optionEncoding) {
        if (optionEncoding == RubyYaccLexer.USASCII_ENCODING) return 'n';
        if (optionEncoding == org.jcodings.specific.EUCJPEncoding.INSTANCE) return 'e';
        if (optionEncoding == org.jcodings.specific.SJISEncoding.INSTANCE) return 's';
        if (optionEncoding == RubyYaccLexer.UTF8_ENCODING) return 'u';

        return ' ';
    }

    protected void compileError(Encoding optionEncoding, Encoding encoding) {
        throw new SyntaxException(PID.REGEXP_ENCODING_MISMATCH, lexer.getPosition(), lexer.getCurrentLine(),
                "regexp encoding option '" + optionsEncodingChar(optionEncoding) +
                "' differs from source encoding '" + encoding + "'");
    }

    @Override
    public void regexpFragmentCheck(RegexpNode end, ByteList value) {
        setRegexpEncoding(end, value);
        RubyRegexp.preprocessCheck(configuration.getRuntime(), value);
    }

    @Override
    public Node getMatchNode(Node firstNode, Node secondNode) {
        if (firstNode instanceof DRegexpNode) {
            return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
        } else if (firstNode instanceof RegexpNode) {
            int[] locals = allocateNamedLocals((RegexpNode) firstNode);

            if (locals.length > 0) {
                return new Match2CaptureNode(firstNode.getPosition(), firstNode, secondNode, locals);
            } else {
                return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
            }
        } else if (secondNode instanceof DRegexpNode || secondNode instanceof RegexpNode) {
            return new Match3Node(firstNode.getPosition(), secondNode, firstNode);
        }

        return getOperatorCallNode(firstNode, "=~", secondNode);
    }

}
