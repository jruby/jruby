package org.jruby.api;

import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.load.LoadService;

public class Access {
    /**
     * Retrieve the instance of the class Array
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass arrayClass(ThreadContext context) {
        return context.runtime.getArray();
    }

    /**
     * Retrieve ARGF (RubyArgsFile) instance
     * @param context the current thread context
     * @return the instance
     */
    public static IRubyObject argsFile(ThreadContext context) {
        return context.runtime.getArgsFile();
    }

    /**
     * Retrieve the instance of the class ArgumentError
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass argumentErrorClass(ThreadContext context) {
        return context.runtime.getArgumentError();
    }

    /**
     * Retrieve the instance of the class BasicObject
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass basicObjectClass(ThreadContext context) {
        return context.runtime.getBasicObject();
    }

    /**
     * Retrieve the instance of the class Class
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass classClass(ThreadContext context) {
        return context.runtime.getClassClass();
    }

    /**
     * Retrieve the instance of the module Comparable.
     * @param context the current thread context
     * @return the Module
     */
    public static RubyModule comparableModule(ThreadContext context) {
        return context.runtime.getComparable();
    }

    /**
     * Retrieve the instance of the class Dir
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass dirClass(ThreadContext context) {
        return context.runtime.getDir();
    }

    /**
     * Retrieve the encoding service object
     * @param context the current thread context
     * @return the object
     */
    public static EncodingService encodingService(ThreadContext context) {
        return context.runtime.getEncodingService();
    }

    /**
     * Retrieve the instance of the module Enumerable.
     * @param context the current thread context
     * @return the Module
     */
    public static RubyModule enumerableModule(ThreadContext context) {
        return context.runtime.getEnumerable();
    }

    /**
     * Retrieve the instance of the class Enumerator
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass enumeratorClass(ThreadContext context) {
        return context.runtime.getEnumerator();
    }

    /**
     * Retrieve the instance of the module Errno.
     * @param context the current thread context
     * @return the Module
     */
    public static RubyModule errnoModule(ThreadContext context) {
        return context.runtime.getErrno();
    }

    /**
     * Retrieve the instance of the class Exception
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass exceptionClass(ThreadContext context) {
        return context.runtime.getException();
    }

    /**
     * Retrieve the instance of the class File
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass fileClass(ThreadContext context) {
        return context.runtime.getFile();
    }

    /**
     * Retrieve the instance of the module FileTest.
     * @param context the current thread context
     * @return the Module
     */
    public static RubyModule fileTestModule(ThreadContext context) {
        return context.runtime.getFileTest();
    }

    /**
     * Retrieve the object containing Ruby Global Variables.
     * @param context the current thread context
     * @return the object
     */
    public static GlobalVariables globalVariables(ThreadContext context) {
        return context.runtime.getGlobalVariables();
    }

    /**
     * Retrieve the instance of the class Hash
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass hashClass(ThreadContext context) {
        return context.runtime.getHash();
    }


    /**
     * Retrieve the instance of the class Integer
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass integerClass(ThreadContext context) {
        return context.runtime.getInteger();
    }

    /**
     * Retrieve our runtimes instance config which holds many configurable options
     * which are set up from command-line properties or Java system properties.
     *
     * @param context the current thread context
     * @return the object
     */
    public static RubyInstanceConfig instanceConfig(ThreadContext context) {
        return context.runtime.getInstanceConfig();
    }

    /**
     * Retrieve the instance of the class IO
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass ioClass(ThreadContext context) {
        return context.runtime.getIO();
    }

    /**
     * Retrieve the instance of the module Kernel.
     * @param context the current thread context
     * @return the Module
     */
    public static RubyModule kernelModule(ThreadContext context) {
        return context.runtime.getKernel();
    }

    /**
     * Retrieve LoadService instance
     * @param context the current thread context
     * @return the instance
     */
    public static LoadService loadService(ThreadContext context) {
        return context.runtime.getLoadService();
    }

    /**
     * Retrieve the instance of the class Module
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass moduleClass(ThreadContext context) {
        return context.runtime.getModule();
    }

    /**
     * Retrieve the instance of the class NilClass
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass nilClass(ThreadContext context) {
        return context.runtime.getNilClass();
    }

    /**
     * Retrieve the instance of the class Object
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass objectClass(ThreadContext context) {
        return context.runtime.getObject();
    }

    /**
     * Retrieve the instance of the class Proc
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass procClass(ThreadContext context) {
        return context.runtime.getProc();
    }

    /**
     * Retrieve the instance of the module Process
     * @param context the current thread context
     * @return the Class
     */
    public static RubyModule processModule(ThreadContext context) {
        return context.runtime.getProcess();
    }

    /**
     * Retrieve the instance of the class RuntimeError
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass runtimeErrorClass(ThreadContext context) {
        return context.runtime.getRuntimeError();
    }

    /**
     * Retrieve the instance of the class StandardError
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass standardErrorClass(ThreadContext context) {
        return context.runtime.getStandardError();
    }

    /**
     * Retrieve the instance of the class String
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass stringClass(ThreadContext context) {
        return context.runtime.getString();
    }

    /**
     * Retrieve the instance of the class Struct
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass structClass(ThreadContext context) {
        return context.runtime.getStructClass();
    }

    /**
     * Retrieve the instance of the class Symbol
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass symbolClass(ThreadContext context) {
        return context.runtime.getSymbol();
    }

    /**
     * Retrieve the instance of the class Time
     * @param context the current thread context
     * @return the Class
     */
    public static RubyClass timeClass(ThreadContext context) {
        return context.runtime.getTime();
    }
}
