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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaArray;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.builtin.meta.IOMetaClass;
import org.jruby.runtime.builtin.meta.ProcMetaClass;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.util.BuiltinScript;

/**
 * In this class there are references to the core (or built-in) classes
 * and modules of Ruby and JRuby. There is also a Map of referenced to the
 * named classes in a Ruby runtime.
 *
 * The default classes are:
 * <ul>
 * <li>Array</li>
 * <li>Bignum</li>
 * <li>Binding</li>
 * <li>Class</li>
 * <li>Continuation</li>
 * <li>Dir</li>
 * <li>Exception</li>
 * <li>FalseClass</li>
 * <li>File</li>
 * <li>File::Stat</li>
 * <li>Fixnum</li>
 * <li>Float</li>
 * <li>Hash</li>
 * <li>Integer</li>
 * <li>IO</li>
 * <li>JavaObject</li>
 * <li>MatchData</li>
 * <li>RubyMethod</li>
 * <li>Module</li>
 * <li>NilClass</li>
 * <li>Numeric</li>
 * <li>Object</li>
 * <li>Proc</li>
 * <li>Range</li>
 * <li>RegExp</li>
 * <li>String</li>
 * <li>Struct</li>
 * <li>Struct::Tms</li>
 * <li>Symbol</li>
 * <li>Thread</li>
 * <li>ThreadGroup</li>
 * <li>Time</li>
 * <li>TrueClass</li>
 * </ul>
 *
 * The default modules are:
 *
 * <ul>
 * <li>Comparable</li>
 * <li>Enumerable</li>
 * <li>GC</li>
 * <li>Kernel</li>
 * <li>Marshal</li>
 * <li>Math</li>
 * <li>ObjectSpace</li>
 * <li>Process</li>
 * </ul>
 *
 * You can access the references by the get&lt;classname&gt;Class or
 * get&lt;modulename&gt;Module methods.
 *
 * @author jpetersen
 * @since 0.1.8
 */
public class RubyClasses {
    private Ruby runtime;

    private RubyClass arrayClass;
    private RubyClass bignumClass;
    private RubyClass bindingClass;
    private RubyClass classClass;
    private RubyClass continuationClass;
    private RubyClass dirClass;
    private RubyClass exceptionClass;
    private RubyClass falseClass;
    private RubyClass fileClass;
    private RubyClass fileStatClass;
    private RubyClass fixnumClass;
    private RubyClass floatClass;
    private RubyClass hashClass;
    private RubyClass integerClass;
    private RubyClass ioClass;
    private RubyClass javaObjectClass;
    private RubyClass javaArrayClass;
    private RubyClass matchDataClass;
    private RubyClass methodClass;
    private RubyClass moduleClass;
    private RubyClass nilClass;
    private RubyClass numericClass;
    private RubyClass objectClass;
    private RubyClass procClass;
    private RubyClass rangeClass;
    private RubyClass regExpClass;
    private RubyClass stringClass;
    private RubyClass structClass;
    private RubyClass structTmsClass;
    private RubyClass symbolClass;
    private RubyClass threadClass;
    private RubyClass threadGroupClass;
    private RubyClass timeClass;
    private RubyClass trueClass;
    private RubyClass unboundMethodClass;

    private RubyModule comparableModule;
    private RubyModule enumerableModule;
    private RubyModule fileTestModule;
    private RubyModule gcModule;
    private RubyModule javaModule;
    private RubyModule kernelModule;
    private RubyModule marshalModule;
    private RubyModule mathModule;
    private RubyModule objectSpaceModule;
    private RubyModule processModule;
    private RubyModule precisionModule;

    private Map topLevelClasses;
    private Map nonTopLevelClasses;

    /**
     * Creates a new RubyClasses instance and defines all the
     * core classes and modules in the Ruby runtime.
     *
     * @param runtime The Ruby runtime.
     */
    public RubyClasses(Ruby runtime) {
        this.runtime = runtime;

        topLevelClasses = new HashMap();
        nonTopLevelClasses = new HashMap();
    }

    /**
     * rb_define_boot?
     *
     * @param name Description of the Parameter
     * @param superClass Description of the Parameter
     * @return Description of the Return Value
     */
    private RubyClass defineBootClass(String name, RubyClass superClass) {
        RubyClass bootClass = RubyClass.newClass(runtime, superClass, null, name);
        topLevelClasses.put(name, bootClass);

        return bootClass;
    }

