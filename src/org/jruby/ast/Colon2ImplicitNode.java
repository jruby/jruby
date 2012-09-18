/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Represents a bare class declaration (e.g. class Foo/module Foo).  This is slightly misnamed
 * since it contains no double colons (::), but our cname production needs to be a common type.
 * In JRuby 2, we will rename this.
 */
public class Colon2ImplicitNode extends Colon2Node {
    public Colon2ImplicitNode(ISourcePosition position, String name) {
        super(position, null, name);
    }

   /** 
    * Get parent module/class that this module represents
    */
    @Override
    public RubyModule getEnclosingModule(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return context.getCurrentScope().getStaticScope().getModule();
    }

    /**
     * This type of node will never get interpreted since it only gets created via class/module
     * declaration time.  Those node types interact with this node in a different way.
     */
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        assert false: "interpret cannot ever happen for Colon2ImplicitNode";
        return null;
    }

    /**
     * This type of node will never get created as part of a defined? call since it will then
     * appear to be a ConstNode.
     */
    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        assert false: "definition should not ever happen for Colon2ImplicitNode";
        return null;
    }
}
