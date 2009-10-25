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
package org.jruby.embed;

/**
 * Predefined keys of System properties to get configurations
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public enum PropertyName {
    /**
     * A key used to get/set classpath setting.
     */
    CLASSPATH("org.jruby.embed.class.path"),

    /**
     * A key to get/set local context scope. The assigned value is one of
     * threadsafe, singlethread, or singleton
     */
    LOCALCONTEXT_SCOPE("org.jruby.embed.localcontext.scope"),

    /**
     * A key to get/set local variable behavior. The assigned value is one of
     * transient, persistent, or global for Embed Core and JSR223. BSF can choose
     * bsf only.
     */
    LOCALVARIABLE_BEHAVIOR("org.jruby.embed.localvariable.behavior"),

    /**
     * A key to get/set compile mode. The assigend value is one of jit or force.
     */
    COMPILEMODE("org.jruby.embed.compilemode"),

    /**
     * A key to get/set compatible version to Ruby. If the assigend value matches
     * "[jJ]?(r|R)(u|U)(b|B)(y|Y)1[\\._]?9", then Ruby 1.9 will be chosen.
     */
    COMPATVERSION("org.jruby.embed.compat.version");

    private final String fqpn;

    /**
     * Creates an PropertyName Enum type instance.
     *
     * @param fqan a fully qualified property name
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
     * @param fqan fully qualified property name
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
