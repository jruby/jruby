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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.common;

import org.jruby.IRuby;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 *
 */
public class RubyWarnings implements IRubyWarnings {
    private IRuby runtime;

    public RubyWarnings(IRuby runtime) {
        this.runtime = runtime;
    }

    public void warn(ISourcePosition position, String message) {
    	assert position != null;
    	
        StringBuffer buffer = new StringBuffer(100);

        buffer.append(position.getFile()).append(':').append(position.getEndLine()).append(' ');
        buffer.append("warning: ").append(message).append('\n');
        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        errorStream.callMethod(runtime.getCurrentContext(), "write", runtime.newString(buffer.toString()));
    }

    public boolean isVerbose() {
        return runtime.getVerbose().isTrue();
    }

    public void warn(String message) {
        warn(runtime.getCurrentContext().getPosition(), message);
    }

    public void warning(String message) {
        warning(runtime.getCurrentContext().getPosition(), message);
    }
    
    public void warning(ISourcePosition position, String message) {
        if (isVerbose()) {
            warn(position, message);
        }
    }
}
