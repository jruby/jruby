package org.jruby.util.collections;

public class Stack {
    private StackObject firstStackObject = null;
    
    public Stack() {
    }

    public StackObject getStackObject() {
        return firstStackObject;
    }

    public void push(StackObject newStackObject) {
        newStackObject.setNext(firstStackObject);
        firstStackObject = newStackObject;
    }

    public StackObject pop() {
        if (!isEmpty()) {
        	StackObject result = firstStackObject;
            firstStackObject = firstStackObject.getNext();
            return result;
        } else {
            // Log bug
            return null;
        }
    }

    public boolean isEmpty() {
        return firstStackObject == null;
    }
}