    /**
     * This method defines the core classes and modules in
     * the Ruby runtime.
     *
     * Ruby's Class Hierarchy Chart
     *
     * <pre>
     *
     *                           +------------------+
     *                           |                  |
     *             Object---->(Object)              |
     *              ^  ^        ^  ^                |
     *              |  |        |  |                |
     *              |  |  +-----+  +---------+      |
     *              |  |  |                  |      |
     *              |  +-----------+         |      |
     *              |     |        |         |      |
     *       +------+     |     Module--->(Module)  |
     *       |            |        ^         ^      |
     *  OtherClass-->(OtherClass)  |         |      |
     *                             |         |      |
     *                           Class---->(Class)  |
     *                             ^                |
     *                             |                |
     *                             +----------------+
     *
     * </pre>
     *
     *   + All metaclasses are instances of the class `Class'.
     */
    public void initCoreClasses() {
        objectClass = defineBootClass("Object", null);
        moduleClass = defineBootClass("Module", objectClass);
        classClass = defineBootClass("Class", moduleClass);

        RubyClass metaClass = objectClass.makeMetaClass(classClass);
        metaClass = moduleClass.makeMetaClass(metaClass);
        metaClass = classClass.makeMetaClass(metaClass);

        kernelModule = RubyKernel.createKernelModule(runtime);
        objectClass.includeModule(kernelModule);

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyClasses.class);
        objectClass.definePrivateMethod("initialize", callbackFactory.getNilMethod(-1));
        classClass.definePrivateMethod("inherited", callbackFactory.getNilMethod(1));

        RubyObject.createObjectClass(objectClass);

        RubyClass.createClassClass(classClass);
        RubyModule.createModuleClass(moduleClass);

        nilClass = RubyNil.createNilClass(runtime);

        falseClass = RubyBoolean.createFalseClass(runtime);
        trueClass = RubyBoolean.createTrueClass(runtime);

        threadGroupClass = RubyThreadGroup.createThreadGroupClass(runtime);
        threadClass = RubyThread.createThreadClass(runtime);

        runtime.getLoadService().addAutoload("UnboundMethod", new IAutoloadMethod() {
            public IRubyObject load(Ruby runtime, String name) {
                return RubyUnboundMethod.defineUnboundMethodClass(runtime);
            }
        });
        
