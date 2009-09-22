package org.jruby.util.io;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Visibility;
import org.jruby.javasupport.JavaUtil;

public class JRubyObjectInputStream extends RubyObject {
    JRubyObjectInputStreamImpl impl;
    private static final ObjectAllocator JROIS_ALLOCATOR = new ObjectAllocator() {
	    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
		return new JRubyObjectInputStream(runtime, klass);
        }
    };
    public static RubyClass createJRubyObjectInputStream(Ruby runtime) {
	RubyClass result = runtime.defineClass("JRubyObjectInputStream",runtime.getObject(),JROIS_ALLOCATOR);
	result.defineAnnotatedMethods(JRubyObjectInputStream.class);
	return result;
    }

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject obj = ((RubyClass)recv).allocate();
        obj.callMethod(recv.getRuntime().getCurrentContext(), "initialize", args, block);
        return obj;
    }


    public JRubyObjectInputStream(Ruby runtime, RubyClass rubyClass) {
	super(runtime,rubyClass);
    }
    
    @JRubyMethod(name="initialize",required=1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject wrappedStream) throws IOException {
	InputStream stream = (InputStream)wrappedStream.toJava(InputStream.class);
	impl = new JRubyObjectInputStreamImpl(getRuntime(),stream);
	return this;
    }

    @JRubyMethod(name="read_object", alias="readObject")
	public IRubyObject readObject() throws IOException, ClassNotFoundException {
	return JavaObject.wrap(getRuntime(),impl.readObject());
    }


    @JRubyMethod(name="close")
    public IRubyObject close() throws IOException {
	impl.close();
	return this;
    }

    class JRubyObjectInputStreamImpl extends ObjectInputStream {
        protected Ruby runtime;

        public JRubyObjectInputStreamImpl(Ruby rt,InputStream in) throws IOException {
            super(in);
            runtime = rt;
        }
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return Class.forName(desc.getName(),true,runtime.getJRubyClassLoader());
        }
    }
}
