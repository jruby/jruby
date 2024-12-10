/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.JavaSites.BasicObjectSites;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.ivars.VariableAccessor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

import static org.jruby.anno.FrameField.*;
import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.ir.runtime.IRRuntimeHelpers.dupIfKeywordRestAtCallsite;
import static org.jruby.ir.runtime.IRRuntimeHelpers.getCurrentClassBase;
import static org.jruby.runtime.Helpers.invokeChecked;
import static org.jruby.runtime.ThreadContext.*;
import static org.jruby.runtime.Visibility.*;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.CoreObjectType;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;
import org.jruby.util.TypeConverter;
import org.jruby.util.unsafe.UnsafeHolder;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;
import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.runtime.invokedynamic.MethodNames.INSPECT;
import static org.jruby.util.Inspector.*;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;
import static org.jruby.util.io.EncodingUtils.encStrBufCat;

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
@SuppressWarnings("ComparableType")
public class RubyBasicObject implements Cloneable, IRubyObject, Serializable, Comparable<IRubyObject>, CoreObjectType, InstanceVariables, InternalVariables {

    //private static final Logger LOG = LoggerFactory.getLogger(RubyBasicObject.class);

    //private static final boolean DEBUG = false;

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
    public static final int FALSE_F = ObjectFlags.FALSE_F;
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
    public static final int NIL_F = ObjectFlags.NIL_F;
    public static final int FROZEN_F = ObjectFlags.FROZEN_F;

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
    public static final ObjectAllocator BASICOBJECT_ALLOCATOR = RubyBasicObject::new;

    /**
     * Will create the Ruby class Object in the runtime
     * specified. This method needs to take the actual class as an
     * argument because of the Object class' central part in runtime
     * initialization.
     */
    public static void createBasicObjectClass(Ruby runtime, RubyClass Object) {
        Object.classIndex(ClassIndex.OBJECT).defineAnnotatedMethodsIndividually(RubyBasicObject.class);
        recacheBuiltinMethods(runtime);
    }

