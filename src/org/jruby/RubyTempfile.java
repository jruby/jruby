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
 * Copyright (C) 2004-2009 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ext.tempfile.*;

import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A temporary stub superclass installed to handle existing code that references
 * "RubyTempfile" directly.
 */
@JRubyClass(name="Tempfile", parent="File")
@Deprecated
public abstract class RubyTempfile extends RubyFile {
    public static RubyClass createTempfileClass(Ruby runtime) {
        return Tempfile.createTempfileClass(runtime);
    }

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public RubyTempfile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public abstract IRubyObject initialize(IRubyObject[] args, Block block);
    public abstract IRubyObject make_tmpname(ThreadContext context, IRubyObject basename, IRubyObject n, Block block);
    public abstract IRubyObject open();
    public abstract IRubyObject _close(ThreadContext context);
    public abstract IRubyObject close(ThreadContext context, IRubyObject[] args, Block block);
    public abstract IRubyObject close_bang(ThreadContext context);
    public abstract IRubyObject unlink(ThreadContext context);
    public abstract IRubyObject size(ThreadContext context);
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return Tempfile.open(context, recv, args, block);
    }
}
