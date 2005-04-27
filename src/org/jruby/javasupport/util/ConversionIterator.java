/*
 * Created on Apr 24, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.javasupport.util;

import java.util.Iterator;

import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

public class ConversionIterator implements Iterator {
	private Iterator iterator;

	public ConversionIterator(Iterator iterator) {
		this.iterator = iterator;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Object next() {
		IRubyObject element = (IRubyObject) iterator.next();
		
		return JavaUtil.convertRubyToJava(element, Object.class); 
	}

	public void remove() {
		iterator.remove();
	}
}
