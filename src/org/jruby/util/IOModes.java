/*
 * IOModes.java
 *
 * Copyright (C) 2004 Thomas E Enebo
 * Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.ErrnoError;

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
    
    private Ruby runtime;
    private int modes;
    
    public IOModes(Ruby runtime) {
    	modes = 0;
        this.runtime = runtime;
    }
    
    public IOModes(Ruby runtime, String modesString) {
    	this(runtime, convertModesStringToModesInt(runtime, modesString));
    }
    
    public IOModes(Ruby runtime, long modes) {
    	// TODO: Ruby does not seem to care about invalid numeric mode values
    	// I am not sure if ruby overflows here also...
        this.modes = (int)modes;
        this.runtime = runtime;
    }
    
    public boolean isReadable() {
        return (modes & RDWR) != 0 || modes == RDONLY;
    }
    
    public boolean isWriteable() {
    	return (modes & RDWR) != 0 || (modes & WRONLY) != 0;
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
            throw ErrnoError.getErrnoError(runtime, "EINVAL", "bad permissions");
        }
    }
    
    // TODO: Make this more intelligible value
    public String toString() {
        return ""+modes;
    }
    
    public static int convertModesStringToModesInt(Ruby runtime, 
    		String modesString) {
    	int modes = 0;
    	
        if (modesString.length() == 0) {
            throw new ArgumentError(runtime, "illegal access mode");
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
            throw new ArgumentError(runtime, "illegal access mode " + modes);
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
                    throw new ArgumentError(runtime, "illegal access mode " + modes);
                }
            }
        }
    	return modes;
    }
}
