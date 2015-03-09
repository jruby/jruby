/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jcodings.Encoding;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ivars.VariableAccessor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

import static org.jruby.anno.FrameField.*;
import static org.jruby.runtime.Visibility.*;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.CoreObjectType;
import org.jruby.util.IdUtil;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.unsafe.UnsafeHolder;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;
import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.runtime.invokedynamic.MethodNames.INSPECT;
import org.jruby.runtime.ivars.VariableTableManager;

/**
 * RubyBasicObject is the only implementation of the
 * {@link org.jruby.runtime.builtin.IRubyObject}. Every Ruby object in JRuby
 * is represented by something that is an instance of RubyBasicObject. In
 * the core class implementations, this means doing a subclass
 * that extends RubyBasicObject. In other cases it means using a simple
 * RubyBasicObject instance and its data fields to store specific
 * information about the Ruby object.
 *
 * Some care has been taken to make the implementation be as
 * monomorphic as possible, so that the Java Hotspot engine can
 * improve performance of it. That is the reason for several patterns
 * that might seem odd in this class.
 *
 * The IRubyObject interface used to have lots of methods for
 * different things, but these have now mostly been refactored into
 * several interfaces that gives access to that specific part of the
 * object. This gives us the possibility to switch out that subsystem
 * without changing interfaces again. For example, instance variable
 * and internal variables are handled this way, but the implementation
 * in RubyObject only returns "this" in {@link #getInstanceVariables()} and
 * {@link #getInternalVariables()}.
 * 
 * Methods that are implemented here, such as "initialize" should be implemented
 * with care; reification of Ruby classes into Java classes can produce
 * conflicting method names in rare cases. See JRUBY-5906 for an example.
 */
public class RubyBasicObject implements Cloneable, IRubyObject, Serializable, Comparable<IRubyObject>, CoreObjectType, InstanceVariables, InternalVariables {

    private static final Logger LOG = LoggerFactory.getLogger("RubyBasicObject");

    private static final boolean DEBUG = false;
    
    /** The class of this object */
    protected transient RubyClass metaClass;

    /** object flags */
    protected int flags;

    /** variable table, lazily allocated as needed (if needed) */
    public transient Object[] varTable;
    
    /** locking stamp for Unsafe ops updating the vartable */
    public transient volatile int varTableStamp;
    
    /** offset of the varTable field in RubyBasicObject */
    public static final long VAR_TABLE_OFFSET = UnsafeHolder.fieldOffset(RubyBasicObject.class, "varTable");
    
    /** offset of the varTableTamp field in RubyBasicObject */
    public static final long STAMP_OFFSET = UnsafeHolder.fieldOffset(RubyBasicObject.class, "varTableStamp");

    /**
     * The error message used when some one tries to modify an
     * instance variable in a high security setting.
     */
    public static final String ERR_INSECURE_SET_INST_VAR  = "Insecure: can't modify instance variable";

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

    public static final int FL_USHIFT = 4;

    public static final int USER0_F = (1<<(FL_USHIFT+0));
    public static final int USER1_F = (1<<(FL_USHIFT+1));
    public static final int USER2_F = (1<<(FL_USHIFT+2));
    public static final int USER3_F = (1<<(FL_USHIFT+3));
    public static final int USER4_F = (1<<(FL_USHIFT+4));
    public static final int USER5_F = (1<<(FL_USHIFT+5));
    public static final int USER6_F = (1<<(FL_USHIFT+6));
    public static final int USER7_F = (1<<(FL_USHIFT+7));
    public static final int USER8_F = (1<<(FL_USHIFT+8));
    public static final int USER9_F = (1<<(FL_USHIFT+9));
    public static final int USERA_F = (1<<(FL_USHIFT+10));
    public static final int REFINED_MODULE_F = USER9_F;
    public static final int IS_OVERLAID_F = USERA_F;

    public static final int COMPARE_BY_IDENTITY_F = USER8_F;

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
    public static final ObjectAllocator BASICOBJECT_ALLOCATOR = new ObjectAllocator() {
        @Override
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
        objectClass.setClassIndex(ClassIndex.OBJECT);

        objectClass.defineAnnotatedMethods(RubyBasicObject.class);

        recacheBuiltinMethods(runtime);

        return objectClass;
    }

