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

import org.jruby.ast.visitor.rewriter.utils.Indentor;

public class DefaultFormatHelper implements FormatHelper {
	
	private static final String empty = "";
	private static final String oneSpace = " ";
	Indentor indentor = new Indentor(2, ' ');

	public String getListSeparator() {
		return ", ";
	}

	public String beforeCallArguments() {
		return oneSpace;
	}

	public String afterCallArguments() {
		return empty;
	}

	public String beforeMethodArguments() {
		return oneSpace;
	}

	public String afterMethodArguments() {
		return empty;
	}
	
	public String hashAssignment() {
		return " => ";
	}

	public String beforeHashContent() {
		return empty;
	}

	public String afterHashContent() {
		return empty;
	}
	
	public String matchOperator() {
		return " =~ ";
	}

	public String beforeAssignment() {
		return oneSpace;
	}

	public String beforeIterBrackets() {
		return oneSpace;
	}
	
	public String afterAssignment() {
		return oneSpace;
	}

	public String beforeIterVars() {
		return empty;
	}

	public String afterIterVars() {
		return oneSpace;
	}

	public String beforeClosingIterBrackets() {
		return empty;
	}

	public String classBodyElementsSeparator() {
		return empty;
	}

	public Indentor getIndentor() {
		return indentor;
	}

	public String getLineDelimiter() {
		return "\n";
	}

}