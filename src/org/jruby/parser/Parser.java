/*
 * Copyright (C) 2002 Anders Bengtsson
 * Copyright (C) 2004 Thomas E Enebo
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Thomas E Enebo <enebo@acm.org>
 * 
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.parser;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyFile;
import org.jruby.ast.Node;
import org.jruby.exceptions.SyntaxError;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;

/**
 * Serves as a simple facade for all the parsing magic.
 */
public class Parser {
    private final Ruby runtime;

    public Parser(Ruby runtime) {
        this.runtime = runtime;
    }

    public Node parse(String file, String content) {
        return parse(file, new StringReader(content));
    }

    public Node parse(String file, Reader content) {
        return parse(file, content, new RubyParserConfiguration());
    }

    private Node parse(String file, Reader content, RubyParserConfiguration config) {
        config.setLocalVariables(runtime.getScope().getLocalNames());
        
        DefaultRubyParser parser = null;
        RubyParserResult result = null;
        try {
            parser = RubyParserPool.getInstance().borrowParser();
            parser.setWarnings(runtime.getWarnings());
            parser.init(config);
            LexerSource lexerSource = LexerSource.getSource(file, content);
            result = parser.parse(lexerSource);
            if (result.isEndSeen()) {
            	runtime.defineGlobalConstant("DATA", new RubyFile(runtime, file, content));
            	result.setEndSeen(false);
            }
        } catch (SyntaxException e) {
            StringBuffer buffer = new StringBuffer(100);
            buffer.append(e.getPosition().getFile()).append(':');
            buffer.append(e.getPosition().getLine()).append(": ");
            buffer.append(e.getMessage());
            throw new SyntaxError(runtime, buffer.toString());
        } finally {
            RubyParserPool.getInstance().returnParser(parser);
        }

        if (hasNewLocalVariables(result)) {
            expandLocalVariables(result.getLocalVariables());
        }
        result.addAppendBeginAndEndNodes();
        return result.getAST();
    }

    private void expandLocalVariables(List localVariables) {
        int oldSize = 0;
        if (runtime.getScope().getLocalNames() != null) {
            oldSize = runtime.getScope().getLocalNames().size();
        }
        List newNames = localVariables.subList(oldSize, localVariables.size());
        runtime.getScope().addLocalVariables(newNames);
    }

    private boolean hasNewLocalVariables(RubyParserResult result) {
       int newSize = 0;
        if (result.getLocalVariables() != null) {
            newSize = result.getLocalVariables().size();
        }
        int oldSize = 0;
        if (runtime.getScope().hasLocalVariables()) {
            oldSize = runtime.getScope().getLocalNames().size();
        }
        return newSize > oldSize;
    }
}
