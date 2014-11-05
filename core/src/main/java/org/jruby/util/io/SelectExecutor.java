package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyIO;
import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by headius on 6/3/14.
 */
public class SelectExecutor {
    public SelectExecutor(IRubyObject read, IRubyObject write, IRubyObject except, Long timeout) {
        this.read = read;
        this.write = write;
        this.except = except;
        this.timeout = timeout;
        this.runtime = read.getRuntime();
    }

    public IRubyObject go(ThreadContext context) {
        try {
            return selectCall(context);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } finally {
            try {selectEnd(context);} catch (IOException ioe) {}
        }
    }

    IRubyObject selectCall(ThreadContext context) throws IOException {
        return selectInternal(context);
    }

    IRubyObject selectEnd(ThreadContext context) throws IOException {
        fdTerm(readKeyList);
        fdTerm(writeKeyList);
        fdTerm(errorKeyList);

        for (Selector selector : selectors.values()) {
            // if it is a JDK selector, cache it
            if (selector.provider() == SelectorProvider.provider()) {
                // clear cancelled keys (with selectNow) and return to pool
                selector.selectNow();
                context.runtime.getSelectorPool().put(selector);
            } else {
                selector.close();
            }
        }

        for (ENXIOSelector enxioSelector : enxioSelectors) {
            enxioSelector.pipe.sink().close();
            enxioSelector.pipe.source().close();
        }

        // TODO: reset blocking status
//        if (readBlocking != null) {
//            for (int i = 0; i < readBlocking.length; i++) {
//                if (readBlocking[i] != null) {
//                    try {
//                        ((SelectableChannel) readIOs[i].getChannel()).configureBlocking(readBlocking[i]);
//                    } catch (IllegalBlockingModeException ibme) {
//                        throw runtime.newConcurrencyError("can not set IO blocking after select; concurrent select detected?");
//                    }
//                }
//            }
//        }
//        if (writeBlocking != null) {
//            for (int i = 0; i < writeBlocking.length; i++) {
//                if (writeBlocking[i] != null) {
//                    try {
//                        ((SelectableChannel) writeIOs[i].getChannel()).configureBlocking(writeBlocking[i]);
//                    } catch (IllegalBlockingModeException ibme) {
//                        throw runtime.newConcurrencyError("can not set IO blocking after select; concurrent select detected?");
//                    }
//                }
//            }
//        }

        return context.nil;
    }

