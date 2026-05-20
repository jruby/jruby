package org.jruby.embed.internal;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.variable.BiVariable.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Basically mirrors the {@link AbstractVariable} class hierarchy, but isn't
 * meant to be held as one instance per variable. Instead, there is now one
 * instance for each kind of variable.
 */
abstract class VariableAccessor {
	private final Pattern validNamePattern;
	private final boolean hasReceicer;
	private Type type;

	private static final VariableAccessor ARGV = new ArgvAccessor();
	private static final VariableAccessor LOCALGLOBAL = new LocalGlobalVariableAccessor();
	private static final VariableAccessor LOCAL = new LocalVariableAccessor();
	private static final VariableAccessor GLOBAL = new GlobalVariableAccessor();
	private static final VariableAccessor INSTANCE = new InstanceVariableAccessor();
	private static final VariableAccessor CLASS = new ClassVariableAccessor();
	private static final VariableAccessor CONSTANT = new ConstantAccessor();

	static List<VariableAccessor> getAll(LocalVariableBehavior behavior) {
		switch (behavior) {
		case GLOBAL:
			return List.of(ARGV, LOCALGLOBAL);
		case BSF:
			return List.of(ARGV, LOCAL, GLOBAL);
		case PERSISTENT:
		case TRANSIENT:
			return List.of(ARGV, GLOBAL, INSTANCE, CLASS, CONSTANT, LOCAL);
		default:
			throw new IllegalArgumentException(behavior.toString());
		}
	}

	static VariableAccessor getInstance(LocalVariableBehavior behavior, String name) {
		for (final VariableAccessor acc : getAll(behavior))
			if (acc.isValidName(name))
				return acc;
		return null;
	}

	VariableAccessor(final String validNamePattern, final boolean hasReceicer, final Type type) {
		this.validNamePattern = Pattern.compile(validNamePattern);
		this.hasReceicer = hasReceicer;
		this.type = type;
	}

	boolean isValidName(final String name) {
		if (name == null)
			return false;
		return validNamePattern.matcher(name).matches();
	}

	boolean hasReceiver() {
		return hasReceicer;
	}

	Type getType() {
		return type;
	}

	/**
	 * Tests whether the given variable exists.
	 * 
	 * @param runtime  the BiVariableMap object
	 * @param receiver a receiver object. <code>null</code> to set a value on the
	 *                 top self object. ignored for types of variable that aren't
	 *                 associated with a ruby object, such as local or global
	 *                 variables.
	 * @param name     name of the variable. must match {@link #isValidName(String)}
	 * @return <code>true</code> if a variable of the given name exists
	 */
	boolean containsKey(BiVariableMap map, IRubyObject receiver, String name) {
		// this is a separate method because keySet produces a large temporary list for
		// ConstantAccessor and LocalGlobalVariableAccessor. containsKey is overridden
		// for these two cases.
		return keySet(map, receiver).contains(name);
	}

	/**
	 * Lists all variables of the given type. The returned set may or may not be a
	 * copy and should never be modified externally. This operation may be somewhat
	 * expensive and can return a large result.
	 * 
	 * @param runtime  the BiVariableMap object
	 * @param receiver a receiver object. <code>null</code> to set a value on the
	 *                 top self object. ignored for types of variable that aren't
	 *                 associated with a ruby object, such as local or global
	 *                 variables.
	 * @return a Collection of variable names. need not actually be a Set
	 */
	abstract Collection<String> keySet(BiVariableMap map, IRubyObject receiver);

	/**
	 * Creates or updates a variable.
	 * 
	 * @param runtime  the BiVariableMap object
	 * @param receiver a receiver object. <code>null</code> to write a variable on
	 *                 the top self object. ignored for types of variable that
	 *                 aren't associated with a ruby object, such as local or global
	 *                 variables.
	 * @param name     name of the variable. must match {@link #isValidName(String)}
	 * @param value    new value of the variable
	 */
	abstract void inject(BiVariableMap map, IRubyObject receiver, String name, IRubyObject value);

	/**
	 * Reads the value of a variable. Reading nonexistent variables may cause a
	 * warning to be issued.
	 * 
	 * @param runtime  the BiVariableMap object
	 * @param receiver a receiver object. <code>null</code> to read a variable on
	 *                 the top self object. ignored for types of variable that
	 *                 aren't associated with a ruby object, such as local or global
	 *                 variables.
	 * @param name     name of the variable. must match {@link #isValidName(String)}
	 * @return the value of the variable. <code>null</code> if it is set to
	 *         {@code nil} or doesn't exist.
	 */
	abstract IRubyObject retrieve(BiVariableMap map, IRubyObject receiver, String name);

	/**
	 * Unsets / deletes a variable, or sets it to {@code nil}. Deleting a
	 * nonexistent global variable may actually end up creating it but with a nil
	 * value. FIXME that isn't very consistent!! Also, deletion is ignored if a
	 * variable doesn't exist – that, at least, is logical.
	 * 
	 * @param runtime  the BiVariableMap object
	 * @param receiver a receiver object. <code>null</code> to set a value on the
	 *                 top self object. ignored for types of variable that aren't
	 *                 associated with a ruby object, such as local or global
	 *                 variables.
	 * @param name     name of the variable. must match {@link #isValidName(String)}
	 */
	abstract void remove(BiVariableMap map, IRubyObject receiver, String name);
}
