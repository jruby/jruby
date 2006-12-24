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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.AccessControlException;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import org.jruby.ast.Node;
import org.jruby.compiler.InstructionCompiler2;
import org.jruby.ast.executable.Script;
import org.jruby.common.RubyWarnings;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaSupport;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.libraries.IConvLibrary;
import org.jruby.libraries.JRubyLibrary;
import org.jruby.libraries.RbConfigLibrary;
import org.jruby.libraries.SocketLibrary;
import org.jruby.libraries.StringIOLibrary;
import org.jruby.libraries.StringScannerLibrary;
import org.jruby.libraries.ZlibLibrary;
import org.jruby.libraries.YamlLibrary;
import org.jruby.libraries.EnumeratorLibrary;
import org.jruby.libraries.BigDecimalLibrary;
import org.jruby.ext.openssl.RubyOpenSSL;
import org.jruby.libraries.DigestLibrary;
import org.jruby.ext.Generator;
import org.jruby.ext.Readline;
import org.jruby.parser.Parser;
import org.jruby.runtime.Block;
import org.jruby.runtime.CacheMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.ArrayMetaClass;
import org.jruby.runtime.builtin.meta.BignumMetaClass;
import org.jruby.runtime.builtin.meta.BindingMetaClass;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.builtin.meta.FixnumMetaClass;
import org.jruby.runtime.builtin.meta.HashMetaClass;
import org.jruby.runtime.builtin.meta.IOMetaClass;
import org.jruby.runtime.builtin.meta.IntegerMetaClass;
import org.jruby.runtime.builtin.meta.ModuleMetaClass;
import org.jruby.runtime.builtin.meta.NumericMetaClass;
import org.jruby.runtime.builtin.meta.ObjectMetaClass;
import org.jruby.runtime.builtin.meta.ProcMetaClass;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.builtin.meta.SymbolMetaClass;
import org.jruby.runtime.builtin.meta.TimeMetaClass;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.BuiltinScript;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JRubyFile;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * The jruby runtime.
 */
public final class Ruby implements IRuby {
	private static String[] BUILTIN_LIBRARIES = {"fcntl", "yaml", "etc", "nkf" };
    
	private CacheMap cacheMap = new CacheMap();
    private ThreadService threadService = new ThreadService(this);

    private int stackTraces = 0;

    private ObjectSpace objectSpace = new ObjectSpace();

    private final RubyFixnum[] fixnumCache = new RubyFixnum[256];
    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable();
    private Hashtable ioHandlers = new Hashtable();
    private long randomSeed = 0;
    private long randomSeedSequence = 0;
    private Random random = new Random();

    private RubyProc traceFunction;
    private boolean globalAbortOnExceptionEnabled = false;
    private boolean doNotReverseLookupEnabled = false;
    private final boolean objectSpaceEnabled;
    
    /**
     * What encoding should we read source files in as...
     * @see org.jruby.util.CommandlineParser#processArgument()
     */
    private String encoding = "ISO8859_1";

    /** safe-level:
    		0 - strings from streams/environment/ARGV are tainted (default)
    		1 - no dangerous operation by tainted value
    		2 - process/file operations prohibited
    		3 - all genetated objects are tainted
    		4 - no global (non-tainted) variable modification/no direct output
    */
    private int safeLevel = 0;

    // Default classes/objects
    private IRubyObject nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    private RubyClass objectClass;
    private StringMetaClass stringClass;
    private RubyClass systemCallError = null;
    private RubyModule errnoModule = null;
    private IRubyObject topSelf;
    
    // former java.lang.System concepts now internalized for MVM
    private String currentDirectory;
    
    private long startTime = System.currentTimeMillis();
    
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    
    private IRubyObject verbose;
    private IRubyObject debug;

    // Java support
    private JavaSupport javaSupport;

    private Parser parser = new Parser(this);

    private LoadService loadService;
    private GlobalVariables globalVariables = new GlobalVariables(this);
    private RubyWarnings warnings = new RubyWarnings(this);

    // Contains a list of all blocks (as Procs) that should be called when
    // the runtime environment exits.
    private Stack atExitBlocks = new Stack();

    private RubyModule kernelModule;

    private RubyClass nilClass;

    private FixnumMetaClass fixnumClass;
    
    private IRubyObject tmsStruct;

    /**
     * Create and initialize a new jruby Runtime.
     */
    private Ruby(InputStream in, PrintStream out, PrintStream err) {
        this.in = in;
        this.out = out;
        this.err = err;
        
        objectSpaceEnabled = true;
        
        try {
            currentDirectory = JRubyFile.getFileProperty("user.dir");
        } catch (AccessControlException accessEx) {
            // default to "/" as current dir for applets (which can't read from FS anyway)
            currentDirectory = "/";
        }
    }

    /**
     * Create and initialize a new jruby Runtime.
     */
    private Ruby(InputStream in, PrintStream out, PrintStream err, boolean osEnabled) {
        this.in = in;
        this.out = out;
        this.err = err;
        
        objectSpaceEnabled = osEnabled;
        
        try {
            currentDirectory = JRubyFile.getFileProperty("user.dir");
        } catch (AccessControlException accessEx) {
            // default to "/" as current dir for applets (which can't read from FS anyway)
            currentDirectory = "/";
        }
    }

