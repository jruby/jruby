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

public class GCDefinition extends ModuleDefinition {
private static final int GC = 0xf000;
private static final int STATIC = GC | 0x100;
public static final int START = STATIC | 1;

public GCDefinition(Ruby runtime) {
super(runtime);
}

protected RubyModule createModule(Ruby runtime) {
return runtime.defineModule("GC");
}

protected void defineMethods(MethodContext context) {
}

protected void defineModuleFunctions(ModuleFunctionsContext context) {
context.create("start", START, 0);
context.create("garbage_collect", START, 0);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case START :
return org.jruby.RubyGC.start(receiver);
default :
Asserts.notReached();
return null;
}
}
}