    static void recacheBuiltinMethods(Ruby runtime) {
        RubyModule objectClass = runtime.getBasicObject();

        // Since method_missing is marked module we actually define two builtin versions
        runtime.setDefaultMethodMissing(objectClass.searchMethod("method_missing"),
                objectClass.metaClass.searchMethod("method_missing"));
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
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
     * Path for objects that don't enter objectspace.
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

    /** rb_frozen_class_p
     *
     * Helper to test whether this object is frozen, and if it is will
     * throw an exception based on the message.
     */
   protected final void testFrozen(String message) {
       if (isFrozen()) {
           throw getRuntime().newFrozenError(message, this);
       }
   }

    /** rb_frozen_class_p
     *
     * Helper to test whether this object is frozen, and if it is will
     * throw an exception based on the message.
     */
   protected final void testFrozen() {
       if (isFrozen()) {
           throw getRuntime().newFrozenError((isClass() ? "Class: " : (isModule() ? "Module: " : "object: ")) + inspect(), this);
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
     * Will invoke a named method with no arguments and no block if that method or a custom
     * method missing exists. Otherwise returns null. 1.9: rb_check_funcall
     */
    @Override
    public final IRubyObject checkCallMethod(ThreadContext context, JavaSites.CheckedSites sites) {
        return Helpers.invokeChecked(context, this, sites);
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
        return Helpers.invoke(metaClass.runtime.getCurrentContext(), this, name, args);
    }

    public final IRubyObject callMethod(String name, IRubyObject arg) {
        return Helpers.invoke(metaClass.runtime.getCurrentContext(), this, name, arg);
    }

    public final IRubyObject callMethod(String name) {
        return Helpers.invoke(metaClass.runtime.getCurrentContext(), this, name);
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

    // MRI: special_object_p
    public boolean isSpecialObject() {
        // This is broader than MRI but immediates and numeric overlap so I don't think it will hurt.
        // RubyNumeric vs limited list to also include Numeric/Integer so we need not duplicate clone
        // (and potentially others).
        return isImmediate() || this instanceof RubyNumeric;
    }

    /**
     * if exist return the meta-class else return the type of the object.
     *
     */
    @Override
    public final RubyClass getMetaClass() {
        return metaClass;
    }

    public static RubyClass getMetaClass(IRubyObject arg) {
        return ((RubyBasicObject) arg).metaClass;
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
        RubyClass klass = metaClass.toSingletonClass(this);

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

        klass.setMetaClass(superClass.getRealClass().metaClass);

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
    public final RubyClass getType() {
        return metaClass.getRealClass();
    }

    /**
     * Does this object respond to the specified message? Uses a
     * shortcut if it can be proved that respond_to? and respond_to_missing?
     * haven't been overridden.
     */
    @Override
    public final boolean respondsTo(String name) {
        final Ruby runtime = metaClass.runtime;
        final CacheEntry entry = metaClass.searchWithCache("respond_to?");
        final DynamicMethod respondTo = entry.method;

        // fastest path; builtin respond_to? and respond_to_missing? so we just check isMethodBound
        if ( respondTo.equals(runtime.getRespondToMethod()) &&
                metaClass.searchMethod("respond_to_missing?").equals(runtime.getRespondToMissingMethod()) ) {
            return metaClass.respondsToMethod(name, false);
        }

        final ThreadContext context = runtime.getCurrentContext();
        final RubySymbol mname = asSymbol(context, name);

        // respond_to? or respond_to_missing? is not defined, so we must dispatch to trigger method_missing
        if ( respondTo.isUndefined() ) {
            return sites(context).respond_to.call(context, this, this, mname).isTrue();
        } else {
            return respondTo.callRespondTo(context, this, "respond_to?", entry.sourceModule, mname);
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
    public final boolean respondsToMissing(String name, boolean incPrivate) {
        CacheEntry entry = metaClass.searchWithCache("respond_to_missing?");
        DynamicMethod method = entry.method;
        // perhaps should try a smart version as for respondsTo above?
        if (method.isUndefined())return false;
        var context = getRuntime().getCurrentContext();
        return method.call(context, this, entry.sourceModule, "respond_to_missing?",
                asSymbol(context, name), incPrivate ? context.tru : context.fals).isTrue();
    }

    /**
     * Will return the runtime that this object is associated with.
     *
     * @return current runtime
     */
    @Override
    public final Ruby getRuntime() {
        return metaClass.runtime;
    }

    // As part of reducing usage of getRuntime() this method was added so it is
    // easier to know how many real uses of getRuntime() we still need to eliminate.
    // IMPORTANT: This method should only be used in deprecated methods.  If you do
    // not have access then you should continue using getRuntime().getCurrentContext()
    // until we can plumb ThreadContext into whatever method needs it.
    @Deprecated
    public final ThreadContext getCurrentContext() {
        return getRuntime().getCurrentContext();
    }

    /**
     * Will return the Java interface that most closely can represent
     * this object, when working through Java integration translations.
     * @return the true Java class of this (Ruby) object
     */
    @Override
    public Class<?> getJavaClass() {
        Object obj = JavaUtil.unwrapJava(dataGetStruct(), null);
        return obj != null ? obj.getClass() : getClass();
    }

    /** rb_to_id
     *
     * Will try to convert this object to a String using the Ruby
     * "to_str" if the object isn't already a String. If this still
     * doesn't work, will throw a Ruby TypeError.
     * @return a (Java) string
     */
    @Override
    public String asJavaString() {
        IRubyObject str = checkStringType();
        if (str.isNil()) throw typeError(getRuntime().getCurrentContext(), this, "String");
        return str.asJavaString();
    }

    /** rb_obj_as_string
     *
     * First converts this object into a String using the "to_s"
     * method and returns it. If
     * to_s doesn't return a Ruby String, {@link #anyToString} is used
     * instead.
     */
    @Override
    public RubyString asString() {
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);
        IRubyObject str = sites.to_s.call(context, this, this);

        if (!(str instanceof RubyString)) return (RubyString) anyToString();
        return (RubyString) str;
    }

    /**
     * Tries to convert this object to a Ruby Array using the "to_ary" method.
     * @return array representation of this
     */
    @Override
    public RubyArray convertToArray() {
        var context = getRuntime().getCurrentContext();
        return (RubyArray) TypeConverter.convertToType(context, this, arrayClass(context), sites(context).to_ary_checked);
    }

    /**
     * Tries to convert this object to a Ruby Hash using the "to_hash" method.
     * @return hash representation of this
     */
    @Override
    public RubyHash convertToHash() {
        ThreadContext context = getRuntime().getCurrentContext();
        return (RubyHash) TypeConverter.convertToType(context, this, hashClass(context), sites(context).to_hash_checked);
    }

    /**
     * Tries to convert this object to a Ruby Float using the "to_f" method.
     * @return float representation of this
     */
    @Override
    public RubyFloat convertToFloat() {
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);
        return (RubyFloat) TypeConverter.convertToType(context, this, runtime.getFloat(), sites.to_f_checked);
    }

    /**
     * Tries to convert this object to a Ruby Integer using the "to_int" method.
     * @return an integer representation of this
     */
    @Override
    public RubyInteger convertToInteger() {
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);

        IRubyObject result = TypeConverter.convertToType(context, this, runtime.getInteger(), sites.to_int_checked, true);

        if (!(result instanceof RubyInteger)) throw typeError(context, "", this, "#to_int should return Integer");

        return (RubyInteger) result;
    }

    /**
     * Tries to convert this object to a Ruby Integer using the supplied conversion method.
     * @param convertMethod conversion method to use e.g. "to_i"
     * @return an integer representation of this
     */
    @Override
    public RubyInteger convertToInteger(String convertMethod) {
        if (convertMethod.equals("to_int")) return convertToInteger();

        IRubyObject result;
        Ruby runtime = metaClass.runtime;
        if (convertMethod.equals("to_i")) {
            ThreadContext context = runtime.getCurrentContext();
            BasicObjectSites sites = sites(context);
            result = TypeConverter.convertToType(context, this, runtime.getInteger(), sites.to_i_checked, true);
        } else {
            result = TypeConverter.convertToType(this, runtime.getInteger(), convertMethod, true);
        }

        if (!(result instanceof RubyInteger)) {
            throw typeError(runtime.getCurrentContext(), str(runtime, types(runtime, getMetaClass()),
                    "#", ids(runtime, convertMethod), " should return Integer"));
        }

        return (RubyInteger) result;
    }

    /**
     * Tries to convert this object to a Ruby String using the "to_str" method.
     * @return a string representation of this
     */
    @Override
    public RubyString convertToString() {
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);
        return (RubyString) TypeConverter.convertToType(context, this, runtime.getString(), sites.to_str_checked);
    }

    /**
     * Internal method that helps to convert any object into the
     * format of a class name and a hex string inside of #&lt;&gt;.
     */
    @Override
    public IRubyObject anyToString() {
        Ruby runtime = metaClass.runtime;

        /* 6:tags 16:addr 1:eos */
        String hex = Integer.toHexString(System.identityHashCode(this));
        ByteList className = metaClass.getRealClass().toRubyString(runtime.getCurrentContext()).getByteList();
        ByteList bytes = new ByteList(2 + className.realSize() + 3 + hex.length() + 1);
        bytes.setEncoding(className.getEncoding());
        bytes.append('#').append('<');
        bytes.append(className);
        bytes.append(':').append('0').append('x');
        bytes.append(hex.getBytes());
        bytes.append('>');

        RubyString str = RubyString.newString(runtime, bytes);
        return str;
    }

    /**
     * raw (id) strings are not properly encoded but in an iso_8859_1 form.  This method will lookup
     * properly encoded string from the symbol table.
     */
    public RubyString decode(String id) {
        var context = getRuntime().getCurrentContext();
        return (RubyString) asSymbol(context, id).to_s(context);
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
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);
        return TypeConverter.checkStringType(context, sites.to_str_checked, this);
    }

    /** rb_check_array_type
    *
    * Returns the result of trying to convert this object to an Array with "to_ary".
    */
    @Override
    public IRubyObject checkArrayType() {
        Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        BasicObjectSites sites = sites(context);
        return TypeConverter.checkArrayType(context, sites.to_ary_checked, this);
    }

    /**
     * @see IRubyObject#toJava
     */
    @Override
    public <T> T toJava(Class<T> target) {
        return defaultToJava(target);
    }

    final <T> T defaultToJava(Class<T> target) {
        // for callers that unconditionally pass null retval type (JRUBY-4737)
        if (target == void.class) return null;

        final Object value = unwrap_java_object();
        if (value != null) {
            // ensure the object is associated with the wrapper we found it in,
            // so that if it comes back we don't re-wrap it
            if (target.isAssignableFrom(value.getClass())) {
                getRuntime().getJavaSupport().getObjectProxyCache().put(value, this);
                return (T) value;
            }
        }
        else if (JavaUtil.isDuckTypeConvertable(getClass(), target)) {
            synchronized (this) {
                if (unwrap_java_object() != null) { // double check under lock
                    return defaultToJava(target); // concurrent proxy interface impl initialization
                }
                return JavaUtil.convertProcToInterface(getRuntime().getCurrentContext(), this, target);
            }
        }
        else if (!target.isAssignableFrom(getClass())) {
            throw typeError(getRuntime().getCurrentContext(), "cannot convert instance of ", this, " to " + target);
        }

        return (T) this;
    }

    private Object unwrap_java_object() {
        final Object innerWrapper = dataGetStruct(); // java_object
        if (innerWrapper instanceof JavaProxy) { // for interface impls
            return ((JavaProxy) innerWrapper).getObject(); // never null
        }
        return null;
    }

    @Override
    public IRubyObject dup() {
        if (isSpecialObject()) {
            return this;
        }

        IRubyObject dup = metaClass.getRealClass().allocate();
        ThreadContext context = getRuntime().getCurrentContext();

        initCopy(context, dup, this);
        sites(context).initialize_dup.call(context, dup, dup, this);

        return dup;
    }

    /** init_copy
     *
     * Initializes a copy with variable and special instance variable
     * information, and then call the initialize_copy Ruby method.
     */
    private static void initCopy(ThreadContext context, IRubyObject clone, IRubyObject original) {
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        original.copySpecialInstanceVariables(clone);

        if (original.hasVariables()) {
            clone.syncVariables(original);
            ((RubyBasicObject) clone).dupFinalizer();
        }
        if (original instanceof RubyModule) {
            RubyModule cloneMod = (RubyModule)clone;
            cloneMod.syncConstants((RubyModule)original);
            cloneMod.syncClassVariables((RubyModule)original);
        }
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
            throw typeError(obj.getRuntime().getCurrentContext(), "initialize_copy should take same class object");
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
     * Preferred over callMethod(context, "inspect")
     */
    static RubyString inspect(ThreadContext context, IRubyObject object) {
        return RubyString.objAsString(context, invokedynamic(context, object, INSPECT));
    }

    @Override
    public IRubyObject rbClone() {
        Ruby runtime = getRuntime();
        return rbCloneInternal(runtime.getCurrentContext(), runtime.getNil());
    }

    public IRubyObject rbClone(ThreadContext context, IRubyObject maybeOpts) {
        IRubyObject kwfreeze = ArgsUtil.getFreezeOpt(context, maybeOpts);

        return rbCloneInternal(context, kwfreeze);
    }

    // MRI: rb_dup_setup
    protected RubyBasicObject dupSetup(ThreadContext context, RubyBasicObject dup) {
        initCopy(context, dup, this);
        sites(context).initialize_dup.call(context, dup, dup, this);

        return dup;
    }

    // MRI: rb_clone_setup
    protected RubyBasicObject cloneSetup(ThreadContext context, RubyBasicObject clone, IRubyObject freeze) {
        clone.setMetaClass(getSingletonClassCloneAndAttach(clone));

        initCopy(context, clone, this);

        if (freeze == context.nil) {
            sites(context).initialize_clone.call(context, clone, clone, this);
            if (this instanceof RubyString str && str.isChilled()) {
                ((RubyString) clone).chill();
            } else if (isFrozen()) {
                clone.setFrozen(true);
            }
        } else { // will always be true or false (MRI has bulletproofing to catch odd values (rb_bug explodes).
            // FIXME: MRI uses C module variables to make a single hash ever for this setup.  We build every time.
            RubyHash opts = RubyHash.newHash(context.runtime, asSymbol(context, "freeze"), freeze);
            context.callInfo = CALL_KEYWORD;
            sites(context).initialize_clone.call(context, clone, clone, this, opts);
            if (freeze == context.tru) clone.freeze(context);
            if (clone.getMetaClass().isSingleton()) clone.getMetaClass().setFrozen(clone.isFrozen());
        }

        return clone;
    }


    // freeze (false, true, nil)
    private RubyBasicObject rbCloneInternal(ThreadContext context, IRubyObject freeze) {

        // MRI: immutable_obj_clone
        if (isSpecialObject()) {
            final Ruby runtime = context.runtime;
            if (freeze == runtime.getFalse()) throw argumentError(context, str(runtime, "can't unfreeze ", types(runtime, getType())));

            return this;
        }

        // We're cloning ourselves, so we know the result should be a RubyObject
        RubyBasicObject clone = (RubyBasicObject) metaClass.getRealClass().allocate();

        return cloneSetup(context, clone, freeze);
    }


    protected RubyClass getSingletonClassClone() {
        return getSingletonClassCloneAndAttach(null);
    }

    /** rb_singleton_class_clone
     *
     * Will make sure that if the current objects class is a
     * singleton, it will get cloned.
     *
     * @return either a real class, or a clone of the current singleton class
     */
    protected RubyClass getSingletonClassCloneAndAttach(RubyBasicObject attach) {
        RubyClass klass = getMetaClass();

        if (!klass.isSingleton()) {
            return klass;
        }

        MetaClass clone = new MetaClass(getRuntime(), klass.getSuperClass(), attach);
        clone.flags = klass.flags;

        if (this instanceof RubyClass) {
            clone.setMetaClass(clone);
        } else {
            clone.setMetaClass(klass.getSingletonClassClone());
        }

        if (klass.hasVariables()) clone.syncVariables(klass);

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
    public void dataWrapStruct(Object obj) {
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
    public Object dataGetStruct() {
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
     * parent as a lazy store for an object symbol. IDs are generated atomically,
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
     *     obj.inspect   =&gt; string
     *
     *  Returns a string containing a human-readable representation of
     *  <i>obj</i>. If not overridden, uses the <code>to_s</code> method to
     *  generate the string.
     *
     *     [ 1, 2, 3..4, 'five' ].inspect   #=&gt; "[1, 2, 3..4, \"five\"]"
     *     Time.new.inspect                 #=&gt; "Wed Apr 09 08:54:39 CDT 2003"
     */
    @Override
    public IRubyObject inspect() {
        return !isImmediate() && !(this instanceof RubyModule) && hasVariables() ?
                hashyInspect() : to_s(getRuntime().getCurrentContext());
    }

    public final IRubyObject hashyInspect() {
        final Ruby runtime = getRuntime();

        RubyString part = inspectPrefix(runtime.getCurrentContext(), metaClass.getRealClass(), inspectHashCode());

        if (runtime.isInspecting(this)) {
            encStrBufCat(runtime, part, SPACE_DOT_DOT_DOT_GT);
            return part;
        }
        try {
            runtime.registerInspecting(this);
            return inspectObj(runtime, part);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    // MRI: rb_inspect, which does dispatch
    public static IRubyObject rbInspect(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.runtime;
        RubyString str = sites(context).inspect.call(context, obj, obj).asString();
        Encoding enc = runtime.getDefaultInternalEncoding();
        if (enc == null) enc = runtime.getDefaultExternalEncoding();
        if (!enc.isAsciiCompatible()) {
            if (!str.isAsciiOnly()) {
                return RubyString.rbStrEscape(context, str);
            }
            return str;
        }
        if (str.getEncoding() != enc && !str.isAsciiOnly()) {
            return RubyString.rbStrEscape(context, str);
        }
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
    private RubyString inspectObj(final Ruby runtime, RubyString part) {
        final ThreadContext context = runtime.getCurrentContext();

        boolean first = true;
        for (Map.Entry<String, VariableAccessor> entry : metaClass.getVariableTableManager().getVariableAccessorsForRead().entrySet()) {
            Object value = entry.getValue().get(this);
            if (!(value instanceof IRubyObject)) continue;
            RubySymbol symbol = asSymbol(context, entry.getKey());
            if (!symbol.validInstanceVariableName()) continue;

            IRubyObject obj = (IRubyObject) value;

            if (!first) encStrBufCat(runtime, part, COMMA);
            encStrBufCat(runtime, part, SPACE);
            // FIXME: bytelist_love: EPICLY wrong but something in MRI gets around identifiers of arbitrary encoding.
            encStrBufCat(runtime, part, symbol.asString().encode(context, runtime.getEncodingService().convertEncodingToRubyEncoding(part.getEncoding())).asString().getByteList());
            encStrBufCat(runtime, part, EQUALS);
            encStrBufCat(runtime, part, sites(context).inspect.call(context, obj, obj).convertToString().getByteList());

            first = false;
        }
        encStrBufCat(runtime, part, GT);
        return part;
    }

    // Methods of the Object class (rb_obj_*):


    @JRubyMethod(name = "!")
    public IRubyObject op_not(ThreadContext context) {
        return asBoolean(context, !isTrue());
    }

    /**
     * The != method implemented for BasicObject.
     *
     * @param context thread context
     * @param other other object
     * @return false if this == other, true otherwise
     */
    @JRubyMethod(name = "!=")
    public IRubyObject op_not_equal(ThreadContext context, IRubyObject other) {
        return asBoolean(context, !sites(context).op_equal.call(context, this, this, other).isTrue());
    }

    /**
     * Compares this Ruby object with another.
     *
     * @param other another IRubyObject
     * @return 0 if equal,
     *         &lt; 0 if this is less than other,
     *         &gt; 0 if this is greater than other
     */
    @Override
    public int compareTo(IRubyObject other) {
        final Ruby runtime = metaClass.runtime;

        IRubyObject cmp = invokedynamic(runtime.getCurrentContext(), this, OP_CMP, other);

        // if RubyBasicObject#op_cmp is used, the result may be nil (not comparable)
        if ( ! cmp.isNil() ) {
            return (int) cmp.convertToInteger().getLongValue();
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

    /** rb_obj_equal
     *
     * Will by default use identity equality to compare objects. This
     * follows the Ruby semantics.
     *
     * The name of this method doesn't follow the convention because hierarchy problems
     */
    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return asBoolean(context, this == obj);
    }

    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        // Remain unimplemented due to problems with the double java hierarchy
        return context.nil;
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
        return invokedynamic(metaClass.runtime.getCurrentContext(), this, EQL, other).isTrue();
    }

    @Deprecated
    @Override
    public void addFinalizer(IRubyObject f) {
        addFinalizer(getRuntime().getCurrentContext(), f);
    }

    /**
     * Adds the specified object as a finalizer for this object.
     */
    @Override
    public IRubyObject addFinalizer(ThreadContext context, IRubyObject f) {
        Finalizer finalizer = (Finalizer) getInternalVariable("__finalizer__");
        if (finalizer == null) {
            IRubyObject fixnumId = registerWithObjectSpace();

            finalizer = new Finalizer(fixnumId);
            setInternalVariable("__finalizer__", finalizer);
            getRuntime().addFinalizer(finalizer);
        }
        return finalizer.addFinalizer(context, f);
    }

    private IRubyObject registerWithObjectSpace() {
        // since this is the first time we're registering a finalizer, we
        // must also register this object in ObjectSpace, so that future
        // calls to undefine_finalizer, which takes an object symbol, can
        // locate the object properly. See JRUBY-4839.
        long id = getObjectId();
        IRubyObject fixnumId = id();

        getRuntime().getObjectSpace().registerObjectId(id, this);
        return fixnumId;
    }

    /**
     * Stange method.  We will dup the __finalizer__ variable in a freshly dup'd object,
     * but it needs to be set to this objects __finalizer__.
     */
    protected void dupFinalizer() {
        Finalizer finalizer = (Finalizer) getInternalVariable("__finalizer__");
        if (finalizer != null) {
            // We need ObjectSpace to make this object reachable for the finalization
            IRubyObject fixnumId = registerWithObjectSpace();

            finalizer = new Finalizer(fixnumId, finalizer);
            setInternalVariable("__finalizer__", finalizer);
            getRuntime().addFinalizer(finalizer);
        }
    }

    /**
     * Remove all the finalizers for this object.
     */
    @Override
    public void removeFinalizers() {
        checkFrozen();
        Finalizer finalizer = (Finalizer) getInternalVariable("__finalizer__");
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
     * Check whether this object has any *set* instance variables unrelated to object_id, FFI, and ObjectSpace (which
     * also use hidden ivar slots).
     *
     * @return true if there are set instance variables, false otherwise
     */
    protected boolean hasInstanceVariables() {
        return metaClass.getVariableTableManager().hasInstanceVariables(this);
    }

    /**
     * Gets a list of all variables in this object.
     */
    @Override
    public List<Variable<Object>> getVariableList() {
        var ivarAccessors = metaClass.getVariableAccessorsForRead();
        var list = new ArrayList<Variable<Object>>(ivarAccessors.size());
        for (var entry : ivarAccessors.entrySet()) {
            var value = entry.getValue().get(this);
            if (value == null) continue;
            list.add(new VariableEntry<>(entry.getKey(), value));
        }
        return list;
    }

    /**
     * @see IRubyObject#getMarshalVariableList()
     */
    public List<Variable<Object>> getMarshalVariableList() {
        var ivarAccessors = metaClass.getVariableAccessorsForRead();
        var list = new ArrayList<Variable<Object>>(ivarAccessors.size());
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
            var value = entry.getValue().get(this);
            if (value == null || !(value instanceof Serializable)) continue;
            list.add(new VariableEntry<>(entry.getKey(), value));
        }
        return list;
    }

    /**
     * Gets a name list of all variables in this object.
     */
    @Override
    public List<String> getVariableNameList() {
        var ivarAccessors = metaClass.getVariableAccessorsForRead();
        var list = new ArrayList<String>(ivarAccessors.size());
        for (var entry : ivarAccessors.entrySet()) {
            var value = entry.getValue().get(this);
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
        return (IRubyObject) variableTableStore(name, value);
    }

    /**
     * @see org.jruby.runtime.builtin.InstanceVariables#removeInstanceVariable
     */
    @Override
    public IRubyObject removeInstanceVariable(String name) {
        ensureInstanceVariablesSettable();
        return (IRubyObject) variableTableRemove(name);
    }

    /**
     * Gets a list of all variables in this object.
     */
    @Override
    public List<Variable<IRubyObject>> getInstanceVariableList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<Variable<IRubyObject>> list = new ArrayList<>(ivarAccessors.size());
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue().get(this);
            if (!(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(key)) continue;
            list.add(new VariableEntry<>(key, (IRubyObject) value));
        }
        return list;
    }

    /**
     * Gets a name list of all variables in this object.
     */
    @Override
    public List<String> getInstanceVariableNameList() {
        Map<String, VariableAccessor> ivarAccessors = metaClass.getVariableAccessorsForRead();
        ArrayList<String> list = new ArrayList<>(ivarAccessors.size());
        for (Map.Entry<String, VariableAccessor> entry : ivarAccessors.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue().get(this);
            if (!(value instanceof IRubyObject) || !IdUtil.isInstanceVariable(key)) continue;
            list.add(key);
        }
        return list;
    }

    @Override
    public void forEachInstanceVariableName(Consumer<String> consumer) {
        metaClass.getVariableAccessorsForRead().forEach((name, var) -> {
            if (IdUtil.isInstanceVariable(name) && var.get(this) instanceof IRubyObject) {
                consumer.accept(name);
            }
        });
    }

    @Override
    public void copyInstanceVariablesInto(final InstanceVariables other) {
        forEachInstanceVariable(other::setInstanceVariable);
    }

    /**
     * Makes sure that instance variables can be set on this object,
     * including information about whether this object is frozen.
     * Will throw a suitable exception in that case.
     */
    public void ensureInstanceVariablesSettable() {
        if (isFrozen()) {
            raiseFrozenError();
        }
    }

    @Override
    public void forEachInstanceVariable(BiConsumer<String, IRubyObject> accessor) {
        metaClass.getVariableAccessorsForRead().forEach((name, var) -> {
            if (IdUtil.isInstanceVariable(name) && var.get(this) instanceof IRubyObject rubyObject) {
                accessor.accept(name, rubyObject);
            }
        });
    }

    private void raiseFrozenError() throws RaiseException {
        if (this instanceof RubyModule) {
            throw getRuntime().newFrozenError("class/module ", this);
        } else {
            throw getRuntime().newFrozenError(this);
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
     * method, i.e. a method built-in to JRuby and loaded during its core boot process.
     *
     * @param methodName to look for.
     * @return true if so
     */
    public boolean isBuiltin(String methodName) {
        return metaClass.isMethodBuiltin(methodName);
    }

    @JRubyMethod(name = "singleton_method_added", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_added(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "singleton_method_removed", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_removed(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "singleton_method_undefined", module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_undefined(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "method_missing", rest = true, module = true, omit = true, visibility = PRIVATE)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        if (args.length == 0 || !(args[0] instanceof RubySymbol sym)) throw argumentError(context, "no id given");

        return RubyKernel.methodMissingDirect(context, recv, sym, lastVis, lastCallType, args);
    }

    @JRubyMethod(name = "__send__", omit = true, keywords = true)
    public IRubyObject send(ThreadContext context, IRubyObject arg0, Block block) {
        String name = RubySymbol.idStringFromObject(context, arg0);

        StaticScope staticScope = context.getCurrentStaticScope();

        return getMetaClass().finvokeWithRefinements(context, this, staticScope, name, block);
    }
    @JRubyMethod(name = "__send__", omit = true, keywords = true)
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        String name = RubySymbol.idStringFromObject(context, arg0);

        StaticScope staticScope = context.getCurrentStaticScope();

        arg1 = dupIfKeywordRestAtCallsite(context, arg1);
        return getMetaClass().finvokeWithRefinements(context, this, staticScope, name, arg1, block);
    }
    @JRubyMethod(name = "__send__", omit = true, keywords = true)
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        String name = RubySymbol.idStringFromObject(context, arg0);

        StaticScope staticScope = context.getCurrentStaticScope();

        arg2 = dupIfKeywordRestAtCallsite(context, arg2);

        return getMetaClass().finvokeWithRefinements(context, this, staticScope, name, arg1, arg2, block);
    }
    @JRubyMethod(name = "__send__", required = 1, rest = true, checkArity = false, omit = true, keywords = true)
    public IRubyObject send(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);
        int callInfo = context.callInfo;

        // FIXME: Likely all methods which can pass the last value to another ruby call must do this.
        // MRI: from vm_args.setup_parameters_complex()
        if (argc > 0) {
            if ((callInfo & CALL_SPLATS) != 0) {
                IRubyObject last = args[argc - 1];
                if (last instanceof RubyHash && ((RubyHash) last).isRuby2KeywordHash()) {
                    if (((RubyHash) last).isEmpty()) { // empty kwargs hashes should never get passed on.
                        argc--;
                        args = ArraySupport.newCopy(args, argc);
                    } else {
                        args[argc - 1] = ((RubyHash) last).dupFast(context);
                        ((RubyHash) args[argc - 1]).setRuby2KeywordHash(false);
                        context.callInfo |= (CALL_KEYWORD | CALL_KEYWORD_REST);
                    }
                }
            } else if (argc > 1) {
              args[argc - 1] = dupIfKeywordRestAtCallsite(context, args[argc - 1]);
            }
        }
        String name = RubySymbol.idStringFromObject(context, args[0]);

        StaticScope staticScope = context.getCurrentStaticScope();

        final int length = argc - 1;
        args = ( length == 0 ) ? IRubyObject.NULL_ARRAY : ArraySupport.newCopy(args, 1, length);
        return getMetaClass().finvokeWithRefinements(context, this, staticScope, name, args, block);
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
                IRubyObject valueInYield = RubyArray.newArrayMayCopy(context.runtime, args);
                return setupBlock(block, evalType).yieldArray(context, valueInYield, this);  // context.getRubyClass());
            }
        } finally {
            block.getBinding().setVisibility(savedVisibility);
            block.getBinding().setSelf(savedBindingSelf);
            block.getBinding().getFrame().setSelf(savedFrameSelf);

            context.postExecuteUnder();
        }
    }

    private Block setupBlock(Block block, EvalType evalType) {
        if (block.getProcObject() == null) {
            // FIXME: This is an ugly hack to resolve JRUBY-1381; I'm not proud of it
            block = block.cloneBlockForEval(this, evalType);
        } else {
            block = block.deepCloneBlockForEval(this, evalType);
        }

        block.getBinding().setVisibility(PUBLIC);

        return block;
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

        try {
            return setupBlock(block, evalType).yieldNonArray(context, this, this); //, context.getRubyClass());
        } finally {
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
        if (!block.isGiven()) throw argumentError(context, 0, 1, 3);

        return yieldUnder(context, mod, block, evalType);
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
        if (block.isGiven()) throw argumentError(context, 1, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr = arg instanceof RubyString str ? str : arg.convertToString();
        RubyStackTraceElement singleBacktrace = context.getSingleBacktrace();
        String file = "(eval at " + singleBacktrace.getFileName() + ":" +  + singleBacktrace.getLineNumber() + ")";
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
        if (block.isGiven()) throw argumentError(context, 2, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr = arg0 instanceof RubyString str ? str : arg0.convertToString();
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
        if (block.isGiven()) throw argumentError(context, 2, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr = arg0 instanceof RubyString str ? str : arg0.convertToString();
        String file = arg1.convertToString().asJavaString();
        int line = (int)(arg2.convertToInteger().getLongValue() - 1);

        return evalUnder(context, mod, evalStr, file, line, evalType);
    }

    protected RubyModule getInstanceEvalClass() {
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            return getRuntime().getDummy();
        }
        return getSingletonClass();
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
        private final IRubyObject id;
        private final AtomicBoolean finalized;
        private IRubyObject firstFinalizer;
        private List<IRubyObject> finalizers;

        public Finalizer(RubyFixnum id) {
            this((IRubyObject) id);
        }

        /**
         * Cloning finalizer needs new copy with its own id.
         * @param id
         * @param original
         */
        public Finalizer(IRubyObject id, Finalizer original) {
            this(id);
            this.firstFinalizer = original.firstFinalizer;
            this.finalizers = original.finalizers == null ? null : new ArrayList<>(original.finalizers);
        }

        Finalizer(IRubyObject id) {
            this.id = id;
            this.finalized = new AtomicBoolean(false);
        }

        @Deprecated
        public void addFinalizer(IRubyObject finalizer) {
            addFinalizer(finalizer.getRuntime().getCurrentContext(), finalizer);
        }

        public IRubyObject addFinalizer(ThreadContext context, IRubyObject finalizer) {
            if (firstFinalizer == null) {
                firstFinalizer = finalizer;

                return finalizer;
            }

            IRubyObject existing = firstFinalizer;

            if (existing.op_equal(context, finalizer).isTrue()) {
                // do not add equivalent finalizer twice
                return existing;
            }

            if (finalizers == null) {
                finalizers = new ArrayList<>(4);
            } else {
                for (int i = 0; i < finalizers.size(); i++) {
                    existing = finalizers.get(i);

                    if (existing.op_equal(context, finalizer).isTrue()) {
                        // do not add equivalent finalizer twice
                        return existing;
                    }
                }
            }

            finalizers.add(finalizer);

            return finalizer;
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
            ThreadContext context = finalizer.getRuntime().getCurrentContext();
            try {
                sites(context).call.call(context, finalizer, finalizer, id);
            } finally {
                // clear last error so it is not seen by future finalizers
                context.setErrorInfo(context.nil);
            }
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
    @JRubyMethod(name = "equal?")
    public IRubyObject equal_p(ThreadContext context, IRubyObject other) {
        return asBoolean(context, this == other);
    }

    /** rb_obj_equal
     *
     * Just like "==" and "equal?", "eql?" will use identity equality for Object.
     */
    public IRubyObject eql_p(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (this == other || invokedynamic(context, this, OP_EQUAL, other).isTrue()){
            return RubyFixnum.zero(context.runtime);
        }
        return context.nil;
    }

    /**
     * @param original object
     * @return
     * @deprecated Use {@link org.jruby.RubyBasicObject#initialize_copy(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0")
    public IRubyObject initialize_copy(IRubyObject original) {
        return initialize_copy(getCurrentContext(), original);
    }

    /**
     *
     * Initializes this object as a copy of the original, that is the
     * parameter to this object. Will make sure that the argument
     * actually has the same real class as this object. It shouldn't
     * be possible to initialize an object with something totally
     * different.
     */
    // MRI: rb_obj_init_copy
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        if (this == original) return this;

        checkFrozen();

        if (getMetaClass().getRealClass() != original.getMetaClass().getRealClass()) {
            throw typeError(context, "initialize_copy should take same class object");
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
     * respond_to?( aSymbol, includePriv=false )$ -&gt; true or false
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
    @Deprecated // NOTE: does not match Ruby 2.x rules (does method bound check only)
    public final RubyBoolean respond_to_p(IRubyObject mname) {
        return getRuntime().newBoolean(getMetaClass().respondsToMethod(mname.asJavaString(), true));
    }

    @Deprecated // NOTE: does not match Ruby 2.x rules (does method bound check only)
    public final RubyBoolean respond_to_p(IRubyObject mname, IRubyObject includePrivate) {
        String name = mname.asJavaString();
        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, !includePrivate.isTrue()));
    }

    final RubyBoolean respond_to_p(ThreadContext context, IRubyObject methodName, final boolean includePrivate) {
        RubySymbol name = TypeConverter.checkID(methodName);

        if (getMetaClass().respondsToMethod(name.idString(), !includePrivate, context.getCurrentStaticScope())) return context.tru;

        Ruby runtime = context.runtime;
        IRubyObject result = sites(context).respond_to_missing.call(context, this, this, name, runtime.newBoolean(includePrivate));
        return context.runtime.newBoolean(result.isTrue());
    }

    /**
     * Will return the hash code of this object. In comparison to MRI,
     * this method will use the Java identity hash code instead of
     * using rb_obj_id, since the usage of id in JRuby will incur the
     * cost of some. ObjectSpace maintenance.
     * @deprecated Use {@link RubyBasicObject#hash(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public RubyFixnum hash() {
        return hash(getCurrentContext());
    }

    /**
     * Will return the hash code of this object. In comparison to MRI,
     * this method will use the Java identity hash code instead of
     * using rb_obj_id, since the usage of id in JRuby will incur the
     * cost of some. ObjectSpace maintenance.
     *
     * @param context the current thread context
     * @return the hash value
     */
    // MRI: rb_obj_id
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, super.hashCode());
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
    @Deprecated
    public RubyClass type_deprecated() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Object#type is deprecated; use Object#class");
        return type();
    }

    /** rb_obj_display
     *
     *  call-seq:
     *     obj.display(port=$&gt;)    =&gt; nil
     *
     *  Prints <i>obj</i> on the given port (default <code>$&gt;</code>).
     *  Equivalent to:
     *
     *     def display(port=$&gt;)
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
        IRubyObject port = args.length == 0 ? globalVariables(context).get("$>") : args[0];

        port.callMethod(context, "write", this);

        return context.nil;
    }

    /** rb_obj_freeze
     *
     *  call-seq:
     *     obj.freeze    =&gt; obj
     *
     *  Prevents further modifications to <i>obj</i>. A
     *  <code>TypeError</code> will be raised if modification is attempted.
     *  There is no way to unfreeze a frozen object. See also
     *  <code>Object#frozen?</code>.
     *
     *     a = [ "a", "b", "c" ]
     *     a.freeze
     *     a &lt;&lt; "z"
     *
     *  <em>produces:</em>
     *
     *     prog.rb:3:in `&lt;&lt;': can't modify frozen array (TypeError)
     *     	from prog.rb:3
     */
    public IRubyObject freeze(ThreadContext context) {
        setFrozen(true);
        RubyClass metaClass = getMetaClass();
        if (metaClass.isSingleton()) metaClass.setFrozen(true);
        return this;
    }

    /** rb_obj_frozen_p
     *
     *  call-seq:
     *     obj.frozen?    =&gt; true or false
     *
     *  Returns the freeze status of <i>obj</i>.
     *
     *     a = [ "a", "b", "c" ]
     *     a.freeze    #=&gt; ["a", "b", "c"]
     *     a.frozen?   #=&gt; true
     */
    public RubyBoolean frozen_p(ThreadContext context) {
        return asBoolean(context, isFrozen());
    }

    /** rb_obj_is_instance_of
     *
     *  call-seq:
     *     obj.instance_of?(class)    =&gt; true or false
     *
     *  Returns <code>true</code> if <i>obj</i> is an instance of the given
     *  class. See also <code>Object#kind_of?</code>.
     */
    public RubyBoolean instance_of_p(ThreadContext context, IRubyObject type) {
        if (type() == type) return context.tru;
        if (!(type instanceof RubyModule)) throw typeError(context, "class or module required");

        return context.fals;
    }


    /** rb_obj_is_kind_of
     *
     *  call-seq:
     *     obj.is_a?(class)       =&gt; true or false
     *     obj.kind_of?(class)    =&gt; true or false
     *
     *  Returns <code>true</code> if <i>class</i> is the class of
     *  <i>obj</i>, or if <i>class</i> is one of the superclasses of
     *  <i>obj</i> or modules included in <i>obj</i>.
     *
     *     module M;    end
     *     class A
     *       include M
     *     end
     *     class B &lt; A; end
     *     class C &lt; B; end
     *     b = B.new
     *     b.instance_of? A   #=&gt; false
     *     b.instance_of? B   #=&gt; true
     *     b.instance_of? C   #=&gt; false
     *     b.instance_of? M   #=&gt; false
     *     b.kind_of? A       #=&gt; true
     *     b.kind_of? B       #=&gt; true
     *     b.kind_of? C       #=&gt; false
     *     b.kind_of? M       #=&gt; true
     */
    public RubyBoolean kind_of_p(ThreadContext context, IRubyObject type) {
        return asBoolean(context, castAsModule(context, type, "class or module required").isInstance(this));
    }

    /** rb_obj_methods
     *
     *  call-seq:
     *     obj.methods    =&gt; array
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
     *     k.methods[0..9]    #=&gt; ["kMethod", "freeze", "nil?", "is_a?",
     *                             "class", "instance_variable_set",
     *                              "methods", "extend", "__send__", "instance_eval"]
     *     k.methods.length   #=&gt; 42
     */
    public IRubyObject methods(ThreadContext context, IRubyObject... args) {
        return methodsImpl(context, args.length == 1 ? args[0].isTrue() : true);
    }

    final IRubyObject methodsImpl(ThreadContext context, final boolean all) {
        final var methods = newArray(context);
        final Set<String> seen = new HashSet<>();

        RubyClass metaClass = getMetaClass();
        if (metaClass.isSingleton()) {
            metaClass.populateInstanceMethodNames(context, seen, methods, PRIVATE, false, true, false);
            if (all) {
                metaClass.getSuperClass().populateInstanceMethodNames(context, seen, methods, PRIVATE, false, true, true);
            }
        } else if (all) {
            metaClass.populateInstanceMethodNames(context, seen, methods, PRIVATE, false, true, true);
        } // else - do nothing, leave empty

        return methods;
    }

    @Deprecated
    public final IRubyObject methods(ThreadContext context, IRubyObject[] args, boolean useSymbols) {
        return methodsImpl(context, args.length == 1 ? args[0].isTrue() : true);
    }

    /** rb_obj_public_methods
     *
     *  call-seq:
     *     obj.public_methods(all=true)   =&gt; array
     *
     *  Returns the list of public methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     */
    public IRubyObject public_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().instanceMethods(args, PUBLIC, true, false);
    }

    /** rb_obj_protected_methods
     *
     *  call-seq:
     *     obj.protected_methods(all=true)   =&gt; array
     *
     *  Returns the list of protected methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     *
     *  Internally this implementation uses the
     *  {@link RubyModule#protected_instance_methods} method.
     */
    public IRubyObject protected_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().instanceMethods(args, PROTECTED, true, false);
    }

    /** rb_obj_private_methods
     *
     *  call-seq:
     *     obj.private_methods(all=true)   =&gt; array
     *
     *  Returns the list of private methods accessible to <i>obj</i>. If
     *  the <i>all</i> parameter is set to <code>false</code>, only those methods
     *  in the receiver will be listed.
     *
     *  Internally this implementation uses the
     *  {@link RubyModule#private_instance_methods} method.
     */
    public IRubyObject private_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().instanceMethods(args, PRIVATE, true, false);
    }

    /** rb_obj_singleton_methods
     *
     *  call-seq:
     *     obj.singleton_methods(all=true)    =&gt; array
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
     *     class &lt;&lt; a
     *       include Other
     *       def two()
     *       end
     *     end
     *
     *     Single.singleton_methods    #=&gt; ["four"]
     *     a.singleton_methods(false)  #=&gt; ["two", "one"]
     *     a.singleton_methods         #=&gt; ["two", "one", "three"]
     */
    // TODO: This is almost RubyModule#instance_methods on the metaClass.  Perhaps refactor.
    public RubyArray singleton_methods(final ThreadContext context, IRubyObject[] args) {
        boolean all = args.length != 1 || args[0].isTrue();
        RubyClass klass = metaClass;
        RubyModule origin = klass.getMethodLocation();
        Set<RubySymbol> names = (klass.isSingleton() || all) ? new HashSet<>() : Collections.EMPTY_SET;

        if (klass.isSingleton()) {
            // TODO: needs to use method_entry_i logic from MRI
            origin.getMethods().forEach((k, v) -> {
                if (v.getVisibility() != PRIVATE) names.add(asSymbol(context, k));
            });
            klass = klass.getSuperClass();
        }

        if (all) {
            while (klass != null && (klass.isSingleton() || klass.isIncluded())) {
                if (klass != origin) {
                    klass.getMethods().forEach((k, v) -> {
                        if (v.getVisibility() != PRIVATE) names.add(asSymbol(context, k));
                    });
                }
                klass = klass.getSuperClass();
            }
        }

        if (names.isEmpty()) return newEmptyArray(context);

        return RubyArray.newArray(context.runtime, names);
    }

    public IRubyObject singleton_method(IRubyObject name) {
        RubySymbol symbol = TypeConverter.checkID(name);
        final String methodName = symbol.idString();
        final RubyClass klass = metaClass;
        if (klass.isSingleton()) {
            CacheEntry entry = klass.searchWithCache(methodName);
            if (klass == entry.method.getDefinedClass()) { // ! method.isUndefined()
                AbstractRubyMethod newMethod = RubyMethod.newMethod(klass, methodName, klass, methodName, entry, this);
                return newMethod;
            }
        }
        throw getRuntime().newNameError(str(getRuntime(), "undefined method '", symbol,  "' for '", inspect(), "'"), symbol);
    }

    /** rb_obj_method
     *
     *  call-seq:
     *     obj.method(sym)    =&gt; method
     *
     *  Looks up the named method as a receiver in <i>obj</i>, returning a
     *  <code>Method</code> object (or raising <code>NameError</code>). The
     *  <code>Method</code> object acts as a closure in <i>obj</i>'s object
     *  instance, so instance variables and the value of <code>self</code>
     *  remain available.
     *
     *     class Demo
     *       def initialize(n)
     *         {@literal @}iv = n
     *       end
     *       def hello()
     *         "Hello, {@literal @}iv = #{{@literal @}iv}"
     *       end
     *     end
     *
     *     k = Demo.new(99)
     *     m = k.method(:hello)
     *     m.call   #=&gt; "Hello, {@literal @}iv = 99"
     *
     *     l = Demo.new('Fred')
     *     m = l.method("hello")
     *     m.call   #=&gt; "Hello, {@literal @}iv = Fred"
     */
    public IRubyObject method(IRubyObject name) {
        final RubySymbol symbol = TypeConverter.checkID(name);
        return getMetaClass().newMethod(this, symbol.idString(), true, null, true);
    }

    /**
     * Like {@link #method(IRubyObject)} but using the given refinement scope to search for the method.
     *
     * @param name the name of the method
     * @param refinedScope the static scope for the caller method
     * @return
     */
    public IRubyObject method(IRubyObject name, StaticScope refinedScope) {
        final RubySymbol symbol = TypeConverter.checkID(name);
        return getMetaClass().newMethod(this, symbol.idString(), refinedScope, true, null, true);
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyBasicObject#to_s(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject to_s() {
        return to_s(getCurrentContext());
    }

    /** rb_any_to_s
     *
     *  call-seq:
     *     obj.to_s    =&gt; string
     *
     *  Returns a string representing <i>obj</i>. The default
     *  <code>to_s</code> prints the object's class and an encoding of the
     *  object id. As a special case, the top-level object that is the
     *  initial execution context of Ruby programs returns ``main.''
     */
    public IRubyObject to_s(ThreadContext context) {
    	return anyToString();
    }

    /** rb_any_to_a
     *
     *  call-seq:
     *     obj.to_a$ -&gt; anArray
     *
     *  Returns an array representation of <i>obj</i>. For objects of class
     *  <code>Object</code> and others that don't explicitly override the
     *  method, the return value is an array containing <code>self</code>.
     *  However, this latter behavior will soon be obsolete.
     *
     *     self.to_a       #=&gt; -:1: warning: default `to_a' will be obsolete
     *     "hello".to_a    #=&gt; ["hello"]
     *     Time.new.to_a   #=&gt; [39, 54, 8, 9, 4, 2003, 3, 99, true, "CDT"]
     *
     *  The default to_a method is deprecated.
     */
    public RubyArray to_a(ThreadContext context) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "default 'to_a' will be obsolete");
        return newArray(context,this);
    }

    /** rb_obj_instance_eval
     *
     *  call-seq:
     *     obj.instance_eval(string [, filename [, lineno]] )   =&gt; obj
     *     obj.instance_eval {| | block }                       =&gt; obj
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
     *         {@literal @}secret = 99
     *       end
     *     end
     *     k = Klass.new
     *     k.instance_eval { {@literal @}secret }   #=&gt; 99
     */

    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval(ThreadContext context, Block block) {
        return specificEval(context, getInstanceEvalClass(), block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, block, EvalType.INSTANCE_EVAL);
    }
    @JRubyMethod(name = "instance_eval",
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, arg2, block, EvalType.INSTANCE_EVAL);
    }

    // This is callable and will work but the rest = true is put so we can match the expected arity error message
    // Just relying on annotations will give us: got n expected 0..3 when we want got n expected 1..3.
    @JRubyMethod(name = "instance_eval", rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_eval(ThreadContext context, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> instance_eval(context, block);
            case 1 -> instance_eval(context, args[0], block);
            case 2 -> instance_eval(context, args[0], args[1], block);
            case 3 -> instance_eval(context, args[0], args[1], args[2], block);
            default -> throw argumentError(context, args.length, 1, 3);
        };
    }

    /** rb_obj_instance_exec
     *
     *  call-seq:
     *     obj.instance_exec(arg...) {|var...| block }                       =&gt; obj
     *
     *  Executes the given block within the context of the receiver
     *  (_obj_). In order to set the context, the variable +self+ is set
     *  to _obj_ while the code is executing, giving the code access to
     *  _obj_'s instance variables.  Arguments are passed as block parameters.
     *
     *     class Klass
     *       def initialize
     *         {@literal @}secret = 99
     *       end
     *     end
     *     k = Klass.new
     *     k.instance_exec(5) {|x| {@literal @}secret+x }   #=&gt; 104
     */
    @JRubyMethod(name = "instance_exec", rest = true, keywords = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject instance_exec(ThreadContext context, IRubyObject[] args, Block block) {
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

    /** rb_obj_extend
     *
     *  call-seq:
     *     obj.extend(module, ...)    =&gt; obj
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
     *     k.hello         #=&gt; "Hello from Klass.\n"
     *     k.extend(Mod)   #=&gt; #&lt;Klass:0x401b3bc8&gt;
     *     k.hello         #=&gt; "Hello from Mod.\n"
     */
    public IRubyObject extend(IRubyObject[] args) {
        var context = getRuntime().getCurrentContext();

        // Make sure all arguments are modules before calling the callbacks
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (!arg.isModule()) throw typeError(context, arg, "Module");
            if (((RubyModule) arg).isRefinement()) throw typeError(context, "Cannot extend object with refinement");
        }

        // MRI extends in order from last to first
        for (int i = args.length - 1; i >= 0; i--) {
            args[i].callMethod(context, "extend_object", this);
            args[i].callMethod(context, "extended", this);
        }
        return this;
    }

    /** rb_false
     *
     * call_seq:
     *   nil.nil?               =&gt; true
     *   &lt;anything_else&gt;.nil?   =&gt; false
     *
     * Only the object <i>nil</i> responds <code>true</code> to <code>nil?</code>.
     */
    public IRubyObject nil_p(ThreadContext context) {
        return context.fals;
    }

    /** rb_obj_pattern_match
     *
     *  call-seq:
     *     obj =~ other  =&gt; nil
     *
     *  Pattern Match---Overridden by descendents (notably
     *  <code>Regexp</code> and <code>String</code>) to provide meaningful
     *  pattern-match semantics.
     */
    public IRubyObject op_match(ThreadContext context, IRubyObject arg) {
        return context.nil;
    }

    /**
     * Invert the match operator.
     *
     * @param context
     * @param arg
     * @return
     */
    public IRubyObject op_not_match(ThreadContext context, IRubyObject arg) {
        return asBoolean(context, !sites(context).match.call(context, this, this, arg).isTrue());
    }


    //
    // INSTANCE VARIABLE RUBY METHODS
    //

    /** rb_obj_ivar_defined
     *
     *  call-seq:
     *     obj.instance_variable_defined?(symbol)    =&gt; true or false
     *
     *  Returns <code>true</code> if the given instance variable is
     *  defined in <i>obj</i>.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         {@literal @}a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_defined?(:{@literal @}a)    #=&gt; true
     *     fred.instance_variable_defined?("{@literal @}b")   #=&gt; true
     *     fred.instance_variable_defined?("@c")   #=&gt; false
     */
    public IRubyObject instance_variable_defined_p(ThreadContext context, IRubyObject name) {
        return asBoolean(context, variableTableContains(validateInstanceVariable(name)));
    }

    /** rb_obj_ivar_get
     *
     *  call-seq:
     *     obj.instance_variable_get(symbol)    =&gt; obj
     *
     *  Returns the value of the given instance variable, or nil if the
     *  instance variable is not set. The <code>@</code> part of the
     *  variable name should be included for regular instance
     *  variables. Throws a <code>NameError</code> exception if the
     *  supplied symbol is not valid as an instance variable name.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         {@literal @}a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_get(:{@literal @}a)    #=&gt; "cat"
     *     fred.instance_variable_get("{@literal @}b")   #=&gt; 99
     */
    public IRubyObject instance_variable_get(ThreadContext context, IRubyObject name) {
        Object value = variableTableFetch(validateInstanceVariable(name));

        return value != null ? (IRubyObject) value : context.nil;
    }

    /** rb_obj_ivar_set
     *
     *  call-seq:
     *     obj.instance_variable_set(symbol, obj)    =&gt; obj
     *
     *  Sets the instance variable names by <i>symbol</i> to
     *  <i>object</i>, thereby frustrating the efforts of the class's
     *  author to attempt to provide proper encapsulation. The variable
     *  did not have to exist prior to this call.
     *
     *     class Fred
     *       def initialize(p1, p2)
     *         {@literal @}a, @b = p1, p2
     *       end
     *     end
     *     fred = Fred.new('cat', 99)
     *     fred.instance_variable_set(:{@literal @}a, 'dog')   #=&gt; "dog"
     *     fred.instance_variable_set(:@c, 'cat')   #=&gt; "cat"
     *     fred.inspect                             #=&gt; "#&lt;Fred:0x401b3da8 {@literal @}a=\"dog\", @b=99, @c=\"cat\"&gt;"
     */
    public IRubyObject instance_variable_set(IRubyObject name, IRubyObject value) {
        // no need to check for ensureInstanceVariablesSettable() here, that'll happen downstream in setVariable
        return (IRubyObject) variableTableStore(validateInstanceVariable(name), value);
    }

    /** rb_obj_remove_instance_variable
     *
     *  call-seq:
     *     obj.remove_instance_variable(symbol)    =&gt; obj
     *
     *  Removes the named instance variable from <i>obj</i>, returning that
     *  variable's value.
     *
     *     class Dummy
     *       attr_reader :var
     *       def initialize
     *         {@literal @}var = 99
     *       end
     *       def remove
     *         remove_instance_variable(:{@literal @}var)
     *       end
     *     end
     *     d = Dummy.new
     *     d.var      #=&gt; 99
     *     d.remove   #=&gt; 99
     *     d.var      #=&gt; nil
     */
    public IRubyObject remove_instance_variable(ThreadContext context, IRubyObject name, Block block) {
        String id = validateInstanceVariable(name);
        ensureInstanceVariablesSettable();
        IRubyObject value = (IRubyObject) variableTableRemove(id);
        if (value != null) return value;
        throw context.runtime.newNameError("instance variable %1$s not defined", this, name);
    }

    /** rb_obj_instance_variables
     *
     *  call-seq:
     *     obj.instance_variables    =&gt; array
     *
     *  Returns an array of instance variable names for the receiver. Note
     *  that simply defining an accessor does not create the corresponding
     *  instance variable.
     *
     *     class Fred
     *       attr_accessor :a1
     *       def initialize
     *         {@literal @}iv = 3
     *       end
     *     end
     *     Fred.new.instance_variables   #=&gt; [:"{@literal @}iv"]
     */
    public RubyArray instance_variables(ThreadContext context) {
        var array = newRawArray(context, getMetaClass().getVariableAccessorsForRead().size());
        forEachInstanceVariableName(name -> array.append(context, asSymbol(context, name)));
        return array.finishRawArray(context);
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;

        if (other instanceof IRubyObject) {
            ThreadContext context = metaClass.runtime.getCurrentContext();
            BasicObjectSites sites = sites(context);
            IRubyObject equals = invokeChecked(context, this, sites.equals_checked, (IRubyObject)other);
            if (equals == null) return false;
            return equals.isTrue();
        }

        return false;
    }

    /**
     * Override the Object#hashCode method to make sure that the Ruby
     * hash is actually used as the hashcode for Ruby objects. If the
     * Ruby "hash" method doesn't return a number, the Object#hashCode
     * implementation will be used instead.
     */
    @Override
    public int hashCode() {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        IRubyObject hashValue = invokeChecked(context, this, sites(context).hash_checked);
        if (hashValue == null) return super.hashCode();
        if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue);
        return nonFixnumHashCode(hashValue);
    }

    protected static int nonFixnumHashCode(IRubyObject hashValue) {
        RubyInteger integer = hashValue.convertToInteger();
        if (integer instanceof RubyBignum) {
            return integer.getBigIntegerValue().intValue();
        }
        return (int) integer.getLongValue();
    }

    /**
     * Checks if the name parameter represents a legal instance variable name, and otherwise throws a Ruby NameError
     */
    @Deprecated
    protected String validateInstanceVariable(String name) {
        if (IdUtil.isValidInstanceVariableName(name)) return name;

        throw getRuntime().newNameError("'%1$s' is not allowable as an instance variable name", this, name);
    }

    @Deprecated
    protected String validateInstanceVariable(IRubyObject name, String _unused_) {
        return validateInstanceVariable(name);
    }

    protected String validateInstanceVariable(IRubyObject name) {
        return RubySymbol.retrieveIDSymbol(name, (sym, newSym) -> {
            if (!sym.validInstanceVariableName()) {
                throw getRuntime().newNameError("'%1$s' is not allowable as an instance variable name", this, name);
            }
        }).idString();
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

    /**
     * Retrieve the call sites for this class.
     *
     * It is expected that all Java-based core classes that do call site caching will have a similar method.
     */
    private static BasicObjectSites sites(ThreadContext context) {
        return context.sites.BasicObject;
    }

    // Deprecated methods below this line

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

    @Override
    @Deprecated
    public boolean isUntrusted() {
        return false;
    }

    @Override
    @Deprecated
    public void setUntrusted(boolean untrusted) {
    }

    @Deprecated
    public RubyBoolean untrusted_p(ThreadContext context) {
        return context.fals;
    }

    @Deprecated
    public IRubyObject untrust(ThreadContext context) {
        return this;
    }

    @Deprecated
    public IRubyObject trust(ThreadContext context) {
        return this;
    }

    @Deprecated
    public final Object getNativeHandle() {
        return null;
    }

    @Deprecated
    public final void setNativeHandle(Object value) {
    }

    @Override
    @Deprecated
    public synchronized Object dataGetStructChecked() {
        TypeConverter.checkData(this);
        return getInternalVariable("__wrap_struct__");
    }

    @Deprecated
    public RubyArray to_a() {
        return to_a(getRuntime().getCurrentContext());
    }

    @Deprecated
    public RubyBoolean tainted_p(ThreadContext context) {
        return context.fals;
    }

    @Deprecated
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    @Deprecated
    IRubyObject tainted() {
        return this;
    }

    @Deprecated
    protected final void taint(Ruby runtime) {
    }

    @Deprecated
    public IRubyObject untaint(ThreadContext context) {
        return this;
    }

    @Deprecated
    @Override
    public boolean isTaint() {
        return false;
    }

    @Deprecated
    @Override
    public void setTaint(boolean taint) {
    }

    @Deprecated
    @Override
    public IRubyObject infectBy(IRubyObject obj) {
        return this;
    }

    @Deprecated
    final RubyBasicObject infectBy(RubyBasicObject obj) {
        return this;
    }

    @Deprecated
    final RubyBasicObject infectBy(int tuFlags) {
        return this;
    }

    @Deprecated
    public static final int FL_USHIFT = 4;
    @Deprecated
    public static final int USER0_F = (1<<(FL_USHIFT+0));
    @Deprecated
    public static final int USER1_F = (1<<(FL_USHIFT+1));
    @Deprecated
    public static final int USER2_F = (1<<(FL_USHIFT+2));
    @Deprecated
    public static final int USER3_F = (1<<(FL_USHIFT+3));
    @Deprecated
    public static final int USER4_F = (1<<(FL_USHIFT+4));
    @Deprecated
    public static final int USER5_F = (1<<(FL_USHIFT+5));
    @Deprecated
    public static final int USER6_F = (1<<(FL_USHIFT+6));
    @Deprecated
    public static final int USER7_F = (1<<(FL_USHIFT+7));
    @Deprecated
    public static final int USER8_F = (1<<(FL_USHIFT+8));
    @Deprecated
    public static final int USER9_F = (1<<(FL_USHIFT+9));
    @Deprecated
    public static final int USERA_F = (1<<(FL_USHIFT+10));
    @Deprecated
    public static final int REFINED_MODULE_F = USER9_F;
    @Deprecated
    public static final int IS_OVERLAID_F = USERA_F;
    @Deprecated
    public static final int COMPARE_BY_IDENTITY_F = USER8_F;
    @Deprecated
    public static final int TAINTED_F = 0;
}