    static void recacheBuiltinMethods(Ruby runtime) {
        RubyModule objectClass = runtime.getBasicObject();

        runtime.setDefaultMethodMissing(objectClass.searchMethod("method_missing"));
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize19(ThreadContext context) {
        return context.nil;
    }

    /**
     * Standard path for object creation. Objects are entered into ObjectSpace
     * only if ObjectSpace is enabled.
     */
    public RubyBasicObject(Ruby runtime, RubyClass metaClass) {
        this.metaClass = metaClass;

        runtime.addToObjectSpace(true, this);
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
    protected RubyBasicObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        this.metaClass = metaClass;

        runtime.addToObjectSpace(useObjectSpace, this);
    }

    protected void taint(Ruby runtime) {
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
     *  <li>{@link #NIL_F}</li>
     *  <li>{@link #FROZEN_F}</li>
     *  <li>{@link #TAINTED_F}</li>
     *  <li>{@link #USER0_F}</li>
     *  <li>{@link #USER1_F}</li>
     *  <li>{@link #USER2_F}</li>
     *  <li>{@link #USER3_F}</li>
     *  <li>{@link #USER4_F}</li>
     *  <li>{@link #USER5_F}</li>
     *  <li>{@link #USER6_F}</li>
     *  <li>{@link #USER7_F}</li>
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
     *  <li>{@link #NIL_F}</li>
     *  <li>{@link #FROZEN_F}</li>
     *  <li>{@link #TAINTED_F}</li>
     *  <li>{@link #USER0_F}</li>
     *  <li>{@link #USER1_F}</li>
     *  <li>{@link #USER2_F}</li>
     *  <li>{@link #USER3_F}</li>
     *  <li>{@link #USER4_F}</li>
     *  <li>{@link #USER5_F}</li>
     *  <li>{@link #USER6_F}</li>
     *  <li>{@link #USER7_F}</li>
     * </ul>
     *
     * @param flag the flag to get
     * @return true if the flag is set, false otherwise
     */
    public final boolean getFlag(int flag) {
        return (flags & flag) != 0;
    }

    /**
     * Will invoke a named method with no arguments and no block if that method or a custom
     * method missing exists. Otherwise returns null. 1.9: rb_check_funcall
     */
    @Override
    public final IRubyObject checkCallMethod(ThreadContext context, String name) {
        return Helpers.invokeChecked(context, this, name);
    }

    /**
     * Will invoke a named method with no arguments and no block.
     */
    @Override
    public final IRubyObject callMethod(ThreadContext context, String name) {
        return Helpers.invoke(context, this, name);
    }

    /**
     * Will invoke a named method with one argument and no block with
     * functional invocation.
     */
    @Override
     public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) {
        return Helpers.invoke(context, this, name, arg);
    }

    /**
     * Will invoke a named method with the supplied arguments and no
     * block with functional invocation.
     */
    @Override
    public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) {
        return Helpers.invoke(context, this, name, args);
    }

    public final IRubyObject callMethod(String name, IRubyObject... args) {
        return Helpers.invoke(getRuntime().getCurrentContext(), this, name, args);
    }

    public final IRubyObject callMethod(String name) {
        return Helpers.invoke(getRuntime().getCurrentContext(), this, name);
    }

    /**
     * Will invoke a named method with the supplied arguments and
     * supplied block with functional invocation.
     */
    @Override
    public final IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) {
        return Helpers.invoke(context, this, name, args, block);
    }

    /**
     * Does this object represent nil? See the docs for the {@link
     * #NIL_F} flag for more information.
     */
    @Override
    public final boolean isNil() {
        return (flags & NIL_F) != 0;
    }

    /**
     * Is this value a truthy value or not? Based on the {@link #FALSE_F} flag.
     */
    @Override
    public final boolean isTrue() {
        return (flags & FALSE_F) == 0;
    }

    /**
     * Is this value a falsey value or not? Based on the {@link #FALSE_F} flag.
     */
    public final boolean isFalse() {
        return (flags & FALSE_F) != 0;
    }

    /**
     * Gets the taint. Shortcut for getFlag(TAINTED_F).
     *
     * @return true if this object is tainted
     */
    @Override
    public boolean isTaint() {
        return (flags & TAINTED_F) != 0;
    }

    /**
     * Sets the taint flag. Shortcut for setFlag(TAINTED_F, taint)
     *
     * @param taint should this object be tainted or not?
     */
    @Override
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
    @Override
    public IRubyObject infectBy(IRubyObject obj) {
        if (obj.isTaint()) setTaint(true);
        return this;
    }

    final RubyBasicObject infectBy(RubyBasicObject obj) {
        flags |= (obj.flags & TAINTED_F);
        return this;
    }

    final RubyBasicObject infectBy(int tuFlags) {
        flags |= (tuFlags & TAINTED_F);
        return this;
    }

    /**
     * Is this value frozen or not? Shortcut for doing
     * getFlag(FROZEN_F).
     *
     * @return true if this object is frozen, false otherwise
     */
    @Override
    public boolean isFrozen() {
        return (flags & FROZEN_F) != 0;
    }

    /**
     * Sets whether this object is frozen or not. Shortcut for doing
     * setFlag(FROZEN_F, frozen).
     *
     * @param frozen should this object be frozen?
     */
    @Override
    public void setFrozen(boolean frozen) {
        if (frozen) {
            flags |= FROZEN_F;
        } else {
            flags &= ~FROZEN_F;
        }
    }

    /**
     *  Is object immediate (def: Fixnum, Symbol, true, false, nil?).
     */
    @Override
    public boolean isImmediate() {
    	return false;
    }

    @Override
    public boolean isSpecialConst() {
        return isImmediate() || !isTrue();
    }

    /**
     * if exist return the meta-class else return the type of the object.
     *
     */
    @Override
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
    @Override
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
        MetaClass klass = new MetaClass(getRuntime(), superClass, this); // rb_class_boot
        setMetaClass(klass);

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
    @Override
    public RubyClass getType() {
        return getMetaClass().getRealClass();
    }

    /**
     * Does this object respond to the specified message? Uses a
     * shortcut if it can be proved that respond_to? haven't been
     * overridden.
     */
    @Override
    public final boolean respondsTo(String name) {
        Ruby runtime = getRuntime();

        DynamicMethod method = getMetaClass().searchMethod("respond_to?");
        if(method.equals(runtime.getRespondToMethod())) {
            // fastest path; builtin respond_to? which just does isMethodBound
            return getMetaClass().isMethodBound(name, false);
        } else if (!method.isUndefined()) {
            // medium path, invoke user's respond_to? if defined

            // We have to check and enforce arity
            Arity arity = method.getArity();
            ThreadContext context = runtime.getCurrentContext();
            if (arity.isFixed() && arity.required() == 1) {
                return method.call(context, this, metaClass, "respond_to?", runtime.newSymbol(name)).isTrue();
            } else if (arity.isFixed() && arity.required() != 2) {
                throw runtime.newArgumentError("respond_to? must accept 1 or 2 arguments (requires " + arity.getValue() + ")");
            } else {

            }

            return method.call(context, this, metaClass, "respond_to?", runtime.newSymbol(name), runtime.newBoolean(true)).isTrue();

        } else {
            // slowest path, full callMethod to hit method_missing if present, or produce error
            return callMethod(runtime.getCurrentContext(), "respond_to?", runtime.newSymbol(name)).isTrue();
        }
    }

    /**
     * Does this object respond to the specified message via "method_missing?"
     */
    @Override
    public final boolean respondsToMissing(String name) {
        return respondsToMissing(name, true);
    }

    /**
     * Does this object respond to the specified message via "method_missing?"
     */
    @Override
    public final boolean respondsToMissing(String name, boolean priv) {
        DynamicMethod method = getMetaClass().searchMethod("respond_to_missing?");
        // perhaps should try a smart version as for respondsTo above?
        if(method.isUndefined()) {
            return false;
        } else {
            return method.call(
                    getRuntime().getCurrentContext(),
                    this,
                    metaClass,
                    "respond_to_missing?",
                    getRuntime().newSymbol(name),
                    getRuntime().newBoolean(priv)).isTrue();
        }
    }

    /**
     * Will return the runtime that this object is associated with.
     *
     * @return current runtime
     */
    @Override
    public final Ruby getRuntime() {
        return getMetaClass().getClassRuntime();
    }

    /**
     * Will return the Java interface that most closely can represent
     * this object, when working through JAva integration
     * translations.
     */
    @Override
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
    @Override
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
    @Override
    public RubyString asString() {
        IRubyObject str = Helpers.invoke(getRuntime().getCurrentContext(), this, "to_s");

        if (!(str instanceof RubyString)) return (RubyString)anyToString();
        if (isTaint()) str.setTaint(true);
        return (RubyString) str;
    }

 /**
     * Tries to convert this object to a Ruby Array using the "to_ary"
     * method.
     */
    @Override
    public RubyArray convertToArray() {
        return (RubyArray) TypeConverter.convertToType(this, getRuntime().getArray(), "to_ary");
    }

    /**
     * Tries to convert this object to a Ruby Hash using the "to_hash"
     * method.
     */
    @Override
    public RubyHash convertToHash() {
        return (RubyHash)TypeConverter.convertToType(this, getRuntime().getHash(), "to_hash");
    }

    /**
     * Tries to convert this object to a Ruby Float using the "to_f"
     * method.
     */
    @Override
    public RubyFloat convertToFloat() {
        return (RubyFloat) TypeConverter.convertToType(this, getRuntime().getFloat(), "to_f");
    }

    /**
     * Tries to convert this object to a Ruby Integer using the "to_int"
     * method.
     */
    @Override
    public RubyInteger convertToInteger() {
        return convertToInteger("to_int");
    }

    /**
     * Tries to convert this object to a Ruby Integer using the
     * supplied conversion method.
     */
    @Override
    public RubyInteger convertToInteger(String convertMethod) {
        IRubyObject val = TypeConverter.convertToType(this, getRuntime().getInteger(), convertMethod, true);
        if (!(val instanceof RubyInteger)) throw getRuntime().newTypeError(getMetaClass().getName() + "#" + convertMethod + " should return Integer");
        return (RubyInteger)val;
    }

    /**
     * Tries to convert this object to a Ruby String using the
     * "to_str" method.
     */
    @Override
    public RubyString convertToString() {
        return (RubyString) TypeConverter.convertToType(this, getRuntime().getString(), "to_str");
    }

    /**
     * Internal method that helps to convert any object into the
     * format of a class name and a hex string inside of #<>.
     */
    @Override
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
    @Override
    public IRubyObject checkStringType() {
        IRubyObject str = TypeConverter.convertToTypeWithCheck(this, getRuntime().getString(), "to_str");
        if(!str.isNil() && !(str instanceof RubyString)) {
            str = RubyString.newEmptyString(getRuntime());
        }
        return str;
    }

    /** rb_check_string_type
     *
     * Tries to return a coerced string representation of this object,
     * using "to_str". If that returns something other than a String
     * or nil, an empty String will be returned.
     *
     */
    @Override
    public IRubyObject checkStringType19() {
        IRubyObject str = TypeConverter.convertToTypeWithCheck19(this, getRuntime().getString(), "to_str");
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
    @Override
    public IRubyObject checkArrayType() {
        return TypeConverter.convertToTypeWithCheck(this, getRuntime().getArray(), "to_ary");
    }

    /**
     * @see IRubyObject#toJava
     */
    @Override
    public Object toJava(Class target) {
        // for callers that unconditionally pass null retval type (JRUBY-4737)
        if (target == void.class) return null;

        if (dataGetStruct() instanceof JavaObject) {
            // for interface impls

            JavaObject innerWrapper = (JavaObject)dataGetStruct();

            // ensure the object is associated with the wrapper we found it in,
            // so that if it comes back we don't re-wrap it
            if (target.isAssignableFrom(innerWrapper.getValue().getClass())) {
                getRuntime().getJavaSupport().getObjectProxyCache().put(innerWrapper.getValue(), this);

                return innerWrapper.getValue();
            }
        } else if (JavaUtil.isDuckTypeConvertable(getClass(), target)) {
            if (!respondsTo("java_object")) {
                return JavaUtil.convertProcToInterface(getRuntime().getCurrentContext(), this, target);
            }
        } else if (target.isAssignableFrom(getClass())) {
            return this;
        }
        
        throw getRuntime().newTypeError("cannot convert instance of " + getClass() + " to " + target);
    }

    @Override
    public IRubyObject dup() {
        Ruby runtime = getRuntime();

        if (isImmediate()) throw runtime.newTypeError("can't dup " + getMetaClass().getName());

        IRubyObject dup = getMetaClass().getRealClass().allocate();
        if (isTaint()) dup.setTaint(true);

        initCopy(dup, this, "initialize_dup");

        return dup;
    }

    /** init_copy
     *
     * Initializes a copy with variable and special instance variable
     * information, and then call the initialize_copy Ruby method.
     */
    private static void initCopy(IRubyObject clone, IRubyObject original, String method) {
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        original.copySpecialInstanceVariables(clone);

        if (original.hasVariables()) clone.syncVariables(original);
        if (original instanceof RubyModule) {
            RubyModule cloneMod = (RubyModule)clone;
            cloneMod.syncConstants((RubyModule)original);
            cloneMod.syncClassVariables((RubyModule)original);
        }

        /* FIXME: finalizer should be dupped here */
        clone.callMethod(clone.getRuntime().getCurrentContext(), method, original);
    }

    protected static boolean OBJ_INIT_COPY(IRubyObject obj, IRubyObject orig) {
        if (obj == orig) return false;

        objInitCopy(obj, orig);
        return true;
    }

    protected static void objInitCopy(IRubyObject obj, IRubyObject orig) {
        if (obj == orig) return;
        // FIXME: booooo!
        ((RubyBasicObject)obj).checkFrozen();
        // Not implemented
//        checkTrusted();
        if (obj.getClass() != orig.getClass() || obj.getMetaClass().getRealClass() != orig.getMetaClass().getRealClass()) {
            throw obj.getRuntime().newTypeError("initialize_copy should take same class object");
        }
    }

    /**
     * Lots of MRI objects keep their state in non-lookupable ivars
     * (e:g. Range, Struct, etc). This method is responsible for
     * dupping our java field equivalents
     */
    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
    }

    /** rb_inspect
     *
     * The internal helper that ensures a RubyString instance is returned
     * so dangerous casting can be omitted
     * Prefered over callMethod(context, "inspect")
     */
    static RubyString inspect(ThreadContext context, IRubyObject object) {
        return RubyString.objAsString(context, invokedynamic(context, object, INSPECT));
    }

    @Override
    public IRubyObject rbClone() {
        Ruby runtime = getRuntime();

        if (isImmediate()) throw runtime.newTypeError("can't clone " + getMetaClass().getName());

        // We're cloning ourselves, so we know the result should be a RubyObject
        RubyBasicObject clone = (RubyBasicObject)getMetaClass().getRealClass().allocate();
        clone.setMetaClass(getSingletonClassClone());
        if (isTaint()) clone.setTaint(true);

        initCopy(clone, this, "initialize_clone");

        if (isFrozen()) clone.setFrozen(true);
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

        if (!klass.isSingleton()) {
            return klass;
        }

        MetaClass clone = new MetaClass(getRuntime(), klass.getSuperClass(), ((MetaClass) klass).getAttached());
        clone.flags = flags;

        if (this instanceof RubyClass) {
            clone.setMetaClass(clone);
        } else {
            clone.setMetaClass(klass.getSingletonClassClone());
        }

        if (klass.hasVariables()) {
            clone.syncVariables(klass);
        }
        clone.syncConstants(klass);

        klass.cloneMethods(clone);

        ((MetaClass) clone.getMetaClass()).setAttached(clone);

        return clone;
    }

    /**
     * Specifically polymorphic method that are meant to be overridden
     * by modules to specify that they are modules in an easy way.
     */
    @Override
    public boolean isModule() {
        return false;
    }

    /**
     * Specifically polymorphic method that are meant to be overridden
     * by classes to specify that they are classes in an easy way.
     */
    @Override
    public boolean isClass() {
        return false;
    }


    /**
     * @see org.jruby.runtime.builtin.IRubyObject#dataWrapStruct(Object)
     */
    @Override
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
    @Override
    public synchronized Object dataGetStruct() {
        return getInternalVariable("__wrap_struct__");
    }

    // Equivalent of Data_Get_Struct
    // This will first check that the object in question is actually a T_DATA equivalent.
    @Override
    public synchronized Object dataGetStructChecked() {
        TypeConverter.checkData(this);
        return getInternalVariable("__wrap_struct__");
    }

    /** rb_obj_id
     *
     * Return the internal id of an object.
     */
    @JRubyMethod(name = "__id__")
    @Override
    public IRubyObject id() {
        return getRuntime().newFixnum(getObjectId());
    }

    /**
     * The logic here is to use the special objectId accessor slot from the
     * parent as a lazy store for an object ID. IDs are generated atomically,
     * in serial, and guaranteed unique for up to 2^63 objects. The special
     * objectId slot is managed separately from the "normal" vars so it
     * does not marshal, clone/dup, or refuse to be initially set when the
     * object is frozen.
     */
    protected long getObjectId() {
        return metaClass.getRealClass().getVariableTableManager().getObjectId(this);
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
    @Override
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        if ((!isImmediate()) && !(this instanceof RubyModule) && hasVariables()) {
            return hashyInspect();
        } else {
            if (isNil()) return RubyNil.inspect(runtime.getCurrentContext(), this);
            return to_s();
        }
    }

    public IRubyObject hashyInspect() {
        Ruby runtime = getRuntime();
        StringBuilder part = new StringBuilder();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(":0x");
        part.append(Integer.toHexString(inspectHashCode()));

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

    // MRI: rb_inspect, which does dispatch
    public static IRubyObject rbInspect(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.runtime;
        RubyString str = obj.callMethod(context, "inspect").asString();
        Encoding ext = EncodingUtils.defaultExternalEncoding(runtime);
        if (!ext.isAsciiCompatible()) {
            if (!str.isAsciiOnly())
                throw runtime.newEncodingCompatibilityError("inspected result must be ASCII only if default external encoding is ASCII incompatible");
            return str;
        }
        if (str.getEncoding() != ext && !str.isAsciiOnly())
            throw runtime.newEncodingCompatibilityError("inspected result must be ASCII only or use the default external encoding");
        return str;
    }
    /**
     * For most objects, the hash used in the default #inspect is just the
     * identity hashcode of the actual object.
     *
     * See org.jruby.java.proxies.JavaProxy for a divergent case.
     *
     * @return The identity hashcode of this object
     */
    protected int inspectHashCode() {
        return System.identityHashCode(this);
    }

    /** inspect_obj
     *
     * The internal helper method that takes care of the part of the
     * inspection that inspects instance variables.
     */
    private StringBuilder inspectObj(StringBuilder part) {
        ThreadContext context = getRuntime().getCurrentContext();
        String sep = "";

        for (Map.Entry<String, VariableAccessor> entry : metaClass.getVariableTableManager().getVariableAccessorsForRead().entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null || !(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(entry.getKey())) continue;
            
            part.append(sep).append(" ").append(entry.getKey()).append("=");
            part.append(invokedynamic(context, (IRubyObject)value, INSPECT));
            sep = ",";
        }
        part.append(">");
        return part;
    }

    // Methods of the Object class (rb_obj_*):


    @JRubyMethod(name = "!")
    public IRubyObject op_not(ThreadContext context) {
        return context.runtime.newBoolean(!this.isTrue());
    }

    @JRubyMethod(name = "!=", required = 1)
    public IRubyObject op_not_equal(ThreadContext context, IRubyObject other) {
        return context.runtime.newBoolean(!invokedynamic(context, this, OP_EQUAL, other).isTrue());
    }

    /**
     * Compares this Ruby object with another.
     *
     * @param other another IRubyObject
     * @return 0 if equal,
     *         &lt; 0 if this is less than other,
     *         &gt; 0 if this is greater than other
     * @throws IllegalArgumentException if the objects cannot be compared.
     */
    @Override
    public int compareTo(IRubyObject other) {
        IRubyObject oldExc = getRuntime().getGlobalVariables().get("$!");
        try {
            IRubyObject cmp = invokedynamic(getRuntime().getCurrentContext(),
                    this, OP_CMP, other);
            
            // if RubyBasicObject#op_cmp is used, the result may be nil
            if (!cmp.isNil()) {
                return (int) cmp.convertToInteger().getLongValue();
            }
        } catch (RaiseException ex) {
            getRuntime().getGlobalVariables().set("$!", oldExc);
        }
        
        /* We used to raise an error if two IRubyObject were not comparable, but
         * in order to support the new ConcurrentHashMapV8 and other libraries
         * and containers that arbitrarily call compareTo expecting it to always
         * succeed, we have opted to return 0 here. This will allow all
         * RubyBasicObject subclasses to be compared, but if the comparison is
         * not valid we they will appear the same for sorting purposes.
         * 
         * See https://jira.codehaus.org/browse/JRUBY-7013
         */
        return 0;
    }

    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return op_equal_19(context, obj);
    }

    /** rb_obj_equal
     *
     * Will by default use identity equality to compare objects. This
     * follows the Ruby semantics.
     *
     * The name of this method doesn't follow the convention because hierarchy problems
     */
    @JRubyMethod(name = "==")
    public IRubyObject op_equal_19(ThreadContext context, IRubyObject obj) {
        return this == obj ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        // Remain unimplemented due to problems with the double java hierarchy
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "equal?", required = 1)
    public IRubyObject equal_p19(ThreadContext context, IRubyObject other) {
        return op_equal_19(context, other);
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "==" method.
     */
    protected static boolean equalInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        return that == other || invokedynamic(context, that, OP_EQUAL, other).isTrue();
    }

    /** method used for Hash key comparison (specialized for String, Symbol and Fixnum)
     *
     * Will by default just call the Ruby method "eql?"
     */
    @Override
    public boolean eql(IRubyObject other) {
        return invokedynamic(getRuntime().getCurrentContext(), this, EQL, other).isTrue();
    }

    /**
     * Adds the specified object as a finalizer for this object.
     */
    @Override
    public void addFinalizer(IRubyObject f) {
        Finalizer finalizer = (Finalizer)getInternalVariable("__finalizer__");
        if (finalizer == null) {
            // since this is the first time we're registering a finalizer, we
            // must also register this object in ObjectSpace, so that future
            // calls to undefine_finalizer, which takes an object ID, can
            // locate the object properly. See JRUBY-4839.
            long id = getObjectId();
            RubyFixnum fixnumId = (RubyFixnum)id();

            getRuntime().getObjectSpace().registerObjectId(id, this);

            finalizer = new Finalizer(fixnumId);
            fastSetInternalVariable("__finalizer__", finalizer);
            getRuntime().addFinalizer(finalizer);
        }
        finalizer.addFinalizer(f);
    }

    /**
     * Remove all the finalizers for this object.
     */
    @Override
    public void removeFinalizers() {
        Finalizer finalizer = (Finalizer)getInternalVariable("__finalizer__");
        if (finalizer != null) {
            finalizer.removeFinalizers();
            removeInternalVariable("__finalizer__");
            getRuntime().removeFinalizer(finalizer);
        }
    }

    @Override
    public Object getVariable(int index) {
        return VariableAccessor.getVariable(this, index);
    }
    
    @Override
    public void setVariable(int index, Object value) {
        ensureInstanceVariablesSettable();
        if (index < 0) return;
        metaClass.getVariableTableManager().setVariableInternal(this, index, value);
    }

    public final Object getFFIHandle() {
        return metaClass.getVariableTableManager().getFFIHandle(this);
    }

    public final void setFFIHandle(Object value) {
        metaClass.getVariableTableManager().setFFIHandle(this, value);
    }

    //
    // COMMON VARIABLE METHODS
    //

    /**
     * Returns true if object has any variables
     * 
     * @see VariableTableManager#hasVariables(org.jruby.RubyBasicObject) 
     */
    @Override
    public boolean hasVariables() {
        return metaClass.getVariableTableManager().hasVariables(this);
    }

    /**
     * Gets a list of all variables in this object.
     */
    // TODO: must override in RubyModule to pick up constants
    @Override
    public List<Variable<Object>> getVariableList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<Variable<Object>> list = new ArrayList<Variable<Object>>();
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
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
    @Override
   public List<String> getVariableNameList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
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
        return metaClass.getVariableAccessorForRead(name).get(this) != null;
    }

    /**
     * Fetch an object from the variable table based on the name.
     *
     * @return the object or null if not found
     */
    protected Object variableTableFetch(String name) {
        return metaClass.getVariableAccessorForRead(name).get(this);
    }

    /**
     * Store a value in the variable store under the specific name.
     */
    protected Object variableTableStore(String name, Object value) {
        metaClass.getVariableAccessorForWrite(name).set(this, value);
        return value;
    }

    /**
     * Removes the entry with the specified name from the variable
     * table, and returning the removed value.
     */
    protected Object variableTableRemove(String name) {
        return metaClass.getVariableTableManager().clearVariable(this, name);
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
    @Override
    public InternalVariables getInternalVariables() {
        return this;
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#hasInternalVariable
     */
    @Override
    public boolean hasInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableContains(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#getInternalVariable
     */
    @Override
    public Object getInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableFetch(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#setInternalVariable
     */
    @Override
    public void setInternalVariable(String name, Object value) {
        assert !IdUtil.isRubyVariable(name);
        variableTableStore(name, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InternalVariables#removeInternalVariable
     */
    @Override
    public Object removeInternalVariable(String name) {
        assert !IdUtil.isRubyVariable(name);
        return variableTableRemove(name);
    }
    
    /**
     * Sync one this object's variables with other's - this is used to make
     * rbClone work correctly.
     */
    @Override
    public void syncVariables(IRubyObject other) {
        metaClass.getVariableTableManager().syncVariables(this, other);
    }
    
    //
    // INSTANCE VARIABLE API METHODS
    //

    /**
     * Dummy method to avoid a cast, and to avoid polluting the
     * IRubyObject interface with all the instance variable management
     * methods.
     */
    @Override
    public InstanceVariables getInstanceVariables() {
        return this;
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#hasInstanceVariable
     */
    @Override
    public boolean hasInstanceVariable(String name) {
        return variableTableContains(name);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#getInstanceVariable
     */
    @Override
    public IRubyObject getInstanceVariable(String name) {
        return (IRubyObject)variableTableFetch(name);
    }

    /** rb_iv_set / rb_ivar_set
    *
    * @see org.jruby.runtime.builtin.InstanceVariables#setInstanceVariable
    */
    @Override
    public IRubyObject setInstanceVariable(String name, IRubyObject value) {
        assert value != null;
        ensureInstanceVariablesSettable();
        return (IRubyObject)variableTableStore(name, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#removeInstanceVariable
     */
    @Override
    public IRubyObject removeInstanceVariable(String name) {
        ensureInstanceVariablesSettable();
        return (IRubyObject)variableTableRemove(name);
    }

    /**
     * Gets a list of all variables in this object.
     */
    // TODO: must override in RubyModule to pick up constants
    @Override
    public List<Variable<IRubyObject>> getInstanceVariableList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<Variable<IRubyObject>> list = new ArrayList<Variable<IRubyObject>>();
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
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
    @Override
   public List<String> getInstanceVariableNameList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
            Object value = entry.getValue().get(this);
            if (value == null || !(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(entry.getKey())) continue;
            list.add(entry.getKey());
        }
        return list;
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#getInstanceVariableNameList
     */
    @Override
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
    public final void ensureInstanceVariablesSettable() {
        if (!isFrozen() || isImmediate()) {
            return;
        }
        raiseFrozenError();
    }

    private void raiseFrozenError() throws RaiseException {
        if (this instanceof RubyModule) {
            throw getRuntime().newFrozenError("class/module ");
        } else {
            throw getRuntime().newFrozenError(getMetaClass().toString());
        }
    }

    @Deprecated
    @Override
    public final int getNativeTypeIndex() {
        return getNativeClassIndex().ordinal();
    }
    
    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.BASICOBJECT;
    }

    /**
     * A method to determine whether the method named by methodName is a builtin
     * method.  This means a method with a JRubyMethod annotation written in
     * Java.
     *
     * @param methodName to look for.
     * @return true if so
     */
    public boolean isBuiltin(String methodName) {
        return getMetaClass().isMethodBuiltin(methodName);
    }

    @JRubyMethod(name = "singleton_method_added", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_added19(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "singleton_method_removed", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_removed19(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "singleton_method_undefined", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_undefined19(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "method_missing", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject method_missing19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        if (args.length == 0 || !(args[0] instanceof RubySymbol)) {
            throw context.runtime.newArgumentError("no id given");
        }

        return RubyKernel.methodMissingDirect(context, recv, (RubySymbol)args[0], lastVis, lastCallType, args, block);
    }

    @JRubyMethod(name = "__send__", omit = true)
    public IRubyObject send19(ThreadContext context, IRubyObject arg0, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, block);
    }
    @JRubyMethod(name = "__send__", omit = true)
    public IRubyObject send19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, arg1, block);
    }
    @JRubyMethod(name = "__send__", omit = true)
    public IRubyObject send19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, arg1, arg2, block);
    }
    @JRubyMethod(name = "__send__", required = 1, rest = true, omit = true)
    public IRubyObject send19(ThreadContext context, IRubyObject[] args, Block block) {
        String name = RubySymbol.objectToSymbolString(args[0]);
        int newArgsLength = args.length - 1;

        IRubyObject[] newArgs;
        if (newArgsLength == 0) {
            newArgs = IRubyObject.NULL_ARRAY;
        } else {
            newArgs = new IRubyObject[newArgsLength];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }

        return getMetaClass().finvoke(context, this, name, newArgs, block);
    }
    
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval19(ThreadContext context, Block block) {
        return specificEval(context, getInstanceEvalClass(), block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval19(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, arg2, block, EvalType.INSTANCE_EVAL);
    }

    @JRubyMethod(name = "instance_exec", optional = 3, rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_exec19(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }

        RubyModule klazz;
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            klazz = context.runtime.getDummy();
        } else {
            klazz = getSingletonClass();
        }

        return yieldUnder(context, klazz, args, block, EvalType.INSTANCE_EVAL);
    }

    /**
     * Will yield to the specific block changing the self to be the
     * current object instead of the self that is part of the frame
     * saved in the block frame. This method is the basis for the Ruby
     * instance_eval and module_eval methods. The arguments sent in to
     * it in the args array will be yielded to the block. This makes
     * it possible to emulate both instance_eval and instance_exec
     * with this implementation.
     */
    protected IRubyObject yieldUnder(final ThreadContext context, RubyModule under, IRubyObject[] args, Block block, EvalType evalType) {
        context.preExecuteUnder(this, under, block);

        IRubyObject savedBindingSelf = block.getBinding().getSelf();
        IRubyObject savedFrameSelf = block.getBinding().getFrame().getSelf();
        Visibility savedVisibility = block.getBinding().getVisibility();
        block.getBinding().setVisibility(PUBLIC);

        try {
            if (args.length == 1) {
                IRubyObject valueInYield = args[0];
                return setupBlock(block, evalType).yieldNonArray(context, valueInYield, this); // context.getRubyClass());
            } else {
                IRubyObject valueInYield = RubyArray.newArrayNoCopy(context.runtime, args);
                return setupBlock(block, evalType).yieldArray(context, valueInYield, this);  // context.getRubyClass());
            }
            //TODO: Should next and return also catch here?
        } catch (JumpException.BreakJump bj) {
            return (IRubyObject) bj.getValue();
        } finally {
            block.getBinding().setVisibility(savedVisibility);
            block.getBinding().setSelf(savedBindingSelf);
            block.getBinding().getFrame().setSelf(savedFrameSelf);

            context.postExecuteUnder();
        }
    }

    private Block setupBlock(Block block, EvalType evalType) {
        // FIXME: This is an ugly hack to resolve JRUBY-1381; I'm not proud of it
        return block.cloneBlockForEval(this, evalType);
    }

    /**
     * Will yield to the specific block changing the self to be the
     * current object instead of the self that is part of the frame
     * saved in the block frame. This method is the basis for the Ruby
     * instance_eval and module_eval methods. The arguments sent in to
     * it in the args array will be yielded to the block. This makes
     * it possible to emulate both instance_eval and instance_exec
     * with this implementation.
     */
    protected IRubyObject yieldUnder(final ThreadContext context, RubyModule under, Block block, EvalType evalType) {
        context.preExecuteUnder(this, under, block);

        IRubyObject savedBindingSelf = block.getBinding().getSelf();
        IRubyObject savedFrameSelf = block.getBinding().getFrame().getSelf();
        Visibility savedVisibility = block.getBinding().getVisibility();
        block.getBinding().setVisibility(PUBLIC);

        try {
            return setupBlock(block, evalType).yieldNonArray(context, this, this); //, context.getRubyClass());
            //TODO: Should next and return also catch here?
        } catch (JumpException.BreakJump bj) {
            return (IRubyObject) bj.getValue();
        } finally {
            block.getBinding().setVisibility(savedVisibility);
            block.getBinding().setSelf(savedBindingSelf);
            block.getBinding().getFrame().setSelf(savedFrameSelf);

            context.postExecuteUnder();
        }
    }


    /** specific_eval
     *
     * Evaluates the block or string inside of the context of this
     * object, using the supplied arguments. If a block is given, this
     * will be yielded in the specific context of this object. If no
     * block is given then a String-like object needs to be the first
     * argument, and this string will be evaluated. Second and third
     * arguments in the args-array is optional, but can contain the
     * filename and line of the string under evaluation.
     */
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, Block block, EvalType evalType) {
        if (block.isGiven()) {
            return yieldUnder(context, mod, block, evalType);
        } else {
            throw context.runtime.newArgumentError("block not supplied");
        }
    }

    /** specific_eval
     *
     * Evaluates the block or string inside of the context of this
     * object, using the supplied arguments. If a block is given, this
     * will be yielded in the specific context of this object. If no
     * block is given then a String-like object needs to be the first
     * argument, and this string will be evaluated. Second and third
     * arguments in the args-array is optional, but can contain the
     * filename and line of the string under evaluation.
     */
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg, Block block, EvalType evalType) {
        if (block.isGiven()) {
            throw context.runtime.newArgumentError(1, 0);
        }

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg instanceof RubyString) {
            evalStr = (RubyString)arg;
        } else {
            evalStr = arg.convertToString();
        }

        String file = "(eval)";
        int line = 0;

        return evalUnder(context, mod, evalStr, file, line, evalType);
    }

    /** specific_eval
     *
     * Evaluates the block or string inside of the context of this
     * object, using the supplied arguments. If a block is given, this
     * will be yielded in the specific context of this object. If no
     * block is given then a String-like object needs to be the first
     * argument, and this string will be evaluated. Second and third
     * arguments in the args-array is optional, but can contain the
     * filename and line of the string under evaluation.
     */
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg0, IRubyObject arg1, Block block, EvalType evalType) {
        if (block.isGiven()) {
            throw context.runtime.newArgumentError(2, 0);
        }

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg0 instanceof RubyString) {
            evalStr = (RubyString)arg0;
        } else {
            evalStr = arg0.convertToString();
        }

        String file = arg1.convertToString().asJavaString();
        int line = 0;

        return evalUnder(context, mod, evalStr, file, line, evalType);
    }

    /** specific_eval
     *
     * Evaluates the block or string inside of the context of this
     * object, using the supplied arguments. If a block is given, this
     * will be yielded in the specific context of this object. If no
     * block is given then a String-like object needs to be the first
     * argument, and this string will be evaluated. Second and third
     * arguments in the args-array is optional, but can contain the
     * filename and line of the string under evaluation.
     */
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block, EvalType evalType) {
        if (block.isGiven()) {
            throw context.runtime.newArgumentError(2, 0);
        }

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg0 instanceof RubyString) {
            evalStr = (RubyString)arg0;
        } else {
            evalStr = arg0.convertToString();
        }

        String file = arg1.convertToString().asJavaString();
        int line = (int)(arg2.convertToInteger().getLongValue() - 1);

        return evalUnder(context, mod, evalStr, file, line, evalType);
    }

    protected RubyModule getInstanceEvalClass() {
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            return getRuntime().getDummy();
        } else {
            return getSingletonClass();
        }
    }

    /**
     * Evaluates the string src with self set to the current object,
     * using the module under as the context.
     */
    public IRubyObject evalUnder(final ThreadContext context, RubyModule under, RubyString src, String file, int line, EvalType evalType) {
        return Interpreter.evalSimple(context, under, this, src, file, line, evalType);
    }

    /**
     * Class that keeps track of the finalizers for the object under
     * operation.
     */
    public static class Finalizer implements Finalizable {
        private RubyFixnum id;
        private IRubyObject firstFinalizer;
        private List<IRubyObject> finalizers;
        private AtomicBoolean finalized;

        public Finalizer(RubyFixnum id) {
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
            Helpers.invoke(
                    finalizer.getRuntime().getCurrentContext(),
                    finalizer, "call", id);
        }
    }

    // These are added to allow their being called against BasicObject and
    // subclass instances. Because Kernel can be included into BasicObject and
    // subclasses, there's a possibility of calling them from Ruby.
    // See JRUBY-4871

    /** rb_obj_equal
     *
     * Will use Java identity equality.
     */
    public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
        return this == obj ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    /** rb_obj_equal
     *
     * Just like "==" and "equal?", "eql?" will use identity equality for Object.
     */
    public IRubyObject eql_p(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (this == other || invokedynamic(context, this, OP_EQUAL, other).isTrue()){
            return RubyFixnum.zero(runtime);
        }
        return runtime.getNil();
    }

    /** rb_obj_init_copy
     *
     * Initializes this object as a copy of the original, that is the
     * parameter to this object. Will make sure that the argument
     * actually has the same real class as this object. It shouldn't
     * be possible to initialize an object with something totally
     * different.
     */
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) {
            return this;
        }
        checkFrozen();

        if (getMetaClass().getRealClass() != original.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("initialize_copy should take same class object");
        }

        return this;
    }

    /**
     * The actual method that checks frozen with the default frozen message from MRI.
     * If possible, call this instead of {@link #testFrozen}.
     */
    public void checkFrozen() {
        testFrozen();
    }

    /** obj_respond_to
     *
     * respond_to?( aSymbol, includePriv=false ) -> true or false
     *
     * Returns true if this object responds to the given method. Private
     * methods are included in the search only if the optional second
     * parameter evaluates to true.
     *
     * @return true if this responds to the given method
     *
     * !!! For some reason MRI shows the arity of respond_to? as -1, when it should be -2; that's why this is rest instead of required, optional = 1
     *
     * Going back to splitting according to method arity. MRI is wrong
     * about most of these anyway, and since we have arity splitting
     * in both the compiler and the interpreter, the performance
     * benefit is important for this method.
     */
    public RubyBoolean respond_to_p(IRubyObject mname) {
        String name = mname.asJavaString();
        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, true));
    }

    public IRubyObject respond_to_p19(IRubyObject mname) {
        String name = mname.asJavaString();
        IRubyObject respond = getRuntime().newBoolean(getMetaClass().isMethodBound(name, true, true));
        if (!respond.isTrue()) {
            respond = Helpers.invoke(getRuntime().getCurrentContext(), this, "respond_to_missing?", mname, getRuntime().getFalse());
            respond = getRuntime().newBoolean(respond.isTrue());
        }
        return respond;
    }

    /** obj_respond_to
     *
     * respond_to?( aSymbol, includePriv=false ) -> true or false
     *
     * Returns true if this object responds to the given method. Private
     * methods are included in the search only if the optional second
     * parameter evaluates to true.
     *
     * @return true if this responds to the given method
     *
     * !!! For some reason MRI shows the arity of respond_to? as -1, when it should be -2; that's why this is rest instead of required, optional = 1
     *
     * Going back to splitting according to method arity. MRI is wrong
     * about most of these anyway, and since we have arity splitting
     * in both the compiler and the interpreter, the performance
     * benefit is important for this method.
     */
    public RubyBoolean respond_to_p(IRubyObject mname, IRubyObject includePrivate) {
        String name = mname.asJavaString();
        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, !includePrivate.isTrue()));
    }

    public IRubyObject respond_to_p19(IRubyObject mname, IRubyObject includePrivate) {
        String name = mname.asJavaString();
        IRubyObject respond = getRuntime().newBoolean(getMetaClass().isMethodBound(name, !includePrivate.isTrue()));
        if (!respond.isTrue()) {
            respond = Helpers.invoke(getRuntime().getCurrentContext(), this, "respond_to_missing?", mname, includePrivate);
            respond = getRuntime().newBoolean(respond.isTrue());
        }
        return respond;
    }

    /** rb_obj_id
     *
     * Will return the hash code of this object. In comparison to MRI,
     * this method will use the Java identity hash code instead of
     * using rb_obj_id, since the usage of id in JRuby will incur the
     * cost of some. ObjectSpace maintenance.
     */
    public RubyFixnum hash() {
        return getRuntime().newFixnum(super.hashCode());
    }

    /** rb_obj_class
     *
     * Returns the real class of this object, excluding any
     * singleton/meta class in the inheritance chain.
     */
    public RubyClass type() {
        return getMetaClass().getRealClass();
    }

    /** rb_obj_type
     *
     * The deprecated version of type, that emits a deprecation
     * warning.
     */
    public RubyClass type_deprecated() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Object#type is deprecated; use Object#class");
        return type();
    }

    /** rb_obj_display
     *
     *  call-seq:
     *     obj.display(port=$>)    => nil
     *
     *  Prints <i>obj</i> on the given port (default <code>$></code>).
     *  Equivalent to:
     *
     *     def display(port=$>)
     *       port.write self
     *     end
     *
     *  For example:
     *
     *     1.display
     *     "cat".display
     *     [ 4, 5, 6 ].display
     *     puts
     *
     *  <em>produces:</em>
     *
     *     1cat456
     *
     */
    public IRubyObject display(ThreadContext context, IRubyObject[] args) {
        IRubyObject port = args.length == 0 ? context.runtime.getGlobalVariables().get("$>") : args[0];

        port.callMethod(context, "write", this);

        return context.runtime.getNil();
    }

    /** rb_obj_tainted
     *
     *  call-seq:
     *     obj.tainted?    => true or false
     *
     *  Returns <code>true</code> if the object is tainted.
     *
     */
    public RubyBoolean tainted_p(ThreadContext context) {
        return context.runtime.newBoolean(isTaint());
    }

    /** rb_obj_taint
     *
     *  call-seq:
     *     obj.taint -> obj
     *
     *  Marks <i>obj</i> as tainted---if the <code>$SAFE</code> level is
     *  set appropriately, many method calls which might alter the running
     *  programs environment will refuse to accept tainted strings.
     */
    public IRubyObject taint(ThreadContext context) {
        taint(context.runtime);
        return this;
    }

    /** rb_obj_untaint
     *
     *  call-seq:
     *     obj.untaint    => obj
     *
     *  Removes the taint from <i>obj</i>.
     *
     *  Only callable in if more secure than 3.
     */
    public IRubyObject untaint(ThreadContext context) {
        if (isTaint()) {
            testFrozen();
            setTaint(false);
        }

        return this;
    }

    /** rb_obj_freeze
     *
     *  call-seq:
     *     obj.freeze    => obj
     *
     *  Prevents further modifications to <i>obj</i>. A
     *  <code>TypeError</code> will be raised if modification is attempted.
     *  There is no way to unfreeze a frozen object. See also
     *  <code>Object#frozen?</code>.
     *
     *     a = [ "a", "b", "c" ]
     *     a.freeze
     *     a << "z"
     *
     *  <em>produces:</em>
     *
     *     prog.rb:3:in `<<': can't modify frozen array (TypeError)
     *     	from prog.rb:3
     */
    public IRubyObject freeze(ThreadContext context) {
        Ruby runtime = context.runtime;
        if ((flags & FROZEN_F) == 0) {
            flags |= FROZEN_F;
        }
        return this;
    }

    /** rb_obj_frozen_p
     *
     *  call-seq:
     *     obj.frozen?    => true or false
     *
     *  Returns the freeze status of <i>obj</i>.
     *
     *     a = [ "a", "b", "c" ]
     *     a.freeze    #=> ["a", "b", "c"]
     *     a.frozen?   #=> true
     */
    public RubyBoolean frozen_p(ThreadContext context) {
        return context.runtime.newBoolean(isFrozen());
    }

    /** rb_obj_is_instance_of
     *
     *  call-seq:
     *     obj.instance_of?(class)    => true or false
     *
     *  Returns <code>true</code> if <i>obj</i> is an instance of the given
     *  class. See also <code>Object#kind_of?</code>.
     */
    public RubyBoolean instance_of_p(ThreadContext context, IRubyObject type) {
        if (type() == type) {
            return context.runtime.getTrue();
        } else if (!(type instanceof RubyModule)) {
            throw context.runtime.newTypeError("class or module required");
        } else {
            return context.runtime.getFalse();
        }
    }


    /** rb_obj_is_kind_of
     *
     *  call-seq:
     *     obj.is_a?(class)       => true or false
     *     obj.kind_of?(class)    => true or false
     *
     *  Returns <code>true</code> if <i>class</i> is the class of
     *  <i>obj</i>, or if <i>class</i> is one of the superclasses of
     *  <i>obj</i> or modules included in <i>obj</i>.
     *
     *     module M;    end
     *     class A
     *       include M
     *     end
     *     class B < A; end
     *     class C < B; end
     *     b = B.new
     *     b.instance_of? A   #=> false
     *     b.instance_of? B   #=> true
     *     b.instance_of? C   #=> false
     *     b.instance_of? M   #=> false
     *     b.kind_of? A       #=> true
     *     b.kind_of? B       #=> true
     *     b.kind_of? C       #=> false
     *     b.kind_of? M       #=> true
     */
    public RubyBoolean kind_of_p(ThreadContext context, IRubyObject type) {
        // TODO: Generalize this type-checking code into IRubyObject helper.
        if (!(type instanceof RubyModule)) {
            // TODO: newTypeError does not offer enough for ruby error string...
            throw context.runtime.newTypeError("class or module required");
        }

        return context.runtime.newBoolean(((RubyModule) type).isInstance(this));
    }

    /** rb_obj_methods
     *
     *  call-seq:
     *     obj.methods    => array
     *
     *  Returns a list of the names of methods publicly accessible in
     *  <i>obj</i>. This will include all the methods accessible in
     *  <i>obj</i>'s ancestors.
     *
     *     class Klass
     *       def kMethod()
     *       end
     *     end
     *     k = Klass.new
     *     k.methods[0..9]    #=> ["kMethod", "freeze", "nil?", "is_a?",
     *                             "class", "instance_variable_set",
     *                              "methods", "extend", "__send__", "instance_eval"]
     *     k.methods.length   #=> 42
     */
    public IRubyObject methods(ThreadContext context, IRubyObject[] args) {
        return methods(context, args, false);
    }
    public IRubyObject methods19(ThreadContext context, IRubyObject[] args) {
        return methods(context, args, true);
    }

    public IRubyObject methods(ThreadContext context, IRubyObject[] args, boolean useSymbols) {
        boolean all = args.length == 1 ? args[0].isTrue() : true;
        Ruby runtime = getRuntime();
        RubyArray methods = runtime.newArray();
        Set<String> seen = new HashSet<String>();

        if (getMetaClass().isSingleton()) {
            getMetaClass().populateInstanceMethodNames(seen, methods, PRIVATE, true, useSymbols, false);
            if (all) {
                getMetaClass().getSuperClass().populateInstanceMethodNames(seen, methods, PRIVATE, true, useSymbols, true);
            }
        } else if (all) {
            getMetaClass().populateInstanceMethodNames(seen, methods, PRIVATE, true, useSymbols, true);
        } else {
            // do nothing, leave empty
        }

        return methods;
    }

    /** rb_obj_public_methods
     *
     *  call-seq:
     *     obj.public_methods(all=true)   => array
     *
     *  Returns the list of public methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     */
    public IRubyObject public_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().public_instance_methods(trueIfNoArgument(context, args));
    }

    public IRubyObject public_methods19(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().public_instance_methods19(trueIfNoArgument(context, args));
    }

    /** rb_obj_protected_methods
     *
     *  call-seq:
     *     obj.protected_methods(all=true)   => array
     *
     *  Returns the list of protected methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     *
     *  Internally this implementation uses the
     *  {@link RubyModule#protected_instance_methods} method.
     */
    public IRubyObject protected_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().protected_instance_methods(trueIfNoArgument(context, args));
    }

    public IRubyObject protected_methods19(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().protected_instance_methods19(trueIfNoArgument(context, args));
    }

    /** rb_obj_private_methods
     *
     *  call-seq:
     *     obj.private_methods(all=true)   => array
     *
     *  Returns the list of private methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     *
     *  Internally this implementation uses the
     *  {@link RubyModule#private_instance_methods} method.
     */
    public IRubyObject private_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().private_instance_methods(trueIfNoArgument(context, args));
    }

    public IRubyObject private_methods19(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().private_instance_methods19(trueIfNoArgument(context, args));
    }

    // FIXME: If true array is common enough we should pre-allocate and stick somewhere
    private IRubyObject[] trueIfNoArgument(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? new IRubyObject[] { context.runtime.getTrue() } : args;
    }

    /** rb_obj_singleton_methods
     *
     *  call-seq:
     *     obj.singleton_methods(all=true)    => array
     *
     *  Returns an array of the names of singleton methods for <i>obj</i>.
     *  If the optional <i>all</i> parameter is true, the list will include
     *  methods in modules included in <i>obj</i>.
     *
     *     module Other
     *       def three() end
     *     end
     *
     *     class Single
     *       def Single.four() end
     *     end
     *
     *     a = Single.new
     *
     *     def a.one()
     *     end
     *
     *     class << a
     *       include Other
     *       def two()
     *       end
     *     end
     *
     *     Single.singleton_methods    #=> ["four"]
     *     a.singleton_methods(false)  #=> ["two", "one"]
     *     a.singleton_methods         #=> ["two", "one", "three"]
     */
    // TODO: This is almost RubyModule#instance_methods on the metaClass.  Perhaps refactor.
    public RubyArray singleton_methods(ThreadContext context, IRubyObject[] args) {
        return singletonMethods(context, args, methodsCollector);
    }

    public RubyArray singleton_methods19(ThreadContext context, IRubyObject[] args) {
        return singletonMethods(context, args, methodsCollector19);
    }

    private RubyArray singletonMethods(ThreadContext context, IRubyObject[] args, MethodsCollector collect) {
        boolean all = true;
        if(args.length == 1) {
            all = args[0].isTrue();
        }

        if (getMetaClass().isSingleton()) {
            IRubyObject[] methodsArgs = new IRubyObject[]{context.runtime.getFalse()};
            RubyArray singletonMethods = collect.instanceMethods(getMetaClass(), methodsArgs);

            if (all) {
                RubyClass superClass = getMetaClass().getSuperClass();
                while (superClass.isSingleton() || superClass.isIncluded()) {
                    singletonMethods.concat(collect.instanceMethods(superClass, methodsArgs));
                    superClass = superClass.getSuperClass();
                }
            }

            singletonMethods.uniq_bang(context);
            return singletonMethods;
        }

        return context.runtime.newEmptyArray();
    }

    private abstract static class MethodsCollector {
        public abstract RubyArray instanceMethods(RubyClass rubyClass, IRubyObject[] args);
    };

    private static final MethodsCollector methodsCollector = new MethodsCollector() {
        @Override
        public RubyArray instanceMethods(RubyClass rubyClass, IRubyObject[] args) {
            return rubyClass.instance_methods(args);
        }
    };

    private static final MethodsCollector methodsCollector19 = new MethodsCollector() {
        @Override
        public RubyArray instanceMethods(RubyClass rubyClass, IRubyObject[] args) {
            return rubyClass.instance_methods19(args);
        }
    };

    /** rb_obj_method
     *
     *  call-seq:
     *     obj.method(sym)    => method
     *
     *  Looks up the named method as a receiver in <i>obj</i>, returning a
     *  <code>Method</code> object (or raising <code>NameError</code>). The
     *  <code>Method</code> object acts as a closure in <i>obj</i>'s object
     *  instance, so instance variables and the value of <code>self</code>
     *  remain available.
     *
     *     class Demo
     *       def initialize(n)
     *         @iv = n
     *       end
     *       def hello()
     *         "Hello, @iv = #{@iv}"
     *       end
     *     end
     *
     *     k = Demo.new(99)
     *     m = k.method(:hello)
     *     m.call   #=> "Hello, @iv = 99"
     *
     *     l = Demo.new('Fred')
     *     m = l.method("hello")
     *     m.call   #=> "Hello, @iv = Fred"
     */
    public IRubyObject method(IRubyObject symbol) {
        return getMetaClass().newMethod(this, symbol.asJavaString(), true, null);
    }

    public IRubyObject method19(IRubyObject symbol) {
        return getMetaClass().newMethod(this, symbol.asJavaString(), true, null, true);
    }

    /** rb_any_to_s
     *
     *  call-seq:
     *     obj.to_s    => string
     *
     *  Returns a string representing <i>obj</i>. The default
     *  <code>to_s</code> prints the object's class and an encoding of the
     *  object id. As a special case, the top-level object that is the
     *  initial execution context of Ruby programs returns ``main.''
     */
    public IRubyObject to_s() {
    	return anyToString();
    }

    /** rb_any_to_a
     *
     *  call-seq:
     *     obj.to_a -> anArray
     *
     *  Returns an array representation of <i>obj</i>. For objects of class
     *  <code>Object</code> and others that don't explicitly override the
     *  method, the return value is an array containing <code>self</code>.
     *  However, this latter behavior will soon be obsolete.
     *
     *     self.to_a       #=> -:1: warning: default `to_a' will be obsolete
     *     "hello".to_a    #=> ["hello"]
     *     Time.new.to_a   #=> [39, 54, 8, 9, 4, 2003, 3, 99, true, "CDT"]
     *
     *  The default to_a method is deprecated.
     */
    public RubyArray to_a() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "default 'to_a' will be obsolete");
        return getRuntime().newArray(this);
    }

    /** rb_obj_instance_eval
     *
     *  call-seq:
     *     obj.instance_eval(string [, filename [, lineno]] )   => obj
     *     obj.instance_eval {| | block }                       => obj
     *
     *  Evaluates a string containing Ruby source code, or the given block,
     *  within the context of the receiver (_obj_). In order to set the
     *  context, the variable +self+ is set to _obj_ while
     *  the code is executing, giving the code access to _obj_'s
     *  instance variables. In the version of <code>instance_eval</code>
     *  that takes a +String+, the optional second and third
     *  parameters supply a filename and starting line number that are used
     *  when reporting compilation errors.
     *
     *     class Klass
     *       def initialize
     *         @secret = 99
     *       end
     *     end
     *     k = Klass.new
     *     k.instance_eval { @secret }   #=> 99
     */
    public IRubyObject instance_eval(ThreadContext context, Block block) {
        return specificEval(context, getInstanceEvalClass(), block, EvalType.INSTANCE_EVAL);
    }
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, block, EvalType.INSTANCE_EVAL);
    }
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, block, EvalType.INSTANCE_EVAL);
    }
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, arg2, block, EvalType.INSTANCE_EVAL);
    }

    /** rb_obj_instance_exec
     *
     *  call-seq:
     *     obj.instance_exec(arg...) {|var...| block }                       => obj
     *
     *  Executes the given block within the context of the receiver
     *  (_obj_). In order to set the context, the variable +self+ is set
     *  to _obj_ while the code is executing, giving the code access to
     *  _obj_'s instance variables.  Arguments are passed as block parameters.
     *
     *     class Klass
     *       def initialize
     *         @secret = 99
     *       end
     *     end
     *     k = Klass.new
     *     k.instance_exec(5) {|x| @secret+x }   #=> 104
     */
    public IRubyObject instance_exec(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) {
            throw context.runtime.newArgumentError("block not supplied");
        }

        RubyModule klazz;
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            klazz = context.runtime.getDummy();
        } else {
            klazz = getSingletonClass();
        }

