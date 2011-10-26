package org.jruby.util.cli;

import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;
import static org.jruby.util.cli.Properties.Category.*;

public class Properties {
    public static abstract class Property<T> {
        public Property(Category category, String name, Class<T> type, T[] options, T defval, String description) {
            this.category = category;
            this.name = name;
            this.type = type;
            this.options = options == null ? new String[]{type.getSimpleName()} : options;
            this.defval = defval;
            this.description = description;
        }
        
        public final Category category;
        public final String name;
        public final Class type;
        public final Object[] options;
        public final T defval;
        public final String description;
    }
    
    public enum Category {
        COMPILER("compiler"),
        INVOKEDYNAMIC("invokedynamic"),
        JIT("jit"),
        IR("intermediate representation"),
        NATIVE("native"),
        THREADPOOL("thread pooling"),
        MISCELLANEOUS("miscellaneous"),
        DEBUG("debugging and logging"),
        JAVA_INTEGRATION("java integration");
        
        private final String desc;
        
        Category(String desc) {
            this.desc = desc;
        }
        
        public String desc() {
            return desc;
        }
        
        public String toString() {
            return desc;
        }
    }
    
    public static class StringProperty extends Property<String> {
        public StringProperty(Category category, String name, String[] options, String defval, String description) {
            super(category, name, String.class, options, defval, description);
        }
        
        public String load() {
            return SafePropertyAccessor.getProperty("jruby." + name, defval);
        }
    }
    
    public static class BooleanProperty extends Property<Boolean> {
        public BooleanProperty(Category category, String name, Boolean defval, String description) {
            super(category, name, Boolean.class, new Boolean[] {true, false}, defval, description);
        }
        
        public Boolean load() {
            return SafePropertyAccessor.getBoolean("jruby." + name, defval);
        }
    }
    
    public static class IntegerProperty extends Property<Integer> {
        public IntegerProperty(Category category, String name, Integer defval, String description) {
            super(category, name, Integer.class, null, defval, description);
        }
        
        public Integer load() {
            return SafePropertyAccessor.getInt("jruby." + name, defval);
        }
    }
    
    public static final StringProperty COMPILE_MODE =
            new StringProperty(COMPILER, "compile.mode", new String[]{"JIT", "FORCE", "OFF"}, "JIT", "Set compilation mode. JIT = at runtime; FORCE = before execution.");
    public static final BooleanProperty COMPILE_DUMP =
            new BooleanProperty(COMPILER, "compile.dump", false, "Dump to console all bytecode generated at runtime.");
    public static final BooleanProperty COMPILE_THREADLESS =
            new BooleanProperty(COMPILER, "compile.threadless", false, "(EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events.");
    public static final BooleanProperty COMPILE_DYNOPT =
            new BooleanProperty(COMPILER, "compile.dynopt", false, "(EXPERIMENTAL) Use interpreter to help compiler make direct calls.");
    public static final BooleanProperty COMPILE_FASTOPS =
            new BooleanProperty(COMPILER, "compile.fastops", false, "Turn on fast operators for Fixnum and Float.");
    public static final IntegerProperty COMPILE_CHAINSIZE =
            new IntegerProperty(COMPILER, "compile.chainsize", Constants.CHAINED_COMPILE_LINE_COUNT_DEFAULT, "Set the number of lines at which compiled bodies are \"chained\".");
    public static final BooleanProperty COMPILE_LAZYHANDLES =
            new BooleanProperty(COMPILER, "compile.lazyHandles", false, "Generate method bindings (handles) for compiled methods lazily.");
    public static final BooleanProperty COMPILE_PEEPHOLE =
            new BooleanProperty(COMPILER, "compile.peephole", true, "Enable or disable peephole optimizations.");
    public static final BooleanProperty COMPILE_NOGUARDS =
            new BooleanProperty(COMPILER, "compile.noguards", false, "Compile calls without guards, for experimentation.");
    public static final BooleanProperty COMPILE_FASTEST =
            new BooleanProperty(COMPILER, "compile.fastest", false, "Compile with all \"mostly harmless\" compiler optimizations.");
    public static final BooleanProperty COMPILE_FASTSEND =
            new BooleanProperty(COMPILER, "compile.fastsend", false, "Compile obj.send(:sym, ...) as obj.sym(...).");
    public static final BooleanProperty COMPILE_INLINEDYNCALLS =
            new BooleanProperty(COMPILER, "compile.inlineDyncalls", false, "Emit method lookup + invoke inline in bytecode.");
    public static final BooleanProperty COMPILE_FASTMASGN =
            new BooleanProperty(COMPILER, "compile.fastMasgn", false, "Return true from multiple assignment instead of a new array.");
    public static final BooleanProperty COMPILE_INVOKEDYNAMIC =
            new BooleanProperty(COMPILER, "compile.invokedynamic", true, "Use invokedynamic on Java 7+.");
    
