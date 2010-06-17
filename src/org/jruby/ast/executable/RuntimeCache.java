package org.jruby.ast.executable;

import java.math.BigInteger;
import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyClass.VariableAccessor;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ByteList;

public class RuntimeCache {

    public RuntimeCache() {
    }

    public final StaticScope getScope(ThreadContext context, String varNamesDescriptor, int index) {
        StaticScope scope = scopes[index];
        if (scope == null) {
            String[] varNames = varNamesDescriptor.split(";");
            for (int i = 0; i < varNames.length; i++) {
                varNames[i] = varNames[i].intern();
            }
            scope = scopes[index] = new LocalStaticScope(context.getCurrentScope().getStaticScope(), varNames);
        }
        return scope;
    }

    public final CallSite getCallSite(int index) {
        return callSites[index];
    }

    /**
     * descriptor format is
     *
     * closure_method_name,arity,varname1;varname2;varname3,has_multi_args_head,arg_type,light
     *
     * @param context
     * @param index
     * @param descriptor
     * @return
     */
    public final BlockBody getBlockBody(Object scriptObject, ThreadContext context, int index, String descriptor) {
        BlockBody body = blockBodies[index];
        if (body == null) {
            return createBlockBody(scriptObject, context, index, descriptor);
        }
        return body;
    }

    /**
     * descriptor format is
     *
     * closure_method_name,arity,varname1;varname2;varname3,has_multi_args_head,arg_type,light
     *
     * @param context
     * @param index
     * @param descriptor
     * @return
     */
    public final BlockBody getBlockBody19(Object scriptObject, ThreadContext context, int index, String descriptor) {
        BlockBody body = blockBodies[index];
        if (body == null) {
            return createBlockBody19(scriptObject, context, index, descriptor);
        }
        return body;
    }

    public final CompiledBlockCallback getBlockCallback(Object scriptObject, Ruby runtime, int index, String method) {
        CompiledBlockCallback callback = blockCallbacks[index];
        if (callback == null) {
            return createCompiledBlockCallback(scriptObject, runtime, index, method);
        }
        return callback;
    }

    public final RubySymbol getSymbol(Ruby runtime, int index, String name) {
        RubySymbol symbol = symbols[index];
        if (symbol == null) {
            return symbols[index] = runtime.newSymbol(name);
        }
        return symbol;
    }

    public final RubyString getString(Ruby runtime, int index) {
        return RubyString.newStringShared(runtime, byteLists[index]);
    }

