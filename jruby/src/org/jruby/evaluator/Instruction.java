/*
 * Created on Sep 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.evaluator;



public interface Instruction {
	public void execute(EvaluationState state, InstructionContext ctx);
}
