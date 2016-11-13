/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
package org.jruby.truffle.parser.parser;

import org.jruby.RubyIO;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.parser.RubyWarnings;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.lexer.ByteListLexerSource;
import org.jruby.truffle.parser.lexer.GetsLexerSource;
import org.jruby.truffle.parser.lexer.LexerSource;
import org.jruby.truffle.parser.lexer.SyntaxException;
import org.jruby.truffle.parser.scope.DynamicScope;
import org.jruby.util.ByteList;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Serves as a simple facade for all the parsing magic.
 */
public class Parser {
    private final RubyContext context;
    private volatile long totalTime;
    private volatile int totalBytes;

    public Parser(RubyContext context) {
        this.context = context;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    @SuppressWarnings("unchecked")
    public ParseNode parse(String file, ByteList content, DynamicScope blockScope,
                           ParserConfiguration configuration) {
        configuration.setDefaultEncoding(content.getEncoding());
        List<ByteList> list = getLines(configuration, file);
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), content, list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    @SuppressWarnings("unchecked")
    public ParseNode parse(String file, byte[] content, DynamicScope blockScope,
                           ParserConfiguration configuration) {
        List<ByteList> list = getLines(configuration, file);
        ByteList in = new ByteList(content, configuration.getDefaultEncoding());
        LexerSource lexerSource = new ByteListLexerSource(file, configuration.getLineNumber(), in,  list);
        return parse(file, lexerSource, blockScope, configuration);
    }

    @SuppressWarnings("unchecked")
    public ParseNode parse(String file, InputStream content, DynamicScope blockScope,
                           ParserConfiguration configuration) {
        if (content instanceof LoadServiceResourceInputStream) {
            return parse(file, ((LoadServiceResourceInputStream) content).getBytes(), blockScope, configuration);
        } else {
            List<ByteList> list = getLines(configuration, file);
            RubyIO io = null;
            /*if (content instanceof FileInputStream) {
                io = new RubyFile(context.getJRubyRuntime(), file, ((FileInputStream) content).getChannel());
            } else {
                io = RubyIO.newIO(context.getJRubyRuntime(), Channels.newChannel(content));
            }*/
            LexerSource lexerSource = new GetsLexerSource(file, configuration.getLineNumber(), io, list, configuration.getDefaultEncoding());
            return parse(file, lexerSource, blockScope, configuration);
        }
    }

    @SuppressWarnings("unchecked")
    public ParseNode parse(String file, LexerSource lexerSource, DynamicScope blockScope,
                           ParserConfiguration configuration) {
        // We only need to pass in current scope if we are evaluating as a block (which
        // is only done for evals).  We need to pass this in so that we can appropriately scope
        // down to captured scopes when we are parsing.
        if (blockScope != null) {
            configuration.parseAsBlock(blockScope);
        }

        long startTime = System.nanoTime();
        RubyParser parser = new RubyParser(context, lexerSource, new RubyWarnings(configuration.getContext()));
        RubyParserResult result;
        try {
            result = parser.parse(configuration);
        } catch (IOException e) {
            // Enebo: We may want to change this error to be more specific,
            // but I am not sure which conditions leads to this...so lame message.
            throw new RaiseException(context.getCoreExceptions().syntaxError("Problem reading source: " + e, null));
        } catch (SyntaxException e) {
            switch (e.getPid()) {
                case UNKNOWN_ENCODING:
                case NOT_ASCII_COMPATIBLE:
                    throw new RaiseException(context.getCoreExceptions().argumentError(e.getMessage(), null));
                default:
                    StringBuilder buffer = new StringBuilder(100);
                    buffer.append(e.getFile()).append(':');
                    buffer.append(e.getLine() + 1).append(": ");
                    buffer.append(e.getMessage());

                    throw new RaiseException(context.getCoreExceptions().syntaxError(buffer.toString(), null));
            }
        } 
        
        // If variables were added then we may need to grow the dynamic scope to match the static
        // one.
        // FIXME: Make this so we only need to check this for blockScope != null.  We cannot
        // currently since we create the DynamicScope for a LocalStaticScope before parse begins.
        // Refactoring should make this fixable.
        if (result.getScope() != null) {
            result.getScope().growIfNeeded();
        }

        ParseNode ast = result.getAST();
        
        totalTime += System.nanoTime() - startTime;
        totalBytes += lexerSource.getOffset();

        return ast;
    }

    private List<ByteList> getLines(ParserConfiguration configuration, String file) {
        return null;
    }

}
