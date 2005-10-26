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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.runtime;

import java.io.Serializable;

import org.jruby.util.IdUtil;

/**
 * This class represents two concepts: method visibility and a mask for 
 * determining a set of valid method visibilities.  The first concept can only
 * be a single value: PUBLIC, PRIVATE, PROTECTED, and MODULE_FUNCTION (see 
 * RubyModule#module_function).  It is used to adorn a method with a _SINGLE_ 
 * visibility.  Some functions (see RubyModule#instance_methods) want to be 
 * able to see methods of multiple visibilities.  The second concept allows 
 * making a vibility which is basically a mask (see 
 * Visibility.PUBLIC_PROTECTED).  The method 'is' can then be used to see if a
 * method visibility is in the mask.
 *  
 * @author jpetersen
 */
public final class Visibility implements Serializable {
	private static final short PUBLIC_VALUE = (short) 1;
	private static final short PROTECTED_VALUE = (short) 2;
	private static final short PRIVATE_VALUE = (short) 4;
	private static final short MODULE_FUNCTION_VALUE = (short) 8;
    public static final Visibility PUBLIC = new Visibility(PUBLIC_VALUE);
    public static final Visibility PROTECTED = new Visibility(PROTECTED_VALUE);
    public static final Visibility PRIVATE = new Visibility(PRIVATE_VALUE);
    public static final Visibility MODULE_FUNCTION = new Visibility(MODULE_FUNCTION_VALUE);
    public static final Visibility PUBLIC_PROTECTED = 
    	new Visibility((short) (PUBLIC_VALUE | PROTECTED_VALUE));

    private final short restore;

    static final long serialVersionUID = 2002102900L;

    /**
     * Constructor for MethodScope.
     */
    private Visibility(short restore) {
        this.restore = restore;
    }

    public boolean isPublic() {
        return (restore & PUBLIC_VALUE) != 0;
    }

    public boolean isProtected() {
        return (restore & PROTECTED_VALUE) != 0;
    }

    public boolean isPrivate() {
        return (restore & PRIVATE_VALUE) != 0;
    }
    
    public boolean isModuleFunction() {
        return (restore & MODULE_FUNCTION_VALUE) != 0;
    }
    
    public boolean is(Visibility other) {
    	return (restore & other.restore) != 0;
    }

    public String toString() {
    	// XXXEnebo: will only print out actual visibilities
    	// and not masks
        switch (restore) {
            case 1:
                return "public";
            case 2:
                return "protected";
            case 4:
                return "private";
            case 8:
                return "module_function";
            default:
                return "mixed mask: " + restore;
        }
    }
    
    public String errorMessageFormat(CallType callType, String name) {
        String format = "undefined method `%s' for %s%s%s";
        if (callType ==  CallType.VARIABLE) {
            if (IdUtil.isLocal(name)) {
                format = "undefined local variable or method '%s' for %s%s%s";
            }
        } else if (this == Visibility.PRIVATE && callType == CallType.NORMAL) {
            format = "private method '%s' called for %s%s%s";
        } else if (this == Visibility.PROTECTED) {
            format = "protected method '%s' called for %s%s%s";
        } else {
        }
        
        return format;
    }
}
