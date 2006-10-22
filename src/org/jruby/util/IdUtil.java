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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

public final class IdUtil {
    public static final int CONSTANT = 0;
    public static final int INSTANCE_VAR = 1;
    public static final int CLASS_VAR = 2;
    public static final int GLOBAL_VAR = 4;
    public static final int LOCAL_VAR = 8;
    
    /**
     * Get type of variable based on Ruby naming conventions.  This is useful when you know
     * you are going to want to know what type it is.  It should in theory be cheaper than
     * calling all the isFoo methods seperately.  It also should be faster than isLocal.
     * 
     * @param id the name to determine its type from
     * @return value representing the type.
     */
    public static int getVarType(String id) {
        char c = id.charAt(0);
        
        switch (c) {
        case '@':
            return id.charAt(1) == '@' ? CLASS_VAR : INSTANCE_VAR;
        case '$':
            return GLOBAL_VAR;
        }
        
        return Character.isUpperCase(c) ? CONSTANT : LOCAL_VAR;
    }
    
	public static boolean isConstant(String id) {
	    return Character.isUpperCase(id.charAt(0));
    }

	public static boolean isClassVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) == '@';
    }

	public static boolean isInstanceVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) != '@';
    }
    
    public static boolean isGlobal(String id) {
        return id.charAt(0) == '$';
    }
    
	public static boolean isLocal(String id) {
	    return !isGlobal(id) && !isClassVariable(id) && !isInstanceVariable(id) && !isConstant(id);
    }

	public static boolean isAttrSet(String id) {
	    return id.endsWith("=");
	}
}
