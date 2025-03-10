package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.ivars.VariableTableManager;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jruby.RubyBasicObject.rbInspect;
import static org.jruby.RubyHash.newSmallHash;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Convert.toSymbol;
import static org.jruby.api.Create.allocArray;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newEmptyHash;
import static org.jruby.api.Create.newSmallHash;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.keywordError;
import static org.jruby.api.Error.typeError;
import static org.jruby.ir.runtime.IRRuntimeHelpers.setCallInfo;
import static org.jruby.runtime.Arity.checkArgumentCount;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.ThreadContext.resetCallInfo;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.util.RubyStringBuilder.str;

public class RubyData {

    private static final String MEMBERS_KEY = "__members__";
    private static final String ACCESSORS_KEY = "__accessors__";

    public static RubyClass createDataClass(ThreadContext context, RubyClass Object) {
        RubyClass Data = defineClass(context, "Data", Object, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR)
                .classIndex(ClassIndex.DATA)
                .defineMethods(context, RubyData.class);

        Data.getSingletonClass().undefMethods(context, "new");

        return Data;
    }

    @JRubyMethod(meta = true, rest = true)
    public static RubyClass define(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        LinkedHashSet<RubySymbol> keySet = new LinkedHashSet<>();
        for (int i = 0 ; i < args.length ; i++) {
            RubySymbol mem = toSymbol(context, args[i]);
            if (mem.validAttrsetName()) {
                throw argumentError(context, "invalid data member: " + mem);
            }
            if (keySet.contains(mem)) {
                throw argumentError(context, "duplicate member: " + mem);
            }
            keySet.add(mem);
        }

        RubyClass dataClass = newDataStruct(context, (RubyClass) self, keySet);

        if (block.isGiven()) {
            dataClass.module_eval(context, block);
        }

        return dataClass;
    }

    @JRubyMethod(keywords = true, rest = true)
    public static void initialize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        RubyBasicObject selfObj = (RubyBasicObject) self;
        selfObj.checkFrozen();
        RubyArray<RubySymbol> members = getStructMembers(self);
        int numMembers = members.size();

        // we don't directly use callInfo here
        ThreadContext.clearCallInfo(context);

        if (args.length == 0) {
            if (numMembers > 0) {
                throw keywordError(context, "missing", members);
            }
            return;
        }

        if (args.length > 1 || !(args[0] instanceof RubyHash)) {
            throw argumentError(context, args.length, 0, 0);
        }

        RubyHash hash = (RubyHash) args[0];

        if (hash.size() < numMembers) {
            RubyArray missing = (RubyArray) members.op_diff(context, hash.keys(context));
            throw keywordError(context, "missing", missing);
        }

        RubyArray[] unknownKeywordsPtr = {null};
        VariableTableManager vtm = selfObj.getMetaClass().getVariableTableManager();
        Map<String, VariableAccessor> variableAccessors = vtm.getVariableAccessorsForRead();