    public static final IntegerProperty INVOKEDYNAMIC_MAXFAIL =
            new IntegerProperty(INVOKEDYNAMIC, "invokedynamic.maxfail", 2, "Maximum size of invokedynamic PIC.");
    public static final BooleanProperty INVOKEDYNAMIC_LOG_BINDING =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.log.binding", false, "Log binding of invokedynamic call sites.");
    public static final BooleanProperty INVOKEDYNAMIC_LOG_CONSTANTS =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.log.constants", false, "Log invokedynamic-based constant lookups.");
    public static final BooleanProperty INVOKEDYNAMIC_ALL =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.all", false, "Enable all possible uses of invokedynamic.");
    public static final BooleanProperty INVOKEDYNAMIC_SAFE =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.safe", false, "Enable all safe (but maybe not fast) uses of invokedynamic.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation", true, "Enable invokedynamic for method invocations.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION_SWITCHPOINT =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation.switchpoint", true, "Use SwitchPoint for class modification guards on invocations.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION_INDIRECT =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation.indirect", true, "Also bind indirect method invokers to invokedynamic.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION_JAVA =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation.java", false, "Bind Ruby to Java invocations with invokedynamic.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION_ATTR =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation.attr", true, "Bind Ruby attribue invocations directly to invokedynamic.");
    public static final BooleanProperty INVOKEDYNAMIC_INVOCATION_FASTOPS =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.invocation.fastops", true, "Bind Fixnum and Float math using optimized logic.");
    public static final BooleanProperty INVOKEDYNAMIC_CACHE =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.cache", true, "Use invokedynamic to load cached values like literals and constants.");
    public static final BooleanProperty INVOKEDYNAMIC_CACHE_CONSTANTS =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.cache.constants", false, "Use invokedynamic to load constants.");
    public static final BooleanProperty INVOKEDYNAMIC_CACHE_LITERALS =
            new BooleanProperty(INVOKEDYNAMIC, "invokedynamic.cache.literals", true, "Use invokedynamic to load literals.");
    
    public static final IntegerProperty JIT_THRESHOLD =
            new IntegerProperty(JIT, "jit.threshold", Constants.JIT_THRESHOLD, "Set the JIT threshold to the specified method invocation count.");
    public static final IntegerProperty JIT_MAX =
            new IntegerProperty(JIT, "jit.max", Constants.JIT_MAX_METHODS_LIMIT, "Set the max count of active methods eligible for JIT-compilation.");
    public static final IntegerProperty JIT_MAXSIZE =
            new IntegerProperty(JIT, "jit.maxsize", Constants.JIT_MAX_SIZE_LIMIT, "Set the maximum full-class byte size allowed for jitted methods.");
    public static final BooleanProperty JIT_LOGGING =
            new BooleanProperty(JIT, "jit.logging", false, "Enable JIT logging (reports successful compilation).");
    public static final BooleanProperty JIT_LOGGING_VERBOSE =
            new BooleanProperty(JIT, "jit.logging.verbose", false, "Enable verbose JIT logging (reports failed compilation).");
    public static final BooleanProperty JIT_DUMPING =
            new BooleanProperty(JIT, "jit.dumping", false, "Enable stdout dumping of JITed bytecode.");
    public static final IntegerProperty JIT_LOGEVERY =
            new IntegerProperty(JIT, "jit.logEvery", 0, "Log a message every n methods JIT compiled.");
    public static final StringProperty JIT_EXCLUDE =
            new StringProperty(JIT, "jit.exclude", new String[]{"ClsOrMod","ClsOrMod::method_name","-::method_name"}, "none", "Exclude methods from JIT. Comma delimited.");
    public static final BooleanProperty JIT_CACHE =
            new BooleanProperty(JIT, "jit.cache", true, "Cache jitted method in-memory bodies across runtimes and loads.");
    public static final StringProperty JIT_CODECACHE =
            new StringProperty(JIT, "jit.codeCache", new String[]{"dir"}, null, "Save jitted methods to <dir> as they're compiled, for future runs.");
    public static final BooleanProperty JIT_DEBUG =
            new BooleanProperty(JIT, "jit.debug", false, "Log loading of JITed bytecode.");
    
