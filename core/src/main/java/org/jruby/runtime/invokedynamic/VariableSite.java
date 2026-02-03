package org.jruby.runtime.invokedynamic;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class VariableSite extends MutableCallSite {
    private final String name;
    private VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;

    private boolean rawValue;
    private final String file;
    private final int line;
    private int chainCount;

    private static final Logger LOG = LoggerFactory.getLogger(VariableSite.class);

    public VariableSite(MethodType type, String name, boolean rawValue, String file, int line) {
        super(type);
        this.name = name;
        this.rawValue = rawValue;
        this.file = file;
        this.line = line;
        this.chainCount = 0;
    }

    public synchronized int chainCount() {
        return chainCount;
    }

    public synchronized void incrementChainCount() {
        chainCount += 1;
    }

    public synchronized void clearChainCount() {
        chainCount = 0;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
    }

    public String name() {
        return name;
    }

    public static final Handle IVAR_ASM_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(VariableSite.class),
            "ivar",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);

    public static CallSite ivar(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = names[1];
        boolean rawValue = names.length > 2;
        VariableSite site = new VariableSite(type, varName, rawValue, "noname", 0);
        MethodHandle handle;

        handle = lookup.findVirtual(VariableSite.class, operation, type);

        handle = handle.bindTo(site);
        site.setTarget(handle.asType(site.type()));

        return site;
    }

    public IRubyObject ivarGet(IRubyObject self) throws Throwable {
        RubyClass realClass = self.getMetaClass().getRealClass();
        VariableAccessor accessor = realClass.getVariableAccessorForRead(name());

        // produce nil if the variable has not been initialize
        MethodHandle nullToNil = rawValue ?
                self.getRuntime().getNullToUndefinedHandle() :
                self.getRuntime().getNullToNilHandle();

        // get variable value and filter with nullToNil
        MethodHandle getValue;
        boolean direct = false;

        if (accessor instanceof FieldVariableAccessor) {
            direct = true;
            getValue = ((FieldVariableAccessor) accessor).getGetter();
            getValue = explicitCastArguments(getValue, methodType(Object.class, IRubyObject.class));
        } else {
            getValue = findStatic(VariableAccessor.class, "getVariable", methodType(Object.class, RubyBasicObject.class, int.class));
            getValue = explicitCastArguments(getValue, methodType(Object.class, IRubyObject.class, int.class));
            getValue = insertArguments(getValue, 1, accessor.getIndex());
        }

        getValue = filterReturnValue(getValue, nullToNil);

        // prepare fallback
        MethodHandle fallback = null;
        if (chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(name() + "\tqet on type " + self.getMetaClass().id + " failed (polymorphic)" + extractSourceInfo());
            fallback = findVirtual(VariableSite.class, "ivarGetFail", methodType(IRubyObject.class, IRubyObject.class));
            fallback = fallback.bindTo(this);
            setTarget(fallback);
            return (IRubyObject)fallback.invokeWithArguments(self);
        } else {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (direct) {
                    LOG.info(name() + "\tget field on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo());
                } else {
                    LOG.info(name() + "\tget on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo());
                }
            }
            fallback = getTarget();
            incrementChainCount();
        }

        // prepare test
        MethodHandle test = findStatic(VariableSite.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());

        MethodHandle guarded = guardWithTest(test, getValue, fallback);

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(name() + "\tget on class " + self.getMetaClass().id + " bound directly" + extractSourceInfo());
        setTarget(guarded);

        return (IRubyObject)getValue.invokeExact(self);
    }

    public IRubyObject ivarGetFail(IRubyObject self) {
        VariableAccessor variableAccessor = accessor;
        RubyClass cls = self.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            accessor = variableAccessor = cls.getVariableAccessorForRead(name);
        }
        IRubyObject value = (IRubyObject) variableAccessor.get(self);
        if (value != null) {
            return value;
        }
        return rawValue ? UndefinedValue.UNDEFINED : self.getRuntime().getNil();
    }

    public void ivarSet(IRubyObject self, IRubyObject value) throws Throwable {
        RubyClass realClass = self.getMetaClass().getRealClass();
        VariableAccessor accessor = realClass.getVariableAccessorForWrite(name());

        // set variable value and fold by returning value
        MethodHandle setValue;
        boolean direct = false;

        if (accessor instanceof FieldVariableAccessor) {
            direct = true;
            setValue = ((FieldVariableAccessor)accessor).getSetter();
            setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, IRubyObject.class));
        } else {
            setValue = findStatic(accessor.getClass(), "setVariableChecked", methodType(void.class, RubyBasicObject.class, RubyClass.class, int.class, Object.class));
            setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, RubyClass.class, int.class, IRubyObject.class));
            setValue = insertArguments(setValue, 1, realClass, accessor.getIndex());
        }

        // prepare fallback
        MethodHandle fallback = null;
        if (chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(name() + "\tset on type " + self.getMetaClass().id + " failed (polymorphic)" + extractSourceInfo());
            fallback = findVirtual(VariableSite.class, "ivarSetFail", methodType(void.class, IRubyObject.class, IRubyObject.class));
            fallback = fallback.bindTo(this);
            setTarget(fallback);
            fallback.invokeExact(self, value);
        } else {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (direct) {
                    LOG.info(name() + "\tset field on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo());
                } else {
                    LOG.info(name() + "\tset on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo());
                }
            }
            fallback = getTarget();
            incrementChainCount();
        }

        // prepare test
        MethodHandle test = findStatic(VariableSite.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());
        test = dropArguments(test, 1, IRubyObject.class);

        MethodHandle guarded = guardWithTest(test, setValue, fallback);

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(name() + "\tset on class " + self.getMetaClass().id + " bound directly" + extractSourceInfo());
        setTarget(guarded);

        setValue.invokeExact(self, value);
    }

    public void ivarSetFail(IRubyObject self, IRubyObject value) {
        VariableAccessor variableAccessor = accessor;
        RubyClass cls = self.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            accessor = variableAccessor = cls.getVariableAccessorForWrite(name);
        }
        variableAccessor.set(self, value);
    }

    private static MethodHandle findStatic(Class target, String name, MethodType type) {
        return findStatic(lookup(), target, name, type);
    }

    private static MethodHandle findStatic(MethodHandles.Lookup lookup, Class target, String name, MethodType type) {
        try {
            return lookup.findStatic(target, name, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle findVirtual(Class target, String name, MethodType type) {
        return findVirtual(lookup(), target, name, type);
    }

    private static MethodHandle findVirtual(MethodHandles.Lookup lookup, Class target, String name, MethodType type) {
        try {
            return lookup.findVirtual(target, name, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractSourceInfo() {
        return " (" + file() + ":" + line() + ")";
    }

    public static boolean testRealClass(int id, IRubyObject self) {
        return id == ((RubyBasicObject)self).getMetaClass().getRealClass().id;
    }
}
