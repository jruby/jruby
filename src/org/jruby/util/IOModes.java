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
    private Ruby runtime;
    private String modes;
    private boolean isReadable = false;
    private boolean isWriteable = false;
    private boolean isAppendable = false;
    
    public IOModes(Ruby runtime) {
        modes = "";
        this.runtime = runtime;
    }
    
    public IOModes(Ruby runtime, String modes) {
        this.modes = modes;
        this.runtime = runtime;
        
        if (modes.length() == 0) {
            throw new ArgumentError(runtime, "illegal access mode");
        }

        switch (modes.charAt(0)) {
        case 'r' :
            isReadable = true;
            break;
        case 'a' :
            isAppendable = true;
        case 'w' :
            isWriteable = true;
            break;
        default :
            throw new ArgumentError(runtime, "illegal access mode " + modes);
        }

        if (modes.length() > 1) {
            int i = modes.charAt(1) == 'b' ? 2 : 1;

            if (modes.length() > i) {
                if (modes.charAt(i) == '+') {
                    isReadable = true;
                    isWriteable = true;
                } else {
                    throw new ArgumentError(runtime, "illegal access mode " + modes);
                }
            }
        }
    }

    public boolean isReadable() {
        return isReadable;
    }
    
    public boolean isWriteable() {
        return isWriteable;
    }
    
    public boolean isAppendable() {
        return isAppendable;
    }
    
    public void checkSubsetOf(IOModes superset) {
        if ((superset.isReadable() == false && isReadable()) ||
            (superset.isWriteable() == false && isWriteable()) ||
            (superset.isAppendable() == false) && isAppendable()) {
            throw ErrnoError.getErrnoError(runtime, "EINVAL", "bad permissions");
        }
    }
    
    public String getModeString() {
        return modes;
    }
}
