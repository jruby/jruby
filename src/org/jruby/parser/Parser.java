/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

/**
 * Serves as a simple facade for all the parsing magic.
 */

package org.jruby.parser;

import org.ablaf.ast.INode;
import org.ablaf.parser.IParser;
import org.ablaf.parser.IParserPool;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.LexerFactory;
import org.jruby.Ruby;

import java.util.ArrayList;
import java.util.List;
import java.io.Reader;
import java.io.StringReader;

public class Parser {
    private final Ruby runtime;
    private final IParserPool pool;

    public Parser(Ruby ruby) {
        this.runtime = ruby;
        this.pool = RubyParserPool.getInstance();
    }

    public INode parse(String file, String content) {
        return parse(file, new StringReader(content));
    }

    public INode parse(String file, Reader content) {
        return parse(file, content, new RubyParserConfiguration());
    }

    public INode parse(String file, String content, List blockVariableNames) {
        RubyParserConfiguration config = new RubyParserConfiguration();
        config.setBlockVariables(blockVariableNames);
        return parse(file, new StringReader(content), config);
    }

    private INode parse(String file, Reader content, RubyParserConfiguration config) {
        config.setLocalVariables(runtime.getScope().getLocalNames());
        
        IParser parser = null;
        IRubyParserResult result = null;
        try {
            parser = pool.borrowParser();
            parser.setErrorHandler(runtime.getErrorHandler());
            parser.init(config);
            ILexerSource lexerSource = LexerFactory.getInstance().getSource(file, content);
            result = (IRubyParserResult) parser.parse(lexerSource);
        } finally {
            pool.returnParser(parser);
        }

        if (hasNewLocalVariables(result)) {
            runtime.getScope().setLocalNames(new ArrayList(result.getLocalVariables()));
        }
        return result.getAST();
    }

    private boolean hasNewLocalVariables(IRubyParserResult result) {
        int newSize = 0;
        if (result.getLocalVariables() != null) {
            newSize = result.getLocalVariables().size();
        }
        int oldSize = 0;
        if (runtime.getScope().getLocalNames() != null) {
            oldSize = runtime.getScope().getLocalNames().size();
        }
        return newSize > oldSize;
    }
}
