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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jruby.common.NullWarnings;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.RubyParserPool;

public class SourceRewriteTester extends TestSuite {
	
	private static final String testRegexp = "^##!(.*)\\s*(\\w*)*$";
	private static final String resultRegexp = "^##=.*$";
	
	// replace with an enum as soon as we can use 1.5
	private static class MatcherState {
		public static final int SKIP = 0;
		public static final int IN_SOURCE = 1;
		public static final int IN_EXPECTED_RESULT= 2;
	}
	
	public static String generateSource(String original) {
		if(original.equals(""))
			return original;
		DefaultRubyParser parser = RubyParserPool.getInstance().borrowParser();
		parser.setWarnings(new NullWarnings());
		
		LexerSource lexerSource = new LexerSource("", new StringReader(original), 0, true);
		StringWriter outputWriter = new StringWriter();
		ReWriteVisitor visitor = new ReWriteVisitor(outputWriter, original);
		parser.parse(new RubyParserConfiguration(), lexerSource).getAST().accept(visitor);
		visitor.flushStream();
		RubyParserPool.getInstance().returnParser(parser);
		return outputWriter.getBuffer().toString();
	}
	
	private static BufferedReader createReader(String file) throws FileNotFoundException {
		return new BufferedReader(new FileReader(file));
	}
	
	public static Test suite() throws Exception {
		
		BufferedReader in = createReader("test/org/jruby/ast/visitor/rewriter/TestReWriteVisitorSource.txt");
		
		ArrayList testCases = createTests(in);

		return createSuite(testCases); 
	}

	private static TestSuite createSuite(ArrayList testCases) {
		TestSuite suite = new TestSuite();
		Iterator it = testCases.iterator();
		while(it.hasNext()) {
			SourceTestCase subject = (SourceTestCase)it.next();
			subject.setGeneratedSource(generateSource(subject.getSource()));
			suite.addTest(subject);
		}
		return suite;
	}

	
	private static boolean lineMatchesBeginOfTest(String line) {
		return createMatcherFromString(testRegexp, line).find();
	}

	private static Matcher createMatcherFromString(String pattern, String line) {
		return Pattern.compile(pattern).matcher(line);
	}
	
	private static String getNameOfTest(String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(testRegexp, line);
		if(matcherBeginOfTest.find())
			return matcherBeginOfTest.group(1);
		else
			return "Not Named";
	}
	
	private static boolean lineMatchesBeginOfResult(String line) {
		return createMatcherFromString(resultRegexp, line).find();
	}

	private static ArrayList createTests(BufferedReader inputReader) throws Exception {
		
		String line;
		SourceTestCase tc = null;
		int matcherState = MatcherState.SKIP;
		ArrayList testCases = new ArrayList();
		
		while ((line = inputReader.readLine()) != null){
			
			if(lineMatchesBeginOfTest(line)) {
				matcherState = MatcherState.IN_SOURCE;
				tc = new SourceTestCase(getNameOfTest(line), true);
				testCases.add(tc);
				continue;
			}	else if (lineMatchesBeginOfResult(line)) {
				matcherState = MatcherState.IN_EXPECTED_RESULT;
				continue;
			}
			
			switch(matcherState) {
			case MatcherState.IN_SOURCE:
				tc.appendLineToSource(line);
				break;
			case MatcherState.IN_EXPECTED_RESULT:
				tc.appendLineToExpectedResult(line);
				break;
			case MatcherState.SKIP:
				break;
			}
		}
		return testCases;
	}
}



