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
 * Copyright (C) 2006 Nick Sieger <nicksieger@gmail.com>
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
package org.jruby.ext.io.wait;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.OpenFile;

/**
 * @author Nick Sieger
 */
public class IOWaitLibrary implements Library {

    public void load(Ruby runtime, boolean wrap) {
        RubyClass ioClass = runtime.getIO();
        ioClass.defineAnnotatedMethods(IOWaitLibrary.class);
    }

    /**
     * returns non-nil if input available without blocking, false if EOF or not open/readable, otherwise nil.
     */
    @JRubyMethod(name = "ready?")
    public static IRubyObject ready(ThreadContext context, IRubyObject obj) {
        RubyIO io = (RubyIO)obj;
        try {
            OpenFile openFile = io.getOpenFile();
            ChannelDescriptor descriptor = openFile.getMainStreamSafe().getDescriptor();
            if (!descriptor.isOpen() || !openFile.getMainStreamSafe().getModes().isReadable() || openFile.getMainStreamSafe().feof()) {
                return context.runtime.getFalse();
            }

            int avail = openFile.getMainStreamSafe().ready();
            if (avail > 0) {
                return context.runtime.newFixnum(avail);
            }
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        } catch (Exception anyEx) {
            return context.runtime.getFalse();
        }
        return context.runtime.getNil();
    }

    /**
     * waits until input available or timed out and returns self, or nil when EOF reached.
     */
    @JRubyMethod
    public static IRubyObject io_wait(ThreadContext context, IRubyObject obj) {
        RubyIO io = (RubyIO)obj;
        try {
            OpenFile openFile = io.getOpenFile();
            if (openFile.getMainStreamSafe().feof()) {
                return context.runtime.getNil();
            }
            openFile.getMainStreamSafe().waitUntilReady();
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        } catch (Exception anyEx) {
            return context.runtime.getNil();
        }
        return obj;
    }
}