    IRubyObject selectInternal(ThreadContext context) throws IOException {
        Ruby runtime = context.runtime;
        RubyArray res, list;
        OpenFile fptr;
        long i;

        RubyArray readAry = null;
        if (!read.isNil()) {
            readAry = read.convertToArray();
            for (i = 0; i < readAry.size(); i++) {
                fptr = TypeConverter.ioGetIO(runtime, readAry.eltOk(i)).getOpenFileChecked();
                fdSetRead(context, fptr.fd());
                if (fptr.READ_DATA_PENDING() || fptr.READ_CHAR_PENDING()) { /* check for buffered data */
                    if (pendingReadFDs == null) pendingReadFDs = new ArrayList(1);
                    pendingReadFDs.add(fptr.fd());
                }
            }
            if (pendingReadFDs != null) {		/* no blocking if there's buffered data */
                timeout = (long) 0;
            }
            readKeys = keyListToArray(readKeyList);
        } else {
            readKeys = null;
        }

        RubyArray writeAry = null;
        if (!write.isNil()) {
            writeAry = write.convertToArray();
            for (i = 0; i < writeAry.size(); i++) {
                RubyIO write_io = TypeConverter.ioGetIO(runtime, writeAry.eltOk(i)).GetWriteIO();
                fptr = write_io.getOpenFileChecked();
                fdSetWrite(context, fptr.fd());
            }
            writeKeys = keyListToArray(writeKeyList);
        } else {
            writeKeys = null;
        }

        RubyArray exceptAry = null;
        if (!except.isNil()) {
            // most of this is commented out since we can't select for error with JDK
            exceptAry = except.convertToArray();
            for (i = 0; i < exceptAry.size(); i++) {
                RubyIO io = TypeConverter.ioGetIO(runtime, exceptAry.eltOk(i));
                RubyIO write_io = io.GetWriteIO();
                fptr = io.getOpenFileChecked();
                if (io != write_io) {
                    fptr = write_io.getOpenFileChecked();
                }
            }
            errorKeys = keyListToArray(errorKeyList);
        } else {
            errorKeys = null;
        }

        int n = threadFdSelect(context);

        if (pendingReadFDs == null && n == 0 && unselectableReadFDs == null && unselectableWriteFDs == null) return context.nil; /* returns nil on timeout */

        res = RubyArray.newArray(runtime, 3);
        res.push(runtime.newArray());
        res.push(runtime.newArray());
        res.push(runtime.newArray());

        if (readKeys != null) {
            list = (RubyArray) res.eltOk(0);
            for (i = 0; i < readAry.size(); i++) {
                IRubyObject obj = readAry.eltOk(i);
                RubyIO io = TypeConverter.ioGetIO(runtime, obj);
                fptr = io.getOpenFileChecked();
                if (fdIsSet(readKeyList, fptr.fd(), READ_ACCEPT_OPS) || (pendingReadFDs != null && pendingReadFDs.contains(fptr.fd()))) {
                    list.push(obj);
                }
            }
        }
        if (unselectableReadFDs != null) {
            list = (RubyArray) res.eltOk(0);
            for (i = 0; i < readAry.size(); i++) {
                IRubyObject obj = readAry.eltOk(i);
                RubyIO io = TypeConverter.ioGetIO(runtime, obj);
                fptr = io.getOpenFileChecked();
                if (unselectableReadFDs.contains(fptr.fd())) {
                    list.push(obj);
                }
            }
        }

        if (writeKeys != null) {
            list = (RubyArray) res.eltOk(1);
            for (i = 0; i < writeAry.size(); i++) {
                IRubyObject obj = writeAry.eltOk(i);
                RubyIO io = TypeConverter.ioGetIO(runtime, obj);
                RubyIO write_io = io.GetWriteIO();
                fptr = write_io.getOpenFileChecked();
                if (fdIsSet(writeKeyList, fptr.fd(), WRITE_CONNECT_OPS)) {
                    list.push(obj);
                }
            }
        }
        if (unselectableWriteFDs != null) {
            list = (RubyArray) res.eltOk(1);
            for (i = 0; i < writeAry.size(); i++) {
                IRubyObject obj = writeAry.eltOk(i);
                RubyIO io = TypeConverter.ioGetIO(runtime, obj);
                fptr = io.getOpenFileChecked();
                if (unselectableWriteFDs.contains(fptr.fd())) {
                    list.push(obj);
                }
            }
        }

        if (errorKeys != null) {
            list = (RubyArray) res.eltOk(2);
            for (i = 0; i < exceptAry.size(); i++) {
                IRubyObject obj = exceptAry.eltOk(i);
                RubyIO io = TypeConverter.ioGetIO(runtime, obj);
                RubyIO write_io = io.GetWriteIO();
                fptr = io.getOpenFileChecked();
                if (errorKeyList.contains(fptr.fd())) {
                    list.push(obj);
                } else if (io != write_io) {
                    fptr = write_io.getOpenFileChecked();
                    if (errorKeyList.contains(fptr.fd())) {
                        list.push(obj);
                    }
                }
            }
        }

        return res;			/* returns an empty array on interrupt */
    }

    private void fdSetRead(ThreadContext context, ChannelFD fd) throws IOException {
        if (fd.chFile != null || fd.isNativeFile) {
            // files are not selectable, so we treat them as ready
            if (unselectableReadFDs == null) unselectableReadFDs = new ArrayList(1);
            unselectableReadFDs.add(fd);
            return;
        }
        SelectionKey key = trySelectRead(context, fd);
        if (key == null) return;
        if (readKeyList == null) readKeyList = new ArrayList(1);
        readKeyList.add(key);
    }

    private void fdSetWrite(ThreadContext context, ChannelFD fd) throws IOException {
        if (fd.chFile != null || fd.isNativeFile) {
            // files are not selectable, so we treat them as ready
            if (unselectableWriteFDs == null) unselectableWriteFDs = new ArrayList(1);
            unselectableWriteFDs.add(fd);
            return;
        }
        SelectionKey key = trySelectWrite(context, fd);
        if (key == null) return;
        if (writeKeyList == null) writeKeyList = new ArrayList(1);
        writeKeyList.add(key);
    }

