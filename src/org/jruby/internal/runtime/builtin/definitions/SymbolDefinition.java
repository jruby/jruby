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

public class SymbolDefinition extends ClassDefinition {
private static final int SYMBOL = 0xf000;
private static final int STATIC = SYMBOL | 0x100;
public static final int TO_I = SYMBOL | 1;
public static final int TO_S = SYMBOL | 2;
public static final int EQUAL = SYMBOL | 3;
public static final int HASH = SYMBOL | 4;
public static final int INSPECT = SYMBOL | 5;
public static final int RBCLONE = SYMBOL | 6;
public static final int FREEZE = SYMBOL | 7;
public static final int TAINT = SYMBOL | 8;

public SymbolDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
return runtime.defineClass("Symbol", (RubyClass) runtime.getClasses().getClass("Object"));
}

protected void defineMethods(MethodContext context) {
context.create("to_i", TO_I, 0);
context.create("to_int", TO_I, 0);
context.create("to_s", TO_S, 0);
context.create("id2name", TO_S, 0);
context.create("==", EQUAL, 1);
context.create("hash", HASH, 0);
context.create("inspect", INSPECT, 0);
context.create("clone", RBCLONE, 0);
context.create("dup", RBCLONE, 0);
context.create("freeze", FREEZE, 0);
context.create("taint", TAINT, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.undefineMethod("new");
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
default :
Asserts.notReached();
return null;
}
}
}
