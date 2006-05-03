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
import java.net.Socket;

import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandlerSocket;
import org.jruby.util.IOHandler;

public class RubyBasicSocket extends RubyIO {
	private static final int SHUTDOWN_RECEIVER = 1;
	private static final int SHUTDOWN_SENDER = 2;
	private static final int SHUTDOWN_BOTH = SHUTDOWN_RECEIVER | SHUTDOWN_SENDER;
	
    public static boolean do_not_reverse_lookup = false;
    private Socket socket;
    
    public RubyBasicSocket(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    public IRubyObject initialize(IRubyObject arg) {
        socket = extractSocket(arg);
        
        try {
            handler = new IOHandlerSocket(getRuntime(), socket.getInputStream(), socket.getOutputStream());
    	} catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        registerIOHandler(handler);
        modes = handler.getModes();
        
        return this;
    }
    
    private Socket extractSocket(IRubyObject proxyObject) {
        return (Socket) ((JavaObject) proxyObject.getInstanceVariable("@java_object")).getValue();
    }

    public IRubyObject write_send(IRubyObject[] args) {
        return syswrite(args[0]);
    }
    
    public IRubyObject recv(IRubyObject[] args) {
        try {
            return getRuntime().newString(((IOHandlerSocket) handler).recv(RubyNumeric.fix2int(args[0])));
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
        return JavaObject.wrap(getRuntime(), socket.getLocalSocketAddress());
    }

    public IRubyObject getpeername() {
        return JavaObject.wrap(getRuntime(), socket.getRemoteSocketAddress());
    }

    public IRubyObject shutdown(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && tainted().isFalse()) {
            throw getRuntime().newSecurityError("Insecure: can't shutdown socket");
        }
        int how = args.length > 0 ? RubyNumeric.fix2int(args[0]) : SHUTDOWN_BOTH; 

        if (how != SHUTDOWN_RECEIVER && how != SHUTDOWN_SENDER && how != SHUTDOWN_BOTH) {
            throw getRuntime().newArgumentError("`how' should be either 0, 1, 2");
        }
        if (how != SHUTDOWN_BOTH) {
            throw getRuntime().newNotImplementedError("Shutdown currently only works with how=2");
        }
        return close();
    }
}
