/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.anno;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum FrameField {
    LASTLINE,
    BACKREF,
    VISIBILITY,
    BLOCK,
    SELF,
    METHODNAME,
    LINE,
    CLASS,
    FILENAME,
    SCOPE;
    
    public boolean needsFrame() {
        switch (this) {
            case LASTLINE:
            case BACKREF:
            case VISIBILITY:
            case BLOCK:
            case SELF:
            case METHODNAME:
            case CLASS:
                return true;
            default:
                return false;
        }
    }

    public static boolean needsFrame(int bits) {
        return (bits &
                (LASTLINE.bit |
                        BACKREF.bit |
                        VISIBILITY.bit |
                        BLOCK.bit |
                        SELF.bit |
                        METHODNAME.bit |
                        CLASS.bit)) != 0;
    }

    public static boolean needsScope(int bits) {
        return (bits & SCOPE.bit) != 0;
    }

    private final int bit;

    FrameField() {
        this.bit = 1 << ordinal();
    }
    
    public boolean needsScope() {
        return this == SCOPE;
    }

    public static int pack(FrameField[] frameFields) {
        int bits = 0;
        for (FrameField frameField : frameFields) {
            bits |= frameField.bit;
        }
        return bits;
    }

    @SuppressWarnings("unchecked")
    public static Set<FrameField> unpack(int bits) {
        Set<FrameField> frameFields = Collections.EMPTY_SET;
        for (FrameField frameField : values()) {
            if ((bits & frameField.bit) != 0) {
                if (frameFields == Collections.EMPTY_SET) frameFields = new HashSet<>();
                frameFields.add(frameField);
            }
        }
        return frameFields;
    }
}