        runtime.getClasses().getFileTestModule();
    }

    public void initBuiltinClasses() {
    	try {
	        new BuiltinScript("FalseClass").load(runtime);
	        new BuiltinScript("TrueClass").load(runtime);
	        new BuiltinScript("Enumerable").load(runtime);
    	} catch (IOException e) {
    		throw new Error("builtin scripts are missing", e);
    	}
    }

    /**
     * Returns the reference to the Binding class.
     *
     * @return the Binding class.
     */
    public RubyClass getBindingClass() {
        return bindingClass;
    }

    /**
     * Returns the reference to the Class class.
     *
     * @return The Class class.
     */
    public RubyClass getClassClass() {
        return classClass;
    }

    /**
     * Returns the reference to the Module class.
     *
     * @return The Module class.
     */
    public RubyClass getModuleClass() {
        return moduleClass;
    }

    /**
     * Returns the reference to the Struct class.
     *
     * @return The Struct class.
     */
    public RubyClass getStructClass() {
        if (structClass == null) {
            structClass = RubyStruct.createStructClass(runtime);
        }
        return structClass;
    }

    /**
     * Returns the reference to the Comparable module.
     *
     * @return The Comparable module.
     */
    public RubyModule getComparableModule() {
        if (comparableModule == null) {
            comparableModule = RubyComparable.createComparable(runtime);
        }
        return comparableModule;
    }

    /**
     * Returns the reference to the Hash class.
     *
     * @return The Hash class.
     */
    public RubyClass getHashClass() {
        if (hashClass == null) {
            hashClass = RubyHash.createHashClass(runtime);
        }
        return hashClass;
    }

    /**
     * Returns the reference to the Math module.
     *
     * @return The Math module.
     */
    public RubyModule getMathModule() {
        if (mathModule == null) {
            mathModule = RubyMath.createMathModule(runtime);
        }
        return mathModule;
    }

    /**
     * Returns the reference to the RegExp class.
     *
     * @return The RegExp class.
     */
    public RubyClass getRegExpClass() {
        if (regExpClass == null) {
            regExpClass = RubyRegexp.createRegexpClass(runtime);
        }
        return regExpClass;
    }

    /**
     * Returns the reference to the IO class.
     *
     * @return The IO class.
     */
    public RubyClass getIoClass() {
        if (ioClass == null) {
            ioClass = new IOMetaClass(runtime);
        }
        return ioClass;
    }

    /**
     * Returns the reference to the ThreadGroup class.
     *
     * @return The ThreadGroup class.
     */
    public RubyClass getThreadGroupClass() {
        return threadGroupClass;
    }

    /**
     * Returns the reference to the Bignum class.
     *
     * @return The Bignum class.
     */
    public RubyClass getBignumClass() {
        if (bignumClass == null) {
            bignumClass = RubyBignum.createBignumClass(runtime);
        }
        return bignumClass;
    }

    /**
     * Returns the reference to the Struct::Tms class.
     *
     * @return The Struct::Tms class.
     */
    public RubyClass getStructTmsClass() {
        return structTmsClass;
    }

    /**
     * Returns the reference to the Range class.
     *
     * @return The Range class.
     */
    public RubyClass getRangeClass() {
        if (rangeClass == null) {
            rangeClass = RubyRange.createRangeClass(runtime);
        }
        return rangeClass;
    }

    /**
     * Returns the reference to the GC module.
     *
     * @return The GC module.
     */
    public RubyModule getGcModule() {
        if (gcModule == null) {
            gcModule = RubyGC.createGCModule(runtime);
        }
        return gcModule;
    }

    /**
     * Returns the reference to the Symbol class.
     *
     * @return The Symbol class.
     */
    public RubyClass getSymbolClass() {
        if (symbolClass == null) {
            symbolClass = RubySymbol.createSymbolClass(runtime);
        }
        return symbolClass;
    }

    /**
     * Returns the reference to the Proc class.
     *
     * @return The Proc class.
     */
    public RubyClass getProcClass() {
        if (procClass == null) {
            procClass = new ProcMetaClass(runtime);
        }
        return procClass;
    }

    /**
     * Returns the reference to the Continuation class.
     *
     * @return The Continuation class.
     */
    public RubyClass getContinuationClass() {
        return continuationClass;
    }

    /**
     * Returns the reference to the FalseClass class.
     *
     * @return The FalseClass class.
     */
    public RubyClass getFalseClass() {
        return falseClass;
    }

    /**
     * Returns the reference to the Float class.
     *
     * @return The Float class.
     */
    public RubyClass getFloatClass() {
        if (floatClass == null) {
            floatClass = RubyFloat.createFloatClass(runtime);
        }
        return floatClass;
    }

    /**
     * Returns the reference to the RubyMethod class.
     *
     * @return The RubyMethod class.
     */
    public RubyClass getMethodClass() {
        if (methodClass == null) {
            methodClass = RubyMethod.createMethodClass(runtime);
        }
        return methodClass;
    }

    /**
     * Returns the reference to the MatchData class.
     *
     * @return The MatchData class.
     */
    public RubyClass getMatchDataClass() {
        if (matchDataClass == null) {
            matchDataClass = RubyMatchData.createMatchDataClass(runtime);
        }
        return matchDataClass;
    }

    /**
     * Returns the reference to the Marshal module.
     *
     * @return The Marshal module.
     */
    public RubyModule getMarshalModule() {
        if (marshalModule == null) {
            marshalModule = RubyMarshal.createMarshalModule(runtime);
        }
        return marshalModule;
    }

    /**
     * Returns the reference to the Fixnum class.
     *
     * @return The Fixnum class.
     */
    public RubyClass getFixnumClass() {
        if (fixnumClass == null) {
            fixnumClass = RubyFixnum.createFixnumClass(runtime);
        }
        return fixnumClass;
    }

    /**
     * Returns the reference to the Object class.
     *
     * @return The Object class.
     */
    public RubyClass getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the reference to the ObjectSpace module.
     *
     * @return The ObjectSpace module.
     */
    public RubyModule getObjectSpaceModule() {
        if (objectSpaceModule == null) {
            objectSpaceModule = RubyObjectSpace.createObjectSpaceModule(runtime);
        }
        return objectSpaceModule;
    }

    /**
     * Returns the reference to the Dir class.
     *
     * @return The Dir class.
     */
    public RubyClass getDirClass() {
        if (dirClass == null) {
            dirClass = RubyDir.createDirClass(runtime);
        }
        return dirClass;
    }

    /**
     * Returns the reference to the Exception class.
     *
     * @return The Exception class.
     */
    public RubyClass getExceptionClass() {
        if (exceptionClass == null) {
            exceptionClass = RubyException.createExceptionClass(runtime);
        }
        return exceptionClass;
    }

    /**
     * Returns the reference to the String class.
     *
     * @return The String class.
     */
    public RubyClass getStringClass() {
        if (stringClass == null) {
            stringClass = new StringMetaClass(runtime);
        }
        return stringClass;
    }

    /**
     * Returns the reference to the TrueClass class.
     *
     * @return The TrueClass class.
     */
    public RubyClass getTrueClass() {
        return trueClass;
    }

    /**
     * Returns the reference to the Integer class.
     *
     * @return The Integer class.
     */
    public RubyClass getIntegerClass() {
        if (integerClass == null) {
            integerClass = RubyInteger.createIntegerClass(runtime);
        }
        return integerClass;
    }

    /**
     * Returns the reference to the Kernel module.
     *
     * @return The Kernel module.
     */
    public RubyModule getKernelModule() {
        return kernelModule;
    }

    /**
     * Returns the reference to the Thread class.
     *
     * @return The Thread class.
     */
    public RubyClass getThreadClass() {
        return threadClass;
    }

    /**
     * Returns the reference to the File class.
     *
     * @return The File class.
     */
    public RubyClass getFileClass() {
        if (fileClass == null) {
            fileClass = new FileMetaClass(runtime);
        }
        return fileClass;
    }

    public RubyClass getFileStatClass() {
        if (fileStatClass == null) {
            fileStatClass = RubyFileStat.createFileStatClass(runtime);
        }
        return fileStatClass;
    }

    /**
     * Returns the reference to the NilClass class.
     *
     * @return The NilClass class.
     */
    public RubyClass getNilClass() {
        return nilClass;
    }

    /**
     * Returns the reference to the Array class.
     *
     * @return The Array class.
     */
    public RubyClass getArrayClass() {
        if (arrayClass == null) {
            arrayClass = RubyArray.createArrayClass(runtime);
        }
        return arrayClass;
    }

    /**
     * Returns the reference to the Enumerable module.
     *
     * @return The Enumerable module.
     */
    public RubyModule getEnumerableModule() {
        if (enumerableModule == null) {
            enumerableModule = RubyEnumerable.createEnumerableModule(runtime);
        }
        return enumerableModule;
    }

    /**
     * Returns the reference to the Enumerable module.
     *
     * @return The Enumerable module.
     */
    public RubyModule getFileTestModule() {
        if (fileTestModule == null) {
            fileTestModule = RubyFileTest.createFileTestModule(runtime);
        }
        return fileTestModule;
    }
    
    /**
     * Gets the precisionModule attribute of the RubyClasses object
     *
     * @return The precisionModule value
     */
    public RubyModule getPrecisionModule() {
        if (precisionModule == null) {
            precisionModule = RubyPrecision.createPrecisionModule(runtime);
        }
        return precisionModule;
    }

    /**
     * Gets the processModule attribute of the RubyClasses object
     *
     * @return The processModule value
     */
    public RubyModule getProcessModule() {
        if (processModule == null) {
            processModule = RubyProcess.createProcessModule(runtime);
        }
        return processModule;
    }

    /**
     * Returns the reference to the JavaObject class.
     *
     * @return The JavaObject class.
     */
    public RubyClass getJavaObjectClass() {
        if (javaObjectClass == null) {
            javaObjectClass = JavaObject.createJavaObjectClass(runtime);
        }
        return javaObjectClass;
    }

    public RubyClass getJavaArrayClass() {
        if (javaArrayClass == null) {
            javaArrayClass = JavaArray.createJavaArrayClass(runtime);
        }
        return javaArrayClass;
    }

    /**
     * Returns the reference to the Java module.
     *
     * @return The Java module.
     */
    public RubyModule getJavaModule() {
        if (javaModule == null) {
            javaModule = Java.createJavaModule(runtime);
        }
        return javaModule;
    }

    /**
     * Returns the reference to the Numeric class.
     *
     * @return The Numeric class.
     */
    public RubyClass getNumericClass() {
        if (numericClass == null) {
            numericClass = RubyNumeric.createNumericClass(runtime);
        }
        return numericClass;
    }

    /**
     * Returns the reference to the Time class.
     *
     * @return The Time class.
     */
    public RubyClass getTimeClass() {
        if (timeClass == null) {
            timeClass = RubyTime.createTimeClass(runtime);
        }
        return timeClass;
    }

    public RubyClass getUnboundMethodClass() {
        if (unboundMethodClass == null) {
            unboundMethodClass = RubyUnboundMethod.defineUnboundMethodClass(runtime);
        }
        return unboundMethodClass;
    }

    /**
     * Returns a RubyMap with references to all named classes in
     * the top-level of the ruby runtime..
     */
    public Map getTopLevelClassMap() {
        return topLevelClasses;
    }
    
    /**
     * Returns a RubyMap with references to all named classes in
     * that are not in the top-level.
     */
    public Map getNonTopLevelClassMap() {
    	return nonTopLevelClasses;
    }

    /**
     * Gets the class attribute of the RubyClasses object
     *
     * @param name Description of the Parameter
     * @return The class value
     */
    public RubyModule getClass(String name) {
        RubyModule type = (RubyModule) topLevelClasses.get(name);
        if (type == null) {
            type = getAutoload(name);
        }
        return type;
    }

    private RubyModule getAutoload(String name) {
        name = name.intern();
        if (name == "Array") {
            return getArrayClass();
        } else if (name == "Bignum") {
            return getBignumClass();
        } else if (name == "Binding") {
            return getBindingClass();
        } else if (name == "Comparable") {
            return getComparableModule();
        } else if (name == "Continuation") {
            return getContinuationClass();
        } else if (name == "Dir") {
            return getDirClass();
        } else if (name == "Enumerable") {
            return getEnumerableModule();
        } else if (name == "Exception") {
            return getExceptionClass();
        } else if (name == "File") {
            return getFileClass();
        } else if (name == "Fixnum") {
            return getFixnumClass();
        } else if (name == "Float") {
            return getFloatClass();
        } else if (name == "GC") {
            return getGcModule();
        } else if (name == "Hash") {
            return getHashClass();
        } else if (name == "Integer") {
            return getIntegerClass();
        } else if (name == "IO") {
            return getIoClass();
        } else if (name == "Java") {
            return getJavaModule();
        } else if (name == "Marshal") {
            return getMarshalModule();
        } else if (name == "MatchData") {
            return getMatchDataClass();
        } else if (name == "Math") {
            return getMathModule();
        } else if (name == "Method") {
            return getMethodClass();
        } else if (name == "Numeric") {
            return getNumericClass();
        } else if (name == "ObjectSpace") {
            return getObjectSpaceModule();
        } else if (name == "Precision") {
            return getPrecisionModule();
        } else if (name == "Proc") {
            return getProcClass();
        } else if (name == "Process") {
            return getProcessModule();
        } else if (name == "Range") {
            return getRangeClass();
        } else if (name == "Regexp") {
            return getRegExpClass();
        } else if (name == "String") {
            return getStringClass();
        } else if (name == "Struct") {
            return getStructClass();
        } else if (name == "Symbol") {
            return getSymbolClass();
        } else if (name == "ThreadGroup") {
            return getThreadGroupClass();
        } else if (name == "Time") {
            return getTimeClass();
        } else if (name == "UnboundMethod") {
            return getUnboundMethodClass();
        }
        return null;
    }

    public RubyModule getClassFromPath(String path) {
        if (path.charAt(0) == '#') {
            throw runtime.newArgumentError("can't retrieve anonymous class " + path);
        }
        IRubyObject type = runtime.evalScript(path);
        if (!(type instanceof RubyModule)) {
            throw runtime.newTypeError("class path " + path + " does not point class");
        }
        return (RubyModule) type;
    }

    public void putClass(String name, RubyModule rbClass, RubyModule parentModule) {
    	if (parentModule == objectClass) {
    		topLevelClasses.put(name, rbClass);
    	} else {
    		nonTopLevelClasses.put(rbClass.name(), rbClass);
    	}
    }

    public Iterator nameIterator() {
        return topLevelClasses.keySet().iterator();
    }
}

