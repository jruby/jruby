package org.jruby.javasupport;

import java.lang.reflect.Proxy;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;

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
 *
 * @version $Revision$ 
 * @author schwardo
 */
public class RubyProxyFactory {
    private Ruby ruby = null;
    private RubyToJavaClassMap classMap = null;
    private RubyConversion conversion = null;

    public RubyProxyFactory(Ruby ruby, RubyToJavaClassMap classMap) {
        this.ruby = ruby;
        this.classMap = classMap;
        this.conversion = new RubyConversion(this);
    }

    public Ruby getRuby() {
        return ruby;
    }

    /**
     * Create a new instance of the RubyClass corresponding to the
     * specified Java interface.  Return a Java object that conforms
     * to this Java interface, but implemented by the RubyClass.
     **/
    public RubyProxy newProxyObject(Class javaInterface) {
        return newProxyObject(javaInterface, new Object[0]);
    }

    public RubyProxy newProxyObject(Class javaInterface, Object[] args) {
        RubyClass rubyClass = getRubyClassForJavaClass(javaInterface);
        IRubyObject[] rubyArgs = convertJavaToRuby(args);
        IRubyObject obj = rubyClass.newInstance(rubyArgs);

        if (obj != null)
            return getProxyForObject(obj, javaInterface);
        else
            return null;
    }

    public RubyProxy getProxyForGlobal(String globalVar) {
        return getProxyForGlobal(globalVar, null);
    }

    public RubyProxy getProxyForGlobal(String globalVar, Class javaInterface) {
        IRubyObject obj = getRuby().getGlobalVariables().get(globalVar);

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
    public RubyProxy getProxyForObject(IRubyObject obj) {
        return getProxyForObject(obj, null);
    }

    public RubyProxy getProxyForObject(IRubyObject obj, Class javaInterface) {
        if (javaInterface == null)
            javaInterface = getJavaClassForRubyClass(obj.getInternalClass());

        return (RubyProxy) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { javaInterface, RubyProxy.class },
            new RubyInvocationHandler(this, obj));
    }

    /*****************************************************
            Helper methods for RubyToJavaClassMap
     *****************************************************/

    protected RubyClass getRubyClassForJavaClass(Class javaClass) {
        String className = classMap.getRubyClassNameForJavaClass(javaClass);
        return getRuby().getClass(className);
    }

    protected Class getJavaClassForRubyClass(RubyClass rubyClass) {
        return classMap.getJavaClassForRubyClass(rubyClass);
    }

    /*****************************************************
              Helper methods for RubyConversion
               (used by RubyInvocationHandler)
     *****************************************************/

    public IRubyObject[] convertJavaToRuby(Object[] obj) {
        return conversion.convertJavaToRuby(obj);
    }

    public Object convertRubyToJava(IRubyObject obj, Class type) {
        return conversion.convertRubyToJava(obj, type);
    }
}

