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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
/**
 * $Id$
 */
package org.jruby.util;

import java.io.IOException;
import java.io.Reader;

import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @version $Revision$
 */
public class IOReader extends Reader {
    private IRubyObject io;

    public IOReader(final IRubyObject io) {
        if(!io.respondsTo("read")) {
            throw new IllegalArgumentException("Object: " + io + " is not a legal argument to this wrapper, cause it doesn't respond to \"read\".");
        }
        this.io = io;
    }

    public void close() throws IOException {
        if(io.respondsTo("close")) {
            io.callMethod("close");
        }
    }

    public int read(final char[] arr, final int off, final int len) {
        final IRubyObject read = io.callMethod("read",io.getRuntime().newFixnum(len));
        if(read.isNil() || ((RubyString)read).getValue().length() == 0) {
            return -1;
        } else {
            final RubyString str = (RubyString)read;
            final CharSequence val = str.getValue();
            System.arraycopy(val.toString().toCharArray(),0,arr,off,val.length());
            return val.length();
        }
    }
}// IOReader

