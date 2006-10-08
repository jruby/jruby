/*
 * Created on Sep 21, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.util.collections;

public class SinglyLinkedList {
    Object value;
    SinglyLinkedList next;
    
    public SinglyLinkedList(Object value, SinglyLinkedList next) {
        this.value = value;
        this.next = next;
    }
    
    public Object getValue() {
        return value;
    }
    
    public SinglyLinkedList getNext() {
        return next;
    }

    public void setNext(SinglyLinkedList next) {
        this.next = next;
    }
}