    /**
     * Returns a default instance of the JRuby runtime.
     *
     * @return the JRuby runtime
     */
    public static IRuby getDefaultInstance() {
        Ruby ruby;
        if(System.getProperty("jruby.objectspace.enabled") != null) {
            ruby = new Ruby(System.in, System.out, System.err, Boolean.getBoolean("jruby.objectspace.enabled"));
        } else {
            ruby = new Ruby(System.in, System.out, System.err);
        }

        ruby.init();
        
        return ruby;
    }

    /**
     * Returns a default instance of the JRuby runtime.
     *
     * @return the JRuby runtime
     */
    public static IRuby newInstance(InputStream in, PrintStream out, PrintStream err, boolean osEnabled) {
        Ruby ruby = new Ruby(in, out, err, osEnabled);
        ruby.init();
        
        return ruby;
    }

    /**
     * Returns a default instance of the JRuby runtime.
     *
     * @return the JRuby runtime
     */
    public static IRuby newInstance(InputStream in, PrintStream out, PrintStream err) {
        return newInstance(in, out, err, true);
    }

    /**
     * Evaluates a script and returns a RubyObject.
     */
    public IRubyObject evalScript(String script) {
        return eval(parse(script, "<script>", getCurrentContext().getCurrentScope()));
    }

    public IRubyObject eval(Node node) {
        try {
            ThreadContext tc = getCurrentContext();
            
            return EvaluationState.eval(tc, node, tc.getFrameSelf());
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
	            return (IRubyObject)je.getSecondaryData();
        	} 

            throw je;
		}
    }

    public IRubyObject compileAndRun(Node node) {
        try {
            ThreadContext tc = getCurrentContext();
            ISourcePosition position = node.getPosition();
            InstructionCompiler2 compiler = new InstructionCompiler2();
            String classname = null;
            
            if (position != null) {
                classname = node.getPosition().getFile();
                if (classname.endsWith(".rb")) {
                    classname = classname.substring(0, classname.length() - 3);
                }
                compiler.compile(classname, position.getFile(), node);
            } else {
                classname = "EVAL";
                compiler.compile(classname, "EVAL", node);
            }
            
            JRubyClassLoader loader = new JRubyClassLoader();
            Class scriptClass = compiler.loadClasses(loader);
            
            Script script = (Script)scriptClass.newInstance();
            
            return script.run(tc, tc.getFrameSelf());
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                return (IRubyObject)je.getSecondaryData();
            } else {
                throw je;
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public RubyClass getObject() {
    	return objectClass;
    }
    
    public RubyModule getKernel() {
        return kernelModule;
    }
    
    public RubyClass getString() {
        return stringClass;
    }
    
    public RubyClass getFixnum() {
        return fixnumClass;
    }
    
    public IRubyObject getTmsStruct() {
        return tmsStruct;
    }

    /** Returns the "true" instance from the instance pool.
     * @return The "true" instance.
     */
    public RubyBoolean getTrue() {
        return trueObject;
    }

    /** Returns the "false" instance from the instance pool.
     * @return The "false" instance.
     */
    public RubyBoolean getFalse() {
        return falseObject;
    }

    /** Returns the "nil" singleton instance.
     * @return "nil"
     */
    public IRubyObject getNil() {
        return nilObject;
    }
    
    public RubyClass getNilClass() {
        return nilClass;
    }

    public RubyModule getModule(String name) {
        return (RubyModule) objectClass.getConstantAt(name);
    }

    /** Returns a class from the instance pool.
     *
     * @param name The name of the class.
     * @return The class.
     */
    public RubyClass getClass(String name) {
        try {
            return objectClass.getClass(name);
        } catch (ClassCastException e) {
            throw newTypeError(name + " is not a Class");
        }
    }

    /** Define a new class with name 'name' and super class 'superClass'.
     *
     * MRI: rb_define_class / rb_define_class_id
     *
     */
    public RubyClass defineClass(String name, RubyClass superClass) {
        return defineClassUnder(name, superClass, objectClass.getCRef());
    }
    
    public RubyClass defineClassUnder(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        if (superClass == null) {
            superClass = objectClass;
        }

        return superClass.newSubClass(name, parentCRef);
    }
    
    /** rb_define_module / rb_define_module_id
     *
     */
    public RubyModule defineModule(String name) {
        return defineModuleUnder(name, objectClass.getCRef());
    }
    
    public RubyModule defineModuleUnder(String name, SinglyLinkedList parentCRef) {
        RubyModule newModule = RubyModule.newModule(this, name, parentCRef);

        ((RubyModule)parentCRef.getValue()).setConstant(name, newModule);
        
        return newModule;
    }
    
    /**
     * In the current context, get the named module. If it doesn't exist a
     * new module is created.
     */
    public RubyModule getOrCreateModule(String name) {
        ThreadContext tc = getCurrentContext();
        RubyModule module = (RubyModule) tc.getRubyClass().getConstantAt(name);
        
        if (module == null) {
            module = (RubyModule) tc.getRubyClass().setConstant(name, 
            		defineModule(name)); 
        } else if (getSafeLevel() >= 4) {
        	throw newSecurityError("Extending module prohibited.");
        }

        if (tc.getWrapper() != null) {
            module.getSingletonClass().includeModule(tc.getWrapper());
            module.includeModule(tc.getWrapper());
        }
        return module;
    }
    

    /** Getter for property securityLevel.
     * @return Value of property securityLevel.
     */
    public int getSafeLevel() {
        return this.safeLevel;
    }

    /** Setter for property securityLevel.
     * @param safeLevel New value of property securityLevel.
     */
    public void setSafeLevel(int safeLevel) {
        this.safeLevel = safeLevel;
    }

    public void secure(int level) {
        if (level <= safeLevel) {
            throw newSecurityError("Insecure operation '" + getCurrentContext().getFrameLastFunc() + "' at level " + safeLevel);
        }
    }
    
    /**
     * Retrieve mappings of cached methods to where they have been cached.  When a cached
     * method needs to be invalidated this map can be used to remove all places it has been
     * cached.
     * 
     * @return the mappings of where cached methods have been stored
     */
    public CacheMap getCacheMap() {
        return cacheMap;
    }

    /** rb_define_global_const
     *
     */
    public void defineGlobalConstant(String name, IRubyObject value) {
        objectClass.defineConstant(name, value);
    }

    public IRubyObject getTopConstant(String name) {
        IRubyObject constant = getModule(name);
        if (constant == null) {
            constant = getLoadService().autoload(name);
        }
        return constant;
    }

    public boolean isClassDefined(String name) {
        return getModule(name) != null;
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public IRubyObject getTopSelf() {
        return topSelf;
    }
    
    public void setCurrentDirectory(String dir) {
        currentDirectory = dir;
    }
    
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /** ruby_init
     *
     */
    // TODO: Figure out real dependencies between vars and reorder/refactor into better methods
    private void init() {
        ThreadContext tc = getCurrentContext();
        nilObject = new RubyNil(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);

        verbose = falseObject;
        debug = falseObject;
        
        javaSupport = new JavaSupport(this);
        
        initLibraries();
        
        tc.preInitCoreClasses();

        initCoreClasses();

        topSelf = TopSelfFactory.createTopSelf(this);

        tc.preInitBuiltinClasses(objectClass, topSelf);

        RubyGlobal.createGlobals(this);

        initBuiltinClasses();
        
        getObject().defineConstant("TOPLEVEL_BINDING", newBinding());
        
        // Load additional definitions and hacks from etc.rb
        getLoadService().smartLoad("builtin/etc.rb");
    }

    private void initLibraries() {
        loadService = new LoadService(this);
        loadService.registerBuiltin("java.rb", new Library() {
                public void load(IRuby runtime) throws IOException {
                    Java.createJavaModule(runtime);
                    new BuiltinScript("javasupport").load(runtime);
                }
            });
        loadService.registerBuiltin("socket.rb", new SocketLibrary());
        loadService.registerBuiltin("rbconfig.rb", new RbConfigLibrary());

        for (int i=0; i<BUILTIN_LIBRARIES.length; i++) {
        	loadService.registerRubyBuiltin(BUILTIN_LIBRARIES[i]);
        }
        
        
        loadService.registerBuiltin("jruby.rb", new JRubyLibrary());
        loadService.registerBuiltin("iconv.rb", new IConvLibrary());
        loadService.registerBuiltin("stringio.rb", new StringIOLibrary());
        loadService.registerBuiltin("strscan.rb", new StringScannerLibrary());
        loadService.registerBuiltin("zlib.rb", new ZlibLibrary());
        loadService.registerBuiltin("yaml_internal.rb", new YamlLibrary());
        loadService.registerBuiltin("enumerator.rb", new EnumeratorLibrary());
        loadService.registerBuiltin("generator_internal.rb", new Generator.Service());
        loadService.registerBuiltin("readline.rb", new Readline.Service());
        loadService.registerBuiltin("openssl.so", new RubyOpenSSL.Service());
        loadService.registerBuiltin("digest.so", new DigestLibrary());
        loadService.registerBuiltin("digest.rb", new DigestLibrary());
        loadService.registerBuiltin("digest/md5.rb", new DigestLibrary.MD5());
        loadService.registerBuiltin("digest/rmd160.rb", new DigestLibrary.RMD160());
        loadService.registerBuiltin("digest/sha1.rb", new DigestLibrary.SHA1());
        loadService.registerBuiltin("digest/sha2.rb", new DigestLibrary.SHA2());
        loadService.registerBuiltin("bigdecimal.rb", new BigDecimalLibrary());
    }

    private void initCoreClasses() {
        ObjectMetaClass objectMetaClass = new ObjectMetaClass(this);
        objectMetaClass.initializeClass();
        
        objectClass = objectMetaClass;
        objectClass.setConstant("Object", objectClass);
        RubyClass moduleClass = new ModuleMetaClass(this, objectClass);
        objectClass.setConstant("Module", moduleClass);
        RubyClass classClass = new RubyClass(this, null /* Would be Class if it could */, moduleClass, null, "Class");
        objectClass.setConstant("Class", classClass);

        // I don't think the containment is correct here (parent cref)
        RubyClass metaClass = objectClass.makeMetaClass(classClass, objectMetaClass.getCRef());
        metaClass = moduleClass.makeMetaClass(metaClass, objectMetaClass.getCRef());
        metaClass = classClass.makeMetaClass(metaClass, objectMetaClass.getCRef());

        ((ObjectMetaClass) moduleClass).initializeBootstrapClass();
        
        kernelModule = RubyKernel.createKernelModule(this);
        objectClass.includeModule(kernelModule);

        RubyClass.createClassClass(classClass);

        nilClass = RubyNil.createNilClass(this);

        // We cannot define this constant until nil itself was made
        objectClass.defineConstant("NIL", getNil());
        
        // Pre-create the core classes we know we will get referenced by starting up the runtime.
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);
        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        stringClass = new StringMetaClass(this);
        stringClass.initializeClass();
        new SymbolMetaClass(this).initializeClass();
        RubyThreadGroup.createThreadGroupClass(this);
        RubyThread.createThreadClass(this);
        RubyException.createExceptionClass(this);
        
        RubyPrecision.createPrecisionModule(this);
        new NumericMetaClass(this).initializeClass();
        new IntegerMetaClass(this).initializeClass();        
        fixnumClass = new FixnumMetaClass(this);
        fixnumClass.initializeClass();
        new HashMetaClass(this).initializeClass();
        new IOMetaClass(this).initializeClass();
        new ArrayMetaClass(this).initializeClass();
        
        RubyClass structClass = RubyStruct.createStructClass(this);
        
        tmsStruct = RubyStruct.newInstance(structClass,
                new IRubyObject[] {
            newString("Tms"),
            newSymbol("utime"),
            newSymbol("stime"),
            newSymbol("cutime"),
            newSymbol("cstime")});
        
        RubyFloat.createFloatClass(this);
        
        new BignumMetaClass(this).initializeClass();
        new BindingMetaClass(this).initializeClass();
        
        RubyMath.createMathModule(this); // depends on all numeric types
        RubyRegexp.createRegexpClass(this);
        RubyRange.createRangeClass(this);
        RubyObjectSpace.createObjectSpaceModule(this);
        RubyGC.createGCModule(this);
        
        new ProcMetaClass(this).initializeClass();
        
        RubyMethod.createMethodClass(this);
        RubyMatchData.createMatchDataClass(this);
        RubyMarshal.createMarshalModule(this);
        RubyDir.createDirClass(this);
        RubyFileTest.createFileTestModule(this);
        
        new FileMetaClass(this).initializeClass(); // depends on IO, FileTest
        
        RubyProcess.createProcessModule(this);
        new TimeMetaClass(this).initializeClass();
        RubyUnboundMethod.defineUnboundMethodClass(this);
        RubyClass exceptionClass = getClass("Exception");
        RubyClass standardError = defineClass("StandardError", exceptionClass);
        RubyClass runtimeError = defineClass("RuntimeError", standardError);
        RubyClass ioError = defineClass("IOError", standardError);
        RubyClass scriptError = defineClass("ScriptError", exceptionClass);
        RubyClass nameError = RubyNameError.createNameErrorClass(this, standardError);
        RubyClass rangeError = defineClass("RangeError", standardError);
        defineClass("SystemExit", exceptionClass);
        defineClass("Fatal", exceptionClass);
        defineClass("Interrupt", exceptionClass);
        defineClass("SignalException", exceptionClass);
        defineClass("TypeError", standardError);
        defineClass("ArgumentError", standardError);
        defineClass("IndexError", standardError);
        defineClass("SyntaxError", scriptError);
        defineClass("LoadError", scriptError);
        defineClass("NotImplementedError", scriptError);
        defineClass("NoMethodError", nameError);
        defineClass("SecurityError", standardError);
        defineClass("NoMemoryError", exceptionClass);
        defineClass("RegexpError", standardError);
        defineClass("EOFError", ioError);
        defineClass("LocalJumpError", standardError);
        defineClass("ThreadError", standardError);
        defineClass("SystemStackError", exceptionClass);
        defineClass("ZeroDivisionError", standardError);
        // FIXME: Actually this somewhere
        defineClass("FloatDomainError", rangeError);
        NativeException.createClass(this, runtimeError);
        systemCallError = defineClass("SystemCallError", standardError);
        errnoModule = defineModule("Errno");
       
        initErrnoErrors();

        defineClass("Data",objectClass);
    }

    private void initBuiltinClasses() {
    	try {
	        new BuiltinScript("FalseClass").load(this);
	        new BuiltinScript("TrueClass").load(this);
    	} catch (IOException e) {
    		throw new Error("builtin scripts are missing", e);
    	}
    }
    
    /**
     * Create module Errno's Variables.  We have this method since Errno does not have it's 
     * own java class.
     */
    private void initErrnoErrors() {
        createSysErr(IErrno.ENOTEMPTY, "ENOTEMPTY");   
        createSysErr(IErrno.ERANGE, "ERANGE");      
        createSysErr(IErrno.ESPIPE, "ESPIPE");      
        createSysErr(IErrno.ENFILE, "ENFILE");      
        createSysErr(IErrno.EXDEV, "EXDEV");       
        createSysErr(IErrno.ENOMEM, "ENOMEM");      
        createSysErr(IErrno.E2BIG, "E2BIG");       
        createSysErr(IErrno.ENOENT, "ENOENT");      
        createSysErr(IErrno.ENOSYS, "ENOSYS");      
        createSysErr(IErrno.EDOM, "EDOM");        
        createSysErr(IErrno.ENOSPC, "ENOSPC");      
        createSysErr(IErrno.EINVAL, "EINVAL");      
        createSysErr(IErrno.EEXIST, "EEXIST");      
        createSysErr(IErrno.EAGAIN, "EAGAIN");      
        createSysErr(IErrno.ENXIO, "ENXIO");       
        createSysErr(IErrno.EILSEQ, "EILSEQ");      
        createSysErr(IErrno.ENOLCK, "ENOLCK");      
        createSysErr(IErrno.EPIPE, "EPIPE");       
        createSysErr(IErrno.EFBIG, "EFBIG");       
        createSysErr(IErrno.EISDIR, "EISDIR");      
        createSysErr(IErrno.EBUSY, "EBUSY");       
        createSysErr(IErrno.ECHILD, "ECHILD");      
        createSysErr(IErrno.EIO, "EIO");         
        createSysErr(IErrno.EPERM, "EPERM");       
        createSysErr(IErrno.EDEADLOCK, "EDEADLOCK");   
        createSysErr(IErrno.ENAMETOOLONG, "ENAMETOOLONG");
        createSysErr(IErrno.EMLINK, "EMLINK");      
        createSysErr(IErrno.ENOTTY, "ENOTTY");      
        createSysErr(IErrno.ENOTDIR, "ENOTDIR");     
        createSysErr(IErrno.EFAULT, "EFAULT");      
        createSysErr(IErrno.EBADF, "EBADF");       
        createSysErr(IErrno.EINTR, "EINTR");       
        createSysErr(IErrno.EWOULDBLOCK, "EWOULDBLOCK"); 
        createSysErr(IErrno.EDEADLK, "EDEADLK");     
        createSysErr(IErrno.EROFS, "EROFS");       
        createSysErr(IErrno.EMFILE, "EMFILE");      
        createSysErr(IErrno.ENODEV, "ENODEV");      
        createSysErr(IErrno.EACCES, "EACCES");      
        createSysErr(IErrno.ENOEXEC, "ENOEXEC");             
        createSysErr(IErrno.ESRCH, "ESRCH");       
        createSysErr(IErrno.ECONNREFUSED, "ECONNREFUSED");
        createSysErr(IErrno.ECONNRESET, "ECONNRESET");
        createSysErr(IErrno.EADDRINUSE, "EADDRINUSE");
    }

    /**
     * Creates a system error.
     * @param i the error code (will probably use a java exception instead)
     * @param name of the error to define.
     **/
    private void createSysErr(int i, String name) {
        errnoModule.defineClassUnder(name, systemCallError).defineConstant("Errno", newFixnum(i));
    }
    
	/** Getter for property isVerbose.
     * @return Value of property isVerbose.
     */
    public IRubyObject getVerbose() {
        return verbose;
    }

    /** Setter for property isVerbose.
     * @param verbose New value of property isVerbose.
     */
    public void setVerbose(IRubyObject verbose) {
        this.verbose = verbose;
    }

	/** Getter for property isDebug.
     * @return Value of property isDebug.
     */
    public IRubyObject getDebug() {
        return debug;
    }

    /** Setter for property isDebug.
     * @param debug New value of property isDebug.
     */
    public void setDebug(IRubyObject debug) {
        this.debug = debug;
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
    }

    /** Defines a global variable
     */
    public void defineVariable(final GlobalVariable variable) {
        globalVariables.define(variable.name(), new IAccessor() {
            public IRubyObject getValue() {
                return variable.get();
            }

            public IRubyObject setValue(IRubyObject newValue) {
                return variable.set(newValue);
            }
        });
    }

    /** defines a readonly global variable
     *
     */
    public void defineReadonlyVariable(String name, IRubyObject value) {
        globalVariables.defineReadonly(name, new ValueAccessor(value));
    }

    public Node parse(Reader content, String file, DynamicScope scope) {
        return parser.parse(file, content, scope);
    }

    public Node parse(String content, String file, DynamicScope scope) {
        return parser.parse(file, content, scope);
    }

    public ThreadService getThreadService() {
        return threadService;
    }

    public ThreadContext getCurrentContext() {
        return threadService.getCurrentContext();
    }

    /**
     * Returns the loadService.
     * @return ILoadService
     */
    public LoadService getLoadService() {
        return loadService;
    }

    public RubyWarnings getWarnings() {
        return warnings;
    }

    public PrintStream getErrorStream() {
        java.io.OutputStream os = ((RubyIO) getGlobalVariables().get("$stderr")).getOutStream();
        if(null != os) {
            return new PrintStream(os);
        } else {
            return new PrintStream(new org.jruby.util.SwallowingOutputStream());
        }
    }

    public InputStream getInputStream() {
        return ((RubyIO) getGlobalVariables().get("$stdin")).getInStream();
    }

    public PrintStream getOutputStream() {
        return new PrintStream(((RubyIO) getGlobalVariables().get("$stdout")).getOutStream());
    }

    public RubyModule getClassFromPath(String path) {
        if (path.charAt(0) == '#') {
            throw newArgumentError("can't retrieve anonymous class " + path);
        }
        IRubyObject type = evalScript(path);
        if (!(type instanceof RubyModule)) {
            throw newTypeError("class path " + path + " does not point class");
        }
        return (RubyModule) type;
    }
    
    /** Prints an error with backtrace to the error stream.
     *
     * MRI: eval.c - error_print()
     *
     */
    public void printError(RubyException excp) {
		if (excp == null || excp.isNil()) {
            return;
        }

        ThreadContext tc = getCurrentContext();
        IRubyObject backtrace = excp.callMethod(tc, "backtrace");

        PrintStream errorStream = getErrorStream();
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (tc.getSourceFile() != null) {
                errorStream.print(tc.getPosition());
            } else {
                errorStream.print(tc.getSourceLine());
            }
        } else if (((RubyArray) backtrace).getLength() == 0) {
            printErrorPos(errorStream);
        } else {
            IRubyObject mesg = ((RubyArray) backtrace).first(IRubyObject.NULL_ARRAY);

            if (mesg.isNil()) {
                printErrorPos(errorStream);
            } else {
                errorStream.print(mesg);
            }
        }

        RubyClass type = excp.getMetaClass();
        String info = excp.toString();

        if (type == getClass("RuntimeError") && (info == null || info.length() == 0)) {
            errorStream.print(": unhandled exception\n");
        } else {
            String path = type.getName();

            if (info.length() == 0) {
                errorStream.print(": " + path + '\n');
            } else {
                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                if (info.indexOf("\n") != -1) {
                    tail = info.substring(info.indexOf("\n") + 1);
                    info = info.substring(0, info.indexOf("\n"));
                }

                errorStream.print(": " + info);

                if (path != null) {
                    errorStream.print(" (" + path + ")\n");
                }

                if (tail != null) {
                    errorStream.print(tail + '\n');
                }
            }
        }

        excp.printBacktrace(errorStream);
	}

	private void printErrorPos(PrintStream errorStream) {
        ThreadContext tc = getCurrentContext();
        if (tc.getSourceFile() != null) {
            if (tc.getFrameLastFunc() != null) {
            	errorStream.print(tc.getPosition());
            	errorStream.print(":in '" + tc.getFrameLastFunc() + '\'');
            } else if (tc.getSourceLine() != 0) {
                errorStream.print(tc.getPosition());
            } else {
            	errorStream.print(tc.getSourceFile());
            }
        }
    }

    /** This method compiles and interprets a Ruby script.
     *
     *  It can be used if you want to use JRuby as a Macro language.
     *
     */
    public void loadScript(RubyString scriptName, RubyString source, boolean wrap) {
        loadScript(scriptName.toString(), new StringReader(source.toString()), wrap);
    }

    public void loadScript(String scriptName, Reader source, boolean wrap) {
        IRubyObject self = getTopSelf();

        ThreadContext context = getCurrentContext();

        RubyModule wrapper = context.getWrapper();

        try {
            if (!wrap) {
                secure(4); /* should alter global state */

                context.preNodeEval(null, objectClass, self);
            } else {
                /* load in anonymous module as toplevel */
                context.preNodeEval(RubyModule.newModule(this, null), context.getWrapper(), self);
                
                self = getTopSelf().rbClone();
                self.extendObject(context.getRubyClass());
            }

        	Node node = parse(source, scriptName, null);
            self.eval(node);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
        		// Make sure this does not bubble out to java caller.
        	} else {
        		throw je;
        	}
        } finally {
            context.postNodeEval(wrapper);
        }
    }

    public void loadNode(String scriptName, Node node, boolean wrap) {
        IRubyObject self = getTopSelf();

        ThreadContext context = getCurrentContext();

        RubyModule wrapper = context.getWrapper();

        try {
            if (!wrap) {
                secure(4); /* should alter global state */
                
                context.preNodeEval(null, objectClass, self);
            } else {

                /* load in anonymous module as toplevel */
                context.preNodeEval(RubyModule.newModule(this, null), context.getWrapper(), self);
                
                self = getTopSelf().rbClone();
                self.extendObject(context.getRubyClass());
            }
            
            self.eval(node);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
        		// Make sure this does not bubble out to java caller.
        	} else {
        		throw je;
        	}
        } finally {
            context.postNodeEval(wrapper);
        }
    }


    /** Loads, compiles and interprets a Ruby file.
     *  Used by Kernel#require.
     *
     *  @mri rb_load
     */
    public void loadFile(File file, boolean wrap) {
        assert file != null : "No such file to load";
        try {
            BufferedReader source = new BufferedReader(new FileReader(file));
            loadScript(file.getPath().replace(File.separatorChar, '/'), source, wrap);
            source.close();
        } catch (IOException ioExcptn) {
            throw newIOErrorFromException(ioExcptn);
        }
    }

    /** Call the trace function
     *
     * MRI: eval.c - call_trace_func
     *
     */
    public void callTraceFunction(ThreadContext context, String event, ISourcePosition position, 
            IRubyObject self, String name, IRubyObject type) {
        if (traceFunction == null) return;

        if (!context.isWithinTrace()) {
            context.setWithinTrace(true);

            ISourcePosition savePosition = context.getPosition();
            String file = position.getFile();

            if (file == null) file = "(ruby)";
            if (type == null) type = getFalse(); 

            context.preTrace();

            try {
                traceFunction.call(new IRubyObject[] { newString(event), newString(file),
                        newFixnum(position.getEndLine()),
                        name != null ? RubySymbol.newSymbol(this, name) : getNil(),
                        self != null ? self : getNil(),
                        type });
            } finally {
                context.postTrace();
                context.setPosition(savePosition);
                context.setWithinTrace(false);
            }
        }
    }

    public RubyProc getTraceFunction() {
        return traceFunction;
    }

    public void setTraceFunction(RubyProc traceFunction) {
        this.traceFunction = traceFunction;
    }
    public GlobalVariables getGlobalVariables() {
        return globalVariables;
    }
    
    // For JSR 223 support: see http://scripting.java.net/
    public void setGlobalVariables(GlobalVariables globalVariables) {
    	this.globalVariables = globalVariables;
    }

    public CallbackFactory callbackFactory(Class type) {
        return CallbackFactory.createFactory(type);
    }

    /**
     * Push block onto exit stack.  When runtime environment exits
     * these blocks will be evaluated.
     * 
     * @return the element that was pushed onto stack
     */
    public IRubyObject pushExitBlock(RubyProc proc) {
    	atExitBlocks.push(proc);
        return proc;
    }
    
    /**
     * Make sure Kernel#at_exit procs get invoked on runtime shutdown.
     * This method needs to be explicitly called to work properly.
     * I thought about using finalize(), but that did not work and I
     * am not sure the runtime will be at a state to run procs by the
     * time Ruby is going away.  This method can contain any other
     * things that need to be cleaned up at shutdown.  
     */
    public void tearDown() {
        while (!atExitBlocks.empty()) {
            RubyProc proc = (RubyProc) atExitBlocks.pop();
            
            proc.call(IRubyObject.NULL_ARRAY);
        }
    }
    
    // new factory methods ------------------------------------------------------------------------
    
    public RubyArray newArray() {
    	return RubyArray.newArray(this);
    }
    
    public RubyArray newArray(IRubyObject object) {
    	return RubyArray.newArray(this, object);
    }

    public RubyArray newArray(IRubyObject car, IRubyObject cdr) {
    	return RubyArray.newArray(this, car, cdr);
    }
    
    public RubyArray newArray(IRubyObject[] objects) {
    	return RubyArray.newArray(this, objects);
    }
    
    public RubyArray newArray(List list) {
    	return RubyArray.newArray(this, list);
    }
    
    public RubyArray newArray(int size) {
    	return RubyArray.newArray(this, size);
    }
    
    public RubyBoolean newBoolean(boolean value) {
    	return RubyBoolean.newBoolean(this, value);
    }
    
    public RubyFileStat newRubyFileStat(File file) {
    	return new RubyFileStat(this, JRubyFile.create(currentDirectory,file.getPath()));
    }
    
    public RubyFixnum newFixnum(long value) {
    	return RubyFixnum.newFixnum(this, value);
    }
    
    public RubyFloat newFloat(double value) {
    	return RubyFloat.newFloat(this, value);
    }

    public RubyNumeric newNumeric() {
    	return RubyNumeric.newNumeric(this);
    }
    
    public RubyProc newProc() {
    	return RubyProc.newProc(this, false);
    }
    
    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this);
    }
    
    public RubyBinding newBinding(Block block) {
    	return RubyBinding.newBinding(this, block);
    }

    public RubyString newString(String string) {
    	return RubyString.newString(this, string);
    }
    
    public RubySymbol newSymbol(String string) {
    	return RubySymbol.newSymbol(this, string);
    }
    
    public RubyTime newTime(long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }
    
    public RaiseException newArgumentError(String message) {
    	return newRaiseException(getClass("ArgumentError"), message);
    }
    
    public RaiseException newArgumentError(int got, int expected) {
    	return newRaiseException(getClass("ArgumentError"), "wrong # of arguments(" + got + " for " + expected + ")");
    }
    
    public RaiseException newErrnoEBADFError() {
    	return newRaiseException(getModule("Errno").getClass("EBADF"), "Bad file descriptor");
    }

    public RaiseException newErrnoEINVALError() {
    	return newRaiseException(getModule("Errno").getClass("EINVAL"), "Invalid file");
    }

    public RaiseException newErrnoENOENTError() {
    	return newRaiseException(getModule("Errno").getClass("ENOENT"), "File not found");
    }

    public RaiseException newErrnoESPIPEError() {
    	return newRaiseException(getModule("Errno").getClass("ESPIPE"), "Illegal seek");
    }

    public RaiseException newErrnoEBADFError(String message) {
    	return newRaiseException(getModule("Errno").getClass("EBADF"), message);
    }

    public RaiseException newErrnoEINVALError(String message) {
    	return newRaiseException(getModule("Errno").getClass("EINVAL"), message);
    }

    public RaiseException newErrnoENOENTError(String message) {
    	return newRaiseException(getModule("Errno").getClass("ENOENT"), message);
    }

    public RaiseException newErrnoESPIPEError(String message) {
    	return newRaiseException(getModule("Errno").getClass("ESPIPE"), message);
    }

    public RaiseException newErrnoEEXISTError(String message) {
    	return newRaiseException(getModule("Errno").getClass("EEXIST"), message);
    }

    public RaiseException newIndexError(String message) {
    	return newRaiseException(getClass("IndexError"), message);
    }
    
    public RaiseException newSecurityError(String message) {
    	return newRaiseException(getClass("SecurityError"), message);
    }
    
    public RaiseException newSystemCallError(String message) {
    	return newRaiseException(getClass("SystemCallError"), message);
    }

    public RaiseException newTypeError(String message) {
    	return newRaiseException(getClass("TypeError"), message);
    }
    
    public RaiseException newThreadError(String message) {
    	return newRaiseException(getClass("ThreadError"), message);
    }
    
    public RaiseException newSyntaxError(String message) {
    	return newRaiseException(getClass("SyntaxError"), message);
    }

    public RaiseException newRangeError(String message) {
    	return newRaiseException(getClass("RangeError"), message);
    }

    public RaiseException newNotImplementedError(String message) {
    	return newRaiseException(getClass("NotImplementedError"), message);
    }

    public RaiseException newNoMethodError(String message, String name) {
        return new RaiseException(new RubyNameError(this, this.getClass("NoMethodError"), message, name), true);
    }

    public RaiseException newNameError(String message, String name) {
        return new RaiseException(new RubyNameError(this, this.getClass("NameError"), message, name), true);
    }

    public RaiseException newLocalJumpError(String message) {
    	return newRaiseException(getClass("LocalJumpError"), message);
    }

    public RaiseException newLoadError(String message) {
    	return newRaiseException(getClass("LoadError"), message);
    }

    public RaiseException newFrozenError(String objectType) {
		// TODO: Should frozen error have its own distinct class?  If not should more share?
    	return newRaiseException(getClass("TypeError"), "can't modify frozen " + objectType);
    }

    public RaiseException newSystemStackError(String message) {
    	return newRaiseException(getClass("SystemStackError"), message);
    }
    
    public RaiseException newSystemExit(int status) {
    	RaiseException re = newRaiseException(getClass("SystemExit"), "");
    	re.getException().setInstanceVariable("status", newFixnum(status));
    	
    	return re;
    }
    
	public RaiseException newIOError(String message) {
		return newRaiseException(getClass("IOError"), message);
    }
    
    public RaiseException newIOErrorFromException(IOException ioe) {
    	return newRaiseException(getClass("IOError"), ioe.getMessage());
    }
    
    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType) {
    	return newRaiseException(getClass("TypeError"), "wrong argument type " + receivedObject.getMetaClass() + " (expected " + expectedType);
    }
    
    public RaiseException newEOFError() {
    	return newRaiseException(getClass("EOFError"), "End of file reached");
    }
    
    public RaiseException newZeroDivisionError() {
    	return newRaiseException(getClass("ZeroDivisionError"), "divided by 0");
    }
    
    /**
	 * @param exceptionClass
	 * @param message
	 * @return
	 */
	private RaiseException newRaiseException(RubyClass exceptionClass, String message) {
		RaiseException re = new RaiseException(this, exceptionClass, message, true);
		return re;
	}


	public RubySymbol.SymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void setStackTraces(int stackTraces) {
		this.stackTraces = stackTraces;
	}

	public int getStackTraces() {
		return stackTraces;
	}

	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public Random getRandom() {
		return random;
	}

	public ObjectSpace getObjectSpace() {
		return objectSpace;
	}

	public Hashtable getIoHandlers() {
		return ioHandlers;
	}

	public RubyFixnum[] getFixnumCache() {
		return fixnumCache;
	}

	public long incrementRandomSeedSequence() {
		return randomSeedSequence++;
	}

	public InputStream getIn() {
		return in;
	}

	public PrintStream getOut() {
		return out;
	}

	public PrintStream getErr() {
		return err;
	}

	public boolean isGlobalAbortOnExceptionEnabled() {
		return globalAbortOnExceptionEnabled;
	}

	public void setGlobalAbortOnExceptionEnabled(boolean enable) {
		globalAbortOnExceptionEnabled = enable;
	}

	public boolean isDoNotReverseLookupEnabled() {
		return doNotReverseLookupEnabled;
	}

	public void setDoNotReverseLookupEnabled(boolean b) {
		doNotReverseLookupEnabled = b;
	}

    private ThreadLocal inspect = new ThreadLocal();
    public boolean registerInspecting(Object obj) {
        java.util.Map val = (java.util.Map)inspect.get();
        if(null == val) {
            val = new java.util.IdentityHashMap();
            inspect.set(val);
        }
        if(val.containsKey(obj)) {
            return false;
        }
        val.put(obj,null);
        return true;
    }

    public void unregisterInspecting(Object obj) {
        java.util.Map val = (java.util.Map)inspect.get();
        val.remove(obj);
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public String getEncoding() {
        return encoding;
    }
}
