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

public class HashDefinition extends ClassDefinition {
private static final int HASH = 0xf000;
private static final int STATIC = HASH | 0x100;
public static final int INITIALIZE = HASH | 1;
public static final int RBCLONE = HASH | 2;
public static final int REHASH = HASH | 3;
public static final int TO_HASH = HASH | 4;
public static final int TO_A = HASH | 5;
public static final int TO_S = HASH | 6;
public static final int INSPECT = HASH | 7;
public static final int EQUAL = HASH | 8;
public static final int AREF = HASH | 9;
public static final int FETCH = HASH | 10;
public static final int ASET = HASH | 11;
public static final int GETDEFAULTVALUE = HASH | 12;
public static final int SETDEFAULTVALUE = HASH | 13;
public static final int INDEX = HASH | 14;
public static final int INDICES = HASH | 15;
public static final int SIZE = HASH | 16;
public static final int EMPTY_P = HASH | 17;
public static final int EACH = HASH | 18;
public static final int EACH_VALUE = HASH | 19;
public static final int EACH_KEY = HASH | 20;
public static final int SORT = HASH | 21;
public static final int KEYS = HASH | 22;
public static final int VALUES = HASH | 23;
public static final int SHIFT = HASH | 24;
public static final int DELETE = HASH | 25;
public static final int DELETE_IF = HASH | 26;
public static final int REJECT = HASH | 27;
public static final int REJECT_BANG = HASH | 28;
public static final int CLEAR = HASH | 29;
public static final int INVERT = HASH | 30;
public static final int UPDATE = HASH | 31;
public static final int REPLACE = HASH | 32;
public static final int HAS_KEY = HASH | 33;
public static final int HAS_VALUE = HASH | 34;
public static final int NEWINSTANCE = STATIC | 1;
public static final int CREATE = STATIC | 2;

public HashDefinition(Ruby runtime) {
super(runtime);
}

protected RubyClass createType(Ruby runtime) {
RubyClass result = runtime.defineClass("Hash", (RubyClass) runtime.getClasses().getClass("Object"));
result.includeModule(runtime.getClasses().getClass("Enumerable"));
return result;
}

protected void defineMethods(MethodContext context) {
context.createOptional("initialize", INITIALIZE, 0);
context.create("clone", RBCLONE, 0);
context.create("rehash", REHASH, 0);
context.create("to_hash", TO_HASH, 0);
context.create("to_a", TO_A, 0);
context.create("to_s", TO_S, 0);
context.create("inspect", INSPECT, 0);
context.create("==", EQUAL, 1);
context.create("[]", AREF, 1);
context.createOptional("fetch", FETCH, 0);
context.create("[]=", ASET, 2);
context.create("store", ASET, 2);
context.create("default", GETDEFAULTVALUE, 0);
context.create("default=", SETDEFAULTVALUE, 1);
context.create("index", INDEX, 1);
context.createOptional("indices", INDICES, 0);
context.createOptional("indexes", INDICES, 0);
context.create("size", SIZE, 0);
context.create("length", SIZE, 0);
context.create("empty?", EMPTY_P, 0);
context.create("each", EACH, 0);
context.create("each_pair", EACH, 0);
context.create("each_value", EACH_VALUE, 0);
context.create("each_key", EACH_KEY, 0);
context.create("sort", SORT, 0);
context.create("keys", KEYS, 0);
context.create("values", VALUES, 0);
context.create("shift", SHIFT, 0);
context.create("delete", DELETE, 1);
context.create("delete_if", DELETE_IF, 0);
context.create("reject", REJECT, 0);
context.create("reject!", REJECT_BANG, 0);
context.create("clear", CLEAR, 0);
context.create("invert", INVERT, 0);
context.create("update", UPDATE, 1);
context.create("replace", REPLACE, 1);
context.create("has_key?", HAS_KEY, 1);
context.create("include?", HAS_KEY, 1);
context.create("member?", HAS_KEY, 1);
context.create("key?", HAS_KEY, 1);
context.create("has_value?", HAS_VALUE, 1);
context.create("value?", HAS_VALUE, 1);
}

protected void defineSingletonMethods(SingletonMethodContext context) {
context.createOptional("new", NEWINSTANCE, 0);
context.createOptional("[]", CREATE, 0);
}
public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
switch (index) {
case NEWINSTANCE :
return org.jruby.RubyHash.newInstance(receiver, args);
case CREATE :
return org.jruby.RubyHash.create(receiver, args);
default :
Asserts.notReached();
return null;
}
}
}
