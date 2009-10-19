/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.CoreObjectType;
import org.jruby.util.IdUtil;
import org.jruby.util.TypeConverter;

/**
 *
 * @author enebo
 */
public class RubyBasicObject implements Cloneable, IRubyObject, Serializable, Comparable<IRubyObject>, CoreObjectType, InstanceVariables, InternalVariables {
    private static final boolean DEBUG = false;
    
    // The class of this object
    protected transient RubyClass metaClass;

    protected int flags; // zeroed by jvm

    /**
     * The error message used when some one tries to modify an
     * instance variable in a high security setting.
     */
    protected static final String ERR_INSECURE_SET_INST_VAR  = "Insecure: can't modify instance variable";

    public static final int ALL_F = -1;
    public static final int FALSE_F = 1 << 0;
    /**
     * This flag is a bit funny. It's used to denote that this value
     * is nil. It's a bit counterintuitive for a Java programmer to
     * not use subclassing to handle this case, since we have a
     * RubyNil subclass anyway. Well, the reason for it being a flag
     * is that the {@link #isNil()} method is called extremely often. So often
     * that it gives a good speed boost to make it monomorphic and
     * final. It turns out using a flag for this actually gives us
     * better performance than having a polymorphic {@link #isNil()} method.
     */
    public static final int NIL_F = 1 << 1;
    public static final int FROZEN_F = 1 << 2;
    public static final int TAINTED_F = 1 << 3;
    public static final int UNTRUSTED_F = 1 << 4;

    public static final int FL_USHIFT = 5;

    public static final int USER0_F = (1<<(FL_USHIFT+0));
    public static final int USER1_F = (1<<(FL_USHIFT+1));
    public static final int USER2_F = (1<<(FL_USHIFT+2));
    public static final int USER3_F = (1<<(FL_USHIFT+3));
    public static final int USER4_F = (1<<(FL_USHIFT+4));
    public static final int USER5_F = (1<<(FL_USHIFT+5));
    public static final int USER6_F = (1<<(FL_USHIFT+6));
    public static final int USER7_F = (1<<(FL_USHIFT+7));

    public static final int COMPARE_BY_IDENTITY_F = (1<<(FL_USHIFT+8));

    /**
     *  A value that is used as a null sentinel in among other places
     *  the RubyArray implementation. It will cause large problems to
     *  call any methods on this object.
     */
    public static final IRubyObject NEVER = new RubyBasicObject();

    /**
     * A value that specifies an undefined value. This value is used
     * as a sentinel for undefined constant values, and other places
     * where neither null nor NEVER makes sense.
     */
    public static final IRubyObject UNDEF = new RubyBasicObject();

    /**
     * It's not valid to create a totally empty RubyObject. Since the
     * RubyObject is always defined in relation to a runtime, that
     * means that creating RubyObjects from outside the class might
     * cause problems.
     */
    private RubyBasicObject(){};

