package org.jruby.javasupport.db4o;

import com.db4o.Db4o;

import org.jruby.*;
import org.jruby.core.*;

public class RubyDb4o {
    public static void initialize(Ruby ruby) {
        Db4o.configure().objectClass("org.jruby.RubyObject").translate(new RubyObjectTranslator(ruby));
        Db4o.configure().objectClass("org.jruby.RubyString").translate(new RubyStringTranslator(ruby));
    }
}