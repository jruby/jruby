package org.jruby.libraries;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.libraries.RubySocket.SocketMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.ObjectMetaClass;

public class SocketMetaClass extends ObjectMetaClass {

	public SocketMetaClass(IRuby runtime) {
		super("Socket", RubySocket.class, runtime.getObject());
	}
	
    public SocketMethod gethostname = new SocketMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubySocket self, IRubyObject[] args) {
        	return self.getRuntime().getNil();
        }
    };
    
    protected class SocketMeta extends Meta {
	    protected void initializeClass() {
	    	defineSingletonMethod("gethostname", Arity.noArguments());
	    	defineSingletonMethod("gethostbyname", Arity.singleArgument());
	    }
    }
    
    public RubyString gethostname() {
    	try {
			String hostName = InetAddress.getLocalHost().getHostName();
			return new RubyString(getRuntime(), hostName);
		} catch (UnknownHostException e) {
			// DSC throw SystemError("gethostname");
			return new RubyString(getRuntime(), "");
		}
    }
    
    public RubyArray gethostbyname(IRubyObject hostname) {
		try {
			RubyString name = (RubyString) hostname;
			InetAddress inetAddress = InetAddress.getByName(name.getValue());
			List parts = new ArrayList();
			parts.add(new RubyString(getRuntime(), inetAddress.getCanonicalHostName()));
			parts.add(RubyArray.newArray(getRuntime()));
			parts.add(new RubyFixnum(getRuntime(),2));
			parts.add(new RubyString(getRuntime(), RubyString.bytesToString(inetAddress.getAddress())));
			return RubyArray.newArray(getRuntime(), parts);
		} catch (UnknownHostException e) {
			// DSC throw SystemError("gethostbyname");
			return RubyArray.newArray(getRuntime());
		}
    }

    protected Meta getMeta() {
    	return new SocketMeta();
    }
}
