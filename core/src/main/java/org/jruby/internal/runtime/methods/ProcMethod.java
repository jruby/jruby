/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class ProcMethod extends DynamicMethod implements PositionAware, IRMethodArgs {
    private RubyProc proc;

    /**
     * Constructor for ProcMethod.
     * @param visibility
     */
    public ProcMethod(RubyModule implementationClass, RubyProc proc, Visibility visibility) {
        // FIXME: set up a call configuration for this
        super(implementationClass, visibility, null);
        this.proc = proc;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return proc.call(context, args, self, block);
    }
    
    public DynamicMethod dup() {
        return new ProcMethod(getImplementationClass(), proc, getVisibility());
    }

    // TODO: Push isSame up to DynamicMethod to simplify general equality
    public boolean isSame(DynamicMethod method) {
        if (!(method instanceof ProcMethod)) return false;

        return ((ProcMethod) method).proc == proc;
    }
    
    @Override
    public Arity getArity() {
        return proc.getBlock().getSignature().arity();
    }

    public String getFile() {
        return proc.getBlock().getBody().getFile();
    }

    public int getLine() {
        return proc.getBlock().getBody().getLine();
    }

    @Override
    public Signature getSignature() {
        return proc.getBlock().getBody().getSignature();
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return proc.getBlock().getBody().getArgumentDescriptors();
    }
}
