package org.jruby.util.string;

/**
 * UcharIterator - an Iterator on Unicode characters in a UTF-8 byte array.
 *
 * <p>A conventional Iterator, remove() is not supported, and there's an extra
 * nextChar() method that returns a naked int as opposed to a wrapped Integer.
 * </p>
 *
 * @see org.jruby.util.string.Ustr
 */

public class UcharIterator implements java.util.Iterator, java.io.Serializable {
    private static final long serialVersionUID = -2821982911687539515L;
    private Ustr u;
    private int next;
    
    /**
     * Creates a new UcharIterator starting at an offset in a buffer.
     *
     * @param s the byte array containing UTF-8-encoded Unicode characters.
     * @param offset how far into the array to start iterating.
     */
    public UcharIterator(byte[] s, int offset) {
        u = new Ustr(s, offset);
        u.prepareNext();
        next = u.nextChar();
    }
    
    /**
     * Tests whether there are any more characters in the buffer.
     *
     * @return true or false depending on whether there are more characters.
     */
    public boolean hasNext() {
        return (next != 0);
    }
    
    /**
     * Retrieve the next Unicode character from a UTF-8 byte buffer, wrapped
     * in an Integer object. Throws NoSuchElementException if hasNext
     * would return false.
     *
     * @return the next Unicode character as a java.lang.Integer
     * @throws NoSuchElementException
     */
    public Object next() {
        if (next == 0)
            throw new java.util.NoSuchElementException("Ran off end of array");
        Integer i = new Integer(next);
        next = u.nextChar();
        return i;
    }
    
    /**
     * Retrieve the next Unicode character from a UTF-8 byte buffer and return
     * it as an int.  Once the null-termination is hit, returns 0 as many times
     * as you want to call it.
     *
     * @return the next Unicode character as an int, 0 on end-of-string.
     */
    public int nextChar() {
        int i = next;
        if (i != 0)
            next = u.nextChar();
        return i;
    }
    
    /**
     * Throws an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException("UcharIterator doesn't remove");
    }
    
}
