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
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.javasupport;

import org.jruby.RubyClass;

public class ReflectionClassMap implements RubyToJavaClassMap {
    private String javaPackage = null;

    public ReflectionClassMap (String pkgName)
    {
        this.javaPackage = pkgName;
    }

    public ReflectionClassMap (Package pkg)
    {
        this.javaPackage = pkg.getName();
    }

    public String getRubyClassNameForJavaClass (Class javaClass)
    {
        String name = javaClass.getName();

        if (name.lastIndexOf(".") >= 0) {
			name = name.substring(name.lastIndexOf(".") + 1);
		}
        return name;
    }

    public Class getJavaClassForRubyClass(RubyClass rubyClass) {
        while (rubyClass != null) {
            try {
                // TODO: This should get real class name of the class implementing
                // the name.
                String rubyClassName = rubyClass.getName();
                String javaClassName = javaPackage + "." + rubyClassName;
                return Class.forName(javaClassName);
            } catch (ClassNotFoundException ex) {
            }

            if (rubyClass.superclass().isNil()) {
                break;
            }
            rubyClass = (RubyClass) rubyClass.superclass();

            if (rubyClass != null && rubyClass.getName() == null) {
                break;
            }
        }
        return null;
    }
}

