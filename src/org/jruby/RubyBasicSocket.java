/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Channel;

import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandlerNio;
import org.jruby.util.IOHandler;

public class RubyBasicSocket extends RubyIO {
    private Channel socketChannel;
    
    public RubyBasicSocket(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    public IRubyObject initialize(IRubyObject arg) {
        socketChannel = extractSocketChannel(arg);
        
        try {
            handler = new IOHandlerNio(getRuntime(), socketChannel);
            handler.setIsSync(true);
    	} catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        registerIOHandler(handler);
        modes = handler.getModes();
        
        return this;
    }
    
    private Channel extractSocketChannel(IRubyObject proxyObject) {
        IRubyObject javaObject = proxyObject.getInstanceVariable("@java_object");
        return (Channel) ((JavaObject) javaObject).getValue();
    }

    public IRubyObject write_send(IRubyObject[] args) {
        return syswrite(args[0]);
    }
    
    public IRubyObject recv(IRubyObject[] args) {
        try {
            return getRuntime().newString(((IOHandlerNio) handler).recv(RubyNumeric.fix2int(args[0])));
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            // recv returns nil on EOF
            return getRuntime().getNil();
    	} catch (IOException e) {
            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...
            if ("Socket not open".equals(e.getMessage())) {
	            throw getRuntime().newIOError(e.getMessage());
            }
    	    throw getRuntime().newSystemCallError(e.getMessage());
    	}
    }

    public IRubyObject getsockname() {
        if (socketChannel instanceof SocketChannel) {
            return JavaObject.wrap(getRuntime(), ((SocketChannel) socketChannel).socket().getLocalSocketAddress());
        } else if (socketChannel instanceof ServerSocketChannel) {
            return JavaObject.wrap(getRuntime(), ((ServerSocketChannel) socketChannel).socket().getLocalSocketAddress());
        } else {
            throw getRuntime().newIOError("Not Supported");
        }
    }

    public IRubyObject getpeername() {
        if (socketChannel instanceof SocketChannel) {
            return JavaObject.wrap(getRuntime(), ((SocketChannel) socketChannel).socket().getRemoteSocketAddress());
        } else {
            throw getRuntime().newIOError("Not Supported");
        }
    }

    public IRubyObject shutdown(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && tainted().isFalse()) {
            throw getRuntime().newSecurityError("Insecure: can't shutdown socket");
        }
        
        int how = 2;
        if (args.length > 0) {
            how = RubyNumeric.fix2int(args[0]);
        }
        if (how < 0 || 2 < how) {
            throw getRuntime().newArgumentError("`how' should be either 0, 1, 2");
        }
        if (how != 2) {
            throw getRuntime().newNotImplementedError("Shutdown currently only works with how=2");
        }
        return close();
    }
}