    public static final BooleanProperty IR_DEBUG =
            new BooleanProperty(IR, "ir.debug", false, "Debug generation of JRuby IR.");
    public static final BooleanProperty IR_COMPILER_DEBUG =
            new BooleanProperty(IR, "ir.compiler.debug", false, "Debug compilation of JRuby IR.");
    public static final BooleanProperty IR_PASS_LIVEVARIABLE =
            new BooleanProperty(IR, "ir.pass.live_variable", false, "Enable live variable analysis of IR.");
    public static final BooleanProperty IR_PASS_DEADCODE =
            new BooleanProperty(IR, "ir.pass.dead_code", false, "Enable dead code elimination in IR.");
    public static final StringProperty IR_PASS_TESTINLINER =
            new StringProperty(IR, "ir.pass.test_inliner", null, "none", "Use specified class for inlining pass in IR.");
    
    public static final BooleanProperty NATIVE_ENABLED =
            new BooleanProperty(NATIVE, "native.enabled", true, "Enable/disable native code, including POSIX features and C exts.");
    public static final BooleanProperty NATIVE_VERBOSE =
            new BooleanProperty(NATIVE, "native.verbose", false, "Enable verbose logging of native extension loading.");
    public static final BooleanProperty CEXT_ENABLED =
            new BooleanProperty(NATIVE, "cext.enabled", true, "Enable or disable C extension support.");
    public static final BooleanProperty FFI_COMPILE_DUMP =
            new BooleanProperty(NATIVE, "ffi.compile.dump", false, "Dump bytecode-generated FFI stubs to console.");
    public static final IntegerProperty FFI_COMPILE_THRESHOLD =
            new IntegerProperty(NATIVE, "ffi.compile.threshold", 100, "Number of FFI invocations before generating a bytecode stub.");
    public static final BooleanProperty FFI_COMPILE_INVOKEDYNAMIC =
            new BooleanProperty(NATIVE, "ffi.compile.invokedynamic", false, "Use invokedynamic to bind FFI invocations.");
    
    public static final BooleanProperty THREADPOOL_ENABLED =
            new BooleanProperty(THREADPOOL, "thread.pool.enabled", false, "Enable reuse of native threads via a thread pool.");
    public static final IntegerProperty THREADPOOL_MIN =
            new IntegerProperty(THREADPOOL, "thread.pool.min", 0, "The minimum number of threads to keep alive in the pool.");
    public static final IntegerProperty THREADPOOL_MAX =
            new IntegerProperty(THREADPOOL, "thread.pool.max", Integer.MAX_VALUE, "The maximum number of threads to allow in the pool.");
    public static final IntegerProperty THREADPOOL_TTL =
            new IntegerProperty(THREADPOOL, "thread.pool.ttl", 60, "The maximum number of seconds to keep alive an idle thread.");
    
