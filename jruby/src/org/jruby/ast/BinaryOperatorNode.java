/*
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.ast;

/**
 * @author cnutter
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface BinaryOperatorNode {
	/**
	 * Gets the firstNode.
	 * @return Returns a Node
	 */
	public abstract Node getFirstNode();

	/**
	 * Gets the secondNode.
	 * @return Returns a Node
	 */
	public abstract Node getSecondNode();
}