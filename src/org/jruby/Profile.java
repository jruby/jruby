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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public interface Profile {
    Profile ALL = new Profile() {
            public boolean allowBuiltin(String name) { return true; }
            public boolean allowClass(String name) { return true; }
            public boolean allowModule(String name) { return true; }
            public boolean allowLoad(String name) { return true; }
            public boolean allowRequire(String name) { return true; }
        };
    Profile DEBUG_ALLOW = new Profile() {
            public boolean allowBuiltin(String name) { System.err.println("allowBuiltin("+name+")"); return true; }
            public boolean allowClass(String name) { System.err.println("allowClass("+name+")"); return true; }
            public boolean allowModule(String name) { System.err.println("allowModule("+name+")"); return true; }
            public boolean allowLoad(String name) { System.err.println("allowLoad("+name+")"); return true; }
            public boolean allowRequire(String name) { System.err.println("allowRequire("+name+")"); return true; }
        };
    Profile NO_FILE_CLASS = new Profile() {
            public boolean allowBuiltin(String name) { return true; }
            public boolean allowClass(String name) { return !(name.equals("File") || name.equals("FileStat")); }
            public boolean allowModule(String name) { return true; }
            public boolean allowLoad(String name) { return true; }
            public boolean allowRequire(String name) { return true; }
        };
    Profile ANY = ALL;
    Profile DEFAULT = ALL;
    
    boolean allowBuiltin(String name);
    boolean allowClass(String name);
    boolean allowModule(String name);
    boolean allowLoad(String name);
    boolean allowRequire(String name);
}// Profile