    public static final StringProperty COMPAT_VERSION =
            new StringProperty(MISCELLANEOUS, "compat.version", new String[]{"1.8","1.9"}, Constants.DEFAULT_RUBY_VERSION, "Specify the major Ruby version to be compatible with.");
    public static final BooleanProperty OBJECTSPACE_ENABLED =
            new BooleanProperty(MISCELLANEOUS, "objectspace.enabled", false, "Enable or disable ObjectSpace.each_object.");
    public static final BooleanProperty LAUNCH_INPROC =
            new BooleanProperty(MISCELLANEOUS, "launch.inproc", false, "Set in-process launching of e.g. system('ruby ...').");
    public static final StringProperty BYTECODE_VERSION =
            new StringProperty(MISCELLANEOUS, "bytecode.version", new String[]{"1.5","1.6","1.7"},
            SafePropertyAccessor.getProperty("java.specification.version", "1.5"), "Specify the major Ruby version to be compatible with.");
    public static final BooleanProperty MANAGEMENT_ENABLED =
            new BooleanProperty(MISCELLANEOUS, "management.enabled", false, "Set whether JMX management is enabled.");
    public static final BooleanProperty JUMP_BACKTRACE =
            new BooleanProperty(MISCELLANEOUS, "jump.backtrace", false, "Make non-local flow jumps generate backtraces.");
    public static final BooleanProperty PROCESS_NOUNWRAP =
            new BooleanProperty(MISCELLANEOUS, "process.noUnwrap", false, "Do not unwrap process streams (issue on some recent JVMs).");
    public static final BooleanProperty REIFY_CLASSES =
            new BooleanProperty(MISCELLANEOUS, "reify.classes", false, "Before instantiation, stand up a real Java class for ever Ruby class.");
    public static final BooleanProperty REIFY_LOGERRORS =
            new BooleanProperty(MISCELLANEOUS, "reify.logErrors", false, "Log errors during reification (reify.classes=true).");
    public static final BooleanProperty REFLECTED_HANDLES =
            new BooleanProperty(MISCELLANEOUS, "reflected.handles", false, "Use reflection for binding methods, not generated bytecode.");
    public static final BooleanProperty BACKTRACE_COLOR =
            new BooleanProperty(MISCELLANEOUS, "backtrace.color", false, "Enable colorized backtraces.");
    public static final StringProperty BACKTRACE_STYLE =
            new StringProperty(MISCELLANEOUS, "backtrace.style", new String[]{"normal","raw","full","mri","rubinius"}, "normal", "Set the style of exception backtraces.");
    public static final StringProperty THREAD_DUMP_SIGNAL =
            new StringProperty(MISCELLANEOUS, "thread.dump.signal", new String[]{"USR1", "USR2", "etc"}, "USR2", "Set the signal used for dumping thread stacks.");
    public static final BooleanProperty NATIVE_NET_PROTOCOL =
            new BooleanProperty(MISCELLANEOUS, "native.net.protocol", false, "Use native impls for parts of net/protocol.");
    public static final BooleanProperty FIBER_COROUTINES =
            new BooleanProperty(MISCELLANEOUS, "fiber.coroutines", false, "Use JVM coroutines for Fiber.");
    
    public static final BooleanProperty DEBUG_LOADSERVICE =
            new BooleanProperty(DEBUG, "debug.loadService", false, "Log require/load file searches.");
    public static final BooleanProperty DEBUG_LOADSERVICE_TIMING =
            new BooleanProperty(DEBUG, "debug.loadService.timing", false, "Log require/load parse+evaluate times.");
    public static final BooleanProperty DEBUG_LAUNCH =
            new BooleanProperty(DEBUG, "debug.launch", false, "Log externally-launched processes.");
    public static final BooleanProperty DEBUG_FULLTRACE =
            new BooleanProperty(DEBUG, "debug.fullTrace", false, "Set whether full traces are enabled (c-call/c-return).");
    public static final BooleanProperty DEBUG_SCRIPTRESOLUTION =
            new BooleanProperty(DEBUG, "debug.scriptResolution", false, "Print which script is executed by '-S' flag.");
    public static final BooleanProperty ERRNO_BACKTRACE =
            new BooleanProperty(DEBUG, "errno.backtrace", false, "Generate backtraces for heavily-used Errno exceptions (EAGAIN).");
    public static final BooleanProperty LOG_EXCEPTIONS =
            new BooleanProperty(DEBUG, "log.exceptions", false, "Log every time an exception is constructed.");
    public static final BooleanProperty LOG_BACKTRACES =
            new BooleanProperty(DEBUG, "log.backtraces", false, "Log every time an exception backtrace is generated.");
    public static final BooleanProperty LOG_CALLERS =
            new BooleanProperty(DEBUG, "log.callers", false, "Log every time a Kernel#caller backtrace is generated.");
    public static final StringProperty LOGGER_CLASS =
            new StringProperty(DEBUG, "logger.class", new String[] {"class name"}, "org.jruby.util.log.JavaUtilLoggingLogger", "Use specified class for logging.");
    
