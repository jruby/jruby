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
import org.jruby.RubyNumeric;
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

    @JRubyMethod
    public static IRubyObject nread(ThreadContext context, IRubyObject _io) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        int len;
//        ioctl_arg n;
        RubyIO io = (RubyIO)_io;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(runtime);
        len = fptr.readPending();
        if (len > 0) return runtime.newFixnum(len);
        // TODO: better effort to get available bytes from our channel
//        if (!FIONREAD_POSSIBLE_P(fptr->fd)) return INT2FIX(0);
//        if (ioctl(fptr->fd, FIONREAD, &n)) return INT2FIX(0);
//        if (n > 0) return ioctl_arg2num(n);
        return RubyNumeric.int2fix(runtime, 0);
    }

    /**
     * returns non-nil if input available without blocking, false if EOF or not open/readable, otherwise nil.
     */
    @JRubyMethod(name = "ready?")
    public static IRubyObject ready(ThreadContext context, IRubyObject _io) {
        Ruby runtime = context.runtime;
        RubyIO io = (RubyIO)_io;
        OpenFile fptr;
//        ioctl_arg n;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(runtime);
        if (fptr.readPending() != 0) return runtime.getTrue();
        // TODO: better effort to get available bytes from our channel
//        if (!FIONREAD_POSSIBLE_P(fptr->fd)) return Qnil;
//        if (ioctl(fptr->fd, FIONREAD, &n)) return Qnil;
//        if (n > 0) return Qtrue;
        return runtime.getFalse();
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject wait_readable(ThreadContext context, IRubyObject _io, IRubyObject[] argv) {
        RubyIO io = (RubyIO)_io;
        Ruby runtime = context.runtime;
        OpenFile fptr;
        boolean i;
//        ioctl_arg n;
        IRubyObject timeout;
        long tv;
//        struct timeval timerec;
//        struct timeval *tv;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(runtime);

        switch (argv.length) {
            case 1:
                timeout = argv[0];
                break;
            default:
                timeout = context.nil;
        }

        if (timeout.isNil()) {
            tv = 0;
        }
        else {
            tv = timeout.convertToInteger().getLongValue() * 1000;
        }

        if (fptr.readPending() != 0) return runtime.getTrue();
        // TODO: better effort to get available bytes from our channel
//        if (!FIONREAD_POSSIBLE_P(fptr->fd)) return Qfalse;
        // TODO: actually use timeout
        i = fptr.waitReadable(runtime, tv);
        fptr.checkClosed(runtime);
//        if (ioctl(fptr->fd, FIONREAD, &n)) rb_sys_fail(0);
//        if (n > 0) return io;
        if (i) return io;
        return context.nil;
    }

    /**
     * waits until input available or timed out and returns self, or nil when EOF reached.
     */
    @JRubyMethod(optional = 1)
    public static IRubyObject wait_writable(ThreadContext context, IRubyObject _io, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        RubyIO io = (RubyIO)_io;
        OpenFile fptr;
        boolean i;
        IRubyObject timeout;
        long tv;

        fptr = io.getOpenFileChecked();
        fptr.checkWritable(context);

        switch (argv.length) {
            case 1:
                timeout = argv[0];
                break;
            default:
                timeout = context.nil;
        }
        if (timeout.isNil()) {
            tv = 0;
        }
        else {
            tv = timeout.convertToInteger().getLongValue() * 1000;
        }

        i = fptr.waitWritable(runtime, tv);
        fptr.checkClosed(runtime);
        if (i)
            return io;
        return context.nil;
    }
}
