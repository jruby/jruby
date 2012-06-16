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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyIO;
import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This is a reimplementation of MRI's IO#select logic. It has been rewritten
 * from an earlier version in JRuby to improve performance and readability.
 * 
 * This version avoids allocating a selector or any data structures to hold
 * data about the channels/IOs being selected unless absolutely necessary. It
 * also uses simple boolean arrays to track characteristics like whether an IO
 * is pending or unselectable, rather than maintaining Set structures. It avoids
 * hitting Java Integration code to get IO objects out of the incoming Array.
 * Finally, it tries to build a minimal number of data structures an reuse them
 * as much as possible.
 */
public class SelectBlob {
    public IRubyObject goForIt(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        this.runtime = runtime;
        try {
            processReads(runtime, args, context);
            processWrites(runtime, args, context);
            if (args.length > 2 && !args[2].isNil()) {
                checkArrayType(runtime, args[2]);
                // Java's select doesn't do anything about this, so we leave it be.
            }
            boolean has_timeout = args.length > 3 && !args[3].isNil();
            long timeout = !has_timeout ? 0 : getTimeoutFromArg(args[3], runtime);
            
            if (timeout < 0) {
                throw runtime.newArgumentError("time interval must be positive");
            }
            
            // If all streams are nil, just sleep the specified time (JRUBY-4699)
            if (args[0].isNil() && args[1].isNil() && args[2].isNil()) {
                if (timeout > 0) {
                    RubyThread thread = context.getThread();
                    long now = System.currentTimeMillis();
                    thread.sleep(timeout);
                    // Guard against spurious wakeup
                    while (System.currentTimeMillis() < now + timeout) {
                        thread.sleep(1);
                    }

                }
            } else {
                doSelect(runtime, has_timeout, timeout);
                processSelectedKeys(runtime);
                processPendingAndUnselectable();
                tidyUp();
            }
            
            if (readResults == null && writeResults == null && errorResults == null) {
                return runtime.getNil();
            }
            return constructResults(runtime);
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException ie) {
            throw runtime.newThreadError("select interrupted");
        } finally {
            for (Selector selector : selectors.values()) {
                try {
                    selector.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void processReads(Ruby runtime, IRubyObject[] args, ThreadContext context) throws BadDescriptorException, IOException {
        if (!args[0].isNil()) {
            // read
            checkArrayType(runtime, args[0]);
            readArray = (RubyArray) args[0];
            readSize = readArray.size();
            if (readSize == 0) {
                // clear reference; we aren't going to do anything
                readArray = null;
            } else {
                readIOs = new RubyIO[readSize];
                Map<Character,Integer> attachment = new HashMap<Character,Integer>(1);
                for (int i = 0; i < readSize; i++) {
                    RubyIO ioObj = saveReadIO(i, context);
                    saveReadBlocking(ioObj, i);
                    saveBufferedRead(ioObj, i);                    
                    attachment.clear();
                    attachment.put('r', i);
                    trySelectRead(context, attachment, ioObj);
                }
            }
        }
    }

    private RubyIO saveReadIO(int i, ThreadContext context) {
        IRubyObject obj = readArray.eltOk(i);
        RubyIO ioObj = RubyIO.convertToIO(context, obj);
        readIOs[i] = ioObj;
        return ioObj;
    }

    private void saveReadBlocking(RubyIO ioObj, int i) {
        // save blocking state
        if (ioObj.getChannel() instanceof SelectableChannel) {
            getReadBlocking()[i] = ((SelectableChannel) ioObj.getChannel()).isBlocking();
        }
    }

    private void saveBufferedRead(RubyIO ioObj, int i) throws BadDescriptorException {
        // already buffered data? don't bother selecting
        if (ioObj.getOpenFile().getMainStreamSafe().readDataBuffered()) {
            getUnselectableReads()[i] = true;
        }
    }

    private void trySelectRead(ThreadContext context, Map<Character,Integer> attachment, RubyIO ioObj) throws IOException {
        if (ioObj.getChannel() instanceof SelectableChannel && registerSelect(context, getSelector(context, (SelectableChannel)ioObj.getChannel()), attachment, ioObj, SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) {
            selectedReads++;
            if (ioObj.writeDataBuffered()) {
                getPendingReads()[(Integer)attachment.get('r')] = true;
            }
        } else {
            if ((ioObj.getOpenFile().getMode() & OpenFile.READABLE) != 0) {
                getUnselectableReads()[(Integer)attachment.get('r')] = true;
            }
        }
    }

    private void processWrites(Ruby runtime, IRubyObject[] args, ThreadContext context) throws IOException {
        if (args.length > 1 && !args[1].isNil()) {
            // write
            checkArrayType(runtime, args[1]);
            writeArray = (RubyArray) args[1];
            writeSize = writeArray.size();
            if (writeArray.size() == 0) {
                // clear reference; we aren't going to do anything
                writeArray = null;
            } else {
                writeIOs = new RubyIO[writeSize];
                Map<Character,Integer> attachment = new HashMap<Character,Integer>(1);
                for (int i = 0; i < writeSize; i++) {
                    RubyIO ioObj = saveWriteIO(i, context);
                    saveWriteBlocking(ioObj, i);
                    attachment.clear();                    
                    attachment.put('w', i);
                    trySelectWrite(context, attachment, ioObj);
                }
            }
        }
    }

    private RubyIO saveWriteIO(int i, ThreadContext context) {
        IRubyObject obj = writeArray.eltOk(i);
        RubyIO ioObj = RubyIO.convertToIO(context, obj);
        writeIOs[i] = ioObj;
        return ioObj;
    }

    private void saveWriteBlocking(RubyIO ioObj, int i) {
        if (ioObj.getChannel() instanceof SelectableChannel) {
            // save blocking state
            if (readBlocking != null) {
                // some read has saved blocking state
                // find obj
                int readIndex = fastSearch(readIOs, ioObj);
                if (readIndex == -1) {
                    // save blocking only if not found
                    getWriteBlocking()[i] = ((SelectableChannel) ioObj.getChannel()).isBlocking();
                }
            } else {
                getWriteBlocking()[i] = ((SelectableChannel) ioObj.getChannel()).isBlocking();
            }
        }
    }

    private void trySelectWrite(ThreadContext context, Map<Character,Integer> attachment, RubyIO ioObj) throws IOException {
        if (!(ioObj.getChannel() instanceof SelectableChannel)
                || !registerSelect(context, getSelector(context, (SelectableChannel)ioObj.getChannel()), attachment, ioObj, SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) {
            selectedReads++;
            if ((ioObj.getOpenFile().getMode() & OpenFile.WRITABLE) != 0) {
                getUnselectableWrites()[(Integer)attachment.get('w')] = true;
            }
        }
    }

    private static long getTimeoutFromArg(IRubyObject timeArg, Ruby runtime) {
        long timeout = 0;
        if (timeArg instanceof RubyFloat) {
            timeout = Math.round(((RubyFloat) timeArg).getDoubleValue() * 1000);
        } else if (timeArg instanceof RubyFixnum) {
            timeout = Math.round(((RubyFixnum) timeArg).getDoubleValue() * 1000);
        } else {
            // TODO: MRI also can hadle Bignum here
            throw runtime.newTypeError("can't convert " + timeArg.getMetaClass().getName() + " into time interval");
        }
        if (timeout < 0) {
            throw runtime.newArgumentError("negative timeout given");
        }
        return timeout;
    }

    private void doSelect(Ruby runtime, final boolean has_timeout, long timeout) throws IOException {
        if (mainSelector != null) {
            if (pendingReads == null && unselectableReads == null && unselectableWrites == null) {
                if (has_timeout && timeout == 0) {
                    for (Selector selector : selectors.values()) selector.selectNow();
                } else {
                    List<Future> futures = new ArrayList<Future>(enxioSelectors.size());
                    for (ENXIOSelector enxioSelector : enxioSelectors) {
                        futures.add(runtime.getExecutor().submit(enxioSelector));
                    }

                    mainSelector.select(has_timeout ? timeout : 0);
                    for (ENXIOSelector enxioSelector : enxioSelectors) enxioSelector.selector.wakeup();
                    // ensure all the enxio threads have finished
                    for (Future f : futures) try {
                        f.get();
                    } catch (InterruptedException iex) {
                    } catch (ExecutionException eex) {
                        if (eex.getCause() instanceof IOException) {
                            throw (IOException) eex.getCause();
                        }
                    }
                }
            } else {
                for (Selector selector : selectors.values()) selector.selectNow();
            }
        }

        // If any enxio selectors woke up, remove them from the selected key set of the main selector
        for (ENXIOSelector enxioSelector : enxioSelectors) {
            Pipe.SourceChannel source = enxioSelector.pipe.source();
            SelectionKey key = source.keyFor(mainSelector);
            if (key != null && mainSelector.selectedKeys().contains(key)) {
                mainSelector.selectedKeys().remove(key);
                ByteBuffer buf = ByteBuffer.allocate(1);
                source.read(buf);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processSelectedKeys(Ruby runtime) throws IOException {
        for (Selector selector : selectors.values()) {
            for (Iterator i = selector.selectedKeys().iterator(); i.hasNext();) {
                SelectionKey key = (SelectionKey) i.next();
                int readIoIndex = 0;
                int writeIoIndex = 0;
                try {
                    int interestAndReady = key.interestOps() & key.readyOps();
                    if (readArray != null && (interestAndReady & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) {
                        readIoIndex = ((Map<Character,Integer>)key.attachment()).get('r');
                        getReadResults().append(readArray.eltOk(readIoIndex));
                        if (pendingReads != null) {
                            pendingReads[readIoIndex] = false;
                        }
                    }
                    if (writeArray != null && (interestAndReady & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
                        writeIoIndex = ((Map<Character,Integer>)key.attachment()).get('w');
                        getWriteResults().append(writeArray.eltOk(writeIoIndex));

                        // not-great logic for JRUBY-5165; we should move finishConnect into RubySocket logic, I think
                        if (key.channel() instanceof SocketChannel) {
                            SocketChannel socketChannel = (SocketChannel)key.channel();
                            if (socketChannel.isConnectionPending()) {
                                socketChannel.finishConnect();
                            }
                        }
                    }
                } catch (CancelledKeyException cke) {
                    // TODO: is this the right thing to do?
                    int interest = key.interestOps();
                    if (readArray != null && (interest & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT | SelectionKey.OP_CONNECT)) != 0) {
                        if (pendingReads != null) {
                            pendingReads[readIoIndex] = false;
                        }
                        if (errorResults != null) {
                            errorResults = RubyArray.newArray(runtime, readArray.size() + writeArray.size());
                        }
                        if (fastSearch(errorResults.toJavaArrayUnsafe(), readIOs[readIoIndex]) == -1) {
                            // only add to error if not there
                            getErrorResults().append(readArray.eltOk(readIoIndex));
                        }
                    }
                    if (writeArray != null && (interest & (SelectionKey.OP_WRITE)) != 0) {
                        if (fastSearch(errorResults.toJavaArrayUnsafe(), writeIOs[writeIoIndex]) == -1) {
                            // only add to error if not there
                            errorResults.append(writeArray.eltOk(writeIoIndex));
                        }
                    }
                }
            }
        }
    }

    private void processPendingAndUnselectable() {
        if (pendingReads != null) {
            for (int i = 0; i < pendingReads.length; i++) {
                if (pendingReads[i]) {
                    getReadResults().append(readArray.eltOk(i));
                }
            }
        }
        if (unselectableReads != null) {
            for (int i = 0; i < unselectableReads.length; i++) {
                if (unselectableReads[i]) {
                    getReadResults().append(readArray.eltOk(i));
                }
            }
        }
        if (unselectableWrites != null) {
            for (int i = 0; i < unselectableWrites.length; i++) {
                if (unselectableWrites[i]) {
                    getWriteResults().append(writeArray.eltOk(i));
                }
            }
        }
    }

    private void tidyUp() throws IOException {
        // make all sockets blocking as configured again
        for (Selector selector : selectors.values()) {
            selector.close(); // close unregisters all channels, so we can safely reset blocking modes
        }

        for (ENXIOSelector enxioSelector : enxioSelectors) {
            enxioSelector.pipe.sink().close();
            enxioSelector.pipe.source().close();
        }

        if (readBlocking != null) {
            for (int i = 0; i < readBlocking.length; i++) {
                if (readBlocking[i] != null) {
                    try {
                        ((SelectableChannel) readIOs[i].getChannel()).configureBlocking(readBlocking[i]);
                    } catch (IllegalBlockingModeException ibme) {
                        throw runtime.newConcurrencyError("can not set IO blocking after select; concurrent select detected?");
                    }
                }
            }
        }
        if (writeBlocking != null) {
            for (int i = 0; i < writeBlocking.length; i++) {
                if (writeBlocking[i] != null) {
                    try {
                        ((SelectableChannel) writeIOs[i].getChannel()).configureBlocking(writeBlocking[i]);
                    } catch (IllegalBlockingModeException ibme) {
                        throw runtime.newConcurrencyError("can not set IO blocking after select; concurrent select detected?");
                    }
                }
            }
        }
    }

    private RubyArray getReadResults() {
        if (readResults == null) {
            readResults = RubyArray.newArray(runtime, readArray.size());
        }
        return readResults;
    }

    private RubyArray getWriteResults() {
        if (writeResults == null) {
            writeResults = RubyArray.newArray(runtime, writeArray.size());
        }
        return writeResults;
    }

    private RubyArray getErrorResults() {
        if (errorResults != null) {
            errorResults = RubyArray.newArray(runtime, readArray.size() + writeArray.size());
        }
        return errorResults;
    }

    private Selector getSelector(ThreadContext context, SelectableChannel channel) throws IOException {
        Selector selector = selectors.get(channel.provider());
        if (selector == null) {
            selector = SelectorFactory.openWithRetryFrom(context.getRuntime(), channel.provider());
            if (selectors.isEmpty()) {
                selectors = new HashMap<SelectorProvider, Selector>();
            }
            selectors.put(channel.provider(), selector);

            if (!selector.provider().equals(SelectorProvider.provider())) {
                // need to create pipe between alt impl selector and native NIO selector
                Pipe pipe = Pipe.open();
                ENXIOSelector enxioSelector = new ENXIOSelector(selector, pipe);
                if (enxioSelectors.isEmpty()) enxioSelectors = new ArrayList<ENXIOSelector>();
                enxioSelectors.add(enxioSelector);
                pipe.source().configureBlocking(false);
                pipe.source().register(getSelector(context, pipe.source()), SelectionKey.OP_READ, enxioSelector);
            } else if (mainSelector == null) {
                mainSelector = selector;
            }
        }

        return selector;
    }

    private Boolean[] getReadBlocking() {
        if (readBlocking == null) {
            readBlocking = new Boolean[readSize];
        }
        return readBlocking;
    }

    private Boolean[] getWriteBlocking() {
        if (writeBlocking == null) {
            writeBlocking = new Boolean[writeSize];
        }
        return writeBlocking;
    }

    private boolean[] getUnselectableReads() {
        if (unselectableReads == null) {
            unselectableReads = new boolean[readSize];
        }
        return unselectableReads;
    }

    private boolean[] getUnselectableWrites() {
        if (unselectableWrites == null) {
            unselectableWrites = new boolean[writeSize];
        }
        return unselectableWrites;
    }

    private boolean[] getPendingReads() {
        if (pendingReads == null) {
            pendingReads = new boolean[readSize];
        }
        return pendingReads;
    }

    private IRubyObject constructResults(Ruby runtime) {
        return RubyArray.newArrayLight(
                runtime,
                readResults == null ? RubyArray.newEmptyArray(runtime) : readResults,
                writeResults == null ? RubyArray.newEmptyArray(runtime) : writeResults,
                errorResults == null ? RubyArray.newEmptyArray(runtime) : errorResults);
    }

    private int fastSearch(Object[] ary, Object obj) {
        for (int i = 0; i < ary.length; i++) {
            if (ary[i] == obj) {
                return i;
            }
        }
        return -1;
    }

    private static void checkArrayType(Ruby runtime, IRubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            throw runtime.newTypeError("wrong argument type "
                    + obj.getMetaClass().getName() + " (expected Array)");
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean registerSelect(ThreadContext context, Selector selector, Map<Character,Integer> obj, RubyIO ioObj, int ops) throws IOException {
        Channel channel = ioObj.getChannel();
        if (channel == null || !(channel instanceof SelectableChannel)) {
            return false;
        }

        ((SelectableChannel) channel).configureBlocking(false);
        int real_ops = ((SelectableChannel) channel).validOps() & ops;
        SelectionKey key = ((SelectableChannel) channel).keyFor(selector);

        if (key == null) {
            Map<Character,Integer>  attachment = new HashMap<Character,Integer> (1);
            attachment.putAll(obj);
            ((SelectableChannel) channel).register(selector, real_ops, attachment );
        } else {
            key.interestOps(key.interestOps() | real_ops);
            Map<Character,Integer> att = (Map<Character,Integer>)key.attachment();
            att.putAll(obj);
            key.attach(att);
        }

        return true;
    }

    private static final class ENXIOSelector implements Callable<Object> {
        private final Selector selector;
        private final Pipe pipe;

        private ENXIOSelector(Selector selector, Pipe pipe) {
            this.selector = selector;
            this.pipe = pipe;
        }

        public Object call() throws Exception {
            try {
                selector.select();
            } finally {
                ByteBuffer buf = ByteBuffer.allocate(1);
                buf.put((byte) 0);
                buf.flip();
                pipe.sink().write(buf);
            }

            return null;
        }
    }
    
    Ruby runtime;
    RubyArray readArray = null;
    int readSize = 0;
    RubyIO[] readIOs = null;
    boolean[] unselectableReads = null;
    boolean[] pendingReads = null;
    Boolean[] readBlocking = null;
    int selectedReads = 0;
    RubyArray writeArray = null;
    int writeSize = 0;
    RubyIO[] writeIOs = null;
    boolean[] unselectableWrites = null;
    Boolean[] writeBlocking = null;
    int selectedWrites = 0;
    Selector mainSelector = null;
    Map<SelectorProvider, Selector> selectors = Collections.emptyMap();
    Collection<ENXIOSelector> enxioSelectors = Collections.emptyList();
    RubyArray readResults = null;
    RubyArray writeResults = null;
    RubyArray errorResults = null;
}
