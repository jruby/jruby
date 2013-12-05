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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ext.net.protocol;

import java.io.IOException;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SelectableChannel;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.RubyObject;
import org.jruby.RubyNumeric;
import org.jruby.RubyIO;
import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelStream;
import org.jruby.exceptions.RaiseException;

import org.jruby.util.io.SelectorFactory;
import java.nio.channels.spi.SelectorProvider;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.Stream;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
@JRubyClass(name="Net::BufferedIO")
public class NetProtocolBufferedIO { 
    public static void create(Ruby runtime) {
        RubyModule mNet = runtime.getModule("Net");

        RubyClass cBufferedIO = (RubyClass)mNet.getConstant("BufferedIO");
        cBufferedIO.defineAnnotatedMethods(NetProtocolBufferedIO.class);

        RubyModule mNativeImpl = cBufferedIO.defineModuleUnder("NativeImplementation");

        mNativeImpl.defineAnnotatedMethods(NativeImpl.class);
    }    

    @JRubyMethod(required = 1)
    public static IRubyObject initialize(IRubyObject recv, IRubyObject io) {
        try {
            if (io instanceof RubyIO) {
                RubyIO rubyIO = (RubyIO)io;
                OpenFile of = rubyIO.getOpenFile();
                Stream stream = of.getMainStreamSafe();
                if (stream instanceof ChannelStream) {
                    ChannelStream cStream = (ChannelStream)stream;
                    if (cStream.getDescriptor().getChannel() instanceof SelectableChannel)  {
                        SelectableChannel selChannel = (SelectableChannel)cStream.getDescriptor().getChannel();

                        ((RubyObject)recv).extend(
                                new IRubyObject[]{((RubyModule)recv.getRuntime().getModule("Net").getConstant("BufferedIO")).getConstant("NativeImplementation")});
                        SelectableChannel sc = (SelectableChannel)(selChannel);
                        recv.dataWrapStruct(new NativeImpl(sc));
                    }
                }
            }

            recv.getInstanceVariables().setInstanceVariable("@io", io);
            recv.getInstanceVariables().setInstanceVariable("@read_timeout", recv.getRuntime().newFixnum(60));
            recv.getInstanceVariables().setInstanceVariable("@debug_output", recv.getRuntime().getNil());
            recv.getInstanceVariables().setInstanceVariable("@rbuf", RubyString.newEmptyString(recv.getRuntime()));

            return recv;
        } catch (BadDescriptorException e) {
            throw recv.getRuntime().newErrnoEBADFError();
        }
    }

    @JRubyModule(name="Net::BufferedIO::NativeImplementation")
    public static class NativeImpl {
        private SelectableChannel channel;
        public NativeImpl(SelectableChannel channel) {
            this.channel = channel;
        }
        
        @JRubyMethod
        public static IRubyObject rbuf_fill(IRubyObject recv) {
            RubyString buf = (RubyString)recv.getInstanceVariables().getInstanceVariable("@rbuf");
            RubyIO io = (RubyIO)recv.getInstanceVariables().getInstanceVariable("@io");

            int timeout = RubyNumeric.fix2int(recv.getInstanceVariables().getInstanceVariable("@read_timeout")) * 1000;
            NativeImpl nim = (NativeImpl)recv.dataGetStruct();

            Selector selector = null;
            synchronized (nim.channel.blockingLock()) {
                boolean oldBlocking = nim.channel.isBlocking();

                try {
                    selector = SelectorFactory.openWithRetryFrom(recv.getRuntime(), SelectorProvider.provider());
                    nim.channel.configureBlocking(false);
                    SelectionKey key = nim.channel.register(selector, SelectionKey.OP_READ);
                    int n = selector.select(timeout);

                    if(n > 0) {
                        IRubyObject readItems = io.read(new IRubyObject[]{recv.getRuntime().newFixnum(1024*16)});
                        return buf.concat(readItems);
                    } else {
                        RubyClass exc = (RubyClass)(recv.getRuntime().getModule("Timeout").getConstant("Error"));
                        throw new RaiseException(RubyException.newException(recv.getRuntime(), exc, "execution expired"),false);
                    }
                } catch(IOException exception) {
                    throw recv.getRuntime().newIOErrorFromException(exception);
                } finally {
                    if (selector != null) {
                        try {
                            selector.close();
                        } catch (Exception e) {
                        }
                    }
                    try {nim.channel.configureBlocking(oldBlocking);} catch (IOException ioe) {}
                }
            }
        }
    }
}// NetProtocolBufferedIO
