package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.util.Asserts;

public class FixnumDefinition extends ClassDefinition {
private static final int FIXNUM = 0xf000;
private static final int STATIC = FIXNUM | 0x100;
public static final int TO_F = FIXNUM | 1;
public static final int TO_S = FIXNUM | 2;
public static final int TAINT = FIXNUM | 3;
public static final int FREEZE = FIXNUM | 4;
public static final int OP_LSHIFT = FIXNUM | 5;
public static final int OP_RSHIFT = FIXNUM | 6;
public static final int OP_PLUS = FIXNUM | 7;
public static final int OP_MINUS = FIXNUM | 8;
public static final int OP_MUL = FIXNUM | 9;
public static final int OP_DIV = FIXNUM | 10;
public static final int OP_MOD = FIXNUM | 11;
public static final int OP_POW = FIXNUM | 12;
public static final int EQUAL = FIXNUM | 13;
public static final int OP_CMP = FIXNUM | 14;
public static final int OP_GT = FIXNUM | 15;
public static final int OP_GE = FIXNUM | 16;
public static final int OP_LT = FIXNUM | 17;
public static final int OP_LE = FIXNUM | 18;
public static final int OP_AND = FIXNUM | 19;
public static final int OP_OR = FIXNUM | 20;
public static final int OP_XOR = FIXNUM | 21;
public static final int SIZE = FIXNUM | 22;
public static final int AREF = FIXNUM | 23;
public static final int HASH = FIXNUM | 24;
public static final int ID2NAME = FIXNUM | 25;
public static final int INVERT = FIXNUM | 26;
public static final int ID = FIXNUM | 27;
public static final int INDUCED_FROM = STATIC | 1;

public FixnumDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
return runtime.defineClass("Fixnum", (RubyClass) runtime.getClasses().getClass("Integer"));
}

protected void defineMethods(MethodContext context) {
context.create("to_f", TO_F, 0);
context.create("to_s", TO_S, 0);
context.create("to_str", TO_S, 0);
context.create("taint", TAINT, 0);
context.create("freeze", FREEZE, 0);
context.create("<<", OP_LSHIFT, 1);
context.create(">>", OP_RSHIFT, 1);
context.create("+", OP_PLUS, 1);
context.create("-", OP_MINUS, 1);
context.create("*", OP_MUL, 1);
context.create("/", OP_DIV, 1);
context.create("%", OP_MOD, 1);
context.create("**", OP_POW, 1);
context.create("==", EQUAL, 1);
context.create("eql?", EQUAL, 1);
context.create("equal?", EQUAL, 1);
context.create("<=>", OP_CMP, 1);
context.create(">", OP_GT, 1);
context.create(">=", OP_GE, 1);
context.create("<", OP_LT, 1);
context.create("<=", OP_LE, 1);
context.create("&", OP_AND, 1);
context.create("|", OP_OR, 1);
context.create("^", OP_XOR, 1);
context.create("size", SIZE, 0);
context.create("[]", AREF, 1);
context.create("hash", HASH, 0);
context.create("id2name", ID2NAME, 0);
context.create("~", INVERT, 0);
context.create("id", ID, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.create("induced_from", INDUCED_FROM, 1);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case INDUCED_FROM :
return org.jruby.RubyFixnum.induced_from(receiver, args[0]);
default :
Asserts.notReached();
return null;
}
}
}
