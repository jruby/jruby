/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.channels.Channels;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubySymbol;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.LineStubVisitor;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;
import org.jruby.lexer.ByteListLexerSource;
import org.jruby.lexer.GetsLexerSource;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.parser.ParserType.*;

/**
 * Serves as a simple facade for all the parsing magic.
 */
public class Parser {
    protected final Ruby runtime;

    public Parser(Ruby runtime) {
        this.runtime = runtime;
    }

    public ParseResult parse(String fileName, int lineNumber, ByteList content, DynamicScope existingScope, ParserType type) {
        return parse(new ByteListLexerSource(fileName, lineNumber, content, getLines(type == EVAL, fileName, -1)),
                existingScope, type);
    }

    protected ParseResult parse(String fileName, int lineNumber, InputStream in, Encoding encoding,
                             DynamicScope existingScope, ParserType type) {
        var list = getLines(type == EVAL, fileName, -1);

        if (in instanceof LoadServiceResourceInputStream) {
            ByteList source = new ByteList(((LoadServiceResourceInputStream) in).getBytes(), encoding);
            LexerSource lexerSource = new ByteListLexerSource(fileName, lineNumber, source, list);
            return parse(lexerSource, existingScope, type);
        } else {
            boolean requiresClosing = false;
            RubyIO io;
            if (in instanceof FileInputStream) {
                io = new RubyFile(runtime, fileName, ((FileInputStream) in).getChannel());
            } else {
                requiresClosing = true;
                io = RubyIO.newIO(runtime, Channels.newChannel(in));
            }
            LexerSource lexerSource = new GetsLexerSource(fileName, lineNumber, io, list, encoding);

            try {
                return parse(lexerSource, existingScope, type);
            } finally {
                var context = runtime.getCurrentContext();
                if (requiresClosing && objectClass(context).getConstantAt(context, "DATA") != io) io.close();

                // In case of GetsLexerSource we actually will dispatch to gets which will increment $.
                // We do not want that in the case of raw parsing.
                runtime.setCurrentLine(0);
            }
        }
    }

    private ParseResult parse(LexerSource lexerSource, DynamicScope existingScope, ParserType type) {
        RubyParser parser = new RubyParser(runtime, lexerSource, existingScope, type);
        RubyParserResult result;
        try {
            result = parser.parse();
            if (parser.isEndSeen() && type == ParserType.MAIN) runtime.defineDATA(lexerSource.getRemainingAsIO());
        } catch (IOException e) {
            throw runtime.newSyntaxError("Problem reading source: " + e, existingScope.getStaticScope().getFile());
        } catch (SyntaxException e) {
            throw runtime.newSyntaxError(e.getFile() + ":" + (e.getLine() + 1) + ": " + e.getMessage(), e.getFile());
        }

        runtime.getParserManager().getParserStats().addParsedBytes(lexerSource.getOffset());

        return (ParseResult) result.getAST();
    }

    @Deprecated
    public Node parse(String file, ByteList content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        configuration.setDefaultEncoding(content.getEncoding());
        var list = getLines(configuration.isEvalParse(), file, -1);
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), content, list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    @Deprecated
    public Node parse(String file, byte[] content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        var list = getLines(configuration.isEvalParse(), file, -1);
        ByteList in = new ByteList(content, configuration.getDefaultEncoding());
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), in,  list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    @Deprecated
    public Node parse(String file, InputStream content, DynamicScope blockScope,
            ParserConfiguration configuration) {
        if (content instanceof LoadServiceResourceInputStream) {
            return parse(file, ((LoadServiceResourceInputStream) content).getBytes(), blockScope, configuration);
        } else {
            var list = getLines(configuration.isEvalParse(), file, -1);
            boolean requiresClosing = false;
            RubyIO io;
            if (content instanceof FileInputStream) {
                io = new RubyFile(runtime, file, ((FileInputStream) content).getChannel());
            } else {
                requiresClosing = true;
                io = RubyIO.newIO(runtime, Channels.newChannel(content));
            }
            LexerSource lexerSource = new GetsLexerSource(file, configuration.getLineNumber(), io, list, configuration.getDefaultEncoding());

            try {
                return parse(file, lexerSource, blockScope, configuration);
            } finally {
                var context = runtime.getCurrentContext();
                if (requiresClosing && objectClass(context).getConstantAt(context, "DATA") != io) io.close();

                // In case of GetsLexerSource we actually will dispatch to gets which will increment $.
                // We do not want that in the case of raw parsing.
                runtime.setCurrentLine(0);
            }
        }
    }

