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

package org.jruby.ast.visitor.rewriter.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jruby.ast.visitor.rewriter.FormatHelper;
import org.jruby.lexer.yacc.ISourcePosition;


public class ReWriterContext {
	
	private final String source;
	private final CallDepth callDepth = new CallDepth();
	private final LocalVariables localVariables = new LocalVariables();
	private final BooleanStateStack printQuotesInString = new BooleanStateStack(true, true);
	private HereDocument hereDocument;
	private boolean skipNextNewline = true;
	private PrintWriter output;
	private FormatHelper formatHelper;

	private ISourcePosition lastPosition;
	
	public LocalVariables getLocalVariables() {
		return localVariables;
	}

	public ReWriterContext(PrintWriter output, String source, FormatHelper formatHelper) {
		super();
		this.output = output;
		this.source = source;
		this.formatHelper = formatHelper;
	}

	public ReWriterContext(StringWriter output, String source, FormatHelper formatHelper){
		this(new PrintWriter(output), source, formatHelper);
	}
	
	public CallDepth getCallDepth() {
		return callDepth;
	}

	public String getSource() {
		return source;
	}

	public Indentor getIndentor() {
		return formatHelper.getIndentor();
	}

	public ISourcePosition getLastPosition() {
		return lastPosition;
	}

	public void setLastPosition(ISourcePosition lastPosition) {
		this.lastPosition = lastPosition;
	}
	
	public BooleanStateStack getPrintQuotesInString() {
		return printQuotesInString;
	}
	
	public boolean hasHereDocument() {
		return hereDocument != null;
	}

	public HereDocument fetchHereDocument() {
		HereDocument hd =  hereDocument;
		hereDocument = null;
		return hd;
	}

	public void depositHereDocument(String hereDocument) {
		this.hereDocument = new HereDocument(hereDocument, this);
	}

	public boolean isSkipNextNewline() {
		return skipNextNewline;
	}

	public void setSkipNextNewline(boolean skipNextNewline) {
		this.skipNextNewline = skipNextNewline;
	}

	public PrintWriter getOutput() {
		return output;
	}

	public void setOutput(PrintWriter output) {
		this.output = output;
	}

	public FormatHelper getFormatHelper() {
		return formatHelper;
	}
}