    /**
     * Default allocator instance for all Ruby objects. The only
     * reason to not use this allocator is if you actually need to
     * have all instances of something be a subclass of RubyObject.
     *
     * @see org.jruby.runtime.ObjectAllocator
     */
    public static final ObjectAllocator OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyBasicObject(runtime, klass);
        }
    };

    /**
     * Will create the Ruby class Object in the runtime
     * specified. This method needs to take the actual class as an
     * argument because of the Object class' central part in runtime
     * initialization.
     */
    public static RubyClass createBasicObjectClass(Ruby runtime, RubyClass objectClass) {
        objectClass.index = ClassIndex.OBJECT;

        //objectClass.defineAnnotatedMethods(BasicObjectMethods.class);
        objectClass.defineAnnotatedMethods(RubyBasicObject.class);

        return objectClass;
    }

    /**
     * Interestingly, the Object class doesn't really have that many
     * methods for itself. Instead almost all of the Object methods
     * are really defined on the Kernel module. This class is a holder
     * for all Object methods.
     *
     * @see RubyKernel
     */
    public static class BasicObjectMethods {
        @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
        public static IRubyObject intialize(IRubyObject self) {
            return self.getRuntime().getNil();
        }
    }

    /**
     * Standard path for object creation. Objects are entered into ObjectSpace
     * only if ObjectSpace is enabled.
     */
    public RubyBasicObject(Ruby runtime, RubyClass metaClass) {
        assert metaClass != null: "NULL Metaclass!!?!?!";

        this.metaClass = metaClass;

        if (runtime.isObjectSpaceEnabled()) addToObjectSpace(runtime);
        if (runtime.getSafeLevel() >= 3) taint(runtime);
    }

    /**
     * Path for objects that don't taint and don't enter objectspace.
     */
    public RubyBasicObject(RubyClass metaClass) {
        this.metaClass = metaClass;
    }

    /**
     * Path for objects who want to decide whether they don't want to be in
     * ObjectSpace even when it is on. (notably used by objects being
     * considered immediate, they'll always pass false here)
     */
    protected RubyBasicObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        this.metaClass = metaClass;

        if (useObjectSpace) addToObjectSpace(runtime);
        if (canBeTainted && runtime.getSafeLevel() >= 3) taint(runtime);
    }

    protected RubyBasicObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        this.metaClass = metaClass;

        if (useObjectSpace) addToObjectSpace(runtime);
        if (runtime.getSafeLevel() >= 3) taint(runtime);
    }

    private void addToObjectSpace(Ruby runtime) {
        assert runtime.isObjectSpaceEnabled();
        runtime.getObjectSpace().add(this);
    }

    protected void taint(Ruby runtime) {
        runtime.secure(4);
        if (!isTaint()) {
        	testFrozen();
            setTaint(true);
        }
    }

    /** rb_frozen_class_p
     *
     * Helper to test whether this object is frozen, and if it is will
     * throw an exception based on the message.
     */
   protected final void testFrozen(String message) {
       if (isFrozen()) {
           throw getRuntime().newFrozenError(message);
       }
   }

    /** rb_frozen_class_p
     *
     * Helper to test whether this object is frozen, and if it is will
     * throw an exception based on the message.
     */
   protected final void testFrozen() {
       if (isFrozen()) {
           throw getRuntime().newFrozenError("object");
       }
   }

    /**
     * Sets or unsets a flag on this object. The only flags that are
     * guaranteed to be valid to use as the first argument is:
     *
     * <ul>
     *  <li>{@link #FALSE_F}</li>
     *  <li>{@link NIL_F}</li>
     *  <li>{@link FROZEN_F}</li>
     *  <li>{@link TAINTED_F}</li>
     *  <li>{@link USER0_F}</li>
     *  <li>{@link USER1_F}</li>
     *  <li>{@link USER2_F}</li>
     *  <li>{@link USER3_F}</li>
     *  <li>{@link USER4_F}</li>
     *  <li>{@link USER5_F}</li>
     *  <li>{@link USER6_F}</li>
     *  <li>{@link USER7_F}</li>
     * </ul>
     *
     * @param flag the actual flag to set or unset.
     * @param set if true, the flag will be set, if false, the flag will be unset.
     */
    public final void setFlag(int flag, boolean set) {
        if (set) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    /**
     * Get the value of a custom flag on this object. The only
     * guaranteed flags that can be sent in to this method is:
     *
     * <ul>
     *  <li>{@link #FALSE_F}</li>
     *  <li>{@link NIL_F}</li>
     *  <li>{@link FROZEN_F}</li>
     *  <li>{@link TAINTED_F}</li>
     *  <li>{@link USER0_F}</li>
     *  <li>{@link USER1_F}</li>
     *  <li>{@link USER2_F}</li>
     *  <li>{@link USER3_F}</li>
     *  <li>{@link USER4_F}</li>
     *  <li>{@link USER5_F}</li>
     *  <li>{@link USER6_F}</li>
     *  <li>{@link USER7_F}</li>
     * </ul>
     *
     * @param flag the flag to get
     * @return true if the flag is set, false otherwise
     */
    public final boolean getFlag(int flag) {
        return (flags & flag) != 0;
    }

    /**
     * See org.jruby.javasupport.util.RuntimeHelpers#invokeSuper
     */
    @Deprecated
    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) {
        return RuntimeHelpers.invokeSuper(context, this, args, block);
    }

    /**
     * Will invoke a named method with no arguments and no block.
     */
    public final IRubyObject callMethod(ThreadContext context, String name) {
        return RuntimeHelpers.invoke(context, this, name);
    }

    /**
     * Will invoke a named method with one argument and no block with
     * functional invocation.
     */
     public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) {
        return RuntimeHelpers.invoke(context, this, name, arg);
    }

    /**
     * Will invoke a named method with the supplied arguments and no
     * block with functional invocation.
     */
    public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) {
        return RuntimeHelpers.invoke(context, this, name, args);
    }

    public final IRubyObject callMethod(String name, IRubyObject... args) {
        return RuntimeHelpers.invoke(getRuntime().getCurrentContext(), this, name, args);
    }

    public final IRubyObject callMethod(String name) {
        return RuntimeHelpers.invoke(getRuntime().getCurrentContext(), this, name);
    }

    /**
     * Will invoke a named method with the supplied arguments and
     * supplied block with functional invocation.
     */
    public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) {
        return RuntimeHelpers.invoke(context, this, name, args, block);
    }

    /**
     * Will invoke an indexed method with the no arguments and no
     * block.
     */
    @Deprecated
    public final IRubyObject callMethod(ThreadContext context, int methodIndex, String name) {
        return RuntimeHelpers.invoke(context, this, name);
    }

    /**
     * Will invoke an indexed method with the one argument and no
     * block with a functional invocation.
     */
    @Deprecated
    public final IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg) {
        return RuntimeHelpers.invoke(context, this, name, arg, Block.NULL_BLOCK);
    }


    /**
     * Does this object represent nil? See the docs for the {@link
     * #NIL_F} flag for more information.
     */
    public final boolean isNil() {
        return (flags & NIL_F) != 0;
    }

    /**
     * Is this value a true value or not? Based on the {@link #FALSE_F} flag.
     */
    public final boolean isTrue() {
        return (flags & FALSE_F) == 0;
    }

    /**
     * Is this value a false value or not? Based on the {@link #FALSE_F} flag.
     */
    public final boolean isFalse() {
        return (flags & FALSE_F) != 0;
    }

    /**
     * Gets the taint. Shortcut for getFlag(TAINTED_F).
     *
     * @return true if this object is tainted
     */
    public boolean isTaint() {
        return (flags & TAINTED_F) != 0;
    }

    /**
     * Sets the taint flag. Shortcut for setFlag(TAINTED_F, taint)
     *
     * @param taint should this object be tainted or not?
     */
    public void setTaint(boolean taint) {
        // JRUBY-4113: callers should not call setTaint on immediate objects
        if (isImmediate()) return;
        
        if (taint) {
            flags |= TAINTED_F;
        } else {
            flags &= ~TAINTED_F;
        }
    }


    /** OBJ_INFECT
     *
     * Infects this object with traits from the argument obj. In real
     * terms this currently means that if obj is tainted, this object
     * will get tainted too. It's possible to hijack this method to do
     * other infections if that would be interesting.
     */
    public IRubyObject infectBy(IRubyObject obj) {
        if (obj.isTaint()) setTaint(true);
        if (obj.isUntrusted()) setUntrusted(true);
        return this;
    }

    final RubyBasicObject infectBy(RubyBasicObject obj) {
        flags |= (obj.flags & (TAINTED_F | UNTRUSTED_F));
        return this;
    }

    final RubyBasicObject infectBy(int tuFlags) {
        flags |= (tuFlags & (TAINTED_F | UNTRUSTED_F));
        return this;
    }

    /**
     * Is this value frozen or not? Shortcut for doing
     * getFlag(FROZEN_F).
     *
     * @return true if this object is frozen, false otherwise
     */
    public boolean isFrozen() {
        return (flags & FROZEN_F) != 0;
    }

    /**
     * Sets whether this object is frozen or not. Shortcut for doing
     * setFlag(FROZEN_F, frozen).
     *
     * @param frozen should this object be frozen?
     */
    public void setFrozen(boolean frozen) {
        if (frozen) {
            flags |= FROZEN_F;
        } else {
            flags &= ~FROZEN_F;
        }
    }


    /**
     * Is this value untrusted or not? Shortcut for doing
     * getFlag(UNTRUSTED_F).
     *
     * @return true if this object is frozen, false otherwise
     */
    public boolean isUntrusted() {
        return (flags & UNTRUSTED_F) != 0;
    }

    /**
     * Sets whether this object is frozen or not. Shortcut for doing
     * setFlag(FROZEN_F, frozen).
     *
     * @param frozen should this object be frozen?
     */
    public void setUntrusted(boolean untrusted) {
        if (untrusted) {
            flags |= UNTRUSTED_F;
        } else {
            flags &= ~UNTRUSTED_F;
        }
    }

    /**
     *  Is object immediate (def: Fixnum, Symbol, true, false, nil?).
     */
    public boolean isImmediate() {
    	return false;
    }

    /**
     * if exist return the meta-class else return the type of the object.
     *
     */
    public final RubyClass getMetaClass() {
        return metaClass;
    }

    /** rb_singleton_class
     *
     * Note: this method is specialized for RubyFixnum, RubySymbol,
     * RubyNil and RubyBoolean
     *
     * Will either return the existing singleton class for this
     * object, or create a new one and return that.
     */
    public RubyClass getSingletonClass() {
        RubyClass klass;

        if (getMetaClass().isSingleton() && ((MetaClass)getMetaClass()).getAttached() == this) {
            klass = getMetaClass();
        } else {
            klass = makeMetaClass(getMetaClass());
        }

        klass.setTaint(isTaint());
        if (isFrozen()) klass.setFrozen(true);

        return klass;
    }

    /** rb_make_metaclass
     *
     * Will create a new meta class, insert this in the chain of
     * classes for this specific object, and return the generated meta
     * class.
     */
    public RubyClass makeMetaClass(RubyClass superClass) {
        MetaClass klass = new MetaClass(getRuntime(), superClass); // rb_class_boot
        setMetaClass(klass);

        klass.setAttached(this);
        klass.setMetaClass(superClass.getRealClass().getMetaClass());

        superClass.addSubclass(klass);

        return klass;
    }

    /**
     * Makes it possible to change the metaclass of an object. In
     * practice, this is a simple version of Smalltalks Become, except
     * that it doesn't work when we're dealing with subclasses. In
     * practice it's used to change the singleton/meta class used,
     * without changing the "real" inheritance chain.
     */
    public void setMetaClass(RubyClass metaClass) {
        this.metaClass = metaClass;
    }

    /**
     * @see org.jruby.runtime.builtin.IRubyObject#getType()
     */
    public RubyClass getType() {
        return getMetaClass().getRealClass();
    }

    /**
     * Does this object respond to the specified message? Uses a
     * shortcut if it can be proved that respond_to? haven't been
     * overridden.
     */
    public final boolean respondsTo(String name) {
        if(getMetaClass().searchMethod("respond_to?") == getRuntime().getRespondToMethod()) {
            return getMetaClass().isMethodBound(name, false);
        } else {
            return callMethod(getRuntime().getCurrentContext(),"respond_to?",getRuntime().newSymbol(name)).isTrue();
        }
    }

    /**
     * Will return the runtime that this object is associated with.
     *
     * @return current runtime
     */
    public final Ruby getRuntime() {
        return getMetaClass().getClassRuntime();
    }

    /**
     * Will return the Java interface that most closely can represent
     * this object, when working through JAva integration
     * translations.
     */
    public Class getJavaClass() {
        Object obj = dataGetStruct();
        if (obj instanceof JavaObject) {
            return ((JavaObject)obj).getValue().getClass();
        }
        return getClass();
    }

    /** rb_to_id
     *
     * Will try to convert this object to a String using the Ruby
     * "to_str" if the object isn't already a String. If this still
     * doesn't work, will throw a Ruby TypeError.
     *
     */
    public String asJavaString() {
        IRubyObject asString = checkStringType();
        if(!asString.isNil()) return ((RubyString)asString).asJavaString();
        throw getRuntime().newTypeError(inspect().toString() + " is not a string");
    }

    /** rb_obj_as_string
     *
     * First converts this object into a String using the "to_s"
     * method, infects it with the current taint and returns it. If
     * to_s doesn't return a Ruby String, {@link #anyToString} is used
     * instead.
     */
    public RubyString asString() {
        IRubyObject str = RuntimeHelpers.invoke(getRuntime().getCurrentContext(), this, "to_s");

        if (!(str instanceof RubyString)) return (RubyString)anyToString();
        if (isTaint()) str.setTaint(true);
        return (RubyString) str;
    }

 /**
     * Tries to convert this object to a Ruby Array using the "to_ary"
     * method.
     */
    public RubyArray convertToArray() {
        return (RubyArray) TypeConverter.convertToType(this, getRuntime().getArray(), "to_ary");
    }

    /**
     * Tries to convert this object to a Ruby Hash using the "to_hash"
     * method.
     */
    public RubyHash convertToHash() {
        return (RubyHash)TypeConverter.convertToType(this, getRuntime().getHash(), "to_hash");
    }

    /**
     * Tries to convert this object to a Ruby Float using the "to_f"
     * method.
     */
    public RubyFloat convertToFloat() {
        return (RubyFloat) TypeConverter.convertToType(this, getRuntime().getFloat(), "to_f");
    }

    /**
     * Tries to convert this object to a Ruby Integer using the "to_int"
     * method.
     */
    public RubyInteger convertToInteger() {
        return convertToInteger("to_int");
    }

    @Deprecated
    public RubyInteger convertToInteger(int methodIndex, String convertMethod) {
        return convertToInteger(convertMethod);
    }

    /**
     * Tries to convert this object to a Ruby Integer using the
     * supplied conversion method.
     */
    public RubyInteger convertToInteger(String convertMethod) {
        IRubyObject val = TypeConverter.convertToType(this, getRuntime().getInteger(), convertMethod, true);
        if (!(val instanceof RubyInteger)) throw getRuntime().newTypeError(getMetaClass().getName() + "#" + convertMethod + " should return Integer");
        return (RubyInteger)val;
    }

    /**
     * Tries to convert this object to a Ruby String using the
     * "to_str" method.
     */
    public RubyString convertToString() {
        return (RubyString) TypeConverter.convertToType(this, getRuntime().getString(), "to_str");
    }

    /**
     * Internal method that helps to convert any object into the
     * format of a class name and a hex string inside of #<>.
     */
    public IRubyObject anyToString() {
        String cname = getMetaClass().getRealClass().getName();
        /* 6:tags 16:addr 1:eos */
        RubyString str = getRuntime().newString("#<" + cname + ":0x" + Integer.toHexString(System.identityHashCode(this)) + ">");
        str.setTaint(isTaint());
        return str;
    }

    /** rb_check_string_type
     *
     * Tries to return a coerced string representation of this object,
     * using "to_str". If that returns something other than a String
     * or nil, an empty String will be returned.
     *
     */
    public IRubyObject checkStringType() {
        IRubyObject str = TypeConverter.convertToTypeWithCheck(this, getRuntime().getString(), "to_str");
        if(!str.isNil() && !(str instanceof RubyString)) {
            str = RubyString.newEmptyString(getRuntime());
        }
        return str;
    }

    /** rb_check_array_type
    *
    * Returns the result of trying to convert this object to an Array
    * with "to_ary".
    */
    public IRubyObject checkArrayType() {
        return TypeConverter.convertToTypeWithCheck(this, getRuntime().getArray(), "to_ary");
    }

    // 1.9 rb_check_to_integer
    IRubyObject checkIntegerType(Ruby runtime, IRubyObject obj, String method) {
        if (obj instanceof RubyFixnum) return obj;
        IRubyObject conv = TypeConverter.convertToType(obj, getRuntime().getInteger(), method, false);
        return conv instanceof RubyInteger ? conv : obj.getRuntime().getNil();
    }

    /**
     * @see IRubyObject.toJava
     */
    public Object toJava(Class target) {
        if (dataGetStruct() instanceof JavaObject) {
            // for interface impls

            JavaObject innerWrapper = (JavaObject)dataGetStruct();

            // ensure the object is associated with the wrapper we found it in,
            // so that if it comes back we don't re-wrap it
            getRuntime().getJavaSupport().getObjectProxyCache().put(innerWrapper.getValue(), this);

            return innerWrapper.getValue();
        } else {
            return JavaUtil.coerceOtherToType(getRuntime().getCurrentContext(), this, target);
        }
    }

    public IRubyObject dup() {
        if (isImmediate()) throw getRuntime().newTypeError("can't dup " + getMetaClass().getName());

        IRubyObject dup = getMetaClass().getRealClass().allocate();
        if (isTaint()) dup.setTaint(true);
        if (isUntrusted()) dup.setUntrusted(true);

        initCopy(dup, this);

        return dup;
    }

    /** init_copy
     *
     * Initializes a copy with variable and special instance variable
     * information, and then call the initialize_copy Ruby method.
     */
    private static void initCopy(IRubyObject clone, IRubyObject original) {
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        original.copySpecialInstanceVariables(clone);

        if (original.hasVariables()) clone.syncVariables(original.getVariableList());
        if (original instanceof RubyModule) ((RubyModule) clone).syncConstants((RubyModule) original);

        /* FIXME: finalizer should be dupped here */
        clone.callMethod(clone.getRuntime().getCurrentContext(), "initialize_copy", original);
    }

    /**
     * Lots of MRI objects keep their state in non-lookupable ivars
     * (e:g. Range, Struct, etc). This method is responsible for
     * dupping our java field equivalents
     */
    public void copySpecialInstanceVariables(IRubyObject clone) {
    }

    /** rb_inspect
     *
     * The internal helper that ensures a RubyString instance is returned
     * so dangerous casting can be omitted
     * Prefered over callMethod(context, "inspect")
     */
    static RubyString inspect(ThreadContext context, IRubyObject object) {
        return RubyString.objAsString(context, object.callMethod(context, "inspect"));
    }

    public IRubyObject rbClone() {
        if (isImmediate()) throw getRuntime().newTypeError("can't clone " + getMetaClass().getName());

        // We're cloning ourselves, so we know the result should be a RubyObject
        RubyObject clone = (RubyObject)getMetaClass().getRealClass().allocate();
        clone.setMetaClass(getSingletonClassClone());
        if (isTaint()) clone.setTaint(true);

        initCopy(clone, this);

        if (isFrozen()) clone.setFrozen(true);
        if (isUntrusted()) clone.setUntrusted(true);
        return clone;
    }

    /** rb_singleton_class_clone
     *
     * Will make sure that if the current objects class is a
     * singleton, it will get cloned.
     *
     * @return either a real class, or a clone of the current singleton class
     */
    protected RubyClass getSingletonClassClone() {
       RubyClass klass = getMetaClass();

       if (!klass.isSingleton()) return klass;

       MetaClass clone = new MetaClass(getRuntime());
       clone.flags = flags;

       if (this instanceof RubyClass) {
           clone.setMetaClass(clone);
       } else {
           clone.setMetaClass(klass.getSingletonClassClone());
       }

       clone.setSuperClass(klass.getSuperClass());

       if (klass.hasVariables()) clone.syncVariables(klass.getVariableList());
       clone.syncConstants(klass);

       klass.cloneMethods(clone);

       ((MetaClass)clone.getMetaClass()).setAttached(clone);

       ((MetaClass)clone).setAttached(((MetaClass)klass).getAttached());

       return clone;
    }

    /**
     * Specifically polymorphic method that are meant to be overridden
     * by modules to specify that they are modules in an easy way.
     */
    public boolean isModule() {
        return false;
    }

    /**
     * Specifically polymorphic method that are meant to be overridden
     * by classes to specify that they are classes in an easy way.
     */
    public boolean isClass() {
        return false;
    }


    /**
     * @see org.jruby.runtime.builtin.IRubyObject#dataWrapStruct()
     */
    public synchronized void dataWrapStruct(Object obj) {
        if (obj == null) {
            removeInternalVariable("__wrap_struct__");
        } else {
            fastSetInternalVariable("__wrap_struct__", obj);
        }
    }

    // The dataStruct is a place where custom information can be
    // contained for core implementations that doesn't necessarily
    // want to go to the trouble of creating a subclass of
    // RubyObject. The OpenSSL implementation uses this heavily to
    // save holder objects containing Java cryptography objects.
    // Java integration uses this to store the Java object ref.
    //protected transient Object dataStruct;
    /**
     * @see org.jruby.runtime.builtin.IRubyObject#dataGetStruct()
     */
    public synchronized Object dataGetStruct() {
        return fastGetInternalVariable("__wrap_struct__");
    }

    // Equivalent of Data_Get_Struct
    // This will first check that the object in question is actually a T_DATA equivalent.
    public synchronized Object dataGetStructChecked() {
        TypeConverter.checkData(this);
        return this.fastGetInternalVariable("__wrap_struct__");
    }

    /** rb_obj_id
     *
     * Return the internal id of an object.
     *
     * FIXME: Should this be renamed to match its ruby name?
     */
    public synchronized IRubyObject id() {
        return getRuntime().newFixnum(getRuntime().getObjectSpace().idOf(this));
    }

    /** rb_obj_inspect
     *
     *  call-seq:
     *     obj.inspect   => string
     *
     *  Returns a string containing a human-readable representation of
     *  <i>obj</i>. If not overridden, uses the <code>to_s</code> method to
     *  generate the string.
     *
     *     [ 1, 2, 3..4, 'five' ].inspect   #=> "[1, 2, 3..4, \"five\"]"
     *     Time.new.inspect                 #=> "Wed Apr 09 08:54:39 CDT 2003"
     */
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        if ((!isImmediate()) && !(this instanceof RubyModule) && hasVariables()) {
            StringBuilder part = new StringBuilder();
            String cname = getMetaClass().getRealClass().getName();
            part.append("#<").append(cname).append(":0x");
            part.append(Integer.toHexString(System.identityHashCode(this)));

            if (runtime.isInspecting(this)) {
                /* 6:tags 16:addr 1:eos */
                part.append(" ...>");
                return runtime.newString(part.toString());
            }
            try {
                runtime.registerInspecting(this);
                return runtime.newString(inspectObj(part).toString());
            } finally {
                runtime.unregisterInspecting(this);
            }
        }

        if (isNil()) return RubyNil.inspect(this);
        return RuntimeHelpers.invoke(runtime.getCurrentContext(), this, "to_s");
    }


    /** inspect_obj
     *
     * The internal helper method that takes care of the part of the
     * inspection that inspects instance variables.
     */
    private StringBuilder inspectObj(StringBuilder part) {
        ThreadContext context = getRuntime().getCurrentContext();
        String sep = "";

        for (Variable<IRubyObject> ivar : getInstanceVariableList()) {
            part.append(sep).append(" ").append(ivar.getName()).append("=");
            part.append(ivar.getValue().callMethod(context, "inspect"));
            sep = ",";
        }
        part.append(">");
        return part;
    }

    // Methods of the Object class (rb_obj_*):


    @JRubyMethod(name = "!", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_not(ThreadContext context) {
        return context.getRuntime().newBoolean(!this.isTrue());
    }

    @JRubyMethod(name = "!=", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_not_equal(ThreadContext context, IRubyObject other) {
        return context.getRuntime().newBoolean(!op_equal(context, other).isTrue());
    }

    public int compareTo(IRubyObject other) {
        return (int)callMethod(getRuntime().getCurrentContext(), "<=>", other).convertToInteger().getLongValue();
    }

    /** rb_obj_equal
     *
     * Will by default use identity equality to compare objects. This
     * follows the Ruby semantics.
     */
    @JRubyMethod(name = "==", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return this == obj ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    /** rb_equal
     *
     * The Ruby "===" method is used by default in case/when
     * statements. The Object implementation first checks Java identity
     * equality and then calls the "==" method too.
     */
    @JRubyMethod(name = "equal?", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return op_equal(context, other);
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "==" method.
     */
    protected static boolean equalInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        return that == other || that.callMethod(context, "==", other).isTrue();
    }

    /** method used for Hash key comparison (specialized for String, Symbol and Fixnum)
     *
     * Will by default just call the Ruby method "eql?"
     */
    public boolean eql(IRubyObject other) {
        return callMethod(getRuntime().getCurrentContext(), "eql?", other).isTrue();
    }

    /**
     * Adds the specified object as a finalizer for this object.
     */
    public void addFinalizer(IRubyObject f) {
        Finalizer finalizer = (Finalizer)fastGetInternalVariable("__finalizer__");
        if (finalizer == null) {
            finalizer = new Finalizer(getRuntime().getObjectSpace().idOf(this));
            fastSetInternalVariable("__finalizer__", finalizer);
            getRuntime().addFinalizer(finalizer);
        }
        finalizer.addFinalizer(f);
    }

    /**
     * Remove all the finalizers for this object.
     */
    public void removeFinalizers() {
        Finalizer finalizer = (Finalizer)fastGetInternalVariable("__finalizer__");
        if (finalizer != null) {
            finalizer.removeFinalizers();
            removeInternalVariable("__finalizer__");
            getRuntime().removeFinalizer(finalizer);
        }
    }

    private static final Object[] NULL_OBJECT_ARRAY = new Object[0];
    private volatile Object[] varTable = NULL_OBJECT_ARRAY;

    private Object[] getIvarTableForRead() {
        return varTable;
    }

    private synchronized Object[] getVariableTableForWrite(int index) {
        if (varTable == NULL_OBJECT_ARRAY) {
            varTable = new Object[getMetaClass().getRealClass().getVariableTableSize()];
        } else if (varTable.length <= index) {
            if (DEBUG) System.out.println("resizing from " + varTable.length + " to " + getMetaClass().getRealClass().getVariableTableSize());
            Object[] newTable = new Object[getMetaClass().getRealClass().getVariableTableSize()];
            System.arraycopy(varTable, 0, newTable, 0, varTable.length);
            varTable = newTable;
        }
        return varTable;
    }

    public Object getVariable(int index) {
        if (index < 0) return null;
        Object[] ivarTable = getIvarTableForRead();
        if (ivarTable.length > index) return ivarTable[index];
        return null;
    }

    public synchronized void setVariable(int index, Object value) {
        ensureInstanceVariablesSettable();
        if (index < 0) return;
        Object[] ivarTable = getVariableTableForWrite(index);
        ivarTable[index] = value;
    }

    //
    // COMMON VARIABLE METHODS
    //

    /**
     * Returns true if object has any variables, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables
     * <li> constants
     * <li> internal variables, such as those used when marshaling Ranges and Exceptions
     * </ul>
     * @return true if object has any variables, else false
     */
    public boolean hasVariables() {
        return varTable.length > 0;
    }

    /**
     * Returns the amount of instance variables, class variables,
     * constants and internal variables this object has.
     */
    public int getVariableCount() {
        return varTable.length;
    }

    /**
     * Gets a list of all variables in this object.
     */
    // TODO: must override in RubyModule to pick up constants
    public List<Variable<Object>> getVariableList() {
        Map<String, RubyClass.VariableAccessor> ivarAccessors = getMetaClass().getRealClass().getVariableAccessorsForRead();
        ArrayList<Variable<Object>> list = new ArrayList<Variable<Object>>();
        for (Map.Entry<String, RubyClass.VariableAccessor> entry : ivarAccessors.entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null) continue;
            list.add(new VariableEntry<Object>(entry.getKey(), value));
        }
        return list;
    }

    /**
     * Gets a name list of all variables in this object.
     */
   // TODO: must override in RubyModule to pick up constants
   public List<String> getVariableNameList() {
        Map<String, RubyClass.VariableAccessor> ivarAccessors = getMetaClass().getRealClass().getVariableAccessorsForRead();
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String, RubyClass.VariableAccessor> entry : ivarAccessors.entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null) continue;
            list.add(entry.getKey());
        }
        return list;
    }

    /**
     * Checks if the variable table contains a variable of the
     * specified name.
     */
    protected boolean variableTableContains(String name) {
        return getMetaClass().getRealClass().getVariableAccessorForRead(name).get(this) != null;
    }

    /**
     * Checks if the variable table contains the the variable of the
     * specified name, where the precondition is that the name must be
     * an interned Java String.
     */
    protected boolean variableTableFastContains(String internedName) {
        return variableTableContains(internedName);
    }

    /**
     * Fetch an object from the variable table based on the name.
     *
     * @return the object or null if not found
     */
    protected Object variableTableFetch(String name) {
        return getMetaClass().getRealClass().getVariableAccessorForRead(name).get(this);
    }

    /**
     * Fetch an object from the variable table based on the name,
     * where the name must be an interned Java String.
     *
     * @return the object or null if not found
     */
    protected Object variableTableFastFetch(String internedName) {
        return variableTableFetch(internedName);
    }

    /**
     * Store a value in the variable store under the specific name.
     */
    protected Object variableTableStore(String name, Object value) {
        getMetaClass().getRealClass().getVariableAccessorForWrite(name).set(this, value);
        return value;
    }

    /**
     * Will store the value under the specified name, where the name
     * needs to be an interned Java String.
     */
    protected Object variableTableFastStore(String internedName, Object value) {
        return variableTableStore(internedName, value);
    }

    /**
     * Removes the entry with the specified name from the variable
     * table, and returning the removed value.
     */
    protected Object variableTableRemove(String name) {
        synchronized(this) {
            Object value = getMetaClass().getRealClass().getVariableAccessorForRead(name).get(this);
            getMetaClass().getRealClass().getVariableAccessorForWrite(name).set(this, null);
            return value;
        }
    }

    /**
     * Get the size of the variable table.
     */
    protected int variableTableGetSize() {
        return getMetaClass().getRealClass().getVariableTableSize();
    }

    /**
     * Synchronize the variable table with the argument. In real terms
     * this means copy all entries into a newly allocated table.
     */
    protected void variableTableSync(List<Variable<Object>> vars) {
        synchronized(this) {
            for (Variable<Object> var : vars) {
                variableTableStore(var.getName(), var.getValue());
            }
        }
    }

    //
    // INTERNAL VARIABLE METHODS
    //

    /**
     * Dummy method to avoid a cast, and to avoid polluting the
     * IRubyObject interface with all the instance variable management
     * methods.
     */
    public InternalVariables getInternalVariables() {
        return this;
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#hasInternalVariable
     */
    public boolean hasInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableContains(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#fastHasInternalVariable
     */
    public boolean fastHasInternalVariable(String internedName) {
        assert !IdUtil.isRubyVariable(internedName);
        return variableTableFastContains(internedName);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#getInternalVariable
     */
    public Object getInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableFetch(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#fastGetInternalVariable
     */
    public Object fastGetInternalVariable(String internedName) {
        assert !IdUtil.isRubyVariable(internedName);
        return variableTableFastFetch(internedName);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#setInternalVariable
     */
    public void setInternalVariable(String name, Object value) {
        assert !IdUtil.isRubyVariable(name);
        variableTableStore(name, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#fastSetInternalVariable
     */
    public void fastSetInternalVariable(String internedName, Object value) {
        assert !IdUtil.isRubyVariable(internedName);
        variableTableFastStore(internedName, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#removeInternalVariable
     */
    public Object removeInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableRemove(name);
    }

    /**
     * Sync one variable table with another - this is used to make
     * rbClone work correctly.
     */
    public void syncVariables(List<Variable<Object>> variables) {
        variableTableSync(variables);
    }

    //
    // INSTANCE VARIABLE API METHODS
    //

    /**
     * Dummy method to avoid a cast, and to avoid polluting the
     * IRubyObject interface with all the instance variable management
     * methods.
     */
    public InstanceVariables getInstanceVariables() {
        return this;
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#hasInstanceVariable
     */
    public boolean hasInstanceVariable(String name) {
        assert IdUtil.isInstanceVariable(name);
        return variableTableContains(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#fastHasInstanceVariable
     */
    public boolean fastHasInstanceVariable(String internedName) {
        assert IdUtil.isInstanceVariable(internedName);
        return variableTableFastContains(internedName);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#getInstanceVariable
     */
    public IRubyObject getInstanceVariable(String name) {
        assert IdUtil.isInstanceVariable(name);
        return (IRubyObject)variableTableFetch(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#fastGetInstanceVariable
     */
    public IRubyObject fastGetInstanceVariable(String internedName) {
        assert IdUtil.isInstanceVariable(internedName);
        return (IRubyObject)variableTableFastFetch(internedName);
    }

    /** rb_iv_set / rb_ivar_set
    *
    * @see org.jruby.runtime.builtin.InstanceVariables#setInstanceVariable
    */
    public IRubyObject setInstanceVariable(String name, IRubyObject value) {
        assert IdUtil.isInstanceVariable(name) && value != null;
        ensureInstanceVariablesSettable();
        return (IRubyObject)variableTableStore(name, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#fastSetInstanceVariable
     */
    public IRubyObject fastSetInstanceVariable(String internedName, IRubyObject value) {
        assert IdUtil.isInstanceVariable(internedName) && value != null;
        ensureInstanceVariablesSettable();
        return (IRubyObject)variableTableFastStore(internedName, value);
     }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#removeInstanceVariable
     */
    public IRubyObject removeInstanceVariable(String name) {
        assert IdUtil.isInstanceVariable(name);
        ensureInstanceVariablesSettable();
        return (IRubyObject)variableTableRemove(name);
    }

    /**
     * Gets a list of all variables in this object.
     */
    // TODO: must override in RubyModule to pick up constants
    public List<Variable<IRubyObject>> getInstanceVariableList() {
        Map<String, RubyClass.VariableAccessor> ivarAccessors = getMetaClass().getVariableAccessorsForRead();
        ArrayList<Variable<IRubyObject>> list = new ArrayList<Variable<IRubyObject>>();
        for (Map.Entry<String, RubyClass.VariableAccessor> entry : ivarAccessors.entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null || !(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(entry.getKey())) continue;
            list.add(new VariableEntry<IRubyObject>(entry.getKey(), (IRubyObject)value));
        }
        return list;
    }

    /**
     * Gets a name list of all variables in this object.
     */
   // TODO: must override in RubyModule to pick up constants
   public List<String> getInstanceVariableNameList() {
        Map<String, RubyClass.VariableAccessor> ivarAccessors = getMetaClass().getRealClass().getVariableAccessorsForRead();
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String, RubyClass.VariableAccessor> entry : ivarAccessors.entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null || !(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(entry.getKey())) continue;
            list.add(entry.getKey());
        }
        return list;
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#getInstanceVariableNameList
     */
    public void copyInstanceVariablesInto(final InstanceVariables other) {
        for (Variable<IRubyObject> var : getInstanceVariableList()) {
            synchronized (this) {
                other.setInstanceVariable(var.getName(), var.getValue());
            }
        }
    }

    /**
     * Makes sure that instance variables can be set on this object,
     * including information about whether this object is frozen, or
     * tainted. Will throw a suitable exception in that case.
     */
    protected final void ensureInstanceVariablesSettable() {
        if (!isFrozen() && (getRuntime().getSafeLevel() < 4 || isTaint())) {
            return;
        }

        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError(ERR_INSECURE_SET_INST_VAR);
        }
        if (isFrozen()) {
            if (this instanceof RubyModule) {
                throw getRuntime().newFrozenError("class/module ");
            } else {
                throw getRuntime().newFrozenError("");
            }
        }
    }

    public int getNativeTypeIndex() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Class that keeps track of the finalizers for the object under
     * operation.
     */
    public class Finalizer implements Finalizable {
        private long id;
        private IRubyObject firstFinalizer;
        private List<IRubyObject> finalizers;
        private AtomicBoolean finalized;

        public Finalizer(long id) {
            this.id = id;
            this.finalized = new AtomicBoolean(false);
        }

        public void addFinalizer(IRubyObject finalizer) {
            if (firstFinalizer == null) {
                firstFinalizer = finalizer;
            } else {
                if (finalizers == null) finalizers = new ArrayList<IRubyObject>(4);
                finalizers.add(finalizer);
            }
        }

        public void removeFinalizers() {
            firstFinalizer = null;
            finalizers = null;
        }

        @Override
        public void finalize() {
            if (finalized.compareAndSet(false, true)) {
                if (firstFinalizer != null) callFinalizer(firstFinalizer);
                if (finalizers != null) {
                    for (int i = 0; i < finalizers.size(); i++) {
                        callFinalizer(finalizers.get(i));
                    }
                }
            }
        }
        
        private void callFinalizer(IRubyObject finalizer) {
            RuntimeHelpers.invoke(
                    finalizer.getRuntime().getCurrentContext(),
                    finalizer, "call", RubyBasicObject.this.id());
        }
    }
}