    @Deprecated
    public Node parse(String file, LexerSource lexerSource, DynamicScope blockScope,
            ParserConfiguration configuration) {
        // We only need to pass in current scope if we are evaluating as a block (which
        // is only done for evals).  We need to pass this in so that we can appropriately scope
        // down to captured scopes when we are parsing.
        if (blockScope != null) {
            configuration.parseAsBlock(blockScope);
        }

        ParserType type = configuration.isEvalParse() ? EVAL :
                configuration.isInlineSource() ? INLINE :
                        configuration.isSaveData() ? MAIN :
                                NORMAL;

        RubyParser parser = new RubyParser(runtime, lexerSource, blockScope, type);
        RubyParserResult result;
        try {
            result = parser.parse();
            if (parser.lexer.isEndSeen() && configuration.isSaveData()) {
                IRubyObject verbose = runtime.getVerbose();
                runtime.setVerbose(runtime.getNil());
                runtime.defineGlobalConstant("DATA", lexerSource.getRemainingAsIO());
                runtime.setVerbose(verbose);
            }
        } catch (IOException e) {
            // Enebo: We may want to change this error to be more specific,
            // but I am not sure which conditions leads to this...so lame message.
            throw runtime.newSyntaxError("Problem reading source: " + e, file);
        } catch (SyntaxException e) {
            throw runtime.newSyntaxError(e.getFile() + ":" + (e.getLine() + 1) + ": " + e.getMessage(), e.getFile());
        }

        return result.getAST();
    }

    protected RubyArray<?> getLines(boolean isEvalParse, String file, int length) {
        if (isEvalParse && !runtime.getCoverageData().isEvalCovered()) return null;

        var context = runtime.getCurrentContext();
        IRubyObject scriptLines = objectClass(context).getConstantAt(context, "SCRIPT_LINES__");
        if (!(scriptLines instanceof RubyHash hash)) return null;

        var list = length == -1 ? newEmptyArray(context) : newArray(context, length);
        hash.op_aset(context, newString(context, file), list);
        return list;
    }

    public IRubyObject getLineStub(ThreadContext context, ParseResult result, int lineCount) {
        var lines = newArray(context);
        LineStubVisitor lineVisitor = new LineStubVisitor(context.runtime, lines);
        lineVisitor.visitRootNode(((RootNode) result));

        for (int i = 0; i <= lineCount - lines.size(); i++) {
            lines.append(context, context.nil);
        }
        return lines;
    }

    public ParseResult addGetsLoop(Ruby runtime, ParseResult oldRoot, boolean printing, boolean processLineEndings, boolean split) {
        int line = oldRoot.getLine();
        BlockNode newBody = new BlockNode(line);

        if (processLineEndings) {
            RubySymbol dollarSlash = runtime.newSymbol(CommonByteLists.DOLLAR_SLASH);
            newBody.add(new GlobalAsgnNode(line, runtime.newSymbol(CommonByteLists.DOLLAR_BACKSLASH), new GlobalVarNode(line, dollarSlash)));
        }

        GlobalVarNode dollarUnderscore = new GlobalVarNode(line, runtime.newSymbol("$_"));

        BlockNode whileBody = new BlockNode(line);
        newBody.add(new WhileNode(line, new VCallNode(line, runtime.newSymbol("gets")), whileBody));

        if (processLineEndings) whileBody.add(new CallNode(line, dollarUnderscore, runtime.newSymbol("chomp!"), null, null, false));
        if (split) whileBody.add(new GlobalAsgnNode(line, runtime.newSymbol("$F"), new CallNode(line, dollarUnderscore, runtime.newSymbol("split"), null, null, false)));

        if (((RootNode) oldRoot).getBodyNode() instanceof BlockNode) {   // common case n stmts
            whileBody.addAll(((BlockNode) ((RootNode) oldRoot).getBodyNode()));
        } else {                                            // single expr script
            whileBody.add(((RootNode) oldRoot).getBodyNode());
        }

        if (printing) whileBody.add(new FCallNode(line, runtime.newSymbol("print"), new ArrayNode(line, dollarUnderscore), null));

        return new RootNode(line, oldRoot.getDynamicScope(), newBody, oldRoot.getFile());
    }

}
