/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

/**
 * Predefined keys of System properties to get configurations
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public enum PropertyName {
    /**
     * A key used to get/set classpath setting. The key is equivalent to a
     * System property, org.jruby.embed.class.path.
     */
    CLASSPATH("org.jruby.embed.class.path"),

    /**
     * A key to get/set local context scope. The key is equivalent to a
     * System property, org.jruby.embed.localcontext.scope. The assigned value
     * must be one of threadsafe, singlethread, or singleton.
     */
    LOCALCONTEXT_SCOPE("org.jruby.embed.localcontext.scope"),

    /**
     * A key to get/set local variable behavior. The key is equivalent to a
     * System property, org.jruby.embed.localvariable.behavior. The assigned value
     * must be one of transient, persistent, or global for Embed Core and JSR223.
     * BSF can choose bsf only.
     */
    LOCALVARIABLE_BEHAVIOR("org.jruby.embed.localvariable.behavior"),

    /**
     * A key to get/set variables/constants retrieval policy. The key is equivalent to a
     * System property, org.jruby.embed.laziness. The assigned value
     * must be true or false. When true is given, ScriptingContainer retrieve
     * variables/constants from Ruby runtime lazily. When a variable or constant is
     * requested from user program, ScriptingContainer actually attemps to get it.
     * However, on JSR223, retrieval is done at the end of evaluation based on
     * keys listed in Bindings.
     */
    LAZINESS("org.jruby.embed.laziness"),

    /**
     * A key to get/set the value for classloader policy. The key is equivalent to a
     * System property, org.jruby.embed.classloader. The assigned value must
     * be "current" or "none." When current is set, JSR223 engine sets a current
     * classloader (the one used to initialize ScriptingContainer) to Ruby runtime.
     * When none is set, no classloader is set to Ruby runtime.
     * Default value is "none" for version 1.5.x, and "current" for 1.6.0 and later.
     *
     * This property is used only for JSR223 since ScriptingContainer users can
     * set any classloader explicitely.
     */
    CLASSLOADER("org.jruby.embed.classloader"),

    /**
     * A key to get/set compile mode. The key is equivalent to a
     * System property, org.jruby.embed.compilemode. The assigned value must be jit or force.
     */
    COMPILEMODE("org.jruby.embed.compilemode"),

    /**
     * A key to get/set compatible version to Ruby. The key is equivalent to a
     * System property, org.jruby.embed.compat.version. If the assigned value matches
     * j?ruby1[\\._]?9, then Ruby 1.9 mode will be used to evaluate a given code.
     */
    COMPATVERSION("org.jruby.embed.compat.version");

    private final String fqpn;

    /**
     * Creates an PropertyName Enum type instance.
     *
     * @param fqpn a fully qualified property name
     */
    PropertyName(String fqpn) {
        this.fqpn = fqpn;
    }

    /**
     * Returns the fully qualified property name of this enum constant.
     *
     * @return a fully qualified property name
     */
    @Override
    public String toString() {
        return fqpn;
    }

    /**
     * Returns a fully qualified property name that corresponds to a given
     * enumerated type identifier.
     *
     * @param fqpn fully qualified property name
     * @return a matched enumerated type identifier
     */
    public static PropertyName getType(String fqpn) {
        PropertyName[] names = PropertyName.values();
        for (int i=0; i<names.length; i++) {
            if (fqpn.equals(names[i].toString())) {
                return names[i];
            }
        }
        return null;
    }
}
