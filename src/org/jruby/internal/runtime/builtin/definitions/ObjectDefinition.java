package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.ModuleDefinition;
import org.jruby.runtime.builtin.definitions.ModuleFunctionsContext;
import org.jruby.util.Asserts;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class ObjectDefinition extends ModuleDefinition {
    private static final int OBJECT = 0x1400;

    public static final int RBCLONE = OBJECT | 0x01;
    public static final int DUP = OBJECT | 0x02;
    public static final int EQUAL = OBJECT | 0x03;
    public static final int EXTEND = OBJECT | 0x04;
    public static final int FREEZE = OBJECT | 0x05;
    public static final int FROZEN_P = OBJECT | 0x06;
    public static final int INSPECT = OBJECT | 0x07;
    public static final int INSTANCE_EVAL = OBJECT | 0x08;
    public static final int INSTANCE_OF = OBJECT | 0x09;
    public static final int INSTANCE_VARIABLES = OBJECT | 0x0a;
    public static final int KIND_OF = OBJECT | 0x0b;
    public static final int HASH = OBJECT | 0x0c;
    public static final int ID = OBJECT | 0x0d;
    public static final int MATCH = OBJECT | 0x0e;
    public static final int METHOD = OBJECT | 0x0f;
    public static final int METHODS = OBJECT | 0x10;
    public static final int METHOD_MISSING = OBJECT | 0x11;
    public static final int NIL = OBJECT | 0x12;
    public static final int PRIVATE_METHODS = OBJECT | 0x13;
    public static final int PROTECTED_METHODS = OBJECT | 0x14;
    public static final int RESPOND_TO = OBJECT | 0x15;
    public static final int SEND = OBJECT | 0x16;
    public static final int SINGLETON_METHODS = OBJECT | 0x17;
    public static final int TAINT = OBJECT | 0x18;
    public static final int TAINTED = OBJECT | 0x19;
    public static final int TO_A = OBJECT | 0x1a;
    public static final int TO_S = OBJECT | 0x1b;
    public static final int TYPE = OBJECT | 0x1c;
    public static final int UNTAINT = OBJECT | 0x1d;

    /**
     * Constructor for ObjectDefinition.
     * @param runtime
     */
    public ObjectDefinition(Ruby runtime) {
        super(runtime);
    }

    /**
     * @see org.jruby.runtime.builtin.definitions.ModuleDefinition#createModule(Ruby)
     */
    protected RubyModule createModule(Ruby runtime) {
        return runtime.getClasses().getKernelModule();
    }

    /**
     * @see org.jruby.runtime.builtin.definitions.ModuleDefinition#defineModuleFunctions(ModuleFunctionsContext)
     */
    protected void defineModuleFunctions(ModuleFunctionsContext context) {
        // don't define any module functions.
    }

    /**
     * @see org.jruby.runtime.builtin.definitions.ModuleDefinition#defineMethods(MethodContext)
     */
    protected void defineMethods(MethodContext context) {
        context.create("==", EQUAL, 1);
        context.createAlias("===", "==");
        context.create("=~", MATCH, 1);
        context.create("class", TYPE, 0);
        context.create("clone", RBCLONE, 0);
        context.create("dup", DUP, 0);
        context.create("eql?", EQUAL, 1);
        context.createAlias("equal?", "==");
        context.createOptional("extend", EXTEND, 1);
        context.create("freeze", FREEZE, 0);
        context.create("frozen?", FROZEN_P, 0);
        context.create("hash", HASH, 0);
        context.create("id", ID, 0);
        context.create("__id__", ID, 0);
        context.create("inspect", INSPECT, 0);
        context.createOptional("instance_eval", INSTANCE_EVAL);
        context.create("instance_of?", INSTANCE_OF, 1);
        context.create("instance_variables", INSTANCE_VARIABLES, 0);
        context.create("is_a?", KIND_OF, 1);
        context.create("kind_of?", KIND_OF, 1);
        context.create("method", METHOD, 1);
        context.create("methods", METHODS, 0);
        context.createOptional("method_missing", METHOD_MISSING);
        context.create("nil?", NIL, 0);
        context.create("private_methods", PRIVATE_METHODS, 0);
        context.create("protected_methods", PROTECTED_METHODS, 0);
        context.create("public_methods", METHODS, 0);
        context.createOptional("respond_to?", RESPOND_TO, 1);
        context.createOptional("send", SEND, 1);
        context.createOptional("__send__", SEND, 1);
        context.create("singleton_methods", SINGLETON_METHODS, 0);
        context.create("taint", TAINT, 0);
        context.create("tainted?", TAINTED, 0);
        context.create("to_a", TO_A, 0);
        context.create("to_s", TO_S, 0);
        context.create("type", TYPE, 0);
        context.create("untaint", UNTAINT, 0);
    }

    /**
     * @see org.jruby.runtime.IStaticCallable#callIndexed(int, IRubyObject, IRubyObject[])
     */
    public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
        Asserts.notReached("no module function is defined in Object.");
        return null;
    }

}