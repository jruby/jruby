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

public class ArrayDefinition extends ClassDefinition {
private static final int ARRAY = 0xf000;
private static final int STATIC = ARRAY | 0x100;
public static final int INITIALIZE = ARRAY | 1;
public static final int INSPECT = ARRAY | 2;
public static final int TO_S = ARRAY | 3;
public static final int FROZEN = ARRAY | 4;
public static final int EQUAL = ARRAY | 5;
public static final int EQL = ARRAY | 6;
public static final int HASH = ARRAY | 7;
public static final int AREF = ARRAY | 8;
public static final int ASET = ARRAY | 9;
public static final int FIRST = ARRAY | 10;
public static final int LAST = ARRAY | 11;
public static final int CONCAT = ARRAY | 12;
public static final int APPEND = ARRAY | 13;
public static final int PUSH = ARRAY | 14;
public static final int POP = ARRAY | 15;
public static final int SHIFT = ARRAY | 16;
public static final int UNSHIFT = ARRAY | 17;
public static final int EACH = ARRAY | 18;
public static final int EACH_INDEX = ARRAY | 19;
public static final int REVERSE_EACH = ARRAY | 20;
public static final int LENGTH = ARRAY | 21;
public static final int EMPTY_P = ARRAY | 22;
public static final int INDEX = ARRAY | 23;
public static final int RINDEX = ARRAY | 24;
public static final int INDICES = ARRAY | 25;
public static final int RBCLONE = ARRAY | 26;
public static final int JOIN = ARRAY | 27;
public static final int REVERSE = ARRAY | 28;
public static final int REVERSE_BANG = ARRAY | 29;
public static final int SORT = ARRAY | 30;
public static final int SORT_BANG = ARRAY | 31;
public static final int COLLECT = ARRAY | 32;
public static final int COLLECT_BANG = ARRAY | 33;
public static final int DELETE = ARRAY | 34;
public static final int DELETE_AT = ARRAY | 35;
public static final int DELETE_IF = ARRAY | 36;
public static final int REJECT_BANG = ARRAY | 37;
public static final int REPLACE = ARRAY | 38;
public static final int CLEAR = ARRAY | 39;
public static final int FILL = ARRAY | 40;
public static final int INCLUDE_P = ARRAY | 41;
public static final int OP_CMP = ARRAY | 42;
public static final int NEWINSTANCE = STATIC | 1;
public static final int CREATE = STATIC | 2;

public ArrayDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
return runtime.defineClass("Array", (RubyClass) runtime.getClasses().getClass("Object"));
}

protected void defineMethods(MethodContext context) {
context.createOptional("initialize", INITIALIZE, 0);
context.create("inspect", INSPECT, 0);
context.create("to_s", TO_S, 0);
context.create("frozen?", FROZEN, 0);
context.create("==", EQUAL, 1);
context.create("eql?", EQL, 1);
context.create("===", EQUAL, 1);
context.create("hash", HASH, 0);
context.createOptional("[]", AREF, 0);
context.createOptional("[]=", ASET, 0);
context.create("first", FIRST, 0);
context.create("last", LAST, 0);
context.create("concat", CONCAT, 1);
context.create("<<", APPEND, 1);
context.createOptional("push", PUSH, 1);
context.create("pop", POP, 0);
context.create("shift", SHIFT, 0);
context.createOptional("unshift", UNSHIFT, 0);
context.create("each", EACH, 0);
context.create("each_index", EACH_INDEX, 0);
context.create("reverse_each", REVERSE_EACH, 0);
context.create("length", LENGTH, 0);
context.create("size", LENGTH, 0);
context.create("empty?", EMPTY_P, 0);
context.create("index", INDEX, 1);
context.create("rindex", RINDEX, 1);
context.createOptional("indices", INDICES, 0);
context.createOptional("indexes", INDICES, 0);
context.create("clone", RBCLONE, 0);
context.createOptional("join", JOIN, 0);
context.create("reverse", REVERSE, 0);
context.create("reverse!", REVERSE_BANG, 0);
context.create("sort", SORT, 0);
context.create("sort!", SORT_BANG, 0);
context.create("collect", COLLECT, 0);
context.create("collect!", COLLECT_BANG, 0);
context.create("map!", COLLECT_BANG, 0);
context.create("filter", COLLECT_BANG, 0);
context.create("delete", DELETE, 1);
context.create("delete_at", DELETE_AT, 1);
context.create("delete_if", DELETE_IF, 0);
context.create("reject!", REJECT_BANG, 0);
context.create("replace", REPLACE, 1);
context.create("clear", CLEAR, 0);
context.createOptional("fill", FILL, 0);
context.create("include?", INCLUDE_P, 1);
context.create("<=>", OP_CMP, 1);
context.createOptional("slice", AREF, 0);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.createOptional("new", NEWINSTANCE, 0);
context.createOptional("[]", CREATE, 0);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case NEWINSTANCE :
return org.jruby.RubyArray.newInstance(receiver, args);
case CREATE :
return org.jruby.RubyArray.create(receiver, args);
default :
Asserts.notReached();
return null;
}
}
}
