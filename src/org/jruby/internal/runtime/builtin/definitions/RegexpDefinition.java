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

public class RegexpDefinition extends ClassDefinition {
private static final int REGEXP = 0xf000;
private static final int STATIC = REGEXP | 0x100;
public static final int INITIALIZE = REGEXP | 1;
public static final int RBCLONE = REGEXP | 2;
public static final int EQUAL = REGEXP | 3;
public static final int MATCH_M = REGEXP | 4;
public static final int MATCH = REGEXP | 5;
public static final int MATCH2 = REGEXP | 6;
public static final int INSPECT = REGEXP | 7;
public static final int SOURCE = REGEXP | 8;
public static final int CASEFOLD = REGEXP | 9;
public static final int NEWINSTANCE = STATIC | 1;
public static final int QUOTE = STATIC | 2;
public static final int LAST_MATCH_S = STATIC | 3;

public RegexpDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
return runtime.defineClass("Regexp", (RubyClass) runtime.getClasses().getClass("Object"));
}

protected void defineMethods(MethodContext context) {
context.createOptional("initialize", INITIALIZE, 0);
context.create("clone", RBCLONE, 0);
context.create("==", EQUAL, 1);
context.create("match", MATCH_M, 1);
context.create("=~", MATCH, 1);
context.create("===", MATCH, 1);
context.create("~", MATCH2, 0);
context.create("inspect", INSPECT, 0);
context.create("source", SOURCE, 0);
context.create("casefold?", CASEFOLD, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.createOptional("new", NEWINSTANCE, 0);
context.createOptional("compile", NEWINSTANCE, 0);
context.create("quote", QUOTE, 1);
context.create("escape", QUOTE, 1);
context.create("last_match", LAST_MATCH_S, 0);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case NEWINSTANCE :
return org.jruby.RubyRegexp.newInstance(receiver, args);
case QUOTE :
return org.jruby.RubyRegexp.quote(receiver, args[0]);
case LAST_MATCH_S :
return org.jruby.RubyRegexp.last_match_s(receiver);
default :
Asserts.notReached();
return null;
}
}
}
