package org.jruby.ast.visitor.rewriter;

import org.jruby.ast.visitor.rewriter.utils.Indentor;

public interface FormatHelper {

	public abstract Indentor getIndentor();

	public abstract String getListSeparator();

	public abstract String beforeCallArguments();

	public abstract String afterCallArguments();

	public abstract String beforeMethodArguments();

	public abstract String afterMethodArguments();

	public abstract String hashAssignment();

	public abstract String beforeHashContent();

	public abstract String afterHashContent();

	public abstract String matchOperator();

	public abstract String beforeAssignment();

	public abstract String beforeIterBrackets();

	public abstract String afterAssignment();

	public abstract String beforeIterVars();

	public abstract String afterIterVars();

	public abstract String beforeClosingIterBrackets();

	public abstract String classBodyElementsSeparator();

	public abstract String getLineDelimiter();
}
