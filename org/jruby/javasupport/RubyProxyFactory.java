package org.jruby.javasupport;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import org.jruby.*;
import org.jruby.util.*;

/**
 * <p> A RubyProxyFactory dispenses RubyProxy objects, which provide a
 * convenient, dynamic wrapper around Ruby objects.  A
 * RubyProxyFactory must be initialized with a Ruby instance,
 * presumably one which has been loaded with classes which you want to
 * instantiate from Java, and an object implementing the
 * RubyToJavaClassMap interface, which provides a way to map between
 * Ruby and Java classes while converting arguments to and from method
 * calls. </p>
 *
 * <p> The API is composed of three primary method calls:
 * newProxyObject, which creates a new RubyObject and returns a
 * wrapper around it, getProxyForObject, which takes an existing
 * RubyObject and returns a wrapper around it, and getProxyForGlobal,
 * which finds the RubyObject stored in a Ruby global variable
 * ($variable) and returns a wrapper around it.  </p>
 **/
public class RubyProxyFactory {
    private Ruby               ruby       = null;
    private RubyToJavaClassMap classMap   = null;
    private RubyConversion     conversion = null;

    public RubyProxyFactory (Ruby ruby, RubyToJavaClassMap classMap)
    {
        this.ruby = ruby;
        this.classMap = classMap;
        this.conversion = new RubyConversion(this);
    }

    public Ruby getRuby ()
    {
        return ruby;
    }

    /**
     * Create a new instance of the RubyClass corresponding to the
     * specified Java interface.  Return a Java object that conforms
     * to this Java interface, but implemented by the RubyClass.
     **/
    public RubyProxy newProxyObject (Class javaInterface)
    {
        return newProxyObject(javaInterface, new Object[0]);
    }

    public RubyProxy newProxyObject (Class javaInterface, Object[] args)
    {
        RubyClass rubyClass = getRubyClassForJavaClass(javaInterface);
        RubyObject[] rubyArgs = convertJavaToRuby(args);
        RubyObject obj = rubyClass.newInstance(rubyArgs);

        if (obj != null)
            return getProxyForObject(obj, javaInterface);
        else
            return null;
    }

    public RubyProxy getProxyForGlobal (String globalVar)
    {
        return getProxyForGlobal(globalVar, null);
    }

    public RubyProxy getProxyForGlobal (String globalVar, Class javaInterface)
    {
        RubyObject obj = getRuby().getGlobalVar(globalVar);

        if (obj != null && !obj.isNil())
          return getProxyForObject(obj, javaInterface);

        return null;
    }

    /**
     * Create a wrapper around the specified RubyObject.  Any method
     * calls will be forwarded to the implementation of the RubyObject
     * and any returned objects will be converted to their
     * corresponding Java objects.
     **/
    public RubyProxy getProxyForObject (RubyObject obj)
    {
        return getProxyForObject(obj, null);
    }

    public RubyProxy getProxyForObject (RubyObject obj, Class javaInterface)
    {
        if (javaInterface == null)
            javaInterface = getJavaClassForRubyClass(obj.getRubyClass());

        return (RubyProxy)Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {javaInterface, RubyProxy.class},
            new RubyInvocationHandler(this, obj));
    }

        /*****************************************************
                Helper methods for RubyToJavaClassMap
         *****************************************************/

    protected RubyClass getRubyClassForJavaClass (Class javaClass)
    {
        String className = classMap.getRubyClassNameForJavaClass(javaClass);
        return getRuby().getRubyClass(className);
    }

    protected Class getJavaClassForRubyClass (RubyClass rubyClass)
    {
      return classMap.getJavaClassForRubyClass(rubyClass);
    }

        /*****************************************************
                  Helper methods for RubyConversion
                   (used by RubyInvocationHandler)
         *****************************************************/

    public RubyObject[] convertJavaToRuby (Object[] obj)
    {
        return conversion.convertJavaToRuby(obj);
    }

    public Object convertRubyToJava (RubyObject obj, Class type)
    {
        return conversion.convertRubyToJava(obj, type);
    }
}

