package org.jruby.javasupport.db4o;

import com.db4o.Db4o;

import org.jruby.*;
import org.jruby.core.*;

public class RubyDb4o {
    public static RubyModule createDb4oModule(Ruby ruby) {
        RubyCallbackMethod initialize = new ReflectionCallbackMethod(RubyDb4o.class, "initialize", RubyObject.class, false, true);

        RubyModule db4oModule = ruby.defineModule("RubyDb4o");

        db4oModule.defineModuleFunction("initialize", initialize);

        return db4oModule;
    }
    
    public static RubyObject initialize(Ruby ruby, RubyObject recv, RubyObject arg) {
        Db4o.configure().objectClass("org.jruby.RubyObject").translate(new RubyObjectTranslator(ruby));
        Db4o.configure().objectClass("org.jruby.RubyString").translate(new RubyStringTranslator(ruby));
        
        return recv;
    }
}