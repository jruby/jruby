/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 The JRuby Community <www.jruby.org>
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

package org.jruby.util.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.jruby.runtime.ThreadContext;

/**
 * A Utility class to emulate blocking I/O operations on non-blocking channels.
 */
public class BlockingIO {
    public static final class Condition {
        private final IOChannel channel;
        Condition(IOChannel channel) {
            this.channel = channel;
        }
        public void cancel() {
            channel.wakeup(false);
        }
        public void interrupt() {
            channel.interrupt();
        }
        public boolean await() throws InterruptedException {
            return channel.await();
        }
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return channel.await(timeout, unit);
        }
    }
    static final class IOChannel {
        final SelectableChannel channel;
        final int ops;        
        private final Object monitor;
        private boolean woken = false;
        private boolean ready = false;
        private boolean interrupted = false;
        
        IOChannel(SelectableChannel channel, int ops, Object monitor) {
            this.channel = channel;
            this.ops = ops;
            this.monitor = monitor;
        }
        public final void wakeup(boolean ready) {
            synchronized (monitor) {
                this.woken = true;
                this.ready = ready;
                monitor.notifyAll();
            }
        }
        public final void interrupt() {
            synchronized (monitor) {
                this.woken = true;
                this.interrupted = true;
                monitor.notifyAll();
            }
        }
        public final boolean await() throws InterruptedException {
            return await(0, TimeUnit.MILLISECONDS);
        }
        public final boolean await(final long timeout, TimeUnit unit) throws InterruptedException {
            synchronized (monitor) {
                if (!woken) {
                    monitor.wait(TimeUnit.MILLISECONDS.convert(timeout, unit));
                }
                if (interrupted) {
                    throw new InterruptedException("Interrupted");
                }
                return ready;
            }
        }
    }
    static final class IOSelector implements Runnable {
        private final Selector selector;
        private final ConcurrentLinkedQueue<IOChannel> registrationQueue;

        public IOSelector(SelectorProvider provider) throws IOException {
            selector = provider.openSelector();
            registrationQueue = new ConcurrentLinkedQueue<IOChannel>();
        }
        public void run() {
            for ( ; ; ) {
                try {
                    //
                    // Wake up any channels that became unblocked
                    //
                    Set<SelectionKey> selected = new HashSet<SelectionKey>(selector.selectedKeys());
                    for (SelectionKey k : selected) {
                        List<IOChannel> waitq = (List<IOChannel>) k.attachment();
                        for (IOChannel ch : waitq) {
                            ch.wakeup(true);
                        }
                        waitq.clear();
                    }

                    //
                    // Register any new blocking I/O requests
                    //
                    IOChannel ch;
                    Set<SelectableChannel> added = new HashSet<SelectableChannel>();
                    while ((ch = registrationQueue.poll()) != null) {
                        SelectionKey k = ch.channel.keyFor(selector);
                        List<IOChannel> waitq = k == null
                                ? new LinkedList<IOChannel>()
                                : (List<IOChannel>) k.attachment();
                        ch.channel.register(selector, ch.ops, waitq);
                        waitq.add(ch);
                        added.add(ch.channel);
                    }

                    // Now clear out any previously selected channels
                    for (SelectionKey k : selected) {
                        if (!added.contains(k.channel())) {
                            k.cancel();
                        }
                    }

                    //
                    // Wait for I/O on any channel
                    //
                    selector.select();
                } catch (IOException ex) {

                }
            }
        }
        Condition add(Channel channel, int ops, Object monitor) {
            IOChannel io = new IOChannel((SelectableChannel) channel, ops, monitor);
            registrationQueue.add(io);
            selector.wakeup();
            return new Condition(io);
        }
        public void await(Channel channel, int op) throws InterruptedException {
            add(channel, op, new Object()).await();
        }
    }
    static final private Map<SelectorProvider, IOSelector> selectors
            = new ConcurrentHashMap<SelectorProvider, IOSelector>();

    private static IOSelector getSelector(SelectorProvider provider) throws IOException {
        IOSelector sel = selectors.get(provider);
        if (sel != null) {
            return sel;
        }

        //
        // Synchronize and re-check to avoid creating more than one Selector per provider
        //
        synchronized (selectors) {
            sel = selectors.get(provider);
            if (sel == null) {
                sel = new IOSelector(provider);
                selectors.put(provider, sel);
                Thread t = new Thread(sel);
                t.setDaemon(true);
                t.start();
            }
        }
        return sel;
    }
    private static IOSelector getSelector(Channel channel) throws IOException {
        if (!(channel instanceof SelectableChannel)) {
            throw new IllegalArgumentException("channel must be a SelectableChannel");
        }        
        return getSelector(((SelectableChannel) channel).provider());
    }
    public static final Condition newCondition(Channel channel, int ops, Object monitor) throws IOException {
        return getSelector(channel).add(channel, ops, monitor);
    }
    public static final Condition newCondition(Channel channel, int ops) throws IOException {
        return newCondition(channel, ops, new Object());
    }
    public static void waitForIO(Channel channel, int op) throws InterruptedException, IOException {
        getSelector(channel).await(channel, op);
    }
    public static void awaitReadable(ReadableByteChannel channel) throws InterruptedException, IOException {
        waitForIO(channel, SelectionKey.OP_READ);
    }
    public static void awaitWritable(WritableByteChannel channel) throws InterruptedException, IOException {
        waitForIO(channel, SelectionKey.OP_WRITE);
    }
    public static int read(ReadableByteChannel channel, ByteBuffer buf, boolean blocking) throws IOException {
        do {
            int n = channel.read(buf);
            if (n != 0 || !blocking || !(channel instanceof SelectableChannel) || !buf.hasRemaining()) {
                return n;
            }
            try {
                awaitReadable(channel);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException(ex.getMessage());
            }
        } while (true);
    }
    public static int write(WritableByteChannel channel, ByteBuffer buf, boolean blocking) throws IOException {
        do {
            int n = channel.write(buf);
            if (n != 0 || !blocking || !(channel instanceof SelectableChannel) || !buf.hasRemaining()) {
                return n;
            }
            try {
                awaitWritable(channel);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException(ex.getMessage());
            }
        } while (true);
    }
    public static int blockingRead(ReadableByteChannel channel, ByteBuffer buf) throws IOException {
        return read(channel, buf, true);
    }
    public static int blockingWrite(WritableByteChannel channel, ByteBuffer buf) throws IOException {
        return write(channel, buf, true);
    }
}
