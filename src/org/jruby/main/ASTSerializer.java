package org.jruby.main;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

import org.ablaf.internal.common.NullErrorHandler;
import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.LexerFactory;
import org.ablaf.parser.IParser;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.DAsgnCurrNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.TrueNode;
import org.jruby.parser.IRubyParserResult;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.RubyParserPool;
import org.jruby.runtime.Visibility;
import org.jruby.util.Asserts;

/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ASTSerializer {
    public ASTSerializer() {
        super();
    }
    
    public static void serialize(File input, File output) throws IOException {
        Reader reader = new BufferedReader(new FileReader(input));
        RubyParserConfiguration config = new RubyParserConfiguration();
        config.setBlockVariables(new ArrayList());
        config.setLocalVariables(new ArrayList());
        
        IParser parser = null;
        IRubyParserResult result = null;
        try {
            parser = RubyParserPool.getInstance().borrowParser();
            parser.setErrorHandler(new NullErrorHandler());
            parser.init(config);
            ILexerSource lexerSource = LexerFactory.getInstance().getSource(input.toString(), reader);
            result = (IRubyParserResult) parser.parse(lexerSource);
        } finally {
            RubyParserPool.getInstance().returnParser(parser);
        }
        reader.close();
        
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
        out.writeObject(result.getAST());
        out.close();
    }

    public static void main(String[] args) throws IOException {
        if ("-f".equals(args[0])) {
            ASTSerializer.serialize(new File(args[1]), new File(args[1] + ".ast.ser"));
        } else if ("-d".equals(args[0])) {
            File file = new File(args[1]);
            
        } else {
            Asserts.notReached();
        }
    }
}