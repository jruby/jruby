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

public class StringDefinition extends ClassDefinition {
private static final int STRING = 0xf000;
private static final int STATIC = STRING | 0x100;
public static final int REPLACE = STRING | 1;
public static final int RBCLONE = STRING | 2;
public static final int DUP = STRING | 3;
public static final int OP_CMP = STRING | 4;
public static final int EQUAL = STRING | 5;
public static final int HASH = STRING | 6;
public static final int OP_PLUS = STRING | 7;
public static final int OP_MUL = STRING | 8;
public static final int FORMAT = STRING | 9;
public static final int AREF = STRING | 10;
public static final int ASET = STRING | 11;
public static final int LENGTH = STRING | 12;
public static final int EMPTY = STRING | 13;
public static final int MATCH = STRING | 14;
public static final int MATCH2 = STRING | 15;
public static final int SUCC = STRING | 16;
public static final int SUCC_BANG = STRING | 17;
public static final int UPTO = STRING | 18;
public static final int INDEX = STRING | 19;
public static final int RINDEX = STRING | 20;
public static final int TO_I = STRING | 21;
public static final int TO_F = STRING | 22;
public static final int TO_S = STRING | 23;
public static final int INSPECT = STRING | 24;
public static final int DUMP = STRING | 25;
public static final int UPCASE = STRING | 26;
public static final int DOWNCASE = STRING | 27;
public static final int CAPITALIZE = STRING | 28;
public static final int CONCAT = STRING | 29;
public static final int INTERN = STRING | 30;
public static final int SUM = STRING | 31;
public static final int NEWINSTANCE = STATIC | 1;

public StringDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
RubyClass result = runtime.defineClass("String", (RubyClass) runtime.getClasses().getClass("Object"));
result.includeModule(runtime.getClasses().getClass("Comparable"));
result.includeModule(runtime.getClasses().getClass("Enumerable"));
return result;
}

protected void defineMethods(MethodContext context) {
context.create("replace", REPLACE, 1);
context.create("initialize", REPLACE, 1);
context.create("clone", RBCLONE, 0);
context.create("dup", DUP, 0);
context.create("<=>", OP_CMP, 1);
context.create("==", EQUAL, 1);
context.create("===", EQUAL, 1);
context.create("eql?", EQUAL, 1);
context.create("hash", HASH, 0);
context.create("+", OP_PLUS, 1);
context.create("*", OP_MUL, 1);
context.create("%", FORMAT, 1);
context.createOptional("[]", AREF, 0);
context.createOptional("[]=", ASET, 0);
context.create("length", LENGTH, 0);
context.create("size", LENGTH, 0);
context.create("empty?", EMPTY, 0);
context.create("=~", MATCH, 1);
context.create("~", MATCH2, 0);
context.create("succ", SUCC, 0);
context.create("succ!", SUCC_BANG, 0);
context.create("next", SUCC, 0);
context.create("next!", SUCC_BANG, 0);
context.create("upto", UPTO, 1);
context.createOptional("index", INDEX, 0);
context.createOptional("rindex", RINDEX, 0);
context.create("to_i", TO_I, 0);
context.create("to_f", TO_F, 0);
context.create("to_s", TO_S, 0);
context.create("to_str", TO_S, 0);
context.create("inspect", INSPECT, 0);
context.create("dump", DUMP, 0);
context.create("upcase", UPCASE, 0);
context.create("downcase", DOWNCASE, 0);
context.create("capitalize", CAPITALIZE, 0);
context.create("concat", CONCAT, 1);
context.create("<<", CONCAT, 1);
context.create("intern", INTERN, 0);
context.createOptional("sum", SUM, 0);
context.createOptional("slice", AREF, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.createOptional("new", NEWINSTANCE, 0);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case NEWINSTANCE :
return org.jruby.RubyString.newInstance(receiver, args);
default :
Asserts.notReached();
return null;
}
}
}