        return yieldUnder(context, klazz, args, block, EvalType.INSTANCE_EVAL);
    }

    /** rb_obj_extend
     *
     *  call-seq:
     *     obj.extend(module, ...)    => obj
     *
     *  Adds to _obj_ the instance methods from each module given as a
     *  parameter.
     *
     *     module Mod
     *       def hello
     *         "Hello from Mod.\n"
     *       end
     *     end
     *
     *     class Klass
     *       def hello
     *         "Hello from Klass.\n"
     *       end
     *     end
     *
     *     k = Klass.new
     *     k.hello         #=> "Hello from Klass.\n"
     *     k.extend(Mod)   #=> #<Klass:0x401b3bc8>
     *     k.hello         #=> "Hello from Mod.\n"
     */
    public IRubyObject extend(IRubyObject[] args) {
        Ruby runtime = getRuntime();

        // Make sure all arguments are modules before calling the callbacks
        for (int i = 0; i < args.length; i++) {
            if (!args[i].isModule()) throw runtime.newTypeError(args[i], runtime.getModule());
        }

        ThreadContext context = runtime.getCurrentContext();

        // MRI extends in order from last to first
        for (int i = args.length - 1; i >= 0; i--) {
            args[i].callMethod(context, "extend_object", this);
            args[i].callMethod(context, "extended", this);
        }
        return this;
    }

    /** rb_f_send
     *
     * send( aSymbol  [, args  ]*   ) -> anObject
     *
     * Invokes the method identified by aSymbol, passing it any arguments
     * specified. You can use __send__ if the name send clashes with an
     * existing method in this object.
     *
     * <pre>
     * class Klass
     *   def hello(*args)
     *     "Hello " + args.join(' ')
     *   end
     * end
     *
     * k = Klass.new
     * k.send :hello, "gentle", "readers"
     * </pre>
     *
     * @return the result of invoking the method identified by aSymbol.
     */
    public IRubyObject send(ThreadContext context, Block block) {
        throw context.runtime.newArgumentError(0, 1);
    }
    public IRubyObject send(ThreadContext context, IRubyObject arg0, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, block);
    }
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, arg1, block);
    }
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        String name = RubySymbol.objectToSymbolString(arg0);

        return getMetaClass().finvoke(context, this, name, arg1, arg2, block);
    }
    public IRubyObject send(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length == 0) return send(context, block);
        
        String name = RubySymbol.objectToSymbolString(args[0]);
        int newArgsLength = args.length - 1;

        IRubyObject[] newArgs;
        if (newArgsLength == 0) {
            newArgs = IRubyObject.NULL_ARRAY;
        } else {
            newArgs = new IRubyObject[newArgsLength];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }

        return getMetaClass().finvoke(context, this, name, newArgs, block);
    }

    /** rb_false
     *
     * call_seq:
     *   nil.nil?               => true
     *   <anything_else>.nil?   => false
     *
     * Only the object <i>nil</i> responds <code>true</code> to <code>nil?</code>.
     */
    public IRubyObject nil_p(ThreadContext context) {
        return context.runtime.getFalse();
    }

    /** rb_obj_pattern_match
     *
     *  call-seq:
     *     obj =~ other  => false
     *
     *  Pattern Match---Overridden by descendents (notably
     *  <code>Regexp</code> and <code>String</code>) to provide meaningful
     *  pattern-match semantics.
     */
    public IRubyObject op_match(ThreadContext context, IRubyObject arg) {
        return context.runtime.getFalse();
    }

    public IRubyObject op_match19(ThreadContext context, IRubyObject arg) {
        return context.runtime.getNil();
    }

    public IRubyObject op_not_match(ThreadContext context, IRubyObject arg) {
        return context.runtime.newBoolean(!callMethod(context, "=~", arg).isTrue());
    }


    //
    // INSTANCE VARIABLE RUBY METHODS
    //

    /** rb_obj_ivar_defined
     *
     *  call-seq:
     *     obj.instance_variable_defined?(symbol)    => true or false
     *
     *  Returns <code>true</code> if the given instance variable is
     *  defined in <i>obj</i>.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         @a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_defined?(:@a)    #=> true
     *     fred.instance_variable_defined?("@b")   #=> true
     *     fred.instance_variable_defined?("@c")   #=> false
     */
    public IRubyObject instance_variable_defined_p(ThreadContext context, IRubyObject name) {
        if (variableTableContains(validateInstanceVariable(name.asJavaString()))) {
            return context.runtime.getTrue();
        }
        return context.runtime.getFalse();
    }

    /** rb_obj_ivar_get
     *
     *  call-seq:
     *     obj.instance_variable_get(symbol)    => obj
     *
     *  Returns the value of the given instance variable, or nil if the
     *  instance variable is not set. The <code>@</code> part of the
     *  variable name should be included for regular instance
     *  variables. Throws a <code>NameError</code> exception if the
     *  supplied symbol is not valid as an instance variable name.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         @a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_get(:@a)    #=> "cat"
     *     fred.instance_variable_get("@b")   #=> 99
     */
    public IRubyObject instance_variable_get(ThreadContext context, IRubyObject name) {
        Object value;
        if ((value = variableTableFetch(validateInstanceVariable(name.asJavaString()))) != null) {
            return (IRubyObject)value;
        }
        return context.runtime.getNil();
    }

    /** rb_obj_ivar_set
     *
     *  call-seq:
     *     obj.instance_variable_set(symbol, obj)    => obj
     *
     *  Sets the instance variable names by <i>symbol</i> to
     *  <i>object</i>, thereby frustrating the efforts of the class's
     *  author to attempt to provide proper encapsulation. The variable
     *  did not have to exist prior to this call.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         @a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_set(:@a, 'dog')   #=> "dog"
     *     fred.instance_variable_set(:@c, 'cat')   #=> "cat"
     *     fred.inspect                             #=> "#<Fred:0x401b3da8 @a=\"dog\", @b=99, @c=\"cat\">"
     */
    public IRubyObject instance_variable_set(IRubyObject name, IRubyObject value) {
        // no need to check for ensureInstanceVariablesSettable() here, that'll happen downstream in setVariable
        return (IRubyObject)variableTableStore(validateInstanceVariable(name.asJavaString()), value);
    }

    /** rb_obj_remove_instance_variable
     *
     *  call-seq:
     *     obj.remove_instance_variable(symbol)    => obj
     *
     *  Removes the named instance variable from <i>obj</i>, returning that
     *  variable's value.
     *
     *     class Dummy
     *       attr_reader :var
     *       def initialize
     *         @var = 99
     *       end
     *       def remove
     *         remove_instance_variable(:@var)
     *       end
     *     end
     *     d = Dummy.new
     *     d.var      #=> 99
     *     d.remove   #=> 99
     *     d.var      #=> nil
     */
    public IRubyObject remove_instance_variable(ThreadContext context, IRubyObject name, Block block) {
        ensureInstanceVariablesSettable();
        IRubyObject value;
        if ((value = (IRubyObject)variableTableRemove(validateInstanceVariable(name.asJavaString()))) != null) {
            return value;
        }
        throw context.runtime.newNameError("instance variable " + name.asJavaString() + " not defined", name.asJavaString());
    }

    /** rb_obj_instance_variables
     *
     *  call-seq:
     *     obj.instance_variables    => array
     *
     *  Returns an array of instance variable names for the receiver. Note
     *  that simply defining an accessor does not create the corresponding
     *  instance variable.
     *
     *     class Fred
     *       attr_accessor :a1
     *       def initialize
     *         @iv = 3
     *       end
     *     end
     *     Fred.new.instance_variables   #=> ["@iv"]
     */
    public RubyArray instance_variables(ThreadContext context) {
        Ruby runtime = context.runtime;
        List<String> nameList = getInstanceVariableNameList();

        RubyArray array = runtime.newArray(nameList.size());

        for (String name : nameList) {
            array.append(runtime.newString(name));
        }

        return array;
    }

    // In 1.9, return symbols
    public RubyArray instance_variables19(ThreadContext context) {
        Ruby runtime = context.runtime;
        List<String> nameList = getInstanceVariableNameList();

        RubyArray array = runtime.newArray(nameList.size());

        for (String name : nameList) {
            array.append(runtime.newSymbol(name));
        }

        return array;
    }

    /**
     * Checks if the name parameter represents a legal instance variable name, and otherwise throws a Ruby NameError
     */
    protected String validateInstanceVariable(String name) {
        if (IdUtil.isValidInstanceVariableName(name)) return name;

        throw getRuntime().newNameError("`" + name + "' is not allowable as an instance variable name", name);
    }
    
    /**
     * Serialization of a Ruby (basic) object involves three steps:
     * 
     * <ol>
     * <li>Dump the object itself</li>
     * <li>Dump a String used to load the appropriate Ruby class</li>
     * <li>Dump each variable from varTable in turn</li>
     * </ol>
     * 
     * The metaClass field is marked transient since Ruby classes generally will
     * not be able to serialize (since they hold references to method tables,
     * other classes, and potentially thread-, runtime-, or jvm-local state.
     * 
     * The varTable field is transient because the layout of the same class may
     * differ across runtimes, since it is determined at runtime based on the
     * order in which variables get assigned for a given class. We serialize
     * entries by name to allow other layouts to work properly.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        if (metaClass.isSingleton()) {
            throw new IOException("can not serialize singleton object");
        }
        
        oos.defaultWriteObject();
        oos.writeUTF(metaClass.getName());
        
        metaClass.getVariableTableManager().serializeVariables(this, oos);
    }
    
    /**
     * Deserialization proceeds as follows:
     * 
     * <ol>
     * <li>Deserialize the object instance. It will have null metaClass and
     * varTable fields.</li>
     * <li>Deserialize the name of the object's class, and retrieve class from a
     * thread-local JRuby instance.</li>
     * <li>Retrieve each variable in turn, re-assigning them by name.</li>
     * </ol>
     * 
     * @see RubyBasicObject#writeObject(java.io.ObjectOutputStream) 
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Ruby ruby = Ruby.getThreadLocalRuntime();
        
        if (ruby == null) {
            throw new IOException("No thread-local org.jruby.Ruby available; can't deserialize Ruby object. Set with Ruby#setThreadLocalRuntime.");
        }
        
        ois.defaultReadObject();
        metaClass = (RubyClass)ruby.getClassFromPath(ois.readUTF());
        
        metaClass.getVariableTableManager().deserializeVariables(this, ois);
    }

    // Deprecated methods below this line

    @Deprecated
    public IRubyObject initialize() {
        return getRuntime().getNil();
    }

    @Deprecated
    protected RubyBasicObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        this(runtime, metaClass, useObjectSpace);
    }

    @Deprecated
    @Override
    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) {
        return Helpers.invokeSuper(context, this, args, block);
    }

    @Deprecated
    @Override
    public final IRubyObject callMethod(ThreadContext context, int methodIndex, String name) {
        return Helpers.invoke(context, this, name);
    }

    @Deprecated
    @Override
    public final IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg) {
        return Helpers.invoke(context, this, name, arg, Block.NULL_BLOCK);
    }

    @Deprecated
    @Override
    public RubyInteger convertToInteger(int methodIndex, String convertMethod) {
        return convertToInteger(convertMethod);
    }

    @Deprecated
    @Override
    public int getVariableCount() {
        return getMetaClass().getVariableTableSize();
    }

    @Deprecated
    protected boolean variableTableFastContains(String internedName) {
        return variableTableContains(internedName);
    }

    @Deprecated
    protected Object variableTableFastFetch(String internedName) {
        return variableTableFetch(internedName);
    }

    @Deprecated
    protected Object variableTableFastStore(String internedName, Object value) {
        return variableTableStore(internedName, value);
    }

    @Deprecated
    @Override
    public boolean fastHasInternalVariable(String internedName) {
        return hasInternalVariable(internedName);
    }

    @Deprecated
    @Override
    public Object fastGetInternalVariable(String internedName) {
        return getInternalVariable(internedName);
    }

    @Deprecated
    @Override
    public void fastSetInternalVariable(String internedName, Object value) {
        setInternalVariable(internedName, value);
    }

    @Deprecated
    @Override
    public void syncVariables(List<Variable<Object>> variables) {
        variableTableSync(variables);
    }

    @Deprecated
    @Override
    public boolean fastHasInstanceVariable(String internedName) {
        return hasInstanceVariable(internedName);
    }

    @Deprecated
    @Override
    public IRubyObject fastGetInstanceVariable(String internedName) {
        return getInstanceVariable(internedName);
    }

    @Deprecated
    @Override
    public IRubyObject fastSetInstanceVariable(String internedName, IRubyObject value) {
        return setInstanceVariable(internedName, value);
    }

    @Deprecated
    public static final int UNTRUST_F = 1 << 3;

    @Override
    @Deprecated
    public boolean isUntrusted() {
        return isTaint();
    }

    @Override
    @Deprecated
    public void setUntrusted(boolean untrusted) {
        setTaint(untrusted);
    }

    @Deprecated
    public RubyBoolean untrusted_p(ThreadContext context) {
        return tainted_p(context);
    }

    @Deprecated
    public IRubyObject untrust(ThreadContext context) {
        return taint(context);
    }

    @Deprecated
    public IRubyObject trust(ThreadContext context) {
        return untaint(context);
    }

    @Deprecated
    public final Object getNativeHandle() {
        return null;
    }

    @Deprecated
    public final void setNativeHandle(Object value) {
    }
}