    private boolean fdIsSet(List<SelectionKey> fds, ChannelFD fd, int operations) {
        if (fds == null) return false;

        for (SelectionKey key : fds) {
            if (key.isValid() && (key.readyOps() & operations) != 0 && ((Set<ChannelFD>)key.attachment()).contains(fd)) return true;
        }
        return false;
    }

    private void fdTerm(List<SelectionKey> keys) {
        if (keys == null) return;
        for (int i = 0; i < keys.size(); i++) {
            SelectionKey key = keys.get(i);
            killKey(key);
        }
    }

    private void killKey(SelectionKey key) {
        try {
            if (key.isValid()) key.cancel();
        } catch (Exception e) {}
    }

    private SelectionKey trySelectRead(ThreadContext context, ChannelFD fd) throws IOException {
        if (fd.chSelect != null) {
            return registerSelect(getSelector(context, fd.chSelect), fd, fd.chSelect, READ_ACCEPT_OPS);
        }
        return null;
    }

    private SelectionKey trySelectWrite(ThreadContext context, ChannelFD fd) throws IOException {
        if (fd.chSelect != null) {
            return registerSelect(getSelector(context, fd.chSelect), fd, fd.chSelect, WRITE_CONNECT_OPS);
        }
        return null;
    }

    private Selector getSelector(ThreadContext context, SelectableChannel channel) throws IOException {
        Selector selector = selectors.get(channel.provider());
        if (selector == null) {
            selector = context.runtime.getSelectorPool().get(channel.provider());
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

    private static SelectionKey registerSelect(Selector selector, ChannelFD attachment, SelectableChannel channel, int ops) throws IOException {
        channel.configureBlocking(false); // FIXME: I'm not sure we ever set it back to old blocking mode

        int real_ops = channel.validOps() & ops;

        SelectionKey key = channel.keyFor(selector);
        if (key != null) {
            key.interestOps(key.interestOps() | real_ops);
            ((Set<ChannelFD>)key.attachment()).add(attachment);
            return key;
        } else {
            Set<ChannelFD> attachmentSet = new HashSet<>();
            attachmentSet.add(attachment);
            return channel.register(selector, real_ops, attachmentSet);
        }

//        } else {
//            key.interestOps(key.interestOps() | real_ops);
//            Map<Character,Integer> att = (Map<Character,Integer>)key.attachment();
//            att.putAll(attachment);
//            key.attach(att);
//        }
//
//        return true;
    }

    private static SelectionKey[] keyListToArray(List<SelectionKey> fds) {
        if (fds == null) return null;
        return fds.toArray(new SelectionKey[fds.size()]);
    }

    // MRI: rb_thread_fd_select
    private int threadFdSelect(ThreadContext context) throws IOException {
        if (readKeys == null && writeKeys == null && errorKeys == null) {
            if (timeout == null) { // sleep forever
                try {
                    context.getThread().sleep(0);
                } catch (InterruptedException ie) {}
                return 0;
            }

            // 0 means "forever" for sleep below, but "zero" here
            if (timeout == 0) return 0;

            try {
                context.getThread().sleep(timeout);
            } catch (InterruptedException ie) {}
            return 0;
        }

        if (readKeys != null) {
//            rb_fd_resize(max - 1, read);
        }
        if (writeKeys != null) {
//            rb_fd_resize(max - 1, write);
        }
        if (errorKeys != null) {
//            rb_fd_resize(max - 1, except);
        }
        return doSelect(context);
    }

    private int doSelect(ThreadContext context) {
        int result;
        int lerrno;
        double limit = 0;
        long wait_rest;
        RubyThread th = context.getThread();

        // Sets up a specific time by which select should timeout
//        if (timeout != null) {
//            limit = timeofday();
//            limit += (double)timeout->tv_sec+(double)timeout->tv_usec*1e-6;
//            wait_rest = *timeout;
//            timeout = &wait_rest;
//        }

        retry:
        lerrno = 0;

        try {
            result = th.executeTask(context, this, SelectTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newErrnoEINTRError();
        }

        context.pollThreadEvents();

//        errno = lerrno;

//        if (result < 0) {
//            switch (errno) {
//                case EINTR:
//                    #ifdef ERESTART
//                case ERESTART:
//                    #endif
//                    if (read)
//                        rb_fd_dup(read, &orig_read);
//                    if (write)
//                        rb_fd_dup(write, &orig_write);
//                    if (except)
//                        rb_fd_dup(except, &orig_except);
//
//                    if (timeout) {
//                        double d = limit - timeofday();
//
//                        wait_rest.tv_sec = (time_t)d;
//                        wait_rest.tv_usec = (int)((d-(double)wait_rest.tv_sec)*1e6);
//                        if (wait_rest.tv_sec < 0)  wait_rest.tv_sec = 0;
//                        if (wait_rest.tv_usec < 0) wait_rest.tv_usec = 0;
//                    }
//
//                    goto retry;
//                default:
//                    break;
//            }
//        }

        // we don't kill the keys here because that breaks them
//        if (readKeys != null)
//            fdTerm(readKeys);
//        if (writeKeys != null)
//            fdTerm(writeKeys);
//        if (errorKeys != null);
//            fdTerm(errorKeys);

        return result;
    }

    private static RubyThread.Task<SelectExecutor, Integer> SelectTask = new RubyThread.Task<SelectExecutor, Integer>() {
        @Override
        public Integer run(ThreadContext context, SelectExecutor s) throws InterruptedException {
            int ready = 0;
            try {
                if (s.mainSelector != null) {
                    if (s.pendingReadFDs == null) {
                        if (s.timeout != null && s.timeout == 0) {
                            for (Selector selector : s.selectors.values()) ready += selector.selectNow();
                        } else {
                            List<Future> futures = new ArrayList<Future>(s.enxioSelectors.size());
                            for (ENXIOSelector enxioSelector : s.enxioSelectors) {
                                futures.add(context.runtime.getExecutor().submit(enxioSelector));
                            }

                            ready += s.mainSelector.select(s.timeout == null ? 0 : s.timeout);

                            // enxio selectors use a thread pool, since we can't select on multiple types
                            // of selector at once.
                            for (ENXIOSelector enxioSelector : s.enxioSelectors) enxioSelector.selector.wakeup();

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
                        for (Selector selector : s.selectors.values()) ready += selector.selectNow();
                    }
                }

                // If any enxio selectors woke up, remove them from the selected key set of the main selector
                for (ENXIOSelector enxioSelector : s.enxioSelectors) {
                    Pipe.SourceChannel source = enxioSelector.pipe.source();
                    SelectionKey key = source.keyFor(s.mainSelector);
                    if (key != null && s.mainSelector.selectedKeys().contains(key)) {
                        s.mainSelector.selectedKeys().remove(key);
                        ByteBuffer buf = ByteBuffer.allocate(1);
                        source.read(buf);
                    }
                }
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }

            return ready;
        }

        @Override
        public void wakeup(RubyThread thread, SelectExecutor selectExecutor) {
            // Should be ok to interrupt selectors or pipes used for selection
            thread.getNativeThread().interrupt();

            // wake up all selectors too
            for (Selector selector : selectExecutor.selectors.values()) selector.wakeup();
        }
    };

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

    final IRubyObject read, write, except;
    List<SelectionKey> readKeyList;
    List<SelectionKey> writeKeyList;
    List<SelectionKey> errorKeyList;
    List<ChannelFD> unselectableReadFDs;
    List<ChannelFD> unselectableWriteFDs;
    List<ChannelFD> pendingReadFDs;
    Selector mainSelector = null;
    Map<SelectorProvider, Selector> selectors = Collections.emptyMap();
    Collection<ENXIOSelector> enxioSelectors = Collections.emptyList();

    SelectionKey[] readKeys;
    SelectionKey[] writeKeys;
    SelectionKey[] errorKeys;
    Long timeout;
    final Ruby runtime;

    public static final int READ_ACCEPT_OPS = SelectionKey.OP_READ | SelectionKey.OP_ACCEPT;
    public static final int WRITE_CONNECT_OPS = SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
}
