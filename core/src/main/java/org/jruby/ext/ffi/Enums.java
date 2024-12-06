/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2014 Timur Duehr <tduehr@gmail.com>
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

package org.jruby.ext.ffi;

import org.jruby.anno.JRubyClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enums", parent="Object")
public final class Enums extends RubyObject {
    public static RubyClass createEnumsClass(ThreadContext context, RubyModule FFI, RubyClass Object, RubyModule DataConverter) {
        return FFI.defineClassUnder(context, "Enums", Object, Enums::new).include(context, DataConverter);
    }

    private Enums(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    protected RubyArray getAllEnums() {
        return (RubyArray) getInstanceVariable("@all_enums");
    }

    protected RubyHash getSymbolMap() {
        return (RubyHash) getInstanceVariable("@symbol_map");
    }

    protected RubyHash getTaggedEnums() {
        return (RubyHash) getInstanceVariable("@tagged_enums");
    }

    public boolean isEmpty(){
        return ( getAllEnums().isEmpty() && getSymbolMap().isEmpty() && getTaggedEnums().isEmpty());
    }

    public IRubyObject mapSymbol(final ThreadContext context, IRubyObject symbol){
        return callMethod(context, "__map_symbol", symbol);
    }
}