        hash.visitAll(context, (c, h, k, v, i) -> {
            String keyString = toSymbol(context, k).idString();
            VariableAccessor variableAccessor = variableAccessors.get(keyString);
            if (variableAccessor != null) {
                selfObj.setInternalVariable(keyString, v);
            } else {
                RubyArray unknownKeywords = unknownKeywordsPtr[0];
                if (unknownKeywords == null) {
                    unknownKeywordsPtr[0] = unknownKeywords = newEmptyArray(context);
                }
                unknownKeywords.append(context, k);
            }
        });
        selfObj.setFrozen(true);
        if (unknownKeywordsPtr[0] != null) {
            throw keywordError(context, "unknown", unknownKeywordsPtr[0]);
        }
    }

    @JRubyMethod(name = "initialize_copy")
    public static IRubyObject initialize_copy(ThreadContext context, IRubyObject copy, IRubyObject original) {
        if (original != copy) {
            original.getMetaClass().getVariableTableManager().syncVariables((RubyBasicObject) copy, original);
        }

        copy.setFrozen(true);

        return copy;
    }

    @JRubyMethod(name = "==")
    public static IRubyObject op_equal(ThreadContext context, IRubyObject self, IRubyObject other) {
        return checkDataEquality(context, (RubyBasicObject) self, (RubyBasicObject) other, "==", RubyData::equalData);
    }

    @JRubyMethod(name = "eql?")
    public static IRubyObject eql(ThreadContext context, IRubyObject self, IRubyObject other) {
        return checkDataEquality(context, (RubyBasicObject) self, (RubyBasicObject) other, "eql?", RubyData::eqlData);
    }

    @JRubyMethod
    public static IRubyObject hash(ThreadContext context, IRubyObject self) {
        RubyBasicObject selfObj = (RubyBasicObject) self;

        int h = selfObj.getType().hashCode();
        VariableAccessor[] accessors = getStructAccessors(selfObj);
        for (int i = 0; i < accessors.length; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            IRubyObject hash = context.safeRecurse(RubyData::hashData, selfObj, (IRubyObject) accessors[i].get(selfObj), "hash", true);
            h ^= toLong(context, hash);
        }

        return asFixnum(context, h);
    }

    @JRubyMethod(alias = "to_s")
    public static IRubyObject inspect(ThreadContext context, IRubyObject self) {
        return context.safeRecurse(
                RubyData::inspectData,
                newString(context, "#<data "), self, "hash", true);
    }

    @JRubyMethod
    public static RubyHash to_h(ThreadContext context, IRubyObject self, Block block) {
        RubyArray<RubySymbol> members = getStructMembers(self);
        VariableAccessor[] accessors = getStructAccessors(self);
        RubyHash h = newSmallHash(context);

        for (int i = 0 ; i < accessors.length; i++) {
            RubySymbol k = members.eltOk(i);
            IRubyObject v = (IRubyObject) accessors[i].get(self);
            if (block.isGiven()) {
                h.fastASetSmallPair(context, block.yieldSpecific(context, k, v));
            } else {
                h.fastASetSmall(k, v);
            }
        }
        return h;
    }

    @JRubyMethod
    public static RubyArray members(ThreadContext context, IRubyObject self) {
        return getStructMembers(self).aryDup();
    }

    @JRubyMethod
    public static RubyArray deconstruct(ThreadContext context, IRubyObject self) {
        VariableAccessor[] accessors = getStructAccessors(self);
        RubyArray ary = allocArray(context, accessors.length);
        for (int i = 0; i < accessors.length; i++) {
            ary.append(context, (IRubyObject) accessors[i].get(self));
        }
        return ary;
    }

    @JRubyMethod
    public static RubyHash deconstruct_keys(ThreadContext context, IRubyObject self, IRubyObject keys) {
        if (keys.isNil()) {
            return to_h(context, self, Block.NULL_BLOCK);
        }
        if (!(keys instanceof RubyArray)) {
            throw typeError(context, "wrong argument type " + keys.getType() + " (expected Array or nil)");
        }
        RubyArray keysAry = (RubyArray) keys;
        RubyArray<RubySymbol> members = getStructMembers(self);
        VariableAccessor[] accessors = getStructAccessors(self);
        if (accessors.length < keysAry.size()) {
            return newEmptyHash(context);
        }
        RubyHash h = newSmallHash(context);
        for (int i = 0; i < keysAry.size(); i++) {
            IRubyObject key = keysAry.eltOk(i);
            int memberIndex = members.indexOf(key);
            if (memberIndex == -1) {
                return h;
            }
            h.fastASetSmall(key, (IRubyObject) accessors[memberIndex].get(self));
        }
        return h;
    }

    @JRubyMethod(keywords = true, optional = 1, checkArity = false)
    public static IRubyObject with(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        IRubyObject kwargs = IRRuntimeHelpers.receiveKeywords(context, args, false, true, false);
        if (kwargs == UndefinedValue.UNDEFINED || kwargs.isNil()) {
            checkArgumentCount(context, args.length, 0, 0);
            return self;
        }

        checkArgumentCount(context, args.length - 1, 0, 0);

        RubyHash kwargsHash = (RubyHash) kwargs;
        RubyHash h = to_h(context, self, Block.NULL_BLOCK);
        h.addAll(context, kwargsHash);
        setCallInfo(context, CALL_KEYWORD);
        return DataMethods.rbNew(context, self.getMetaClass(), h);
    }

    public static class DataMethods {
        @JRubyMethod(name = {"new", "[]"}, keywords = true, rest = true)
        public static IRubyObject rbNew(ThreadContext context, IRubyObject self, IRubyObject[] values) {
            RubyClass klass = (RubyClass) self;

            RubyHash init;
            IRubyObject maybeKwargs = IRRuntimeHelpers.receiveKeywords(context, values, true, true, false);
            if (maybeKwargs instanceof RubyHash) {
                init = (RubyHash) maybeKwargs;
            } else {
                RubyArray<RubySymbol> members = getMembersFromClass(klass);

                checkArgumentCount(context, values.length, 0, members.size());

                init = newSmallHash(context.runtime);

                for (int i = 0; i < values.length; i++) {
                    RubySymbol sym = members.eltOk(i);
                    init.fastASetSmall(sym, values[i]);
                }
            }

            IRubyObject dataObject = klass.getAllocator().allocate(context.runtime, klass);

            setCallInfo(context, ThreadContext.CALL_KEYWORD);

            // TODO: avoid initialize and hash overhead for known types
            dataObject.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE)
                    .call(context, self, dataObject, init);


            return dataObject;
        }

        @JRubyMethod(name = {"new", "[]"}, keywords = true)
        public static IRubyObject rbNew(ThreadContext context, IRubyObject self, IRubyObject hashOrElt) {
            RubyClass klass = (RubyClass) self;

            RubyHash init;
            if (hasKeywords(resetCallInfo(context))) {
                if (!(hashOrElt instanceof RubyHash)) {
                    throw argumentError(context, 1, 0, 0);
                }

                init = (RubyHash) hashOrElt;
            } else {
                RubyArray<RubySymbol> members = getMembersFromClass(klass);

                checkArgumentCount(context, 1, 0, members.size());

                init = newSmallHash(context.runtime);

                RubySymbol sym = members.eltOk(0);
                init.fastASetSmall(sym, hashOrElt);
            }

            IRubyObject dataObject = klass.getAllocator().allocate(context.runtime, klass);

            setCallInfo(context, ThreadContext.CALL_KEYWORD);

            // TODO: avoid initialize and hash overhead for known types
            dataObject.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE)
                    .call(context, self, dataObject, init);


            return dataObject;
        }

        @JRubyMethod(name = "members")
        public static IRubyObject members(ThreadContext context, IRubyObject self) {
            return getMembersFromClass((RubyClass) self).aryDup();
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(ThreadContext context, IRubyObject klass) {
            IRubyObject inspect = ((RubyClass) klass).rubyName(context);
            // TODO: keyword init like struct
//            if (RTEST(rb_struct_s_keyword_init(klass))) {
//                rb_str_cat_cstr(inspect, "(keyword_init: true)");
//            }
            return inspect;
        }
    }

    // TODO: Mostly copied from RubyStruct; unify.
    public static void marshalTo(ThreadContext context, NewMarshal.RubyOutputStream out, IRubyObject data, NewMarshal output) {
        output.registerLinkTarget(data);
        output.dumpDefaultObjectHeader(context, out, 'S', data.getMetaClass());

        RubyArray<RubySymbol> members = getStructMembers(data);
        VariableAccessor[] accessors = getStructAccessors(data);
        int size = members.size();
        output.writeInt(context, out, size);

        for (int i = 0; i < size; i++) {
            RubySymbol name = members.eltInternal(i);
            output.dumpObject(context, out, name);
            output.dumpObject(context, out, (IRubyObject) accessors[i].get(data));
        }
    }

    // TODO: Mostly copied from RubyStruct; unify.
    public static IRubyObject unmarshalFrom(ThreadContext context, UnmarshalStream input, RubyClass rbClass) throws java.io.IOException {
        final RubyArray<RubySymbol> members = getMembersFromClass(rbClass);
        final VariableAccessor[] accessors = getAccessorsFromClass(rbClass);

        final int len = input.unmarshalInt();

        final IRubyObject result = input.entry(rbClass.allocate(context));

        for (int i = 0; i < len; i++) {
            RubySymbol slot = input.symbol();
            RubySymbol elem = members.eltInternal(i);
            if (!elem.equals(slot)) {
                throw typeError(context, str(context.runtime, "struct ", rbClass,
                        " not compatible (:", slot, " for :", elem, ")").toString());
            }
            accessors[i].set(result, input.unmarshalObject());
        }

        result.setFrozen(true);

        return result;
    }

    private static RubyClass newDataStruct(ThreadContext context, RubyClass superClass, LinkedHashSet<RubySymbol> keySet) {
        Ruby runtime = context.runtime;

        RubyClass subclass = RubyClass.newClass(runtime, superClass);

        VariableTableManager vtm = subclass.getVariableTableManager();
        VariableAccessor[] accessors = new VariableAccessor[keySet.size()];
        int i = 0;
        for (RubySymbol sym : keySet) {
            accessors[i++] = vtm.getVariableAccessorForWrite(sym.idString());
        }

        ObjectAllocator allocator =
                runtime.getObjectSpecializer()
                        .specializeForVariables(
                                subclass,
                                keySet.stream().map(RubySymbol::idString).collect(Collectors.toSet()));

        subclass.allocator(allocator);

        RubyArray members = newArray(context, keySet);
        members.freeze(context);
        subclass.setInternalVariable(MEMBERS_KEY, members);
        subclass.setInternalVariable(ACCESSORS_KEY, accessors);

        RubyClass dataSClass = subclass.getSingletonClass();

        dataSClass.undefMethods(context, "define")
                .defineMethods(context, DataMethods.class);

        for (RubySymbol sym : keySet) {
            VariableAccessor accessor = vtm.getVariableAccessorForWrite(sym.idString());
            // TODO: AttrReader expects to potentially see many variable tables; this could be simplified
            subclass.addMethod(context, sym.idString(), new AttrReaderMethod(subclass, Visibility.PUBLIC, accessor));
        }

        return subclass;
    }

    private static IRubyObject checkDataEquality(ThreadContext context, RubyBasicObject self, RubyBasicObject other, String name, ThreadContext.RecursiveFunctionEx<RubyBasicObject> func) {
        RubyBasicObject selfObj = self;
        RubyBasicObject otherObj = other;

        if (selfObj == otherObj) return context.tru;
        RubyClass metaClass = otherObj.getMetaClass();
        if (!metaClass.isKindOfModule(context.runtime.getData())) return context.fals;
        if (metaClass != selfObj.getMetaClass()) return context.fals;
//        if (RSTRUCT_LEN(s) != RSTRUCT_LEN(s2)) {
//            rb_bug("inconsistent struct"); /* should never happen */
//        }

        return context.safeRecurse(func, selfObj, otherObj, name, true);
    }

    private static IRubyObject inspectData(ThreadContext context, RubyString prefix, IRubyObject s, boolean recur) {
        RubyString cname = s.getMetaClass().rubyName(context);
        RubyString str = prefix;
        char first = cname.charAt(0);

        if (recur || first != '#') {
            str.append(cname);
        }
        if (recur) {
            return str.catString(":...>");
        }

        RubyArray<RubySymbol> members = getStructMembers(s);
        VariableAccessor[] accessors = getStructAccessors(s);
        int len = members.size();

        for (int i = 0; i < len; i++) {
            if (i > 0) {
                str.catString(", ");
            } else if (first != '#') {
                str.catString(" ");
            }

            RubySymbol id = members.eltOk(i);
            if (id.validLocalVariableName() || id.validConstantName()) {
                str.append(id.fstring());
            } else {
                str.append(id.inspect(context));
            }
            str.catString("=");
            str.append(rbInspect(context, (IRubyObject) accessors[i].get(s)));
        }
        str.catString(">");

        return str;
    }

    private static RubyArray<RubySymbol> getStructMembers(IRubyObject s) {
        RubyClass metaClass = s.getMetaClass();
        return getMembersFromClass(metaClass);
    }

    private static RubyArray<RubySymbol> getMembersFromClass(RubyClass metaClass) {
        while (metaClass != null) {
            RubyArray<RubySymbol> members = (RubyArray<RubySymbol>) metaClass.getInternalVariable(MEMBERS_KEY);

            if (members != null) return members;

            metaClass = metaClass.getSuperClass();
        }

        throw new RuntimeException("non-Data attempted to access Data members");
    }

    private static VariableAccessor[] getStructAccessors(IRubyObject s) {
        RubyClass metaClass = s.getMetaClass();
        return getAccessorsFromClass(metaClass);
    }

    private static VariableAccessor[] getAccessorsFromClass(RubyClass metaClass) {
        while (metaClass != null) {
            VariableAccessor[] accessors = (VariableAccessor[]) metaClass.getInternalVariable(ACCESSORS_KEY);

            if (accessors != null) return accessors;

            metaClass = metaClass.getSuperClass();
        }

        throw new RuntimeException("non-Data attempted to access Data accessors");
    }

    private static IRubyObject hashData(ThreadContext ctx, RubyBasicObject state, IRubyObject obj, boolean recur) {
        return recur ? asFixnum(ctx, 0) : invokedynamic(ctx, obj, HASH);
    }

    private static IRubyObject eqlData(ThreadContext c, RubyBasicObject s, IRubyObject o, boolean recur) {
        if (recur) return c.tru;
        VariableAccessor[] accessors = getStructAccessors(s);
        for (int i = 0; i < accessors.length; i++) {
            VariableAccessor accessor = accessors[i];
            if (!RubyBasicObject.eqlInternal(c, ((RubyBasicObject) accessor.get(s)), (IRubyObject) accessor.get(o))) {
                return c.fals;
            }
        }

        return c.tru;
    }

    private static IRubyObject equalData(ThreadContext c, RubyBasicObject s, IRubyObject o, boolean recur) {
        if (recur) return c.tru;
        VariableAccessor[] accessors = getStructAccessors(s);
        for (int i = 0; i < accessors.length; i++) {
            VariableAccessor accessor = accessors[i];
            if (!RubyBasicObject.equalInternal(c, ((RubyBasicObject) accessor.get(s)), (IRubyObject) accessor.get(o))) {
                return c.fals;
            }
        }

        return c.tru;
    }
}
