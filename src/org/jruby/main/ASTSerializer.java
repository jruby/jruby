package org.jruby.main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;

import org.ablaf.ast.IAstEncoder;
import org.ablaf.internal.ast.XmlAstMarshal;
import org.ablaf.internal.common.NullErrorHandler;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.LexerFactory;
import org.ablaf.parser.IParser;
import org.jruby.ast.util.AstPersistenceDelegates;
import org.jruby.ast.util.RubyAstMarshal;
import org.jruby.parser.IRubyParserResult;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.RubyParserPool;

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
    
    public static void serialize(File input, File outputFile) throws IOException {
        OutputStream output = new FileOutputStream(outputFile);
        output = new BufferedOutputStream(output);
        IAstEncoder encoder = RubyAstMarshal.getInstance().openEncoder(output);
        serialize(input, encoder);
        encoder.close();
    }
    
    public static void serialize(File input, IAstEncoder encoder) throws IOException {
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
        
        encoder.writeNode(result.getAST());
    }

    public static void main(String[] args) throws IOException {
        XmlAstMarshal marshal = new XmlAstMarshal(AstPersistenceDelegates.get());
        IAstEncoder encoder = marshal.openEncoder(new BufferedOutputStream(System.out));
        serialize(new File(args[0]), encoder);
        encoder.close();
    }
}