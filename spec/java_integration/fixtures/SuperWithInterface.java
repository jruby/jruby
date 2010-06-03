package java_integration.fixtures;

import java.util.Collection;
import java.util.Iterator;

public class SuperWithInterface implements Collection<Object> {
    private static class SubClassWithoutInterfaces extends SuperWithInterface implements Runnable {
        public void run() {}

	    // for testing that a single unique method on child does not cause all parent methods to disappear
	    protected boolean add(String s) {return false;}
	    public boolean add(Object s) {return true;}
    }

    public static SuperWithInterface getSubClassInstance() {
        return new SubClassWithoutInterfaces();
    }

    public static class SuperWithoutInterface {
	    // no interfaces, so we need to test that this doesn't cause
	    // the equivalent child method to get removed
    	public boolean add(Object s) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
    }

    public static class SubWithInterface extends SuperWithoutInterface implements Collection<Object> {
		private boolean add(String e) {
			return false;
		}
		
	    public boolean add(Object e) {
			return true;
	    }

    	// impl methods, just to have them
	    public int size() {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean isEmpty() {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean contains(Object o) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public Iterator<Object> iterator() {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public Object[] toArray() {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public <T> T[] toArray(T[] a) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean remove(Object o) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean containsAll(Collection<?> c) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean addAll(Collection<? extends Object> c) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean removeAll(Collection<?> c) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public boolean retainAll(Collection<?> c) {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }

	    public void clear() {
	        throw new UnsupportedOperationException("Not supported yet.");
	    }
}

    // impl methods, just to have them
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<Object> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean add(Object e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addAll(Collection<? extends Object> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
