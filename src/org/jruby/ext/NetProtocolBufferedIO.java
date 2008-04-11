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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ext;

import java.util.Map;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
@JRubyClass(name="Net::BufferedIO")
public class NetProtocolBufferedIO {
    public static void create(Ruby runtime) {
        RubyModule mNet = runtime.getModule("Net");

        RubyClass orgBufferedIO = (RubyClass)mNet.getConstant("BufferedIO");
        mNet.deleteConstant("BufferedIO"); // We want to override this class definition with Java

        RubyClass cBufferedIO = mNet.defineClassUnder("BufferedIO",runtime.getObject(), runtime.getObject().getAllocator());
        cBufferedIO.defineAnnotatedMethods(NetProtocolBufferedIO.class);


        // One of these will be mixed into the BufferedIO instance at initialize time to provide
        // different implementations based on if the io object is a Ruby IO
        // in which case we can use a native implementation, but otherwise we
        // can fall back on a straight port of the original Ruby implementation instead
        RubyModule mRubyImpl = cBufferedIO.defineModuleUnder("RubyImplementation");

        // This will copy all the methods from the original BufferedIO instead of having to recreate this stuff. Yay for internal meta magic
        for(Map.Entry<String, DynamicMethod> entry : orgBufferedIO.getMethods().entrySet()) {
            if(!"initialize".equals(entry.getKey())) {
                DynamicMethod dm = entry.getValue();
                dm.setImplementationClass(mRubyImpl);
                mRubyImpl.addMethod(entry.getKey(), dm);
            }
        }

        mRubyImpl.defineAnnotatedMethods(RubyImpl.class);

        RubyModule mNativeImpl = cBufferedIO.defineModuleUnder("NativeImplementation");

        mNativeImpl.attr_reader(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("io")});
        mNativeImpl.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("read_timeout")});
        mNativeImpl.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("debug_output")});

        mNativeImpl.defineAnnotatedMethods(NativeImpl.class);
    }    

    @JRubyMethod(required = 1)
    public static IRubyObject initialize(IRubyObject recv, IRubyObject io) {
        // do an extend based on the type of IOs IO implementation.
        /*
          @io = io
          @read_timeout = 60
          @debug_output = nil
          @rbuf = ''
        */
        return recv;
    }

    @JRubyModule(name="Net::BufferedIO::RubyImplementation")
    public static class RubyImpl {
    }

    @JRubyModule(name="Net::BufferedIO::NativeImplementation")
    public static class NativeImpl {
        /*
    def inspect
    def closed?
    def close
    public
    def read(len, dest = '', ignore_eof = false)
    def read_all(dest = '')
    def readuntil(terminator, ignore_eof = false)
    def readline
    private
    def rbuf_fill
    def rbuf_consume(len)
    public
    def write(str)
    def writeline(str)
    private
    def writing
    def write0(str)
    private
    def LOG_off
    def LOG_on
    def LOG(msg)
        */
    }
}// NetProtocolBufferedIO
