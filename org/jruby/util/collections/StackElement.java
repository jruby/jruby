package org.jruby.util.collections;

public interface StackElement {
	public StackElement getNext();
	public void setNext(StackElement newNext);
}

