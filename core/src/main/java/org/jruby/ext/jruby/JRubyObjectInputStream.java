package org.jruby.ext.jruby;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import org.jruby.Ruby;
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

public class JRubyObjectInputStream extends RubyObject {

    JRubyObjectInputStreamImpl impl;

    public static RubyClass createJRubyObjectInputStream(ThreadContext context) {
	    return defineClass(context, "JRubyObjectInputStream", objectClass(context), JRubyObjectInputStream::new).
                defineMethods(context, JRubyObjectInputStream.class);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return newInstance(recv.getRuntime().getCurrentContext(), recv, args, block);
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
    
    @JRubyMethod(name="initialize",required=1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject wrappedStream) {
        InputStream stream = (InputStream) wrappedStream.toJava(InputStream.class);
        try {
            impl = new JRubyObjectInputStreamImpl(getRuntime(), stream);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
        return this;
    }

    @JRubyMethod(name="read_object", alias="readObject")
	public IRubyObject readObject() {
        try {
        	return Java.getInstance(getRuntime(), impl.readObject());
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw getRuntime().newNameError(cnfe.getLocalizedMessage(), cnfe.getMessage(), cnfe);
        }
    }


    @JRubyMethod(name="close")
    public IRubyObject close() {
        try {
            impl.close();
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
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
