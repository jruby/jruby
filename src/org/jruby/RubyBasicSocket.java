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
 * Copyright (C) 2006 Evan <evan@heron.sytes.net>
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandlerSocket;
import org.jruby.util.IOHandler;

public class RubyBasicSocket extends RubyIO {

    public RubyBasicSocket(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    public IRubyObject initialize(IRubyObject[] args) {
        IRubyObject input 	= args[0];
        IRubyObject output 	= args[1];
        
        InputStream inputStream = (InputStream) extractStream(input);
        OutputStream outputStream = (OutputStream) extractStream(output);
        try {
            handler = new IOHandlerSocket(getRuntime(), inputStream, outputStream);
    	} catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        registerIOHandler(handler);
        modes = handler.getModes();
        
        return this;
    }
    
    private Object extractStream(IRubyObject proxyObject) {
        IRubyObject javaObject = proxyObject.getInstanceVariable("@java_object");
        return ((JavaObject)javaObject).getValue();
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
}