    public static final BooleanProperty JI_SETACCESSIBLE =
            new BooleanProperty(JAVA_INTEGRATION, "ji.setAccessible", true, "Try to set inaccessible Java methods to be accessible.");
    public static final BooleanProperty JI_LOGCANSETACCESSIBLE =
            new BooleanProperty(JAVA_INTEGRATION, "ji.logCanSetAccessible", false, "Log whether setAccessible is working.");
    public static final BooleanProperty JI_UPPER_CASE_PACKAGE_NAME_ALLOWED =
            new BooleanProperty(JAVA_INTEGRATION, "ji.upper.case.package.name.allowed", false, "Allow Capitalized Java pacakge names.");
    public static final BooleanProperty INTERFACES_USEPROXY =
            new BooleanProperty(JAVA_INTEGRATION, "interfaces.useProxy", false, "Use java.lang.reflect.Proxy for interface impl.");
    public static final BooleanProperty JAVA_HANDLES =
            new BooleanProperty(JAVA_INTEGRATION, "java.handles", false, "Use generated handles instead of reflection for calling Java.");
    public static final BooleanProperty JI_NEWSTYLEEXTENSION =
            new BooleanProperty(JAVA_INTEGRATION, "ji.newStyleExtension", false, "Extend Java classes without using a proxy object.");
    public static final BooleanProperty JI_OBJECTPROXYCACHE =
            new BooleanProperty(JAVA_INTEGRATION, "ji.objectProxyCache", true, "Cache Java object wrappers between calls.");

    public static final Property[] PROPERTIES = {
            COMPILE_MODE, COMPILE_DUMP, COMPILE_THREADLESS, COMPILE_DYNOPT, COMPILE_FASTOPS,
            COMPILE_CHAINSIZE, COMPILE_LAZYHANDLES, COMPILE_PEEPHOLE, COMPILE_NOGUARDS,
            COMPILE_FASTEST, COMPILE_FASTSEND, COMPILE_INLINEDYNCALLS, COMPILE_FASTMASGN,
            COMPILE_INVOKEDYNAMIC,
            
            INVOKEDYNAMIC_MAXFAIL, INVOKEDYNAMIC_LOG_BINDING, INVOKEDYNAMIC_LOG_CONSTANTS,
            INVOKEDYNAMIC_ALL, INVOKEDYNAMIC_SAFE, INVOKEDYNAMIC_INVOCATION, INVOKEDYNAMIC_INVOCATION_SWITCHPOINT,
            INVOKEDYNAMIC_INVOCATION_INDIRECT, INVOKEDYNAMIC_INVOCATION_JAVA, INVOKEDYNAMIC_INVOCATION_ATTR,
            INVOKEDYNAMIC_INVOCATION_FASTOPS, INVOKEDYNAMIC_CACHE, INVOKEDYNAMIC_CACHE_CONSTANTS,
            INVOKEDYNAMIC_CACHE_LITERALS,
            
            JIT_THRESHOLD, JIT_MAX, JIT_MAXSIZE, JIT_LOGGING, JIT_LOGGING_VERBOSE,
            JIT_DUMPING, JIT_LOGEVERY, JIT_EXCLUDE, JIT_CACHE, JIT_CODECACHE,
            JIT_DEBUG,
            
            IR_DEBUG, IR_COMPILER_DEBUG, IR_PASS_LIVEVARIABLE, IR_PASS_DEADCODE, IR_PASS_TESTINLINER,
            
            NATIVE_ENABLED, NATIVE_VERBOSE, CEXT_ENABLED, FFI_COMPILE_DUMP, FFI_COMPILE_THRESHOLD, FFI_COMPILE_INVOKEDYNAMIC,
            
            THREADPOOL_ENABLED, THREADPOOL_MIN, THREADPOOL_MAX, THREADPOOL_TTL,
            
            COMPAT_VERSION, OBJECTSPACE_ENABLED, LAUNCH_INPROC, BYTECODE_VERSION,
            MANAGEMENT_ENABLED, JUMP_BACKTRACE, PROCESS_NOUNWRAP, REIFY_CLASSES,
            REIFY_LOGERRORS, REFLECTED_HANDLES, BACKTRACE_COLOR, BACKTRACE_STYLE,
            THREAD_DUMP_SIGNAL, NATIVE_NET_PROTOCOL, FIBER_COROUTINES,
            
            DEBUG_LOADSERVICE, DEBUG_LOADSERVICE_TIMING, DEBUG_LAUNCH, DEBUG_FULLTRACE,
            DEBUG_SCRIPTRESOLUTION, ERRNO_BACKTRACE, LOG_EXCEPTIONS, LOG_BACKTRACES,
            LOG_CALLERS, LOGGER_CLASS,
            
            JI_SETACCESSIBLE, JI_UPPER_CASE_PACKAGE_NAME_ALLOWED, INTERFACES_USEPROXY,
            JAVA_HANDLES, JI_NEWSTYLEEXTENSION, JI_OBJECTPROXYCACHE
    };    
}
