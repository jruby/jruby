package org.jruby.util.collections;

public interface StackObject {
	public StackObject getNext();
	public void setNext(StackObject newNext);
}

