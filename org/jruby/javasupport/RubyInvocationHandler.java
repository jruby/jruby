package org.jruby.javasupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.jruby.*;
import org.jruby.util.*;
import org.jruby.javasupport.*;

/**
 * A RubyInvocationHandler intercepts method calls to Proxy objects
 * and forwards them on to the corresponding RubyObject.  Arguments
 * passed to and sent from the method are translated between Java and
 * Ruby objects.  A new RubyInvocationHandler instance is created for
 * each Proxy object created by RubyProxyFactory.
 **/
public class RubyInvocationHandler
    implements InvocationHandler
{
    /**
     * Store a reference to the RubyProxyFactory that created this
     * invocation handler for use in converting arguments that are passed
     * to or returned from Ruby methods.
     **/
    private RubyProxyFactory factory = null;

    /**
     * Store a reference to the RubyObject that we should forward method
     * requests to.
     **/
    private RubyObject rubyObject = null;

    /**
     * Construct a RubyInvocationHandler instance.  This should only
     * be accessible RubyProxyFactory so it is package-scoped.
     **/
    RubyInvocationHandler (RubyProxyFactory factory, RubyObject obj)
    {
        this.factory = factory;
        this.rubyObject = obj;
    }

    /**
     * The invoke method is called by Java's reflection whenever a
     * method is called on a Proxy object.  This handler will forward
     * the method call on to the corresponding RubyObject, converting
     * arguments passed to and returned from the method, and will
     * return the end result.
     **/
    public Object invoke(Object proxy, Method method, Object[] args) 
    {
        String methodName = method.getName();
        RubyString rubyMethodName = new RubyString(getRuby(), methodName);
        RubyObject[] rubyArgs = factory.convertJavaToRuby(args);

        RubyObject out = null;

        // 'get' and 'set' methods (as well as 'is' and 'has') are already
        // handled by JavaSupport.toRubyName.  Should we be using this?

        if (methodName.equals("getRubyObject")) {
            return getRubyObject();
        } else if (methodName.equals("getRubyProxyFactory")) {
            return getRubyProxyFactory();
        } else if (respondsTo(rubyObject, methodName)) {
            out = rubyObject.send(rubyMethodName, rubyArgs);
        } else if (methodName.equals("toString")) {
            out = rubyObject.funcall("to_s");
        } else if (methodName.equals("equals")) {
            out = rubyObject.funcall("equal", rubyArgs); // ?
        } else if (methodName.indexOf("get") == 0) {
            String fieldName = Character.toLowerCase(methodName.charAt(3))
                + methodName.substring(4);
            rubyMethodName = new RubyString(getRuby(), fieldName);

            out = rubyObject.send(rubyMethodName, rubyArgs);
        } else if (methodName.indexOf("set") == 0) {
            String fieldName = Character.toLowerCase(methodName.charAt(3))
                + methodName.substring(4) + "=";
            rubyMethodName = new RubyString(getRuby(), fieldName);
            
            out = rubyObject.send(rubyMethodName, rubyArgs);
        } else {
            throw new RuntimeException("method " + rubyMethodName + " not found.");
        }

        return factory.convertRubyToJava(out, method.getReturnType());
    }

    /** Get the Ruby instance that should be used. */
    protected Ruby getRuby ()
    {
        return factory.getRuby();
    }

    /** Get the RubyProxyFactory instance that created this handler. */
    protected RubyProxyFactory getRubyProxyFactory ()
    {
        return factory;
    }

    /** Get the RubyObject that we should forward method calls to. */
    protected RubyObject getRubyObject ()
    {
        return rubyObject;
    }

    /** Simple helper method for RubyObject.respond_to(RubySymbol).  */
    protected boolean respondsTo (RubyObject obj, String methodName)
    {
        return obj.respondsTo(methodName);
    }
}

