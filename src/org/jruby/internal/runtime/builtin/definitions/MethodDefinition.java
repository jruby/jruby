/* Generated code - do not edit! */

package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.runtime.builtin.definitions.ModuleDefinition;
import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.ModuleFunctionsContext;
import org.jruby.util.Asserts;

public class MethodDefinition extends ClassDefinition {
private static final int METHOD = 0xf000;
private static final int STATIC = METHOD | 0x100;
public static final int CALL = METHOD | 1;
public static final int ARITY = METHOD | 2;
public static final int TO_PROC = METHOD | 3;
public static final int UNBIND = METHOD | 4;

public MethodDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
RubyClass result = runtime.defineClass("Method", (RubyClass) runtime.getClasses().getClass("Object"));
return result;
}

protected void defineMethods(MethodContext context) {
context.createOptional("call", CALL, 0);
context.createOptional("[]", CALL, 0);
context.create("arity", ARITY, 0);
context.create("to_proc", TO_PROC, 0);
context.create("unbind", UNBIND, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
default :
Asserts.notReached();
return null;
}
}
}
