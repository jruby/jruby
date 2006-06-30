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
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.util;

import org.jruby.IRuby;

/**
 * @author enebo
 *
 */
public class IOModes {
    public static final int RDONLY = 0;
    public static final int WRONLY = 1;
    public static final int RDWR = 2;
    public static final int CREAT = 64;
    public static final int EXCL = 128;
    public static final int NOCTTY = 256;
    public static final int TRUNC = 512;
    public static final int APPEND = 1024;
    public static final int NONBLOCK = 2048;
    public static final int BINARY = 4096;
    
    private IRuby runtime;
    private int modes;
    
    public IOModes(IRuby runtime) {
    	modes = 0;
        this.runtime = runtime;
    }
    
    public Object clone() {
    	return new IOModes(runtime, modes);
    }
    
    public IOModes(IRuby runtime, String modesString) {
    	this(runtime, convertModesStringToModesInt(runtime, modesString));
    }
    
    public IOModes(IRuby runtime, long modes) {
    	// TODO: Ruby does not seem to care about invalid numeric mode values
    	// I am not sure if ruby overflows here also...
        this.modes = (int)modes;
        this.runtime = runtime;
    }
    
    public boolean isReadable() {
        return (modes & RDWR) != 0 || modes == RDONLY || modes == BINARY;
    }

    public boolean isWriteable() {
        return isWritable();
    }

    public boolean isBinary() {
        return (modes & BINARY) == BINARY;
    }

    public boolean isWritable() {
    	return (modes & RDWR) != 0 || (modes & WRONLY) != 0 || (modes & CREAT) != 0;
    }
    
    public boolean isAppendable() {
    	return (modes & APPEND) != 0;
    }

    public boolean shouldTruncate() {
    	return (modes & TRUNC) != 0;
    }

    // TODO: Make sure all open flags are added to this check.
    public void checkSubsetOf(IOModes superset) {
        if ((!superset.isReadable() && isReadable()) ||
            (!superset.isWriteable() && isWriteable()) ||
            !superset.isAppendable() && isAppendable()) {
            throw runtime.newErrnoEINVALError("bad permissions");
        }
    }
    
    // TODO: Make this more intelligible value
    public String toString() {
        return ""+modes;
    }
    
    public static int convertModesStringToModesInt(IRuby runtime, 
    		String modesString) {
    	int modes = 0;
    	
        if (modesString.length() == 0) {
            throw runtime.newArgumentError("illegal access mode");
        }

        switch (modesString.charAt(0)) {
        case 'r' :
            modes |= RDONLY;
            break;
        case 'a' :
            modes |= APPEND;
            modes |= WRONLY;
            break;
        case 'w' :
            modes |= WRONLY;
            modes |= TRUNC;
            break;
        default :
            throw runtime.newArgumentError("illegal access mode " + modes);
        }

        if (modesString.length() > 1) {
            int i = modesString.charAt(1) == 'b' ? 2 : 1;

            if (modesString.length() > i) {
                if (modesString.charAt(i) == '+') {
                	if ((modes & APPEND) != 0) {
                		modes = RDWR | APPEND;
                	} else if ((modes & WRONLY) != 0){
                		modes = RDWR | TRUNC;
                	} else {
                		modes = RDWR;
                	}
                } else {
                    throw runtime.newArgumentError("illegal access mode " + modes);
                }
            }
            if (i == 2) {
                modes |= BINARY;
            }

        }
    	return modes;
    }
}