    public final RubyFixnum getFixnum(Ruby runtime, int index, int value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) {
            return fixnums[index] = RubyFixnum.newFixnum(runtime, value);
        }
        return fixnum;
    }

    public final RubyFixnum getFixnum(Ruby runtime, int index, long value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) {
            return fixnums[index] = RubyFixnum.newFixnum(runtime, value);
        }
        return fixnum;
    }

    public final RubyRegexp getRegexp(Ruby runtime, int index, String pattern, int options) {
        RubyRegexp regexp = regexps[index];
        if (regexp == null || runtime.getKCode() != regexp.getKCode()) {
            regexp = RubyRegexp.newRegexp(runtime, pattern, options);
            regexp.setLiteral();
            regexps[index] = regexp;
        }
        return regexp;
    }

    public final RubyRegexp getRegexp(int index) {
        return regexps[index];
    }

    public final RubyRegexp cacheRegexp(int index, RubyString pattern, int options) {
        RubyRegexp regexp = regexps[index];
        if (regexp == null) {
            regexp = RubyRegexp.newRegexp(pattern.getRuntime(), pattern.getByteList(), options);
            regexps[index] = regexp;
        }
        return regexp;
    }

    public final BigInteger getBigInteger(Ruby runtime, int index, String pattern) {
        BigInteger bigint = bigIntegers[index];
        if (bigint == null) {
            return bigIntegers[index] = new BigInteger(pattern, 16);
        }
        return bigint;
    }

    public final IRubyObject getVariable(Ruby runtime, int index, String name, IRubyObject object) {
        VariableAccessor variableAccessor = variableReaders[index];
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            variableReaders[index] = variableAccessor = cls.getVariableAccessorForRead(name);
        }
        IRubyObject value = (IRubyObject) variableAccessor.get(object);
        if (value != null) {
            return value;
        }
        if (runtime.isVerbose()) {
            warnAboutUninitializedIvar(runtime, name);
        }
        return runtime.getNil();
    }

    private void warnAboutUninitializedIvar(Ruby runtime, String name) {
        runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, "instance variable " + name + " not initialized");
    }

    public final IRubyObject setVariable(Ruby runtime, int index, String name, IRubyObject object, IRubyObject value) {
        VariableAccessor variableAccessor = variableWriters[index];
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            variableWriters[index] = variableAccessor = cls.getVariableAccessorForWrite(name);
        }
        variableAccessor.set(object, value);
        return value;
    }

    public final void initScopes(int size) {
        scopes = new StaticScope[size];
    }

    public final void initCallSites(int size) {
        callSites = new CallSite[size];
    }

    /**
     * Given a packed descriptor listing methods and their type, populate the
     * call site cache.
     *
     * The format of the methods portion of the descriptor is
     * name1;type1;name2;type2 where type1 and type2 are a single capital letter
     * N, F, V, or S for the four main call types. After the method portion,
     * the other cache sizes are provided as a packed String of char values
     * representing the numeric sizes. @see RuntimeCache#initOthers.
     *
     * @param descriptor The descriptor to use for populating call sites and caches
     */
    public final void initFromDescriptor(String descriptor) {
        String[] pieces = descriptor.split("\uFFFF");
        CallSite[] sites = new CallSite[pieces.length - 1 / 2];

        // if there's no call sites, don't process it
        if (pieces[0].length() != 0) {
            for (int i = 0; i < pieces.length - 1; i+=2) {
                switch (pieces[i + 1].charAt(0)) {
                case 'N':
                    sites[i/2] = MethodIndex.getCallSite(pieces[i]);
                    break;
                case 'F':
                    sites[i/2] = MethodIndex.getFunctionalCallSite(pieces[i]);
                    break;
                case 'V':
                    sites[i/2] = MethodIndex.getVariableCallSite(pieces[i]);
                    break;
                case 'S':
                    sites[i/2] = MethodIndex.getSuperCallSite();
                    break;
                default:
                    throw new RuntimeException("Unknown call type: " + pieces[i + 1] + " for method " + pieces[i]);
                }
            }

            this.callSites = sites;
        }

        initOthers(pieces[pieces.length - 1]);
    }

    private static final int SCOPE = 0;
    private static final int SYMBOL = SCOPE + 1;
    private static final int FIXNUM = SYMBOL + 1;
    private static final int CONSTANT = FIXNUM + 1;
    private static final int REGEXP = CONSTANT + 1;
    private static final int BIGINTEGER = REGEXP + 1;
    private static final int VARIABLEREADER = BIGINTEGER + 1;
    private static final int VARIABLEWRITER = VARIABLEREADER + 1;
    private static final int BLOCKBODY = VARIABLEWRITER + 1;
    private static final int BLOCKCALLBACK = BLOCKBODY + 1;
    private static final int METHOD = BLOCKCALLBACK + 1;
    private static final int STRING = METHOD + 1;

    /**
     * Given a packed descriptor of other cache sizes, construct the cache arrays
     *
     * The format of the descriptor is the actual size cast to char in this order:
     * <ol>
       <li>scopeCount</li>
       <li>inheritedSymbolCount</li>
       <li>inheritedFixnumCount</li>
       <li>inheritedConstantCount</li>
       <li>inheritedRegexpCount</li>
       <li>inheritedBigIntegerCount</li>
       <li>inheritedVariableReaderCount</li>
       <li>inheritedVariableWriterCount</li>
       <li>inheritedBlockBodyCount</li>
       <li>inheritedBlockCallbackCount</li>
       <li>inheritedMethodCount</li>
       <li>inheritedStringCount</li>
     * </ul>
     *
     * @param descriptor The descriptor to use for preparing caches
     */
    public final void initOthers(String descriptor) {
        int scopeCount = getDescriptorValue(descriptor, SCOPE);
        if (scopeCount > 0) initScopes(scopeCount);
        int symbolCount = getDescriptorValue(descriptor, SYMBOL);
        if (symbolCount > 0) initSymbols(symbolCount);
        int fixnumCount = getDescriptorValue(descriptor, FIXNUM);
        if (fixnumCount > 0) initFixnums(fixnumCount);
        int constantCount = getDescriptorValue(descriptor, CONSTANT);
        if (constantCount > 0) initConstants(constantCount);
        int regexpCount = getDescriptorValue(descriptor, REGEXP);
        if (regexpCount > 0) initRegexps(regexpCount);
        int bigIntegerCount = getDescriptorValue(descriptor, BIGINTEGER);
        if (bigIntegerCount > 0) initBigIntegers(bigIntegerCount);
        int variableReaderCount = getDescriptorValue(descriptor, VARIABLEREADER);
        if (variableReaderCount > 0) initVariableReaders(variableReaderCount);
        int variableWriterCount = getDescriptorValue(descriptor, VARIABLEWRITER);
        if (variableWriterCount > 0) initVariableWriters(variableWriterCount);
        int blockBodyCount = getDescriptorValue(descriptor, BLOCKBODY);
        if (blockBodyCount > 0) initBlockBodies(blockBodyCount);
        int blockCallbackCount = getDescriptorValue(descriptor, BLOCKCALLBACK);
        if (blockCallbackCount > 0) initBlockCallbacks(blockCallbackCount);
        int methodCount = getDescriptorValue(descriptor, METHOD);
        if (methodCount > 0) initMethodCache(methodCount);
        int stringCount = getDescriptorValue(descriptor, STRING);
        if (stringCount > 0) initStrings(stringCount);
    }

    private static int getDescriptorValue(String descriptor, int type) {
        return descriptor.charAt(type);
    }

    public final void initBlockBodies(int size) {
        blockBodies = new BlockBody[size];
    }

    public final void initBlockCallbacks(int size) {
        blockCallbacks = new CompiledBlockCallback[size];
    }

    public final void initSymbols(int size) {
        symbols = new RubySymbol[size];
    }

    public final ByteList[] initStrings(int size) {
        return byteLists = new ByteList[size];
    }

    public final void initFixnums(int size) {
        fixnums = new RubyFixnum[size];
    }

    public final void initRegexps(int size) {
        regexps = new RubyRegexp[size];
    }

    public final void initBigIntegers(int size) {
        bigIntegers = new BigInteger[size];
    }

    public final void initConstants(int size) {
        constants = new IRubyObject[size];
        constantTargetHashes = new int[size];
        constantGenerations = new int[size];
        Arrays.fill(constantGenerations, -1);
        Arrays.fill(constantTargetHashes, -1);
    }

    public final void initVariableReaders(int size) {
        variableReaders = new VariableAccessor[size];
        Arrays.fill(variableReaders, VariableAccessor.DUMMY_ACCESSOR);
    }

    public final void initVariableWriters(int size) {
        variableWriters = new VariableAccessor[size];
        Arrays.fill(variableWriters, VariableAccessor.DUMMY_ACCESSOR);
    }

    public final void initMethodCache(int size) {
        methodCache = new CacheEntry[size];
        Arrays.fill(methodCache, CacheEntry.NULL_CACHE);
    }

    public final IRubyObject getConstant(ThreadContext context, String name, int index) {
        IRubyObject value = getValue(context, name, index);
        // We can callsite cache const_missing if we want
        return value != null ? value : context.getCurrentScope().getStaticScope().getModule().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(name));
    }

    public IRubyObject getValue(ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream
        return isCached(context, value, index) ? value : reCache(context, name, index);
    }

    private boolean isCached(ThreadContext context, IRubyObject value, int index) {
        return value != null && constantGenerations[index] == context.getRuntime().getConstantGeneration();
    }

    public IRubyObject reCache(ThreadContext context, String name, int index) {
        int newGeneration = context.getRuntime().getConstantGeneration();
        IRubyObject value = context.getConstant(name);
        constants[index] = value;
        if (value != null) {
            constantGenerations[index] = newGeneration;
        }
        return value;
    }

    public final IRubyObject getConstantFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = getValueFrom(target, context, name, index);
        // We can callsite cache const_missing if we want
        return value != null ? value : target.fastGetConstantFromConstMissing(name);
    }

    public IRubyObject getValueFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream
        return isCachedFrom(target, context, value, index) ? value : reCacheFrom(target, context, name, index);
    }

    private boolean isCachedFrom(RubyModule target, ThreadContext context, IRubyObject value, int index) {
        return value != null && constantGenerations[index] == context.getRuntime().getConstantGeneration() && constantTargetHashes[index] == target.hashCode();
    }

    public IRubyObject reCacheFrom(RubyModule target, ThreadContext context, String name, int index) {
        int newGeneration = context.getRuntime().getConstantGeneration();
        IRubyObject value = target.fastGetConstantFromNoConstMissing(name);
        constants[index] = value;
        if (value != null) {
            constantGenerations[index] = newGeneration;
            constantTargetHashes[index] = target.hashCode();
        }
        return value;
    }

    private BlockBody createBlockBody(Object scriptObject, ThreadContext context, int index, String descriptor) throws NumberFormatException {
        String[] firstSplit = descriptor.split(",");
        String[] secondSplit;
        if (firstSplit[2].length() == 0) {
            secondSplit = new String[0];
        } else {
            secondSplit = firstSplit[2].split(";");
            // FIXME: Big fat hack here, because scope names are expected to be interned strings by the parser
            for (int i = 0; i < secondSplit.length; i++) {
                secondSplit[i] = secondSplit[i].intern();
            }
        }
        BlockBody body = RuntimeHelpers.createCompiledBlockBody(context, scriptObject, firstSplit[0], Integer.parseInt(firstSplit[1]), secondSplit, Boolean.valueOf(firstSplit[3]), Integer.parseInt(firstSplit[4]), Boolean.valueOf(firstSplit[5]));
        return blockBodies[index] = body;
    }

    private BlockBody createBlockBody19(Object scriptObject, ThreadContext context, int index, String descriptor) throws NumberFormatException {
        String[] firstSplit = descriptor.split(",");
        String[] secondSplit;
        if (firstSplit[2].length() == 0) {
            secondSplit = new String[0];
        } else {
            secondSplit = firstSplit[2].split(";");
            // FIXME: Big fat hack here, because scope names are expected to be interned strings by the parser
            for (int i = 0; i < secondSplit.length; i++) {
                secondSplit[i] = secondSplit[i].intern();
            }
        }
        BlockBody body = RuntimeHelpers.createCompiledBlockBody19(context, scriptObject, firstSplit[0], Integer.parseInt(firstSplit[1]), secondSplit, Boolean.valueOf(firstSplit[3]), Integer.parseInt(firstSplit[4]), Boolean.valueOf(firstSplit[5]));
        return blockBodies[index] = body;
    }

    private CompiledBlockCallback createCompiledBlockCallback(Object scriptObject, Ruby runtime, int index, String method) {
        CompiledBlockCallback callback = RuntimeHelpers.createBlockCallback(runtime, scriptObject, method);
        return blockCallbacks[index] = callback;
    }

    public DynamicMethod getMethod(ThreadContext context, IRubyObject self, int index, String methodName) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, selfType)) {
            return myCache.method;
        }
        return cacheAndGet(context, selfType, index, methodName);
    }

    private DynamicMethod cacheAndGet(ThreadContext context, RubyClass selfType, int index, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (method.isUndefined()) {
            return RuntimeHelpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, CallType.FUNCTIONAL);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return RuntimeHelpers.selectMethodMissing(clazz, method.getVisibility(), name1, CallType.FUNCTIONAL);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6, name7);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7, String name8) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6, name7, name8);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6, name7);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7, String name8) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6, name7, name8);
    }

    private static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    private CacheEntry getCacheEntry(int index) {
        return methodCache[index];
    }

    private static final StaticScope[] EMPTY_SCOPES = {};
    public StaticScope[] scopes = EMPTY_SCOPES;
    private static final CallSite[] EMPTY_CALLSITES = {};
    public CallSite[] callSites = EMPTY_CALLSITES;
    private static final CacheEntry[] EMPTY_CACHEENTRIES = {};
    public CacheEntry[] methodCache = EMPTY_CACHEENTRIES;
    private static final BlockBody[] EMPTY_BLOCKBODIES = {};
    public BlockBody[] blockBodies = EMPTY_BLOCKBODIES;
    private static final CompiledBlockCallback[] EMPTY_COMPILEDBLOCKCALLBACKS = {};
    public CompiledBlockCallback[] blockCallbacks = EMPTY_COMPILEDBLOCKCALLBACKS;
    private static final RubySymbol[] EMPTY_RUBYSYMBOLS = {};
    public RubySymbol[] symbols = EMPTY_RUBYSYMBOLS;
    private static final ByteList[] EMPTY_BYTELISTS = {};
    public ByteList[] byteLists = EMPTY_BYTELISTS;
    private static final RubyFixnum[] EMPTY_FIXNUMS = {};
    public RubyFixnum[] fixnums = EMPTY_FIXNUMS;
    private static final RubyRegexp[] EMPTY_RUBYREGEXPS = {};
    public RubyRegexp[] regexps = EMPTY_RUBYREGEXPS;
    private static final BigInteger[] EMPTY_BIGINTEGERS = {};
    public BigInteger[] bigIntegers = EMPTY_BIGINTEGERS;
    private static final VariableAccessor[] EMPTY_VARIABLE_ACCESSORS = {};
    public VariableAccessor[] variableReaders = EMPTY_VARIABLE_ACCESSORS;
    public VariableAccessor[] variableWriters = EMPTY_VARIABLE_ACCESSORS;
    public IRubyObject[] constants = IRubyObject.NULL_ARRAY;
    private static final int[] EMPTY_INTS = {};
    public int[] constantGenerations = EMPTY_INTS;
    public int[] constantTargetHashes = EMPTY_INTS;
}
