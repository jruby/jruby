package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import org.jruby.Ruby;

public class JRubyObjectInputStream extends ObjectInputStream {
    private final Ruby runtime;

    public JRubyObjectInputStream(Ruby runtime, InputStream input) throws IOException {
        super(input);
        this.runtime = runtime;
    }

    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        try {
            return Class.forName(name, false, runtime.getJRubyClassLoader());
        } catch (ClassNotFoundException ex) {
            return super.resolveClass(desc);
        }
    }
}