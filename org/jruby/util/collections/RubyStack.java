package org.jruby.util.collections;

public class RubyStack {
    private ListObject firstStackObject = null;
    
    public RubyStack() {
    }

    public ListObject getTop() {
        return firstStackObject;
    }

    public void push(ListObject newStackObject) {
        newStackObject.setNext(firstStackObject);
        firstStackObject = newStackObject;
    }

    public ListObject pop() {
        if (!isEmpty()) {
        	ListObject result = firstStackObject;
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