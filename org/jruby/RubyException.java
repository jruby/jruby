/*
 * RubyException.java
 *
 * Created on 18. Oktober 2001, 23:31
 */

package org.jruby;

import org.jruby.exceptions.RubyArgumentException;


/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyException extends RubyObject {

	public RubyException(Ruby ruby, RubyClass rubyClass) {
		super(ruby, rubyClass);
	}

	public static RubyException newException(Ruby ruby, RubyClass excptnClass, String msg) {
		RubyException newException = new RubyException(ruby, excptnClass);
		newException.setInstanceVar("mesg", RubyString.m_newString(ruby, msg));
		return newException;
	}

	public static RubyException s_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
		RubyException newException = new RubyException(ruby, (RubyClass)recv);
		if (args.length == 1) {
			newException.setInstanceVar("mesg", args[0]);
		}
		return newException;
	}

	public static RubyException m_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
		return s_new(ruby, recv.getRubyClass(), args);
	}

	public RubyException m_exception(RubyObject[] args) {
		switch (args.length) {
			case 0 :
				return this;
			case 1 :
				return (RubyException) m_new(getRuby(), this, args);
			default :
				throw new RubyArgumentException(getRuby(), "Wrong argument count");
		}

	}

	public RubyString m_to_s() {
		RubyObject message = getInstanceVar("mesg");

		if (message.isNil()) {
			return getRubyClass().getClassPath();
		} else {
			message.setTaint(isTaint());

			return (RubyString) message;
		}
	}

	/** inspects an object and return a kind of debug information
	 * 
	 *@return A RubyString containing the debug information.
	 */
	public RubyString m_inspect() {
		RubyModule rubyClass = getRubyClass();

		RubyString exception = RubyString.stringValue(this);

		if (exception.getValue().length() == 0) {
			return rubyClass.getClassPath();
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append("#<");
			sb.append(rubyClass.getClassPath().getValue());
			sb.append(": ");
			sb.append(exception.getValue());
			sb.append(">");
			return RubyString.m_newString(getRuby(), sb.toString());
		}
	}
}