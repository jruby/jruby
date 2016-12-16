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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.truffle.parser;

public final class Identifiers {
    public static boolean isValidConstantName19(String id) {
        char c;
        int len;
        if ((len = id.length()) > 0 && (c = id.charAt(0)) <= 'Z' && c >= 'A') {
            return isNameString19(id, 1, len);
        }
        return false;
    }

    public static boolean isValidClassVariableName(String id) {
        int len;
        if ((len = id.length()) > 2 && '@' == id.charAt(0) && '@' == id.charAt(1)) {
            if (isInitialCharacter(id.charAt(2))) {
                return isNameString19(id, 3, len);
            }
        }
        return false;
    }

    public static boolean isInitialCharacter(int c) {
        return Character.isAlphabetic(c) || c == '_';
    }


    private static boolean isNameString19(String id, int start, int limit) {
        for (int i = start; i < limit; i++) {
            char c = id.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

}
