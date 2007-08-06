/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
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

package org.jruby.ast.visitor.rewriter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jruby.ast.Node;
import org.jruby.common.NullWarnings;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.RubyParserPool;

public class SourceRewriterMain {
	
	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Please specify a sourcefile.");
			return;
		}

		DefaultRubyParser parser = RubyParserPool.getInstance().borrowParser();
		parser.setWarnings(new NullWarnings());

		LexerSource lexerSource = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
			lexerSource = new LexerSource(args[0], reader, 0, true);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find the file:");
			System.err.println(args[0]);
			return;
		}

		Node rootNode = parser.parse(new RubyParserConfiguration(), lexerSource).getAST();
		if (rootNode == null) {
			System.err.println("Source File seems to be empty.");
			return;
		}

		StringBuffer buffer = new StringBuffer();
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
            
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append('\n');
			}
		} catch (IOException e) {
			System.err.println("Could not read the Sourcefile.");
		}
        
		ReWriteVisitor visitor = new ReWriteVisitor(System.out, buffer.toString());
		rootNode.accept(visitor);
		visitor.flushStream();

		System.out.println("\n");
	}
}
