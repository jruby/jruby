/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 MenTaLguY <mental@rydia.net>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;
import org.jruby.runtime.marshal.DataType;

/**
 * RubyObject is the only implementation of the
 * {@link org.jruby.runtime.builtin.IRubyObject}. Every Ruby object in JRuby
 * is represented by something that is an instance of RubyObject. In
 * some of the core class implementations, this means doing a subclass
 * that extends RubyObject, in other cases it means using a simple
 * RubyObject instance and the data field to store specific
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
 * @author  jpetersen
 */
@JRubyClass(name="Object", include="Kernel")
public class RubyObject extends RubyBasicObject {
    // Equivalent of T_DATA
    public static class Data extends RubyObject implements DataType {
        public Data(Ruby runtime, RubyClass metaClass, Object data) {
            super(runtime, metaClass);
            dataWrapStruct(data);
        }

        public Data(RubyClass metaClass, Object data) {
            super(metaClass);
            dataWrapStruct(data);
        }
    }

    /**
     * Standard path for object creation. Objects are entered into ObjectSpace
     * only if ObjectSpace is enabled.
     */
    public RubyObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    /**
     * Path for objects that don't taint and don't enter objectspace.
     */
    public RubyObject(RubyClass metaClass) {
        super(metaClass);
    }

    /**
     * Path for objects who want to decide whether they don't want to be in
     * ObjectSpace even when it is on. (notably used by objects being
     * considered immediate, they'll always pass false here)
     */
    protected RubyObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, metaClass, useObjectSpace, canBeTainted);
    }

    protected RubyObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    /**
     * Will create the Ruby class Object in the runtime
     * specified. This method needs to take the actual class as an
     * argument because of the Object class' central part in runtime
     * initialization.
     */
    public static RubyClass createObjectClass(Ruby runtime, RubyClass objectClass) {
        objectClass.index = ClassIndex.OBJECT;

        objectClass.defineAnnotatedMethods(ObjectMethods.class);

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
    public static class ObjectMethods {
        @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
        public static IRubyObject intialize(IRubyObject self) {
            return self.getRuntime().getNil();
        }
    }

    /**
     * Default allocator instance for all Ruby objects. The only
     * reason to not use this allocator is if you actually need to
     * have all instances of something be a subclass of RubyObject.
     *
     * @see org.jruby.runtime.ObjectAllocator
     */
    public static final ObjectAllocator OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObject(runtime, klass);
        }
    };

    /**
     * Will make sure that this object is added to the current object
     * space.
     *
     * @see org.jruby.runtime.ObjectSpace
     */
    public void attachToObjectSpace() {
        getRuntime().getObjectSpace().add(this);
    }

    /**
     * This is overridden in the other concrete Java builtins to provide a fast way
     * to determine what type they are.
     *
     * Will generally return a value from org.jruby.runtime.ClassIndex
     *
     * @see org.jruby.runtime.ClassInde
     */
    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.OBJECT;
    }

    /**
     * Simple helper to print any objects.
     */
    public static void puts(Object obj) {
        System.out.println(obj.toString());
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    @Override
    public boolean equals(Object other) {
        return other == this ||
                other instanceof IRubyObject &&
                callMethod(getRuntime().getCurrentContext(), "==", (IRubyObject) other).isTrue();
    }

    /**
     * The default toString method is just a wrapper that calls the
     * Ruby "to_s" method.
     */
    @Override
    public String toString() {
        RubyString rubyString = RuntimeHelpers.invoke(getRuntime().getCurrentContext(), this, "to_s").convertToString();
        return rubyString.getUnicodeValue();
    }

    /**
     * The actual method that checks frozen with the default frozen message from MRI.
     * If possible, call this instead of {@link #testFrozen}.
     */
   protected void checkFrozen() {
       testFrozen();
   }

    /** init_copy
     *
     * Initializes a copy with variable and special instance variable
     * information, and then call the initialize_copy Ruby method.
     */
    private static void initCopy(IRubyObject clone, RubyObject original) {
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        original.copySpecialInstanceVariables(clone);

        if (original.hasVariables()) clone.syncVariables(original.getVariableList());
        if (original instanceof RubyModule) ((RubyModule) clone).syncConstants((RubyModule) original);

        /* FIXME: finalizer should be dupped here */
        clone.callMethod(clone.getRuntime().getCurrentContext(), "initialize_copy", original);
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(IRubyObject[] args, Block block) {
        callMethod(getRuntime().getCurrentContext(), "initialize", args, block);
    }

    /**
     * Tries to convert this object to the specified Ruby type, using
     * a specific conversion method.
     */
    @Deprecated
    public final IRubyObject convertToType(RubyClass target, int convertMethodIndex) {
        throw new RuntimeException("Not supported; use the String versions");
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
    @Deprecated
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            if (args.length > 0) throw getRuntime().newArgumentError(args.length, 0);

            return yieldUnder(context, mod, block);
        }

        if (args.length == 0) {
            throw getRuntime().newArgumentError("block not supplied");
        } else if (args.length > 3) {
            String lastFuncName = context.getFrameName();
            throw getRuntime().newArgumentError(
                "wrong # of arguments: " + lastFuncName + "(src) or " + lastFuncName + "{..}");
        }

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (args[0] instanceof RubyString) {
            evalStr = (RubyString)args[0];
        } else {
            evalStr = args[0].convertToString();
        }

        String file;
        int line;
        if (args.length > 1) {
            file = args[1].convertToString().asJavaString();
            if (args.length > 2) {
                line = (int)(args[2].convertToInteger().getLongValue() - 1);
            } else {
                line = 0;
            }
        } else {
            file = "(eval)";
            line = 0;
        }

        return evalUnder(context, mod, evalStr, file, line);
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
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, mod, block);
        } else {
            throw context.getRuntime().newArgumentError("block not supplied");
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
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg, Block block) {
        if (block.isGiven()) throw context.getRuntime().newArgumentError(1, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg instanceof RubyString) {
            evalStr = (RubyString)arg;
        } else {
            evalStr = arg.convertToString();
        }

        String file = "(eval)";
        int line = 0;

        return evalUnder(context, mod, evalStr, file, line);
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
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) throw context.getRuntime().newArgumentError(2, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg0 instanceof RubyString) {
            evalStr = (RubyString)arg0;
        } else {
            evalStr = arg0.convertToString();
        }

        String file = arg1.convertToString().asJavaString();
        int line = 0;

        return evalUnder(context, mod, evalStr, file, line);
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
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) throw context.getRuntime().newArgumentError(2, 0);

        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        RubyString evalStr;
        if (arg0 instanceof RubyString) {
            evalStr = (RubyString)arg0;
        } else {
            evalStr = arg0.convertToString();
        }

        String file = arg1.convertToString().asJavaString();
        int line = (int)(arg2.convertToInteger().getLongValue() - 1);

        return evalUnder(context, mod, evalStr, file, line);
    }

    /**
     * Evaluates the string src with self set to the current object,
     * using the module under as the context.
     * @deprecated Call with an int line number and String file
     */
    public IRubyObject evalUnder(final ThreadContext context, RubyModule under, IRubyObject src, IRubyObject file, IRubyObject line) {
        return evalUnder(context, under, src.convertToString(), file.convertToString().toString(), (int) (line.convertToInteger().getLongValue() - 1));
    }

    /**
     * Evaluates the string src with self set to the current object,
     * using the module under as the context.
     */
    public IRubyObject evalUnder(final ThreadContext context, RubyModule under, RubyString src, String file, int line) {
        Visibility savedVisibility = context.getCurrentVisibility();
        context.setCurrentVisibility(Visibility.PUBLIC);
        context.preExecuteUnder(under, Block.NULL_BLOCK);
        try {
            return ASTInterpreter.evalSimple(context, this, src,
                    file, line);
        } finally {
            context.postExecuteUnder();
            context.setCurrentVisibility(savedVisibility);
        }
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
    private IRubyObject yieldUnder(final ThreadContext context, RubyModule under, IRubyObject[] args, Block block) {
        context.preExecuteUnder(under, block);

        Visibility savedVisibility = block.getBinding().getVisibility();
        block.getBinding().setVisibility(Visibility.PUBLIC);

        try {
            IRubyObject valueInYield;
            boolean aValue;
            if (args.length == 1) {
                valueInYield = args[0];
                aValue = false;
            } else {
                valueInYield = RubyArray.newArrayNoCopy(context.getRuntime(), args);
                aValue = true;
            }

            // FIXME: This is an ugly hack to resolve JRUBY-1381; I'm not proud of it
            block = block.cloneBlock();
            block.getBinding().setSelf(RubyObject.this);
            block.getBinding().getFrame().setSelf(RubyObject.this);
            // end hack

            return block.yield(context, valueInYield, RubyObject.this, context.getRubyClass(), aValue);
            //TODO: Should next and return also catch here?
        } catch (JumpException.BreakJump bj) {
            return (IRubyObject) bj.getValue();
        } finally {
            block.getBinding().setVisibility(savedVisibility);

            context.postExecuteUnder();
        }
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
    private IRubyObject yieldUnder(final ThreadContext context, RubyModule under, Block block) {
        context.preExecuteUnder(under, block);

        Visibility savedVisibility = block.getBinding().getVisibility();
        block.getBinding().setVisibility(Visibility.PUBLIC);

        try {
            // FIXME: This is an ugly hack to resolve JRUBY-1381; I'm not proud of it
            block = block.cloneBlock();
            block.getBinding().setSelf(RubyObject.this);
            block.getBinding().getFrame().setSelf(RubyObject.this);
            // end hack

            return block.yield(context, this, this, context.getRubyClass(), false);
            //TODO: Should next and return also catch here?
        } catch (JumpException.BreakJump bj) {
            return (IRubyObject) bj.getValue();
        } finally {
            block.getBinding().setVisibility(savedVisibility);

            context.postExecuteUnder();
        }
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_obj_equal
     *
     * Will by default use identity equality to compare objects. This
     * follows the Ruby semantics.
     */
    @JRubyMethod(name = "==", required = 1, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return super.op_equal(context, obj);
    }

    /** rb_obj_equal
     *
     * Will use Java identity equality.
     */
    @JRubyMethod(name = "equal?", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
        return this == obj ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    /** rb_obj_equal
     *
     * Just like "==" and "equal?", "eql?" will use identity equality for Object.
     */
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** rb_equal
     *
     * The Ruby "===" method is used by default in case/when
     * statements. The Object implementation first checks Java identity
     * equality and then calls the "==" method too.
     */
    @JRubyMethod(name = "===", required = 1)
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return context.getRuntime().newBoolean(equalInternal(context, this, other));
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "==" method.
     */
    protected static boolean equalInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        return that == other || that.callMethod(context, "==", other).isTrue();
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "eql?" method.
     */
    protected static boolean eqlInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        return that.callMethod(context, "eql?", other).isTrue();
    }

    /** rb_obj_init_copy
     *
     * Initializes this object as a copy of the original, that is the
     * parameter to this object. Will make sure that the argument
     * actually has the same real class as this object. It shouldn't
     * be possible to initialize an object with something totally
     * different.
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject original) {
	    if (this == original) return this;
	    checkFrozen();

        if (getMetaClass().getRealClass() != original.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("initialize_copy should take same class object");
	    }

	    return this;
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
    @JRubyMethod(name = "respond_to?")
    public RubyBoolean respond_to_p(IRubyObject mname) {
        String name = mname.asJavaString();
        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, true));
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
    @JRubyMethod(name = "respond_to?")
    public RubyBoolean respond_to_p(IRubyObject mname, IRubyObject includePrivate) {
        String name = mname.asJavaString();
        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, !includePrivate.isTrue()));
    }

    /** rb_obj_id
     *
     * Return the internal id of an object.
     *
     * FIXME: Should this be renamed to match its ruby name?
     */
     @JRubyMethod(name = {"object_id", "__id__"})
     @Override
     public synchronized IRubyObject id() {
        return super.id();
     }

    /** rb_obj_id_obsolete
     *
     * Old id version. This one is bound to the "id" name and will emit a deprecation warning.
     */
    @JRubyMethod(name = "id")
    public synchronized IRubyObject id_deprecated() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Object#id will be deprecated; use Object#object_id", "Object#id", "Object#object_id");
        return id();
    }

    /** rb_obj_id
     *
     * Will return the hash code of this object. In comparison to MRI,
     * this method will use the Java identity hash code instead of
     * using rb_obj_id, since the usage of id in JRuby will incur the
     * cost of some. ObjectSpace maintenance.
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(super.hashCode());
    }

    /**
     * Override the Object#hashCode method to make sure that the Ruby
     * hash is actually used as the hashcode for Ruby objects. If the
     * Ruby "hash" method doesn't return a number, the Object#hashCode
     * implementation will be used instead.
     */
    @Override
    public int hashCode() {
        IRubyObject hashValue = callMethod(getRuntime().getCurrentContext(), "hash");
        if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue);
        return nonFixnumHashCode(hashValue);
    }

    private int nonFixnumHashCode(IRubyObject hashValue) {
        Ruby runtime = getRuntime();
        if (runtime.is1_9()) {
            return (int)hashValue.convertToInteger().getLongValue();
        } else {
            hashValue = hashValue.callMethod(runtime.getCurrentContext(), "%", RubyFixnum.newFixnum(runtime, 536870923L));
            if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue);
            return System.identityHashCode(hashValue);
        }
    }

    /** rb_obj_class
     *
     * Returns the real class of this object, excluding any
     * singleton/meta class in the inheritance chain.
     */
    @JRubyMethod(name = "class")
    public RubyClass type() {
        return getMetaClass().getRealClass();
    }

    /** rb_obj_type
     *
     * The deprecated version of type, that emits a deprecation
     * warning.
     */
    @JRubyMethod(name = "type")
    public RubyClass type_deprecated() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Object#type is deprecated; use Object#class", "Object#type", "Object#class");
        return type();
    }

    /** rb_obj_clone
     *
     * This method should be overridden only by: Proc, Method,
     * UnboundedMethod, Binding. It will use the defined allocated of
     * the object, then clone the singleton class, taint the object,
     * call initCopy and then copy frozen state.
     */
    @JRubyMethod(name = "clone", frame = true)
    @Override
    public IRubyObject rbClone() {
        return super.rbClone();
    }

    /** rb_obj_dup
     *
     * This method should be overridden only by: Proc
     *
     * Will allocate a new instance of the real class of this object,
     * and then initialize that copy. It's different from {@link
     * #rbClone} in that it doesn't copy the singleton class.
     */
    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        return super.dup();
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
    @JRubyMethod(name = "display", optional = 1)
    public IRubyObject display(ThreadContext context, IRubyObject[] args) {
        IRubyObject port = args.length == 0 ? context.getRuntime().getGlobalVariables().get("$>") : args[0];

        port.callMethod(context, "write", this);

        return context.getRuntime().getNil();
    }

    /** rb_obj_tainted
     *
     *  call-seq:
     *     obj.tainted?    => true or false
     *
     *  Returns <code>true</code> if the object is tainted.
     *
     */
    @JRubyMethod(name = "tainted?")
    public RubyBoolean tainted_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isTaint());
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
    @JRubyMethod(name = "taint")
    public IRubyObject taint(ThreadContext context) {
        taint(context.getRuntime());
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
    @JRubyMethod(name = "untaint")
    public IRubyObject untaint(ThreadContext context) {
        context.getRuntime().secure(3);

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
    @JRubyMethod(name = "freeze")
    public IRubyObject freeze(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if ((flags & FROZEN_F) == 0 && (runtime.is1_9() || !isImmediate())) {
            if (runtime.getSafeLevel() >= 4 && !isTaint()) throw runtime.newSecurityError("Insecure: can't freeze object");
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
    @JRubyMethod(name = "frozen?")
    public RubyBoolean frozen_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isFrozen());
    }

    /** rb_obj_untrusted
     *  call-seq:
     *     obj.untrusted?    => true or false
     *
     *  Returns <code>true</code> if the object is untrusted.
     */
    @JRubyMethod(name = "untrusted?", compat = CompatVersion.RUBY1_9)
    public RubyBoolean untrusted_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isUntrusted());
    }

    /** rb_obj_untrust
     *  call-seq:
     *     obj.untrust -> obj
     *
     *  Marks <i>obj</i> as untrusted.
     */
    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject untrust(ThreadContext context) {
        if (!isUntrusted() && !isImmediate()) {
            checkFrozen();
            flags |= UNTRUSTED_F;
        }
        return this;
    }

    /** rb_obj_trust
     *  call-seq:
     *     obj.trust    => obj
     *
     *  Removes the untrusted mark from <i>obj</i>.
     */
    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject trust(ThreadContext context) {
        if (isUntrusted() && !isImmediate()) {
            checkFrozen();
            flags &= ~UNTRUSTED_F;
        }
        return this;
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
    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        return super.inspect();
    }

    /** rb_obj_is_instance_of
     *
     *  call-seq:
     *     obj.instance_of?(class)    => true or false
     *
     *  Returns <code>true</code> if <i>obj</i> is an instance of the given
     *  class. See also <code>Object#kind_of?</code>.
     */
    @JRubyMethod(name = "instance_of?", required = 1)
    public RubyBoolean instance_of_p(ThreadContext context, IRubyObject type) {
        if (type() == type) {
            return context.getRuntime().getTrue();
        } else if (!(type instanceof RubyModule)) {
            throw context.getRuntime().newTypeError("class or module required");
        } else {
            return context.getRuntime().getFalse();
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
    @JRubyMethod(name = {"kind_of?", "is_a?"}, required = 1)
    public RubyBoolean kind_of_p(ThreadContext context, IRubyObject type) {
        // TODO: Generalize this type-checking code into IRubyObject helper.
        if (!(type instanceof RubyModule)) {
            // TODO: newTypeError does not offer enough for ruby error string...
            throw context.getRuntime().newTypeError("class or module required");
        }

        return context.getRuntime().newBoolean(((RubyModule)type).isInstance(this));
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
    @JRubyMethod(name = "methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject methods(ThreadContext context, IRubyObject[] args) {
        return methods(context, args, false);
    }
    @JRubyMethod(name = "methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject methods19(ThreadContext context, IRubyObject[] args) {
        return methods(context, args, true);
    }

    public IRubyObject methods(ThreadContext context, IRubyObject[] args, boolean useSymbols) {
        boolean all = args.length == 1 ? args[0].isTrue() : true;
        Ruby runtime = getRuntime();
        RubyArray methods = runtime.newArray();
        Set<String> seen = new HashSet<String>();

        if (getMetaClass().isSingleton()) {
            getMetaClass().populateInstanceMethodNames(seen, methods, Visibility.PRIVATE, true, useSymbols, false);
            if (all) {
                getMetaClass().getSuperClass().populateInstanceMethodNames(seen, methods, Visibility.PRIVATE, true, useSymbols, true);
            }
        } else if (all) {
            getMetaClass().populateInstanceMethodNames(seen, methods, Visibility.PRIVATE, true, useSymbols, true);
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
    @JRubyMethod(name = "public_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject public_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().public_instance_methods(trueIfNoArgument(context, args));
    }

    @JRubyMethod(name = "public_methods", optional = 1, compat = CompatVersion.RUBY1_9)
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
    @JRubyMethod(name = "protected_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject protected_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().protected_instance_methods(trueIfNoArgument(context, args));
    }

    @JRubyMethod(name = "protected_methods", optional = 1, compat = CompatVersion.RUBY1_9)
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
    @JRubyMethod(name = "private_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject private_methods(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().private_instance_methods(trueIfNoArgument(context, args));
    }

    @JRubyMethod(name = "private_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject private_methods19(ThreadContext context, IRubyObject[] args) {
        return getMetaClass().private_instance_methods19(trueIfNoArgument(context, args));
    }

    // FIXME: If true array is common enough we should pre-allocate and stick somewhere
    private IRubyObject[] trueIfNoArgument(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? new IRubyObject[] { context.getRuntime().getTrue() } : args;
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
    @JRubyMethod(name = "singleton_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public RubyArray singleton_methods(ThreadContext context, IRubyObject[] args) {
        return singletonMethods(context, args, false);
    }

    @JRubyMethod(name = "singleton_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public RubyArray singleton_methods19(ThreadContext context, IRubyObject[] args) {
        return singletonMethods(context, args, true);
    }

    public RubyArray singletonMethods(ThreadContext context, IRubyObject[] args, boolean asSymbols) {
        boolean all = true;
        if(args.length == 1) {
            all = args[0].isTrue();
        }

        RubyArray singletonMethods;
        if (getMetaClass().isSingleton()) {
            if (asSymbols) {
                singletonMethods = getMetaClass().instance_methods19(new IRubyObject[]{context.getRuntime().getFalse()});
            } else {
                singletonMethods = getMetaClass().instance_methods(new IRubyObject[]{context.getRuntime().getFalse()});
            }
            if (all) {
                RubyClass superClass = getMetaClass().getSuperClass();
                while (superClass.isSingleton() || superClass.isIncluded()) {
                    singletonMethods.concat(superClass.instance_methods(new IRubyObject[] {context.getRuntime().getFalse()}));
                    superClass = superClass.getSuperClass();
                }
            }
        } else {
            singletonMethods = context.getRuntime().newEmptyArray();
        }

        return singletonMethods;
    }

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
    @JRubyMethod(name = "method", required = 1)
    public IRubyObject method(IRubyObject symbol) {
        return getMetaClass().newMethod(this, symbol.asJavaString(), true);
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
    @JRubyMethod(name = "to_s")
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
    @JRubyMethod(name = "to_a", visibility = Visibility.PUBLIC)
    public RubyArray to_a() {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "default 'to_a' will be obsolete", "to_a");
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
    @JRubyMethod(name = "instance_eval", frame = true)
    public IRubyObject instance_eval(ThreadContext context, Block block) {
        return specificEval(context, getInstanceEvalClass(), block);
    }
    @JRubyMethod(name = "instance_eval", frame = true)
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, block);
    }
    @JRubyMethod(name = "instance_eval", frame = true)
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, block);
    }
    @JRubyMethod(name = "instance_eval", frame = true)
    public IRubyObject instance_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, getInstanceEvalClass(), arg0, arg1, arg2, block);
    }
    @Deprecated
    public IRubyObject instance_eval(ThreadContext context, IRubyObject[] args, Block block) {
        RubyModule klazz;

        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            klazz = context.getRuntime().getDummy();
        } else {
            klazz = getSingletonClass();
        }

        return specificEval(context, klazz, args, block);
    }

    private RubyModule getInstanceEvalClass() {
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            return getRuntime().getDummy();
        } else {
            return getSingletonClass();
        }
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
    @JRubyMethod(name = "instance_exec", optional = 3, rest = true, frame = true)
    public IRubyObject instance_exec(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) throw context.getRuntime().newArgumentError("block not supplied");

        RubyModule klazz;
        if (isImmediate()) {
            // Ruby uses Qnil here, we use "dummy" because we need a class
            klazz = context.getRuntime().getDummy();
        } else {
            klazz = getSingletonClass();
        }

        return yieldUnder(context, klazz, args, block);
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
    @JRubyMethod(name = "extend", required = 1, rest = true)
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

    /** rb_obj_dummy
     *
     * Default initialize method. This one gets defined in some other
     * place as a Ruby method.
     */
    public IRubyObject initialize() {
        return getRuntime().getNil();
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
    @JRubyMethod(name = {"send", "__send__"})
    public IRubyObject send(ThreadContext context, Block block) {
        throw context.getRuntime().newArgumentError(0, 1);
    }
    @JRubyMethod(name = {"send", "__send__"})
    public IRubyObject send(ThreadContext context, IRubyObject arg0, Block block) {
        String name = arg0.asJavaString();

        return getMetaClass().finvoke(context, this, name, block);
    }
    @JRubyMethod(name = {"send", "__send__"})
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        String name = arg0.asJavaString();

        return getMetaClass().finvoke(context, this, name, arg1, block);
    }
    @JRubyMethod(name = {"send", "__send__"})
    public IRubyObject send(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        String name = arg0.asJavaString();

        return getMetaClass().finvoke(context, this, name, arg1, arg2, block);
    }
    @JRubyMethod(name = {"send", "__send__"}, rest = true)
    public IRubyObject send(ThreadContext context, IRubyObject[] args, Block block) {
        String name = args[0].asJavaString();
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
    @JRubyMethod(name = "nil?")
    public IRubyObject nil_p(ThreadContext context) {
    	return context.getRuntime().getFalse();
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
    @JRubyMethod(name = "=~", required = 1)
    public IRubyObject op_match(ThreadContext context, IRubyObject arg) {
    	return context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "!~", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_not_match(ThreadContext context, IRubyObject arg) {
        return context.getRuntime().newBoolean(! callMethod(context, "=~", arg).isTrue());
    }

    public IRubyObject to_java() {
        throw getRuntime().newTypeError(getMetaClass().getBaseName() + " cannot coerce to a Java type.");
    }

    public IRubyObject as(Class javaClass) {
        throw getRuntime().newTypeError(getMetaClass().getBaseName() + " cannot coerce to a Java type.");
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
    @JRubyMethod(name = "instance_variable_defined?", required = 1)
    public IRubyObject instance_variable_defined_p(ThreadContext context, IRubyObject name) {
        if (variableTableContains(validateInstanceVariable(name.asJavaString()))) {
            return context.getRuntime().getTrue();
        }
        return context.getRuntime().getFalse();
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
    @JRubyMethod(name = "instance_variable_get", required = 1)
    public IRubyObject instance_variable_get(ThreadContext context, IRubyObject name) {
        Object value;
        if ((value = variableTableFetch(validateInstanceVariable(name.asJavaString()))) != null) {
            return (IRubyObject)value;
        }
        return context.getRuntime().getNil();
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
    @JRubyMethod(name = "instance_variable_set", required = 2)
    public IRubyObject instance_variable_set(IRubyObject name, IRubyObject value) {
        ensureInstanceVariablesSettable();
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
    @JRubyMethod(name = "remove_instance_variable", required = 1, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject remove_instance_variable(ThreadContext context, IRubyObject name, Block block) {
        ensureInstanceVariablesSettable();
        IRubyObject value;
        if ((value = (IRubyObject)variableTableRemove(validateInstanceVariable(name.asJavaString()))) != null) {
            return value;
        }
        throw context.getRuntime().newNameError("instance variable " + name.asJavaString() + " not defined", name.asJavaString());
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
    @JRubyMethod(name = "instance_variables")
    public RubyArray instance_variables(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        List<String> nameList = getInstanceVariableNameList();

        RubyArray array = runtime.newArray(nameList.size());

        for (String name : nameList) {
            array.append(runtime.newString(name));
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
     * Tries to support Java serialization of Ruby objects. This is
     * still experimental and might not work.
     */
    // NOTE: Serialization is primarily supported for testing purposes, and there is no general
    // guarantee that serialization will work correctly. Specifically, instance variables pointing
    // at symbols, threads, modules, classes, and other unserializable types are not detected.
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // write out ivar count followed by name/value pairs
        List<String> names = getInstanceVariableNameList();
        out.writeInt(names.size());
        for (String name : names) {
            out.writeObject(name);
            out.writeObject(getInstanceVariables().getInstanceVariable(name));
        }
    }

    /**
     * Tries to support Java unserialization of Ruby objects. This is
     * still experimental and might not work.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // rest in ivar count followed by name/value pairs
        int ivarCount = in.readInt();
        for (int i = 0; i < ivarCount; i++) {
            setInstanceVariable((String)in.readObject(), (IRubyObject)in.readObject());
        }
    }

}
