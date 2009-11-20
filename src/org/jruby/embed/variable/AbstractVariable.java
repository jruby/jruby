/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
abstract class AbstractVariable implements BiVariable {
    protected String name;
    protected Object javaObject = null;
    protected Class javaType = null;
    protected IRubyObject irubyObject = null;

    protected AbstractVariable(Ruby runtime, String name, Object... values) {
        this.name = name;
        updateJavaObject(runtime, values[0]);
        if (values.length > 1) {
            javaType = (Class) values[1];
        } else {
            // Setting null is possible. It will be convered to RubyNil.
            // But, can't get Java type from null.
            if (javaObject != null) {
                javaType = values[0].getClass();
            }
        }
    }

    protected void updateJavaObject(Ruby runtime, Object javaObject) {
        this.javaObject = javaObject;
        this.irubyObject = JavaEmbedUtils.javaToRuby(runtime, javaObject);
    }

    protected AbstractVariable(String name, IRubyObject rubyObject) {
        this.name = name;
        updateRubyObject(rubyObject);
    }

    protected void updateRubyObject(IRubyObject rubyObject) {
        if (rubyObject == null) {
            return;
        }
        this.irubyObject = rubyObject;
        Ruby rt = rubyObject.getRuntime();
        if (javaType != null) {
            // Java originated varibales
            this.javaObject = javaType.cast(JavaEmbedUtils.rubyToJava(rt, rubyObject, javaType));
        } else {
            // Ruby originated variables
            this.javaObject = JavaEmbedUtils.rubyToJava(rubyObject);
            if (this.javaObject != null) {
                this.javaType = this.javaObject.getClass();
            }
        }
    }

    public String getName() {
        return name;
    }

    public Object getJavaObject() {
        return javaObject;
    }

    public void setJavaObject(Ruby runtime, Object javaObject) {
        updateJavaObject(runtime, javaObject);
    }

    public IRubyObject getRubyObject() {
        return irubyObject;
    }

    public void setRubyObject(IRubyObject rubyObject) {
        updateRubyObject(rubyObject);
    }

    protected RubyModule getRubyClass(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        StaticScope scope = context.getCurrentScope().getStaticScope();
        RubyModule rubyClass = scope.getModule();
        return rubyClass;
    }

    protected static boolean isValidName(String pattern, Object name) {
        if (!(name instanceof String)) {
            return false;
        }
        if (((String)name).matches(pattern)) {
            return true;
        } else {
            return false;
        }
    }
}
