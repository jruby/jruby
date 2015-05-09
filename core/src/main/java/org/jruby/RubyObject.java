/*
 ***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;

import org.jcodings.Encoding;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import org.jruby.util.cli.Options;

/**
 * RubyObject represents the implementation of the Object class in Ruby. As such,
 * it defines very few methods of its own, inheriting most from the included
 * Kernel module.
 *
 * Methods that are implemented here, such as "initialize" should be implemented
 * with care; reification of Ruby classes into Java classes can produce
 * conflicting method names in rare cases. See JRUBY-5906 for an example.
 * @author headius
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

    @Deprecated
    protected RubyObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, metaClass, useObjectSpace, canBeTainted);
    }

    /**
     * Path for objects who want to decide whether they don't want to be in
     * ObjectSpace even when it is on. (notably used by objects being
     * considered immediate, they'll always pass false here)
     */
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
        objectClass.setClassIndex(ClassIndex.OBJECT);
        objectClass.setReifiedClass(RubyObject.class);
        return objectClass;
    }

    /**
     * Default allocator instance for all Ruby objects. The only
     * reason to not use this allocator is if you actually need to
     * have all instances of something be a subclass of RubyObject.
     *
     * @see org.jruby.runtime.ObjectAllocator
     */
    public static final ObjectAllocator OBJECT_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObject(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR0_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar0(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR1_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar1(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR2_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar2(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR3_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar3(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR4_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar4(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR5_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar5(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR6_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar6(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR7_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar7(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR8_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar8(runtime, klass);
        }
    };

    public static final ObjectAllocator OBJECT_VAR9_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObjectVar9(runtime, klass);
        }
    };

    public static final ObjectAllocator[] FIELD_ALLOCATORS = {
        OBJECT_ALLOCATOR,
        OBJECT_VAR0_ALLOCATOR,
        OBJECT_VAR1_ALLOCATOR,
        OBJECT_VAR2_ALLOCATOR,
        OBJECT_VAR3_ALLOCATOR,
        OBJECT_VAR4_ALLOCATOR,
        OBJECT_VAR5_ALLOCATOR,
        OBJECT_VAR6_ALLOCATOR,
        OBJECT_VAR7_ALLOCATOR,
        OBJECT_VAR8_ALLOCATOR,
        OBJECT_VAR9_ALLOCATOR
    };

    public static final Class[] FIELD_ALLOCATED_CLASSES = {
        RubyObject.class,
        RubyObjectVar0.class,
        RubyObjectVar1.class,
        RubyObjectVar2.class,
        RubyObjectVar3.class,
        RubyObjectVar4.class,
        RubyObjectVar5.class,
        RubyObjectVar6.class,
        RubyObjectVar7.class,
        RubyObjectVar8.class,
        RubyObjectVar9.class,
    };

    /**
     * Allocator that inspects all methods for instance variables and chooses
     * a concrete class to construct based on that. This allows using
     * specialized subclasses to hold instance variables in fields rather than
     * always holding them in an array.
     */
    public static final ObjectAllocator IVAR_INSPECTING_OBJECT_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            Set<String> foundVariables = klass.discoverInstanceVariables();

            if (Options.DUMP_INSTANCE_VARS.load()) {
                System.err.println(klass + ";" + foundVariables);
            }

            int count = 0;
            for (String name : foundVariables) {
                klass.getVariableTableManager().getVariableAccessorForVar(name, count);
                count++;
                if (count >= 10) break;
            }

            ObjectAllocator allocator = FIELD_ALLOCATORS[count];
            Class reified = FIELD_ALLOCATED_CLASSES[count];
            klass.setAllocator(allocator);
            klass.setReifiedClass(reified);

            return allocator.allocate(runtime, klass);
        }
    };

    public static final ObjectAllocator REIFYING_OBJECT_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            klass.reifyWithAncestors();
            return klass.allocate();
        }
    };

    @Deprecated
    @Override
    public IRubyObject initialize() {
        return getRuntime().getNil();
    }

    public IRubyObject initialize(ThreadContext context) {
        return initialize19(context);
    }

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
     * @see org.jruby.runtime.ClassIndex
     */
    @Override
    public ClassIndex getNativeClassIndex() {
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
                invokedynamic(getRuntime().getCurrentContext(), this, OP_EQUAL, (IRubyObject) other).isTrue();
    }

    /**
     * The default toString method is just a wrapper that calls the
     * Ruby "to_s" method.
     */
    @Override
    public String toString() {
        RubyString rubyString = Helpers.invoke(getRuntime().getCurrentContext(), this, "to_s").convertToString();
        return rubyString.getUnicodeValue();
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(IRubyObject[] args, Block block) {
        Helpers.invoke(getRuntime().getCurrentContext(), this, "initialize", args, block);
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(Block block) {
        Helpers.invoke(getRuntime().getCurrentContext(), this, "initialize", block);
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(IRubyObject arg0, Block block) {
        Helpers.invoke(getRuntime().getCurrentContext(), this, "initialize", arg0, block);
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(IRubyObject arg0, IRubyObject arg1, Block block) {
        Helpers.invoke(getRuntime().getCurrentContext(), this, "initialize", arg0, arg1, block);
    }

    /**
     * Call the Ruby initialize method with the supplied arguments and block.
     */
    public final void callInit(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        Helpers.invoke(getRuntime().getCurrentContext(), this, "initialize", arg0, arg1, arg2, block);
    }

    public final void callInit(ThreadContext context, IRubyObject[] args, Block block) {
        getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, this, this, args, block);
    }

    public final void callInit(ThreadContext context, Block block) {
        getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, this, this, block);
    }

    public final void callInit(ThreadContext context, IRubyObject arg0, Block block) {
        getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, this, this, arg0, block);
    }

    public final void callInit(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, this, this, arg0, arg1, block);
    }

    public final void callInit(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, this, this, arg0, arg1, arg2, block);
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
    public IRubyObject specificEval(ThreadContext context, RubyModule mod, IRubyObject[] args, Block block, EvalType evalType) {
        if (block.isGiven()) {
            if (args.length > 0) throw getRuntime().newArgumentError(args.length, 0);

            return yieldUnder(context, mod, block, evalType);
        }

        if (args.length == 0) {
            throw getRuntime().newArgumentError("block not supplied");
        } else if (args.length > 3) {
            String lastFuncName = context.getFrameName();
            throw getRuntime().newArgumentError(
                "wrong number of arguments: " + lastFuncName + "(src) or " + lastFuncName + "{..}");
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

        return evalUnder(context, mod, evalStr, file, line, evalType);
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_equal
     *
     * The Ruby "===" method is used by default in case/when
     * statements. The Object implementation first checks Java identity
     * equality and then calls the "==" method too.
     */
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return context.runtime.newBoolean(equalInternal(context, this, other));
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "==" method.
     */
    protected static boolean equalInternal(final ThreadContext context, final IRubyObject a, final IRubyObject b){
        if (a == b) {
            return true;
        } else if (a instanceof RubySymbol) {
            return false;
        } else if (a instanceof RubyFixnum && b instanceof RubyFixnum) {
            return ((RubyFixnum)a).fastEqual((RubyFixnum)b);
        } else if (a instanceof RubyFloat && b instanceof RubyFloat) {
            return ((RubyFloat)a).fastEqual((RubyFloat)b);
        } else {
            return invokedynamic(context, a, OP_EQUAL, b).isTrue();
        }
    }

    /**
     * Helper method for checking equality, first using Java identity
     * equality, and then calling the "eql?" method.
     */
    protected static boolean eqlInternal(final ThreadContext context, final IRubyObject a, final IRubyObject b){
        if (a == b) {
            return true;
        } else if (a instanceof RubySymbol) {
            return false;
        } else if (a instanceof RubyNumeric) {
            if (a.getClass() != b.getClass()) return false;
            return equalInternal(context, a, b);
        } else {
            return invokedynamic(context, a, EQL, b).isTrue();
        }
    }

    /**
     * Override the Object#hashCode method to make sure that the Ruby
     * hash is actually used as the hashcode for Ruby objects. If the
     * Ruby "hash" method doesn't return a number, the Object#hashCode
     * implementation will be used instead.
     */
    @Override
    public int hashCode() {
        IRubyObject hashValue = invokedynamic(getRuntime().getCurrentContext(), this, HASH);
        if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue);
        return nonFixnumHashCode(hashValue);
    }

    private int nonFixnumHashCode(IRubyObject hashValue) {
        RubyInteger integer = hashValue.convertToInteger();
        if (integer instanceof RubyBignum) {
            return integer.getBigIntegerValue().intValue();
        }
        return (int) integer.getLongValue();
    }

    /** rb_inspect
     *
     * The internal helper that ensures a RubyString instance is returned
     * so dangerous casting can be omitted
     * Prefered over callMethod(context, "inspect")
     */
    public static RubyString inspect(ThreadContext context, IRubyObject object) {
        Ruby runtime = context.runtime;
        RubyString str = RubyString.objAsString(context, object.callMethod(context, "inspect"));
        Encoding ext = runtime.getDefaultExternalEncoding();
        if (!ext.isAsciiCompatible()) {
            if (!str.isAsciiOnly()) {
                throw runtime.newEncodingCompatibilityError("inspected result must be ASCII only if default external encoding is ASCII incompatible");
            }
            return str;
        }
        if (str.getEncoding() != ext && !str.isAsciiOnly()) {
            throw runtime.newEncodingCompatibilityError("inspected result must be ASCII only or use the default external encoding");
        }
        return str;
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
