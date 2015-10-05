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
package org.jruby.embed.internal;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public abstract class AbstractLocalContextProvider implements LocalContextProvider {

    protected final RubyInstanceConfig config;
    protected final LocalVariableBehavior behavior;
    protected boolean lazy = true;

    protected AbstractLocalContextProvider() {
        this( new RubyInstanceConfig() );
    }

    protected AbstractLocalContextProvider(RubyInstanceConfig config) {
        this.config = config; this.behavior = LocalVariableBehavior.TRANSIENT;
    }

    protected AbstractLocalContextProvider(RubyInstanceConfig config, LocalVariableBehavior behavior) {
        this.config = config; this.behavior = behavior;
    }

    protected AbstractLocalContextProvider(LocalVariableBehavior behavior) {
        this.config = new RubyInstanceConfig(); this.behavior = behavior;
    }

    protected LocalContext getInstance() {
        return new LocalContext(config, behavior, lazy);
    }

    @Override
    public RubyInstanceConfig getRubyInstanceConfig() {
        return config;
    }

    @Override
    public LocalVariableBehavior getLocalVariableBehavior() {
        return behavior;
    }

    boolean isGlobalRuntimeReady() { return Ruby.isGlobalRuntimeReady(); }

    Ruby getGlobalRuntime(AbstractLocalContextProvider provider) {
        if ( isGlobalRuntimeReady() ) {
            return Ruby.getGlobalRuntime();
        }
        return Ruby.newInstance(provider.config);
    }

    RubyInstanceConfig getGlobalRuntimeConfig(AbstractLocalContextProvider provider) {
        // make sure we do not yet initialize the runtime here
        if ( isGlobalRuntimeReady() ) {
            return getGlobalRuntime(provider).getInstanceConfig();
        }
        return provider.config;
    }

    static RubyInstanceConfig getGlobalRuntimeConfigOrNew() {
        return Ruby.isGlobalRuntimeReady() ?
                Ruby.getGlobalRuntime().getInstanceConfig() :
                    new RubyInstanceConfig();
    }

}
