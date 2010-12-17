package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.text.DecimalFormat;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyClass;
import org.jruby.MetaClass;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

public class AbstractProfilePrinter {
    public void printProfile(PrintStream out) {
    }
    
    public void printProfile(RubyIO out) {
        printProfile(new PrintStream(out.getOutStream()));
    }
    
    protected void pad(PrintStream out, int size, String body) {
        pad(out, size, body, true);
    }

    protected void pad(PrintStream out, int size, String body, boolean front) {
        if (front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
        out.print(body);
        if (!front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
    }

    protected String nanoString(long nanoTime) {
        DecimalFormat formatter = new DecimalFormat("##0.00");
        return formatter.format((double) nanoTime / 1.0E9);
    }

    protected String methodName(int serial) {
        if (serial == 0) {
            return "(top)";
        }
        Ruby runtime = Ruby.getGlobalRuntime();
        String[] profiledNames = runtime.profiledNames;
        DynamicMethod[] profiledMethods = runtime.profiledMethods;
        String name = profiledNames[serial];
        DynamicMethod method = profiledMethods[serial];
        return moduleHashMethod(method.getImplementationClass(), name);
    }
    
    protected String moduleHashMethod(RubyModule module, String name) {
        if (module instanceof MetaClass) {
            IRubyObject obj = ((MetaClass) module).getAttached();
            if (obj instanceof RubyModule) {
                module = (RubyModule) obj;
                return module.getName() + "." + name;
            }
            else if (obj instanceof RubyObject) {
                return ((RubyObject) obj).getType().getName() + "(singleton)#" + name;
            }
            else {
                return "unknown#" + name;
            }
        } else if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        } else {
            return module.getName() + "#" + name;
        }
    }
    
}