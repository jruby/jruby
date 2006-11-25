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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sf.net>
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
package org.jruby.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyException;
import org.jruby.RubyString;

public class TestRubyException extends TestCase {

	private IRuby interpreter;
	private RubyException exception;

	public void setUp() {
		interpreter = Ruby.getDefaultInstance();
		exception = new RubyException(interpreter, interpreter.getClass("StandardError"), "test");
	}

	public void testPrintBacktraceWithHiddenLevels() throws Exception {
		setBackTrace(19);
		
		String[] lines = printError();
        
		assertEquals(expectedTraceLine(1), lines[0]);
		assertEquals("\t ... 7 levels...", lines[RubyException.TRACE_HEAD]);
		assertEquals(expectedTraceLine(16), lines[RubyException.TRACE_HEAD + 1]);
	}
	
	public void testPrintBacktrace() throws Exception {
		setBackTrace(18);
		
		String[] lines = printError();
		
		assertEquals(expectedTraceLine(1), lines[0]);
		assertEquals(expectedTraceLine(RubyException.TRACE_HEAD + 1), lines[RubyException.TRACE_HEAD]);
	}

	public void testPrintNilBacktrace() throws Exception {
	    exception.set_backtrace(interpreter.getNil());
		
		String[] lines = printError();
		
		assertEquals(0, lines.length);
	}

	private String[] printError() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
		PrintStream stream = new PrintStream(byteArrayOutputStream);
		exception.printBacktrace(stream);
		String output = new String(byteArrayOutputStream.toByteArray());
		if (output.trim().length() == 0) {
		    return new String[0];
	    } else {
		    return output.split("\n");
        }
	}

	private void setBackTrace(int lineCount) {
		List traceLines = new ArrayList();
		for (int i=0; i<lineCount; i++)
			traceLines.add(RubyString.newString(interpreter, testLine(i)));
		exception.set_backtrace(RubyArray.newArray(interpreter, traceLines));
	}
	
	private String expectedTraceLine(int index) {
		return "\tfrom " + testLine(index);
	}

	private String testLine(int i) {
		return "Line " + i;
	}
}
