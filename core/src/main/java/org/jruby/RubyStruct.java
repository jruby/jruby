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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.StructSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ByteList;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.api.Access.structClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.indexError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.ThreadContext.hasNonemptyKeywords;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.util.RubyStringBuilder.str;

@JRubyClass(name="Struct")
public class RubyStruct extends RubyObject {
    public static final String NO_MEMBER_IN_STRUCT = "no member '%1$s' in struct";
    public static final String IDENTIFIER_NEEDS_TO_BE_CONSTANT = "identifier %1$s needs to be constant";
    public static final String UNINITIALIZED_CONSTANT = "uninitialized constant %1$s";
    private static final String KEYWORD_INIT_VAR = "__keyword_init__";
    private static final String SIZE_VAR = "__size__";
    public static final String MEMBER_VAR = "__member__";
    private final IRubyObject[] values;

    /**
     * Constructor for RubyStruct.
     * @param runtime the runtime
     * @param rubyClass the class
     */
    private RubyStruct(Ruby runtime, RubyClass rubyClass) {
        this(runtime.getCurrentContext(), rubyClass);
    }

    public RubyStruct(ThreadContext context, RubyClass rubyClass) {
        super(context.runtime, rubyClass);

        int size = toInt(context, getInternalVariable(context, rubyClass, SIZE_VAR));

        values = new IRubyObject[size];

        Helpers.fillNil(context, values);
    }

