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
import org.jruby.runtime.builtin.meta.BasicSocketMetaClass;

public class SocketMetaClass extends BasicSocketMetaClass {

	public SocketMetaClass(IRuby runtime) {
		super("Socket", RubySocket.class, runtime.getClass("BasicSocket"));
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
			return getRuntime().newString(hostName);
		} catch (UnknownHostException e) {
			// DSC throw SystemError("gethostname");
			return getRuntime().newString("");
		}
    }
    
    public RubyArray gethostbyname(IRubyObject hostname) {
		try {
			RubyString name = (RubyString) hostname;
			InetAddress inetAddress = InetAddress.getByName(name.toString());
			List parts = new ArrayList();
			parts.add(getRuntime().newString(inetAddress.getCanonicalHostName()));
			parts.add(RubyArray.newArray(getRuntime()));
			parts.add(new RubyFixnum(getRuntime(),2));
			parts.add(getRuntime().newString(RubyString.bytesToString(inetAddress.getAddress())));
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
