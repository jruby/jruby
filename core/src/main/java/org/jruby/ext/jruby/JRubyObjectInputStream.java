package org.jruby.ext.jruby;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.Visibility;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.nameError;

public class JRubyObjectInputStream extends RubyObject {

    JRubyObjectInputStreamImpl impl;

    public static RubyClass createJRubyObjectInputStream(ThreadContext context) {
	    return defineClass(context, "JRubyObjectInputStream", objectClass(context), JRubyObjectInputStream::new).
                defineMethods(context, JRubyObjectInputStream.class);
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return newInstance(((RubyBasicObject) recv).getCurrentContext(), recv, args, block);
    }

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject obj = ((RubyClass)recv).allocate(context);
        obj.callMethod(context, "initialize", args, block);
        return obj;
    }


    public JRubyObjectInputStream(Ruby runtime, RubyClass rubyClass) {
	    super(runtime,rubyClass);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject initialize(IRubyObject wrappedStream) {
        return initialize(getCurrentContext(), wrappedStream);
    }
    
    @JRubyMethod(name="initialize",required=1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject wrappedStream) {
        InputStream stream = wrappedStream.toJava(InputStream.class);
        try {
            impl = new JRubyObjectInputStreamImpl(context.runtime, stream);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
        return this;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject readObject() {
        return readObject(getCurrentContext());
    }

    @JRubyMethod(name="read_object", alias="readObject")
    public IRubyObject readObject(ThreadContext context) {
        try {
        	return Java.getInstance(context.runtime, impl.readObject());
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw nameError(context, cnfe.getLocalizedMessage(), cnfe.getMessage(), cnfe);
        }
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject close() {
        return close(getCurrentContext());
    }

    @JRubyMethod(name="close")
    public IRubyObject close(ThreadContext context) {
        try {
            impl.close();
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
        return this;
    }

    static class JRubyObjectInputStreamImpl extends ObjectInputStream {
        protected final Ruby runtime;

        public JRubyObjectInputStreamImpl(Ruby runtime, InputStream in) throws IOException {
            super(in);
            this.runtime = runtime;
        }
        @Override
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return Class.forName(desc.getName(), true, runtime.getJRubyClassLoader());
        }
    }
}