    public static RubyClass createStructClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        return defineClass(context, "Struct", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyStruct.class).
                classIndex(ClassIndex.STRUCT).
                include(context, Enumerable).
                defineMethods(context, RubyStruct.class);
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.STRUCT;
    }

    private static IRubyObject getInternalVariable(ThreadContext context, RubyClass type, String internedName) {
        RubyClass structClass = structClass(context);

        while (type != null && type != structClass) {
            var variable = (IRubyObject)type.getInternalVariable(internedName);
            if (variable != null) return variable;

            type = type.getSuperClass();
        }

        return context.nil;
    }

    private RubyClass classOf() {
        final RubyClass metaClass = getMetaClass();
        return metaClass instanceof MetaClass ? metaClass.getSuperClass() : metaClass;
    }

    private void modify(ThreadContext context) {
        testFrozen();
    }

    @JRubyMethod
    public IRubyObject deconstruct_keys(ThreadContext context, IRubyObject keysArg) {
        if (keysArg.isNil()) return to_h(context, Block.NULL_BLOCK);

        if (!(keysArg instanceof RubyArray keys)) throw typeError(context, keysArg, "Array or nil");

        int length = keys.size();
        RubyHash hash = newSmallHash(context);
        if (values.length < length) return hash;

        for (int i = 0; i < length; i++) {
            IRubyObject key = keys.eltOk(i);
            try {
                hash.op_aset(context, key, aref(context, key));
            } catch (RaiseException e) {
                break;
            }
        }

        return hash;
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        int h = getType().hashCode();
        IRubyObject[] values = this.values;

        for (int i = 0; i < values.length; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            IRubyObject hash = context.safeRecurse(
                    (ctx, runtime1, obj, recur) -> recur ? asFixnum(ctx, 0) : invokedynamic(ctx, obj, HASH),
                    context.runtime, values[i], "hash", true);
            h ^= toLong(context, hash);
        }

        return asFixnum(context, h);
    }

    private IRubyObject setByName(ThreadContext context, String name, IRubyObject value) {
        final RubyArray member = __member__(context);

        modify(context);

        for ( int i = 0; i < member.getLength(); i++ ) {
            if ( member.eltInternal(i).asJavaString().equals(name) ) {
                return values[i] = value;
            }
        }

        return null;
    }

    private IRubyObject getByName(ThreadContext context, String name) {
        final RubyArray member = __member__(context);

        for ( int i = 0; i < member.getLength(); i++ ) {
            if ( member.eltInternal(i).asJavaString().equals(name) ) {
                return values[i];
            }
        }

        return null;
    }


    // FIXME: We duplicated this from ArgsUtil because for Struct (and seemingly only Struct) we need to
    //   raise TypeError instead of ArgumentError.  When .api.* addresses kwargs we should redesign how
    //   we process kwargs.
    private static IRubyObject extractKeywordArg(final ThreadContext context, final RubyHash options, String validKey) {
        if (options.isEmpty()) return null;

        final RubySymbol testKey = asSymbol(context, validKey);
        IRubyObject ret = options.fastARef(testKey);

        if (ret == null || options.size() > 1) { // other (unknown) keys in options
            options.visitAll(context, (ctxt, self, key, value, index) -> {
                if (!key.equals(testKey)) throw typeError(ctxt, "unknown keyword: " + key.inspect(ctxt));
            });
        }

        return ret;
    }

    // Struct methods

    @Deprecated(since = "10.0.0.0")
    public static RubyClass newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return newInstance(((RubyBasicObject) recv).getCurrentContext(), recv, args, block);
    }

    /** Create new Struct class.
     *
     * MRI: rb_struct_s_def / make_struct
     *
     */
    @JRubyMethod(name = "new", rest = true, checkArity = false, meta = true, keywords = true)
    public static RubyClass newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 0, -1);

        String name = null;
        boolean nilName = false;
        var member = newArray(context);
        IRubyObject keywordInitValue = context.nil;

        if (argc > 0) {
            IRubyObject firstArgAsString = args[0].checkStringType();
            if (!firstArgAsString.isNil()) {
                RubySymbol nameSym = ((RubyString)firstArgAsString).intern(context);
                if (!nameSym.validConstantName()) {
                    throw context.runtime.newNameError(IDENTIFIER_NEEDS_TO_BE_CONSTANT, recv, nameSym.toString());
                }
                name = nameSym.idString();
            } else if (args[0].isNil()) {
                nilName = true;
            }

            final IRubyObject opts = args[argc - 1];
            if (opts instanceof RubyHash) {
                argc--;
                keywordInitValue = extractKeywordArg(context, (RubyHash) opts, "keyword_init");
            }
        }

        Set<IRubyObject> tmpMemberSet = new HashSet<>();
        for (int i = (name == null && !nilName) ? 0 : 1; i < argc; i++) {
            IRubyObject arg = args[i];
            RubySymbol sym;
            if (arg instanceof RubySymbol sym1) {
                sym = sym1;
            } else if (arg instanceof RubyString) {
                sym = asSymbol(context, arg.convertToString().getByteList());
            } else {
                sym = asSymbol(context, arg.asJavaString());
            }
            if (tmpMemberSet.contains(sym)) throw argumentError(context, "duplicate member: " + sym);

            tmpMemberSet.add(sym);
            member.append(context, sym);
        }

        RubyClass newStruct;
        RubyClass superClass = (RubyClass)recv;

        if (name == null || nilName) {
            newStruct = RubyClass.newClass(context, superClass, null).
                    allocator(RubyStruct::new);
            newStruct.makeMetaClass(context, superClass.metaClass);
            superClass.invokeInherited(context, superClass, newStruct);
        } else {
            IRubyObject type = superClass.getConstantAt(context, name);
            if (type != null) {
                context.runtime.getWarnings().warn(ID.STRUCT_CONSTANT_REDEFINED, context.getFile(), context.getLine(), "redefining constant " + type);
                superClass.deleteConstant(context, name);
            }
            newStruct = superClass.defineClassUnder(context, name, superClass, RubyStruct::new);
        }

        // set reified class to RubyStruct, for Java subclasses to use
        newStruct.reifiedClass(RubyStruct.class).
                classIndex(ClassIndex.STRUCT);
        newStruct.setInternalVariable(SIZE_VAR, member.length(context));
        newStruct.setInternalVariable(MEMBER_VAR, member);
        newStruct.setInternalVariable(KEYWORD_INIT_VAR, keywordInitValue);
        newStruct.singletonClass(context).defineMethods(context, StructMethods.class);

        // define access methods.
        for (int i = (name == null && !nilName) ? 0 : 1; i < argc; i++) {
            if (i == argc - 1 && args[i] instanceof RubyHash) break;
            final String memberName = args[i].asJavaString();
            // if we are storing a name as well, index is one too high for values
            final int index = (name == null && !nilName) ? i : i - 1;
            newStruct.addMethod(context, memberName, new Accessor(newStruct, memberName, index));
            String nameAsgn = memberName + '=';
            newStruct.addMethod(context, nameAsgn, new Mutator(newStruct, nameAsgn, index));
        }

        if (block.isGiven()) {
            // Since this defines a new class, run the block as a module-eval.
            block = block.cloneBlockForEval(newStruct, EvalType.MODULE_EVAL);
            // Struct bodies should be public by default, so set block visibility to public. JRUBY-1185.
            block.getBinding().setVisibility(Visibility.PUBLIC);
            block.yieldNonArray(context, newStruct, newStruct);
        }

        return newStruct;
    }

    // For binding purposes on the newly created struct types
    public static class StructMethods {
        @Deprecated(since = "10.0.0.0")
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject[] args, Block block) {
            return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, args, block);
        }

        @JRubyMethod(name = {"new", "[]"}, rest = true, keywords = true)
        public static IRubyObject newStruct(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            return Create.newStruct(context, (RubyClass) recv, args, block);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject newStruct(IRubyObject recv, Block block) {
            return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, block);
        }

        @JRubyMethod(name = {"new", "[]"}, keywords = true)
        public static IRubyObject newStruct(ThreadContext context, IRubyObject recv, Block block) {
            return Create.newStruct(context, (RubyClass) recv, block);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, Block block) {
            return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, block);
        }

        @JRubyMethod(name = {"new", "[]"}, keywords = true)
        public static IRubyObject newStruct(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
            return Create.newStruct(context, (RubyClass) recv, arg0, block);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
            return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, arg1, block);
        }

        @JRubyMethod(name = {"new", "[]"}, keywords = true)
        public static IRubyObject newStruct(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
            return Create.newStruct(context, (RubyClass) recv, arg0, arg1, block);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, arg1, arg2, block);
        }

        @JRubyMethod(name = {"new", "[]"}, keywords = true)
        public static IRubyObject newStruct(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return Create.newStruct(context, (RubyClass) recv, arg0, arg1, arg2, block);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject members(IRubyObject recv, Block block) {
            return members(((RubyBasicObject) recv).getCurrentContext(), recv);
        }

        @JRubyMethod
        public static IRubyObject members(ThreadContext context, IRubyObject recv) {
            return RubyStruct.members(context, (RubyClass) recv);
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject inspect(IRubyObject recv) {
            return inspect(((RubyBasicObject) recv).getCurrentContext(), recv);
        }

        @JRubyMethod
        public static IRubyObject inspect(ThreadContext context, IRubyObject recv) {
            IRubyObject keywordInit = RubyStruct.getInternalVariable(context, (RubyClass) recv, KEYWORD_INIT_VAR);
            var inspected = recv.inspect(context);
            if (!keywordInit.isTrue()) return inspected;
            return inspected.convertToString().catString("(keyword_init: true)");
        }

        @Deprecated(since = "10.0.0.0")
        public static IRubyObject keyword_init_p(IRubyObject self) {
            return keyword_init_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "keyword_init?")
        public static IRubyObject keyword_init_p(ThreadContext context, IRubyObject self) {
            IRubyObject keywordInit = getInternalVariable(context, (RubyClass) self, KEYWORD_INIT_VAR);
            return keywordInit.isTrue() ? context.tru : keywordInit;
        }
    }

    /** Create new Structure.
     *
     * MRI: struct_alloc
     *
     * @deprecated Use {@link org.jruby.api.Create#newStruct(ThreadContext, RubyClass, IRubyObject[], Block)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject[] args, Block block) {
        return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, args, block);
    }

    /**
     * @param recv
     * @param block
     * @return
     * @deprecated Use {@link org.jruby.api.Create#newStruct(ThreadContext, RubyClass, Block)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public static RubyStruct newStruct(IRubyObject recv, Block block) {
        return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, block);
    }

    /**
     * @param recv
     * @param arg0
     * @param block
     * @return
     * @deprecated Use {@link org.jruby.api.Create#newStruct(ThreadContext, RubyClass, IRubyObject, Block)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, Block block) {
        return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, block);
    }

    /**
     * @param recv
     * @param arg0
     * @param arg1
     * @param block
     * @return
     * @deprecated Use {@link org.jruby.api.Create#newStruct(ThreadContext, RubyClass, IRubyObject, IRubyObject, Block)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, arg1, block);
    }

    /**
     * @param recv
     * @param arg0
     * @param arg1
     * @param arg2
     * @param block
     * @return
     * @deprecated Use {@link org.jruby.api.Create#newStruct(ThreadContext, RubyClass, IRubyObject, IRubyObject, IRubyObject, Block)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Create.newStruct(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, arg0, arg1, arg2, block);
    }

    private void checkSize(ThreadContext context, int length) {
        if (length > values.length) throw argumentError(context, "struct size differs (" + length +" for " + values.length + ")");
    }

    private void checkForKeywords(ThreadContext context, boolean keywordInit) {
        if (hasKeywords(ThreadContext.resetCallInfo(context)) && !keywordInit) {
            warn(context, "Passing only keyword arguments to Struct#initialize will behave differently from Ruby 3.2. Please use a Hash literal like .new({k: v}) instead of .new(k: v).");
        }
    }

    @JRubyMethod(rest = true, visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        IRubyObject keywordInit = RubyStruct.getInternalVariable(context, classOf(), KEYWORD_INIT_VAR);
        checkForKeywords(context, !keywordInit.isNil());
        ThreadContext.resetCallInfo(context);
        modify(context);
        checkSize(context, args.length);

        if (keywordInit.isTrue()) {
            if (args.length != 1) throw argumentError(context, args.length, 0);

            return initialize(context, args[0]);
        } else {
            System.arraycopy(args, 0, values, 0, args.length);
            Helpers.fillNil(context, values, args.length, values.length);
        }

        return context.nil;
    }

    private IRubyObject setupStructValuesFromHash(ThreadContext context, RubyHash kwArgs) {
        RubyArray __members__ = __member__(context);
        Set<Map.Entry<IRubyObject, IRubyObject>> entries = kwArgs.directEntrySet();

        entries.stream().forEach(
                entry -> {
                    IRubyObject key = entry.getKey();
                    if (!(key instanceof RubySymbol))
                        key = asSymbol(context, key.convertToString().getByteList());
                    IRubyObject index = __members__.index(context, key);
                    if (index.isNil()) throw argumentError(context, str(context.runtime, "unknown keywords: ", key));
                    values[toInt(context, index)] = entry.getValue();
                });

        return context.nil;
    }

    @JRubyMethod(visibility = PRIVATE)
    @Override
    public IRubyObject initialize(ThreadContext context) {
        IRubyObject nil = context.nil;
        return initializeInternal(context, 0, nil, nil, nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        boolean keywords = hasNonemptyKeywords(ThreadContext.resetCallInfo(context));

        if (keywords && arg0 instanceof RubyHash hash) {
            return setupStructValuesFromHash(context, hash);
        } else if (RubyStruct.getInternalVariable(context, classOf(), KEYWORD_INIT_VAR).isTrue()) {
            if (!(arg0 instanceof RubyHash hash)) throw argumentError(context, 1, 0);
            return setupStructValuesFromHash(context, hash);
        }

        return initializeInternal(context, 1, arg0, context.nil, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject keywordInit = RubyStruct.getInternalVariable(context, classOf(), KEYWORD_INIT_VAR);
        if (keywordInit.isTrue()) throw argumentError(context, 2, 0);
        return initializeInternal(context, 2, arg0, arg1, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject keywordInit = RubyStruct.getInternalVariable(context, classOf(), KEYWORD_INIT_VAR);
        if (keywordInit.isTrue()) throw argumentError(context, 3, 0);
        return initializeInternal(context, 3, arg0, arg1, arg2);
    }

    public IRubyObject initializeInternal(ThreadContext context, int provided, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        modify(context);
        checkSize(context, provided);

        switch (provided) {
        case 3:
            values[2] = arg2;
        case 2:
            values[1] = arg1;
        case 1:
            values[0] = arg0;
        }

        if (provided < values.length) Helpers.fillNil(context, values, provided, values.length);

        return context.nil;
    }

    static RubyArray members(ThreadContext context, RubyClass type) {
        final RubyArray member = __member__(context, type);
        final int len = member.getLength();
        IRubyObject[] result = new IRubyObject[len];

        for ( int i = 0; i < len; i++ ) {
            result[i] = member.eltInternal(i);
        }

        return RubyArray.newArrayNoCopy(context.runtime, result);
    }

    @Deprecated(since = "9.4-") // NOTE: no longer used ... should it get deleted?
    public static RubyArray members(IRubyObject recv, Block block) {
        return members(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv);
    }

    private static RubyArray __member__(ThreadContext context, RubyClass clazz) {
        RubyArray member = (RubyArray) getInternalVariable(context, clazz, MEMBER_VAR);

        assert !member.isNil() : "uninitialized struct";

        return member;
    }

    private RubyArray __member__(ThreadContext context) {
        return __member__(context, classOf());
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray members() {
        return members(getCurrentContext());
    }

    @JRubyMethod(name = "members")
    public RubyArray members(ThreadContext context) {
        return members(context, classOf());
    }

    @JRubyMethod
    public IRubyObject select(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select", RubyStruct::size);

        var array = newArray(context);

        for (int i = 0; i < values.length; i++) {
            if (block.yield(context, values[i]).isTrue()) {
                array.append(context, values[i]);
            }
        }

        return array;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubyStruct recv, IRubyObject[] args) {
        return recv.size(context);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject set(IRubyObject value, int index) {
        return set(getCurrentContext(), value, index);
    }

    private IRubyObject set(ThreadContext context, IRubyObject value, int index) {
        modify(context);

        return values[index] = value;
    }

    private RaiseException notStructMemberError(ThreadContext context, IRubyObject name) {
        return context.runtime.newNameError(NO_MEMBER_IN_STRUCT, this, name);
    }

    public final IRubyObject get(int index) {
        return values[index];
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyStruct struct = (RubyStruct) clone;
        System.arraycopy(values, 0, struct.values, 0, values.length);
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (!(other instanceof RubyStruct)) return context.fals;
        if (getType() != other.getType()) {
            return context.fals;
        }

        if (other == this) return context.tru;

        return RecursiveComparator.compare(context, sites(context).op_equal, this, other, true);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (!(other instanceof RubyStruct)) return context.fals;
        if (metaClass != getMetaClass(other)) {
            return context.fals;
        }

        return RecursiveComparator.compare(context, sites(context).eql, this, other, true);
    }

    public RubyBoolean compare(ThreadContext context, CallSite site, IRubyObject other) {
        if (!(other instanceof RubyStruct)) {
            return context.fals;
        }

        RubyStruct struct = (RubyStruct) other;

        if (values.length != struct.values.length) return context.fals;

        for (int i = 0; i < values.length; i++) {
            IRubyObject a = aref(context, i);
            IRubyObject b = struct.aref(context, i);

            if (!site.call(context, a, a, b).isTrue()) return context.fals;
        }

        return context.tru;
    }

    private static final byte[] STRUCT_BEG = { '#','<','s','t','r','u','c','t',' ' };
    private static final byte[] STRUCT_END = { ':','.','.','.','>' };

    /** inspect_struct
    *
    */
    private RubyString inspectStruct(final ThreadContext context, final boolean recur) {
        RubyString buffer = newString(context, new ByteList(32));
        buffer.cat(STRUCT_BEG);

        String cname = getMetaClass().getRealClass().getName(context);
        final char first = cname.charAt(0);

        if (recur || first != '#') {
            buffer.cat(cname.getBytes());
        }
        if (recur) {
            return buffer.cat(STRUCT_END);
        }

        final RubyArray member = __member__(context);
        for ( int i = 0; i < member.getLength(); i++ ) {
            if (i > 0) {
                buffer.cat(',').cat(' ');
            }
            else if (first != '#') {
                buffer.cat(' ');
            }
            RubySymbol slot = (RubySymbol) member.eltInternal(i);
            if (slot.validLocalVariableName() || slot.validConstantName()) {
                buffer.catWithCodeRange(RubyString.objAsString(context, slot));
            } else {
                buffer.catWithCodeRange(((RubyString) slot.inspect(context)));
            }
            buffer.cat('=');
            buffer.catWithCodeRange(inspect(context, values[i]));
        }

        buffer.cat('>');
        return buffer;
    }

    @JRubyMethod(name = {"inspect", "to_s"})
    public RubyString inspect(final ThreadContext context) {
        // recursion guard
        return (RubyString) context.safeRecurse((ctx, self, obj, recur) -> self.inspectStruct(ctx, recur), this, this, "inspect", false);
    }

    @JRubyMethod(name = {"to_a", "deconstruct", "values"})
    @Override
    public RubyArray to_a(ThreadContext context) {
        return newArray(context, values);
    }

    @Deprecated(since = "9.3.0.0")
    public RubyHash to_h(ThreadContext context) {
        return to_h(context, Block.NULL_BLOCK);
    }

    @JRubyMethod
    public RubyHash to_h(ThreadContext context, Block block) {
        RubyHash hash = newHash(context);
        RubyArray members = __member__(context);

        if (block.isGiven()) {
            for (int i = 0; i < values.length; i++) {
                IRubyObject elt = block.yieldValues(context, new IRubyObject[]{members.eltOk(i), values[i]});
                IRubyObject keyValue = elt.checkArrayType();

                if (keyValue == context.nil) throw typeError(context, "wrong element type ", elt, " at " + i + " (expected array)");
                RubyArray ary = (RubyArray)keyValue;

                if (ary.getLength() != 2) throw argumentError(context, "element has wrong array length (expected 2, was " + ary.getLength() + ")");

                hash.op_aset(context, ary.eltInternal(0), ary.eltInternal(1));
            }
        } else {
            for (int i = 0; i < values.length; i++) {
                hash.op_aset(context, members.eltOk(i), values[i]);
            }
        }

        return hash;
    }

    @JRubyMethod(name = {"size", "length"} )
    public RubyFixnum size(ThreadContext context) {
        return asFixnum(context, values.length);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum size() {
        return size(getCurrentContext());
    }

    public IRubyObject eachInternal(ThreadContext context, Block block) {
        for (int i = 0; i < values.length; i++) {
            block.yield(context, values[i]);
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each(final ThreadContext context, final Block block) {
        return block.isGiven() ? eachInternal(context, block) : enumeratorizeWithSize(context, this, "each", RubyStruct::size);
    }

    public IRubyObject each_pairInternal(ThreadContext context, Block block) {
        var member = __member__(context);

        for (int i = 0; i < values.length; i++) {
            block.yield(context, newArray(context, member.eltInternal(i), values[i]));
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each_pair(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairInternal(context, block) : enumeratorizeWithSize(context, this, "each_pair", RubyStruct::size);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aref(IRubyObject key) {
        return aref(getCurrentContext(), key);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject aref(ThreadContext context, IRubyObject key) {
        return arefImpl(context, key, false);
    }

    private IRubyObject arefImpl(ThreadContext context, IRubyObject key, final boolean nilOnNoMember) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            final String name = key.asJavaString();
            final IRubyObject value = getByName(context, name);
            if (value == null) {
                if (nilOnNoMember) return context.nil;
                throw notStructMemberError(context, key);
            }
            return value;
        }
        return aref(context, toInt(context, key));
    }

    @Deprecated(since = "10.0.0.0")
    final IRubyObject aref(int idx) {
        return aref(getCurrentContext(), idx);
    }

    private static void checkIndexBounds(ThreadContext context, int i, int index, int length) {
        if (i < 0) throw indexError(context, "offset " + index + " too small for struct(size:" + length + ")");
        if (i >= length) throw indexError(context, "offset " + index + " too large for struct(size:" + length + ")");
    }

    final IRubyObject aref(ThreadContext context, int originalIndex) {
        int index = originalIndex < 0 ? values.length + originalIndex : originalIndex;

        checkIndexBounds(context, index, originalIndex, values.length);

        return values[index];
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        return aset(getCurrentContext(), key, value);
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            final String name = key.asJavaString();
            final IRubyObject val = setByName(context, name, value);
            if ( val == null ) throw notStructMemberError(context, key);
            return value;
        }

        return aset(context, toInt(context, key), value);
    }

    private IRubyObject aset(ThreadContext context, int originalIndex, IRubyObject value) {
        int index = originalIndex < 0 ? values.length + originalIndex : originalIndex;

        checkIndexBounds(context, index, originalIndex, values.length);
        modify(context);

        return values[index] = value;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject values_at(IRubyObject[] args) {
        return values_at(getCurrentContext(), args);
    }

    @JRubyMethod(rest = true)
    public IRubyObject values_at(ThreadContext context, IRubyObject[] args) {
        final int olen = values.length;
        var result = allocArray(context, args.length);

        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if (arg instanceof RubyFixnum) {
                result.append(context, aref(context, arg));
                continue;
            }

            final int [] begLen = new int[2];
            if (arg instanceof RubyRange && RubyRange.rangeBeginLength(context, args[i], olen, begLen, 1).isTrue()) {
                final int beg = begLen[0];
                final int len = begLen[1];
                int end = Math.min(olen, beg + len);
                int j;
                for (j = beg; j < end; j++) {
                    result.push(context, aref(context, j));
                }
                if ( beg + len > j ) {
                    // FIXME: This can be a commented storeInternl to beg + len
                    IRubyObject[] tmp = new IRubyObject[beg + len - j];
                    Helpers.fillNil(context, tmp);
                    result.push(context, tmp);
                }
                continue;
            }
            result.push(context, aref(context, toInt(context, arg)));
        }

        return result;
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0) {
        return arefImpl(context, arg0, true);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final IRubyObject val = arefImpl(context, arg0, true);
        return RubyObject.dig1(context, val, arg1);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final IRubyObject val = arefImpl(context, arg0, true);
        return RubyObject.dig2(context, val, arg1, arg2);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        final IRubyObject val = arefImpl(context, args[0], true);
        return argc == 1 ? val : RubyObject.dig(context, val, args, 1);
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(RubyStruct struct, org.jruby.runtime.marshal.MarshalStream output) throws java.io.IOException {
        var context = struct.getRuntime().getCurrentContext();
        output.registerLinkTarget(context, struct);
        output.dumpDefaultObjectHeader(context, 'S', struct.getMetaClass());

        RubyArray member = __member__(context, struct.classOf());
        output.writeInt(member.size());

        for (int i = 0; i < member.size(); i++) {
            RubySymbol name = (RubySymbol) member.eltInternal(i);
            output.dumpObject(name);
            output.dumpObject(struct.values[i]);
        }
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, RubyStruct struct, MarshalDumper output) {
        output.registerLinkTarget(struct);
        output.dumpDefaultObjectHeader(context, out, 'S', struct.getMetaClass());

        RubyArray member = __member__(context, struct.classOf());
        output.writeInt(out, member.size());

        for (int i = 0; i < member.size(); i++) {
            RubySymbol name = (RubySymbol) member.eltInternal(i);
            output.dumpObject(context, out, name);
            output.dumpObject(context, out, struct.values[i]);
        }
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static IRubyObject unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws java.io.IOException {
        final Ruby runtime = input.getRuntime();
        var context = runtime.getCurrentContext();

        RubySymbol className = input.unique();
        RubyClass rbClass = pathToClass(context, className.asJavaString());
        if (rbClass == null) {
            throw runtime.newNameError(UNINITIALIZED_CONSTANT, structClass(context), className);
        }

        final RubyArray member = __member__(context, rbClass);

        final int len = input.unmarshalInt();

        // FIXME: This could all be more efficient, but it's how struct works
        // 1.9 does not appear to call initialize (JRUBY-5875)
        final RubyStruct result = (RubyStruct) input.entry(new RubyStruct(context, rbClass));

        for (int i = 0; i < len; i++) {
            RubySymbol slot = input.symbol();
            RubySymbol elem = (RubySymbol) member.eltInternal(i);
            if (!elem.equals(slot)) {
                throw typeError(context, str(runtime, "struct ", rbClass,
                        " not compatible (:", slot, " for :", elem, ")").toString());
            }
            result.aset(context, i, input.unmarshalObject());
        }
        return result;
    }

    public static IRubyObject unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        Ruby runtime = context.runtime;

        RubySymbol className = input.unique(context, in);
        RubyClass rbClass = pathToClass(context, className.asJavaString());
        if (rbClass == null) {
            throw runtime.newNameError(UNINITIALIZED_CONSTANT, structClass(context), className);
        }

        if (rbClass.isKindOfModule(runtime.getData())) {
            return RubyData.unmarshalFrom(context, in, input, rbClass);
        }

        final RubyArray member = __member__(context, rbClass);

        final int len = input.unmarshalInt(context, in);

        // FIXME: This could all be more efficient, but it's how struct works
        // 1.9 does not appear to call initialize (JRUBY-5875)
        final RubyStruct result = (RubyStruct) input.entry(new RubyStruct(context, rbClass));

        for (int i = 0; i < len; i++) {
            RubySymbol slot = input.symbol(context, in);
            RubySymbol elem = (RubySymbol) member.eltInternal(i);
            if (!elem.equals(slot)) {
                throw typeError(context, str(runtime, "struct ", rbClass,
                        " not compatible (:", slot, " for :", elem, ")").toString());
            }
            result.aset(context, i, input.unmarshalObject(context, in));
        }
        return result;
    }

    static RubyClass pathToClass(ThreadContext context, String path) {
        // FIXME: Throw the right ArgumentError's if the class is missing or if it's a module.
        return (RubyClass) context.runtime.getClassFromPath(path);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject arg) {
        if (this == arg) return this;
        RubyStruct original = (RubyStruct) arg;

        checkFrozen();

        System.arraycopy(original.values, 0, values, 0, original.values.length);

        return this;
    }

    public static class Accessor extends DynamicMethod {
        private final int index;

        public Accessor(RubyClass newStruct, String name, int index) {
            super(newStruct, Visibility.PUBLIC, name);
            this.index = index;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Arity.checkArgumentCount(context, name, args, 0, 0);
            return ((RubyStruct)self).get(index);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return ((RubyStruct)self).get(index);
        }

        @Override
        public DynamicMethod dup() {
            return new Accessor((RubyClass) getImplementationClass(), name, index);
        }

        public int getIndex() {
            return index;
        }
    }

    public static class Mutator extends DynamicMethod {
        private final int index;

        public Mutator(RubyClass newStruct, String name, int index) {
            super(newStruct, Visibility.PUBLIC, name);
            this.index = index;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Arity.checkArgumentCount(context, name, args, 1, 1);
            return ((RubyStruct)self).set(context, args[0], index);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
            return ((RubyStruct)self).set(context, arg, index);
        }

        @Override
        public DynamicMethod dup() {
            return new Mutator((RubyClass) getImplementationClass(), name, index);
        }

        public int getIndex() {
            return index;
        }
    }

    private static class EqlRecursive implements ThreadContext.RecursiveFunctionEx<IRubyObject> {

        static final EqlRecursive INSTANCE = new EqlRecursive();

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject other, IRubyObject self, boolean recur) {
            if (recur) return context.tru;

            IRubyObject[] values = ((RubyStruct) self).values;
            IRubyObject[] otherValues = ((RubyStruct) other).values;
            for (int i = 0; i < values.length; i++) {
                if (!eqlInternal(context, values[i], otherValues[i])) return context.fals;
            }
            return context.tru;
        }
    }

    private static class EqualRecursive implements ThreadContext.RecursiveFunctionEx<IRubyObject> {

        private static final EqualRecursive INSTANCE = new EqualRecursive();

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject other, IRubyObject self, boolean recur) {
            if (recur) return context.tru;

            IRubyObject[] values = ((RubyStruct) self).values;
            IRubyObject[] otherValues = ((RubyStruct) other).values;
            for (int i = 0; i < values.length; i++) {
                if (!equalInternal(context, values[i], otherValues[i])) return context.fals;
            }
            return context.tru;
        }
    }

    private static StructSites sites(ThreadContext context) {
        return context.sites.Struct;
    }

    @Deprecated(since = "9.4-")
    @Override
    public RubyArray to_a() {
        return to_a(getCurrentContext());
    }

}
