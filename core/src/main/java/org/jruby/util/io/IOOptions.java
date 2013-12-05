/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005-2008 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
 * Copyright (C) 2006 Dave Brosius <dbrosius@mebigfatguy.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util.io;

import org.jruby.Ruby;

/**
 * Represents a combination of stream-opening flags (ModeFlags) and encoding
 * settings (EncodingOption).
 */
public class IOOptions implements Cloneable {
    private ModeFlags modeFlags;
    
    /**
     * Construct a new IOOptions object with ModeFlags and EncodingOption
     */
    public IOOptions(ModeFlags modeFlags) {
        this.modeFlags = modeFlags;
    }

    /**
     * Construct a new ModeFlags object by parsing the given string
     *
     * @param modeString the string
     * @throws InvalidValueException
     */
    public IOOptions(Ruby runtime, String modeString) throws InvalidValueException {
        int length = modeString.length();

        if (length == 0) {
            throw new InvalidValueException();
        }

        int colonIndex = modeString.indexOf(':');
        if (colonIndex == -1) {
            modeFlags = new ModeFlags(modeString);
        } else {
            modeFlags = new ModeFlags(modeString.substring(0, colonIndex));
        }
    }

    public ModeFlags getModeFlags() {
        return modeFlags;
    }

    public void setModeFlags(ModeFlags modeFlags) {
        this.modeFlags = modeFlags;
    }
    
    @Override
    public String toString() {
        // TODO: Make this more intelligible value
        return "[ModeFlags: " + modeFlags;
    }
}
