/*
 *  RubyClasses.java - No description
 *  Created on 05. Oktober 2001, 01:43
 *
 *  Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 *  Jan Arne Petersen <jpetersen@uni-bonn.de>
 *  Stefan Matthias Aust <sma@3plus4.de>
 *  Alan Moore <alan_moore@gmx.net>
 *  Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 *  JRuby - http://jruby.sourceforge.net
 *
 *  This file is part of JRuby
 *
 *  JRuby is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  JRuby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JRuby; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby;

import java.util.Iterator;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyHashMap;
import org.jruby.util.RubyMap;
import org.jruby.javasupport.JavaClassClass;

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
 * <li>Method</li>
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
    private Ruby ruby;

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
    private RubyClass javaClassClass;
    private RubyClass javaInterfaceClass;
    private RubyClass javaObjectClass;
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

    private RubyModule comparableModule;
    private RubyModule enumerableModule;
    private RubyModule gcModule;
    private RubyModule javaModule;
    private RubyModule kernelModule;
    private RubyModule marshalModule;
    private RubyModule mathModule;
    private RubyModule objectSpaceModule;
    private RubyModule processModule;
    private RubyModule precisionModule;

    private RubyMap classMap;

    /**
     * Creates a new RubyClasses instance and defines all the
     * core classes and modules in the Ruby runtime.
     *
     * @param ruby The Ruby runtime.
     */
    public RubyClasses(Ruby ruby) {
        this.ruby = ruby;

        classMap = new RubyHashMap();
    }

    /**
     * rb_define_boot?
     *
     * @param name Description of the Parameter
     * @param superClass Description of the Parameter
     * @return Description of the Return Value
     */
    private RubyClass defineBootClass(String name, RubyClass superClass) {
        RubyClass bootClass = RubyClass.newClass(ruby, superClass);
        bootClass.setName(name);
        classMap.put(name, bootClass);

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

        kernelModule = KernelModule.createKernelModule(ruby);
        objectClass.includeModule(kernelModule);

        objectClass.definePrivateMethod("initialize", CallbackFactory.getNilMethod(-1));
        classClass.definePrivateMethod("inherited", CallbackFactory.getNilMethod(1));

        RubyObject.createObjectClass(objectClass);

        RubyClass.createClassClass(classClass);
        RubyModule.createModuleClass(moduleClass);

        nilClass = RubyNil.createNilClass(ruby);

        falseClass = RubyBoolean.createFalseClass(ruby);
        trueClass = RubyBoolean.createTrueClass(ruby);
        
        threadClass = ThreadClass.createThreadClass(ruby);
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
            structClass = RubyStruct.createStructClass(ruby);
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
            comparableModule = RubyComparable.createComparable(ruby);
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
            hashClass = RubyHash.createHashClass(ruby);
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
            mathModule = RubyMath.createMathModule(ruby);
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
            regExpClass = RubyRegexp.createRegexpClass(ruby);
        }
        return regExpClass;
    }

    /**
     * Returns the reference to the Process module.
     *
     * @return The Process module.
     */
    public RubyModule getProcessModule() {
        return processModule;
    }

    /**
     * Returns the reference to the IO class.
     *
     * @return The IO class.
     */
    public RubyClass getIoClass() {
        if (ioClass == null) {
            ioClass = RubyIO.createIOClass(ruby);
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
            bignumClass = RubyBignum.createBignumClass(ruby);
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
            rangeClass = RubyRange.createRangeClass(ruby);
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
            gcModule = RubyGC.createGCModule(ruby);
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
            symbolClass = RubySymbol.createSymbolClass(ruby);
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
            procClass = RubyProc.createProcClass(ruby);
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
            floatClass = RubyFloat.createFloatClass(ruby);
        }
        return floatClass;
    }

    /**
     * Returns the reference to the Method class.
     *
     * @return The Method class.
     */
    public RubyClass getMethodClass() {
        if (methodClass == null) {
            methodClass = RubyMethod.createMethodClass(ruby);
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
            matchDataClass = RubyMatchData.createMatchDataClass(ruby);
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
            marshalModule = RubyMarshal.createMarshalModule(ruby);
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
            fixnumClass = RubyFixnum.createFixnumClass(ruby);
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
            objectSpaceModule = RubyObjectSpace.createObjectSpaceModule(ruby);
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
            dirClass = RubyDir.createDirClass(ruby);
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
            exceptionClass = RubyException.createExceptionClass(ruby);
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
            stringClass = RubyString.createStringClass(ruby);
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
            integerClass = RubyInteger.createIntegerClass(ruby);
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
            fileClass = RubyFile.createFileClass(ruby);
        }
        return fileClass;
    }

    public RubyClass getFileStatClass() {
        if (fileStatClass == null) {
            fileStatClass = FileStatClass.createFileStatClass(ruby);
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
            arrayClass = RubyArray.createArrayClass(ruby);
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
            enumerableModule = RubyEnumerable.createEnumerableModule(ruby);
        }
        return enumerableModule;
    }

    /**
     * Gets the precisionModule attribute of the RubyClasses object
     *
     * @return The precisionModule value
     */
    public RubyModule getPrecisionModule() {
        if (precisionModule == null) {
            precisionModule = RubyPrecision.createPrecisionModule(ruby);
        }
        return precisionModule;
    }

    /**
     * Returns the reference to the JavaObject class.
     *
     * @return The JavaObject class.
     */
    public RubyClass getJavaObjectClass() {
        if (javaObjectClass == null) {
            javaObjectClass = RubyJavaObject.createJavaObjectClass(ruby);
        }
        return javaObjectClass;
    }

    /**
     * Returns the reference to the Java module.
     *
     * @return The Java module.
     */
    public RubyModule getJavaModule() {
        if (javaModule == null) {
            javaModule = RubyJava.createJavaModule(ruby);
        }
        return javaModule;
    }

    public RubyClass getJavaClassClass() {
        if (javaClassClass == null) {
            javaClassClass = JavaClassClass.createJavaClassClass(ruby);
        }
        return javaClassClass;
    }

    /**
     * Returns the reference to the JavaInterface class.
     *
     * @return The JavaInterface class.
     */
    public RubyClass getJavaInterfaceClass() {
        if (javaInterfaceClass == null) {
            javaInterfaceClass = RubyJavaInterface.createJavaInterfaceClass(ruby);
        }
        return javaInterfaceClass;
    }

    /**
     * Returns the reference to the Numeric class.
     *
     * @return The Numeric class.
     */
    public RubyClass getNumericClass() {
        if (numericClass == null) {
            numericClass = RubyNumeric.createNumericClass(ruby);
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
            timeClass = RubyTime.createTimeClass(ruby);
        }
        return timeClass;
    }

    /**
     * Returns a RubyMap with references to all named classes in
     * a ruby runtime..
     *
     * @return A map with references to all named classes.
     */
    public RubyMap getClassMap() {
        return classMap;
    }

    /**
     * Gets the class attribute of the RubyClasses object
     *
     * @param name Description of the Parameter
     * @return The class value
     */
    public RubyModule getClass(String name) {
        RubyModule type = (RubyModule) classMap.get(name);
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
        } else if (name == "Java::JavaClass") {
            return getJavaClassClass();
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
        }
        return null;
    }

    public RubyModule getClassFromPath(String path) {
        if (path.charAt(0) == '#') {
            throw new ArgumentError(ruby, "can't retrieve anonymous class " + path);
        }
        IRubyObject type = ruby.evalScript(path);
        if (!(type instanceof RubyModule)) {
            throw new TypeError(ruby, "class path " + path + " does not point class");
        }
        return (RubyModule) type;
    }

    /**
     * Description of the Method
     *
     * @param name Description of the Parameter
     * @param rbClass Description of the Parameter
     */
    public void putClass(String name, RubyModule rbClass) {
        classMap.put(name, rbClass);
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public Iterator nameIterator() {
        return classMap.keySet().iterator();
    }
}

