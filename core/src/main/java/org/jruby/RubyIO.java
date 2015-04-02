/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
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
package org.jruby;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.posix.POSIX;
import org.jcodings.transcode.EConvFlags;
import org.jruby.runtime.Helpers;
import org.jruby.util.StringSupport;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.EncodingUtils;
import static org.jruby.util.io.EncodingUtils.vmodeVperm;
import static org.jruby.util.io.EncodingUtils.vperm;

import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.POSIXProcess;
import org.jruby.util.io.PopenExecutor;
import org.jruby.util.io.PosixShim;
import jnr.constants.platform.Fcntl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jcodings.Encoding;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.SelectExecutor;
import org.jruby.util.io.IOOptions;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.ShellLauncher;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.STDIO;
import org.jruby.util.io.OpenFile;

import org.jruby.runtime.Arity;

import static org.jruby.RubyEnumerator.enumeratorize;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.internal.runtime.ThreadedRunnable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ShellLauncher.POpenProcess;
import org.jruby.util.io.IOEncodable;

/**
 * 
 * @author jpetersen
 */
@JRubyClass(name="IO", include="Enumerable")
public class RubyIO extends RubyObject implements IOEncodable {
    // We use a highly uncommon string to represent the paragraph delimiter (100% soln not worth it)
    public static final ByteList PARAGRAPH_DELIMETER = ByteList.create("PARAGRPH_DELIM_MRK_ER");
    public static final ByteList PARAGRAPH_SEPARATOR = ByteList.create("\n\n");
    public static final String CLOSED_STREAM_MSG = "closed stream";

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public RubyIO(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    public RubyIO(Ruby runtime, OutputStream outputStream) {
        this(runtime, outputStream, true);
    }

    public RubyIO(Ruby runtime, OutputStream outputStream, boolean autoclose) {
        super(runtime, runtime.getIO());
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw runtime.newRuntimeError("Opening null stream");
        }
        
        openFile = MakeOpenFile();
        openFile.setFD(new ChannelFD(Channels.newChannel(outputStream), runtime.getPosix(), runtime.getFilenoUtil()));
        openFile.setMode(OpenFile.WRITABLE | OpenFile.APPEND);
        openFile.setAutoclose(autoclose);
    }
    
    public RubyIO(Ruby runtime, InputStream inputStream) {
        super(runtime, runtime.getIO());
        
        if (inputStream == null) {
            throw runtime.newRuntimeError("Opening null stream");
        }
        
        openFile = MakeOpenFile();
        openFile.setFD(new ChannelFD(Channels.newChannel(inputStream), runtime.getPosix(), runtime.getFilenoUtil()));
        openFile.setMode(OpenFile.READABLE);
    }
    
    public RubyIO(Ruby runtime, Channel channel) {
        this(runtime, runtime.getIO(), channel);
    }

    public RubyIO(Ruby runtime, RubyClass klass, Channel channel) {
        super(runtime, klass);

        // We only want IO objects with valid streams (better to error now).
        if (channel == null) {
            throw runtime.newRuntimeError("Opening null channel");
        }

        ThreadContext context = runtime.getCurrentContext();
        initializeCommon(context, new ChannelFD(channel, runtime.getPosix(), runtime.getFilenoUtil()), runtime.newFixnum(ModeFlags.oflagsFrom(runtime.getPosix(), channel)), context.nil);
    }

    public RubyIO(Ruby runtime, ShellLauncher.POpenProcess process, IOOptions ioOptions) {
        super(runtime, runtime.getIO());

        ioOptions = updateIOOptionsFromOptions(runtime.getCurrentContext(), null, ioOptions);

        openFile = MakeOpenFile();

        setupPopen(ioOptions.getModeFlags(), process);
    }

    // MRI: prep_stdio
    public static RubyIO prepStdio(Ruby runtime, InputStream f, Channel c, int fmode, RubyClass klass, String path) {
        OpenFile fptr;
        RubyIO io = prepIO(runtime, c, fmode | OpenFile.PREP | EncodingUtils.DEFAULT_TEXTMODE, klass, path);

        fptr = io.getOpenFileChecked();

        // Use standard stdio filenos if we're using System.in et al.
        if (f == System.in) {
            fptr.fd().realFileno = 0;
        }

        prepStdioEcflags(fptr, fmode);
        fptr.stdio_file = f;

        // We checkTTY again here because we're using stdout/stdin to indicate this is stdio
        return recheckTTY(runtime, fptr, io);
    }

    // MRI: prep_stdio
    public static RubyIO prepStdio(Ruby runtime, OutputStream f, Channel c, int fmode, RubyClass klass, String path) {
        OpenFile fptr;
        RubyIO io = prepIO(runtime, c, fmode | OpenFile.PREP | EncodingUtils.DEFAULT_TEXTMODE, klass, path);

        fptr = io.getOpenFileChecked();

        // Use standard stdio filenos if we're using System.in et al.
        if (f == System.out) {
            fptr.fd().realFileno = 1;
        } else if (f == System.err) {
            fptr.fd().realFileno = 2;
        }

        prepStdioEcflags(fptr, fmode);
        fptr.stdio_file = f;

        return recheckTTY(runtime, fptr, io);
    }

    private static RubyIO recheckTTY(Ruby runtime, OpenFile fptr, RubyIO io) {
        // We checkTTY again here because we're using stdout/stdin to indicate this is stdio
        fptr.checkTTY();

        return io;
    }

    // MRI: part of prep_stdio
    private static void prepStdioEcflags(OpenFile fptr, int fmode) {
        boolean locked = fptr.lock();
        try {
            fptr.encs.ecflags |= EncodingUtils.ECONV_DEFAULT_NEWLINE_DECORATOR;
            if (EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != 0) {
                fptr.encs.ecflags |= EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE;
                if ((fmode & OpenFile.READABLE) != 0) {
                    fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }
    }

    // MRI: prep_io
    private static RubyIO prepIO(Ruby runtime, Channel fd, int fmode, RubyClass klass, String path) {
        OpenFile fp;
        RubyIO io = (RubyIO)klass.allocate();

        fp = io.MakeOpenFile();
        fp.setChannel(fd);
        // Can we determine this?
//        if (Platform.IS_CYGWIN) {
//            if (!runtime.getPosix().isatty(fd)) {
//                fmode |= OpenFile.BINMODE;
                // TODO: setmode O_BINARY means what via NIO?
//                setmode(fd, OpenFlags.O_BINARY);
//            }
//        }
        fp.setMode(fmode);
        fp.checkTTY();
        if (path != null) fp.setPath(path);
//        rb_update_max_fd(fd);

        return io;
    }
    
    public static RubyIO newIO(Ruby runtime, Channel channel) {
        return new RubyIO(runtime, channel);
    }
    
    public OpenFile getOpenFile() {
        return openFile;
    }
    
    public OpenFile getOpenFileChecked() {
        openFile.checkClosed();
        return openFile;
    }
    
    private static ObjectAllocator IO_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIO(runtime, klass);
        }
    };

    /*
     * We use FILE versus IO to match T_FILE in MRI.
     */
    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.FILE;
    }

    public static RubyClass createIOClass(Ruby runtime) {
        RubyClass ioClass = runtime.defineClass("IO", runtime.getObject(), IO_ALLOCATOR);

        ioClass.setClassIndex(ClassIndex.IO);
        ioClass.setReifiedClass(RubyIO.class);

        ioClass.kindOf = new RubyModule.JavaClassKindOf(RubyIO.class);

        ioClass.includeModule(runtime.getEnumerable());
        
        ioClass.defineAnnotatedMethods(RubyIO.class);

        // Constants for seek
        ioClass.setConstant("SEEK_SET", runtime.newFixnum(PosixShim.SEEK_SET));
        ioClass.setConstant("SEEK_CUR", runtime.newFixnum(PosixShim.SEEK_CUR));
        ioClass.setConstant("SEEK_END", runtime.newFixnum(PosixShim.SEEK_END));

        ioClass.defineModuleUnder("WaitReadable");
        ioClass.defineModuleUnder("WaitWritable");

        return ioClass;
    }

    public OutputStream getOutStream() {
        // FIXME: Could be faster by caching bytelist or string rather than creating for every call
        return new OutputStream() {
            final Ruby runtime = getRuntime();

            @Override
            public void write(int b) throws IOException {
                putc(runtime.getCurrentContext(), runtime.newFixnum(b));
            }

            @Override
            public void write(byte[] b) throws IOException {
                RubyIO.this.write(runtime.getCurrentContext(), RubyString.newStringNoCopy(runtime, b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                RubyIO.this.write(runtime.getCurrentContext(), RubyString.newStringNoCopy(runtime, b, off, len));
            }

            @Override
            public void flush() throws IOException {
                RubyIO.this.flush(runtime.getCurrentContext());
            }

            @Override
            public void close() throws IOException {
                RubyIO.this.close();
            }
        };
    }

    public InputStream getInStream() {
        // FIXME: Could be faster by caching bytelist or string rather than creating for every call
        return new InputStream() {
            final Ruby runtime = getRuntime();

            @Override
            public int read() throws IOException {
                return getByte(runtime.getCurrentContext());
            }

            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                RubyFixnum c = runtime.newFixnum(len);
                RubyString s = RubyString.newStringNoCopy(runtime, b, off, len);
                IRubyObject i = RubyIO.this.read(runtime.getCurrentContext(), c, s);
                if (i.isNil()) return -1;
                return s.size();
            }

            @Override
            public long skip(long n) throws IOException {
                return seek(runtime.getCurrentContext(), runtime.newFixnum(PosixShim.SEEK_CUR), runtime.newFixnum(n)).getLongValue();
            }

            @Override
            public int available() throws IOException {
                if (RubyIO.this instanceof RubyFile) {
                    long size = ((RubyFixnum)((RubyFile)RubyIO.this).size(runtime.getCurrentContext())).getLongValue();
                    if (size == 0) return 0;
                    if (size >= 0) return (int)(size - pos(runtime.getCurrentContext()).getLongValue());
                }
                return 0;
            }

            @Override
            public void close() throws IOException {
                RubyIO.this.close();
            }
        };
    }

    /**
     * Get the underlying channel from this IO object. Note that IO buffers data internally, so the channel returned
     * here may have been read into those buffers. If the channel and the IO are both being used at the same time, the
     * stream will get out of sync.
     *
     * @return the underlying channel for this IO
     */
    public Channel getChannel() {
        // FIXME: Do we want to make a faux channel that is backed by IO's buffering? Or turn buffering off?
        return openFile.channel();
    }

    // io_reopen
    protected RubyIO reopenIO(ThreadContext context, RubyIO nfile) {
        Ruby runtime = context.runtime;
        OpenFile fptr, orig;
        ChannelFD fd, fd2;
        long pos = 0;

        nfile = TypeConverter.ioGetIO(runtime, nfile);
        fptr = getOpenFileChecked();
        orig = nfile.getOpenFileChecked();

        if (fptr == orig) return this;
        if (fptr.IS_PREP_STDIO()) {
            if ((fptr.stdio_file == System.in && !orig.isReadable()) ||
                    (fptr.stdio_file == System.out && !orig.isWritable()) ||
                    (fptr.stdio_file == System.err && !orig.isWritable())) {
                throw runtime.newArgumentError(fptr.PREP_STDIO_NAME() + " can't change access mode from \"" + fptr.getModeAsString(runtime) + "\" to \"" + orig.getModeAsString(runtime) + "\"");
            }
        }
        // FIXME: three lock acquires...trying to reduce risk of deadlock, but not sure it's possible.

        boolean locked = fptr.lock();
        try {
            if (fptr.isWritable()) {
                if (fptr.io_fflush(context) < 0)
                    throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            } else {
                fptr.tell(context);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        locked = orig.lock();
        try {
            if (orig.isReadable()) {
                pos = orig.tell(context);
            }
            if (orig.isWritable()) {
                if (orig.io_fflush(context) < 0)
                    throw runtime.newErrnoFromErrno(orig.errno(), fptr.getPath());
            }
        } finally {
            if (locked) orig.unlock();
        }

        /* copy rb_io_t structure */
        // NOTE: MRI does not copy sync here, but I can find no way to make stdout/stderr stay sync through a reopen
        locked = fptr.lock();
        boolean locked2 = orig.lock(); // TODO: This WILL deadlock if two threads try to reopen the same IOs in opposite directions. Fix?
        try {
            fptr.setMode(orig.getMode() | (fptr.getMode() & (OpenFile.PREP | OpenFile.SYNC)));
            fptr.setProcess(orig.getProcess());
            fptr.setLineNumber(orig.getLineNumber());
            if (orig.getPath() != null) fptr.setPath(orig.getPath());
            else if (!fptr.IS_PREP_STDIO()) fptr.setPath(null);
            fptr.setFinalizer(orig.getFinalizer());

            // TODO: unsure what to do here
            //        #if defined (__CYGWIN__) || !defined(HAVE_FORK)
            //        if (fptr->finalize == pipe_finalize)
            //            pipe_add_fptr(fptr);
            //        #endif

            fd = fptr.fd();
            fd2 = orig.fd();
            if (fd != fd2) {
                if (fptr.IS_PREP_STDIO() || fd.bestFileno() <= 2 || fptr.stdio_file == null) {
                    /* need to keep FILE objects of stdin, stdout and stderr */
                    checkReopenCloexecDup2(runtime, orig, fd2, fd);
                    //                rb_update_max_fd(fd);
                    fptr.setFD(fd);

                    //                // MRI does not do this, but we seem to need to set some types of channels to sync if they
                    //                // are reopened as stdout/stderr.
                    //                if (fptr.stdio_file == System.out || fptr.stdio_file == System.err) {
                    //                    fd.chFile.force();
                    //                }
                } else {
                    if (fptr.stdio_file != null) try {
                        fptr.stdio_file.close();
                    } catch (IOException ioe) {
                    }
                    fptr.clearStdio();
                    fptr.setFD(null);
                    checkReopenCloexecDup2(runtime, orig, fd2, fd);
                    //                rb_update_max_fd(fd);
                    fptr.setFD(fd);
                }
                // TODO: clear interrupts waiting on this IO?
                //            rb_thread_fd_close(fd);
                if (orig.isReadable() && pos >= 0) {
                    fptr.checkReopenSeek(context, runtime, pos);
                    orig.checkReopenSeek(context, runtime, pos);
                }
            }

            if (fptr.isBinmode()) {
                setBinmode();
            }
        } finally {
            if (locked2) orig.unlock();
            if (locked) fptr.unlock();
        }

        // We simply can't do this and still have real concrete types under RubyIO
//        setMetaClass(nfile.getMetaClass());
        return this;
    }

    private void checkReopenCloexecDup2(Ruby runtime, OpenFile orig, ChannelFD oldfd, ChannelFD newfd) {
        OpenFile.cloexecDup2(new PosixShim(runtime), oldfd, newfd);
    }

    // rb_io_binmode
    private void setBinmode() {
        OpenFile fptr;

        fptr = getOpenFileChecked();
        boolean locked = fptr.lock();
        try {
            if (fptr.readconv != null)
                fptr.readconv.binmode();
            if (fptr.writeconv != null)
                fptr.writeconv.binmode();
            fptr.setBinmode();
            fptr.clearTextMode();
            fptr.writeconvPreEcflags &= ~EConvFlags.NEWLINE_DECORATOR_MASK;
            if (OpenFlags.O_BINARY.defined()) {
                // TODO: Windows
                //            if (fptr.readconv == null) {
                //                SET_BINARY_MODE_WITH_SEEK_CUR(fptr);
                //            }
                //            else {
                // TODO: setmode O_BINARY means what via NIO?
                //                setmode(fptr->fd, O_BINARY);
                //            }
            }
        } finally {
            if (locked) fptr.unlock();
        }
        return;
    }

    // MRI: rb_io_reopen
    @JRubyMethod(name = "reopen", required = 1, optional = 1)
    public IRubyObject reopen(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyIO file = this;
        IRubyObject fname = context.nil, nmode = context.nil, opt = context.nil;
        int[] oflags_p = {0};
        OpenFile fptr;

        switch (args.length) {
            case 3:
                opt = TypeConverter.checkHashType(runtime, args[2]);
                if (opt.isNil()) throw getRuntime().newArgumentError(3, 2);
            case 2:
                if (opt.isNil()) {
                    opt = TypeConverter.checkHashType(runtime, args[1]);
                    if (opt.isNil()) {
                        nmode = args[1];
                        opt = context.nil;
                    }
                } else {
                    nmode = args[1];
                }
            case 1:
                fname = args[0];
        }
        if (args.length == 1) {
            IRubyObject tmp = TypeConverter.ioCheckIO(runtime, fname);
            if (!tmp.isNil()) {
                return file.reopenIO(context, (RubyIO)tmp);
            }
        }

        fname = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, fname));
        // Not implemented
//        fname.checkTaint();
        fptr = file.openFile;
        if (fptr == null) {
            fptr = file.openFile = MakeOpenFile();
        }

        boolean locked = fptr.lock();
        try {
            if (!nmode.isNil() || !opt.isNil()) {
                ConvConfig convconfig = new ConvConfig();
                Object vmode_vperm = vmodeVperm(nmode, null);
                int[] fmode_p = {0};

                EncodingUtils.extractModeEncoding(context, convconfig, vmode_vperm, opt, oflags_p, fmode_p);
                if (fptr.IS_PREP_STDIO() &&
                        ((fptr.getMode() & OpenFile.READWRITE) & (fmode_p[0] & OpenFile.READWRITE)) !=
                                (fptr.getMode() & OpenFile.READWRITE)) {
                    throw runtime.newArgumentError(fptr.PREP_STDIO_NAME() + " can't change access mode from \"" + fptr.getModeAsString(runtime) + "\" to \"" + OpenFile.getStringFromMode(fmode_p[0]));
                }
                fptr.setMode(fmode_p[0]);
                fptr.encs = convconfig;
            } else {
                oflags_p[0] = OpenFile.getModeFlagsAsIntFrom(fptr.getMode());
            }

            fptr.setPath(fname.toString());
            if (fptr.fd() == null) {
                fptr.setFD(sysopen(runtime, fptr.getPath(), oflags_p[0], 0666));
                fptr.clearStdio();
                return file;
            }

            if (fptr.isWritable()) {
                if (fptr.io_fflush(context) < 0)
                    throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            }
            fptr.rbuf.off = fptr.rbuf.len = 0;

            if (fptr.isStdio()) {
                // Logic here reopens the stdio FILE* with a new path and mode. For our purposes, we skip this
                // since we do not want to damage the stdio streams
                //            if (freopen(RSTRING_PTR(fptr.pathv), rb_io_oflags_modestr(oflags), fptr.stdio_file) == 0) {
                //                rb_sys_fail_path(fptr.pathv);
                //            }
                fptr.setFD(sysopen(runtime, fptr.getPath(), oflags_p[0], 0666));

                // This logic fixes the original stdio file descriptor by clearing any CLOEXEC that might have
                // come across with the newly opened file. Since we do not yet support CLOEXEC, we skip this.
                //            fptr.fd = fileno(fptr.stdio_file);
                //            rb_fd_fix_cloexec(fptr.fd);

                // This logic configures buffering (none, line, full) and buffer size to match the original stdio
                // stream associated with this IO. I don't believe we can do this.
                //                #ifdef USE_SETVBUF
                //                if (setvbuf(fptr.stdio_file, NULL, _IOFBF, 0) != 0)
                //                    rb_warn("setvbuf() can't be honoured for %"PRIsVALUE, fptr.pathv);
                //                #endif
                //                if (fptr.stdio_file == stderr) {
                //                    if (setvbuf(fptr.stdio_file, NULL, _IONBF, BUFSIZ) != 0)
                //                        rb_warn("setvbuf() can't be honoured for %"PRIsVALUE, fptr.pathv);
                //                }
                //                else if (fptr.stdio_file == stdout && isatty(fptr.fd)) {
                //                    if (setvbuf(fptr.stdio_file, NULL, _IOLBF, BUFSIZ) != 0)
                //                        rb_warn("setvbuf() can't be honoured for %"PRIsVALUE, fptr.pathv);
                //                }
            } else {
                ChannelFD tmpfd = sysopen(runtime, fptr.getPath(), oflags_p[0], 0666);
                Errno err = null;
                if (fptr.cloexecDup2(fptr.posix, tmpfd, fptr.fd()) < 0)
                    err = fptr.errno();

                if (err != null) {
                    throw runtime.newErrnoFromErrno(err, fptr.getPath());
                }
                fptr.setFD(tmpfd);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return file;
    }

    public IRubyObject getline(ThreadContext context, IRubyObject separator) {
        return getline(context, separator, -1, null);
    }

    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     *
     */
    public IRubyObject getline(ThreadContext context, IRubyObject separator, long limit) {
        return getline(context, separator, limit, null);
    }

    private IRubyObject getline(ThreadContext context, IRubyObject separator, long limit, ByteListCache cache) {
        return getlineInner(context, separator, (int)limit, cache);
    }
    
    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     * mri: rb_io_getline_1 (mostly)
     */
    private IRubyObject getlineInner(ThreadContext context, IRubyObject rs, int _limit, ByteListCache cache) {
        Ruby runtime = context.runtime;
        IRubyObject str = context.nil;
        boolean noLimit = false;
        Encoding enc;
        
        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);

            if (rs.isNil() && _limit < 0) {
                str = fptr.readAll(context, 0, context.nil);
                if (((RubyString) str).size() == 0) return context.nil;
            } else if (_limit == 0) {
                return RubyString.newEmptyString(runtime, fptr.readEncoding(runtime));
            } else if (
                    rs == runtime.getGlobalVariables().getDefaultSeparator()
                            && _limit < 0
                            && !fptr.needsReadConversion()
                            && (enc = fptr.readEncoding(runtime)).isAsciiCompatible()) {
                fptr.NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
                return fptr.getlineFast(context, enc, this);
            }

            // slow path logic
            int c, newline = -1;
            byte[] rsptrBytes = null;
            int rsptr = 0;
            int rslen = 0;
            boolean rspara = false;
            int extraLimit = 16;

            fptr.SET_BINARY_MODE();
            enc = getReadEncoding();

            if (!rs.isNil()) {
                RubyString rsStr = (RubyString) rs;
                ByteList rsByteList = rsStr.getByteList();
                rslen = rsByteList.getRealSize();
                if (rslen == 0) {
                    rsptrBytes = PARAGRAPH_SEPARATOR.unsafeBytes();
                    rsptr = PARAGRAPH_SEPARATOR.getBegin();
                    rslen = 2;
                    rspara = true;
                    fptr.swallow(context, '\n');
                    rs = null;
                    if (!enc.isAsciiCompatible()) {
                        rs = RubyString.newUsAsciiStringShared(runtime, rsptrBytes, rsptr, rslen);
                        rs = EncodingUtils.rbStrEncode(context, rs, runtime.getEncodingService().convertEncodingToRubyEncoding(enc), 0, context.nil);
                        rs.setFrozen(true);
                        rsStr = (RubyString) rs;
                        rsByteList = rsStr.getByteList();
                        rsptrBytes = rsByteList.getUnsafeBytes();
                        rsptr = rsByteList.getBegin();
                        rslen = rsByteList.getRealSize();
                    }
                } else {
                    rsptrBytes = rsByteList.unsafeBytes();
                    rsptr = rsByteList.getBegin();
                }
                newline = rsptrBytes[rsptr + rslen - 1] & 0xFF;
            }

            ByteList buf = cache != null ? cache.allocate(0) : new ByteList(0);
            try {
                boolean bufferString = str instanceof RubyString;
                ByteList[] strPtr = {bufferString ? ((RubyString) str).getByteList() : null};

                int[] limit_p = {_limit};
                while ((c = fptr.appendline(context, newline, strPtr, limit_p)) != OpenFile.EOF) {
                    int s, p, pp, e;

                    if (c == newline) {
                        if (strPtr[0].getRealSize() < rslen) continue;
                        s = strPtr[0].getBegin();
                        e = s + strPtr[0].getRealSize();
                        p = e - rslen;
                        pp = enc.leftAdjustCharHead(strPtr[0].getUnsafeBytes(), s, p, e);
                        if (pp != p) continue;
                        if (ByteList.memcmp(strPtr[0].getUnsafeBytes(), p, rsptrBytes, rsptr, rslen) == 0) break;
                    }
                    if (limit_p[0] == 0) {
                        s = strPtr[0].getBegin();
                        p = s + strPtr[0].getRealSize();
                        pp = enc.leftAdjustCharHead(strPtr[0].getUnsafeBytes(), s, p - 1, p);
                        if (extraLimit != 0 &&
                                StringSupport.MBCLEN_NEEDMORE_P(StringSupport.preciseLength(enc, strPtr[0].getUnsafeBytes(), pp, p))) {
                            limit_p[0] = 1;
                            extraLimit--;
                        } else {
                            noLimit = true;
                            break;
                        }
                    }
                }
                _limit = limit_p[0];
                if (strPtr[0] != null) {
                    if (bufferString) {
                        if (strPtr[0] != ((RubyString) str).getByteList()) {
                            ((RubyString) str).setValue(strPtr[0]);
                        } else {
                            // same BL as before
                        }
                    } else {
                        // create string
                        str = runtime.newString(strPtr[0]);
                    }
                }

                if (rspara && c != OpenFile.EOF) {
                    // FIXME: This may block more often than it should, to clean up extraneous newlines
                    fptr.swallow(context, '\n');
                }
                if (!str.isNil()) {
                    str = EncodingUtils.ioEncStr(runtime, str, fptr);
                }
            } finally {
                if (cache != null) cache.release(buf);
            }

            if (!str.isNil() && !noLimit) {
                fptr.incrementLineno(runtime);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return str;
    }

    // fptr->enc and codeconv->enc
    public Encoding getEnc() {
        return openFile.encs.enc;
    }
    
    // mri: io_read_encoding
    public Encoding getReadEncoding() {
        return openFile.readEncoding(getRuntime());
    }
    
    // fptr->enc2 and codeconv->enc2
    public Encoding getEnc2() {
        return openFile.encs.enc2;
    }
    
    // mri: io_input_encoding
    public Encoding getInputEncoding() {
        return openFile.inputEncoding(getRuntime());
    }

    private static final String VENDOR;
    static { String v = SafePropertyAccessor.getProperty("java.VENDOR") ; VENDOR = (v == null) ? "" : v; };
    private static final String msgEINTR = "Interrupted system call";

    // FIXME: We needed to use this to raise an appropriate error somewhere...find where...I think IRB related when suspending process?
    public static boolean restartSystemCall(Exception e) {
        return VENDOR.startsWith("Apple") && e.getMessage().equals(msgEINTR);
    }

    // IO class methods.

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass)recv;
        
        if (block.isGiven()) {
            String className = klass.getName();
            context.runtime.getWarnings().warn(
                    ID.BLOCK_NOT_ACCEPTED,
                    className + "::new() does not take block; use " + className + "::open() instead");
        }
        
        return klass.newInstance(context, args, block);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass)recv;

        return klass.newInstance(context, args, block);
    }

    private IRubyObject initializeCommon(ThreadContext context, int fileno, IRubyObject vmodeArg, IRubyObject opt) {
        Ruby runtime = context.runtime;

        ChannelFD fd;

        if (!FilenoUtil.isFake(fileno)) {
            // try using existing ChannelFD, then fall back on creating a new one
            fd = runtime.getFilenoUtil().getWrapperFromFileno(fileno);

            if (fd == null) {
                fd = new ChannelFD(new NativeDeviceChannel(fileno), runtime.getPosix(), runtime.getFilenoUtil());
            }
        } else {
            ChannelFD descriptor = runtime.getFilenoUtil().getWrapperFromFileno(fileno);

            if (descriptor == null) throw runtime.newErrnoEBADFError();

            fd = descriptor;
        }

        if (!fd.ch.isOpen()) {
            throw runtime.newErrnoEBADFError();
        }

        return initializeCommon(context, fd, vmodeArg, opt);
    }

    private IRubyObject initializeCommon(ThreadContext context, ChannelFD fd, IRubyObject vmodeArg, IRubyObject opt) {
        Ruby runtime = context.runtime;

        int ofmode;
        int[] oflags_p = {ModeFlags.RDONLY};

        if(opt != null && !opt.isNil() && !(opt instanceof RubyHash) && !(opt.respondsTo("to_hash"))) {
            throw runtime.newArgumentError("last argument must be a hash!");
        }

        if (opt != null && !opt.isNil()) {
            opt = opt.convertToHash();
        }

        if (!fd.ch.isOpen()) {
            throw runtime.newErrnoEBADFError();
        }

        Object pm = EncodingUtils.vmodeVperm(vmodeArg, runtime.newFixnum(0));
        int[] fmode_p = {0};
        ConvConfig convconfig = new ConvConfig();
        EncodingUtils.extractModeEncoding(context, convconfig, pm, opt, oflags_p, fmode_p);

        { // normally done with fcntl...which we *could* do too...but this is just checking read/write
            oflags_p[0] = ModeFlags.oflagsFrom(runtime.getPosix(), fd.ch);

            ofmode = ModeFlags.getOpenFileFlagsFor(oflags_p[0]);
            if (EncodingUtils.vmode(pm) == null || EncodingUtils.vmode(pm).isNil()) {
                fmode_p[0] = ofmode;
            } else if (((~ofmode & fmode_p[0]) & OpenFile.READWRITE) != 0) {
                throw runtime.newErrnoEINVALError();
            }
        }

        if (opt != null && !opt.isNil() && ((RubyHash)opt).op_aref(context, runtime.newSymbol("autoclose")) == runtime.getFalse()) {
            fmode_p[0] |= OpenFile.PREP;
        }

        // JRUBY-4650: Make sure we clean up the old data, if it's present.
        MakeOpenFile();

        openFile.setFD(fd);
        openFile.setMode(fmode_p[0]);
        openFile.encs = convconfig;
        openFile.clearCodeConversion();

        openFile.checkTTY();
        switch (fd.bestFileno()) {
            case 0:
                openFile.stdio_file = System.in;
                break;
            case 1:
                openFile.stdio_file = System.out;
                break;
            case 2:
                openFile.stdio_file = System.err;
                break;
        }

        if (openFile.isBOM()) {
            EncodingUtils.ioSetEncodingByBOM(context, this);
        }

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject fileNumber, Block unused) {
        return initializeCommon(context, RubyNumeric.fix2int(fileNumber), null, context.nil);
    }
    
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject fileNumber, IRubyObject second, Block unused) {
        int fileno = RubyNumeric.fix2int(fileNumber);
        IRubyObject vmode = null;
        IRubyObject options;
        IRubyObject hashTest = TypeConverter.checkHashType(context.runtime, second);
        if (hashTest instanceof RubyHash) {
            options = hashTest;
        } else {
            options = context.nil;
            vmode = second;
        }

        return initializeCommon(context, fileno, vmode, options);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject fileNumber, IRubyObject modeValue, IRubyObject options, Block unused) {
        int fileno = RubyNumeric.fix2int(fileNumber);

        return initializeCommon(context, fileno, modeValue, options);
    }

    // Encoding processing
    protected IOOptions parseIOOptions(IRubyObject arg) {
        Ruby runtime = getRuntime();

        if (arg instanceof RubyFixnum) return newIOOptions(runtime, (int) RubyFixnum.fix2long(arg));

        String modeString = arg.convertToString().toString();
        try {
            return new IOOptions(runtime, modeString);
        } catch (InvalidValueException ive) {
            throw runtime.newArgumentError("invalid access mode " + modeString);
        }
    }

    @JRubyMethod
    public IRubyObject external_encoding(ThreadContext context) {
        EncodingService encodingService = context.runtime.getEncodingService();
        
        if (openFile.encs.enc2 != null) return encodingService.getEncoding(openFile.encs.enc2);
        
        if (openFile.isWritable()) {
            return openFile.encs.enc == null ? context.runtime.getNil() : encodingService.getEncoding(openFile.encs.enc);
        }
        
        return encodingService.getEncoding(getReadEncoding());
    }

    @JRubyMethod
    public IRubyObject internal_encoding(ThreadContext context) {
        if (openFile.encs.enc2 == null) return context.nil;
        
        return context.runtime.getEncodingService().getEncoding(getReadEncoding());
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingObj) {
        setEncoding(context, encodingObj, context.nil, context.nil);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding) {
        IRubyObject opt = TypeConverter.checkHashType(context.runtime, internalEncoding);
        if (!opt.isNil()) {
            setEncoding(context, encodingString, context.nil, opt);
        } else {
            setEncoding(context, encodingString, internalEncoding, context.nil);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding, IRubyObject options) {
        setEncoding(context, encodingString, internalEncoding, options);

        return context.nil;
    }
    
    // mri: io_encoding_set
    public void setEncoding(ThreadContext context, IRubyObject v1, IRubyObject v2, IRubyObject opt) {
        IOEncodable.ConvConfig holder = new IOEncodable.ConvConfig();
        int ecflags = openFile.encs.ecflags;
        IRubyObject[] ecopts_p = {context.nil};
        IRubyObject tmp;
        
        if (!v2.isNil()) {
            holder.enc2 = EncodingUtils.rbToEncoding(context, v1);
            tmp = v2.checkStringType19();
            
            if (!tmp.isNil()) {
                RubyString internalAsString = (RubyString)tmp;
                
                // No encoding '-'
                if (internalAsString.size() == 1 && internalAsString.asJavaString().equals("-")) {
                    /* Special case - "-" => no transcoding */
                    holder.enc = holder.enc2;
                    holder.enc2 = null;
                } else {
                    holder.enc = EncodingUtils.rbToEncoding(context, internalAsString);
                }
                
                if (holder.enc == holder.enc2) {
                    /* Special case - "-" => no transcoding */
                    holder.enc2 = null;
                }
            } else {
                holder.enc = EncodingUtils.rbToEncoding(context, v2);
                
                if (holder.enc == holder.enc2) {
                    /* Special case - "-" => no transcoding */
                    holder.enc2 = null;
                }
            }
            EncodingUtils.SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(holder.getEnc2(), ecflags);
            ecflags = EncodingUtils.econvPrepareOptions(context, opt, ecopts_p, ecflags);
        } else {
            if (v1.isNil()) {
                EncodingUtils.ioExtIntToEncs(context, holder, null, null, 0);
                EncodingUtils.SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(holder.getEnc2(), ecflags);
                ecopts_p[0] = context.nil;
            } else {
                tmp = v1.checkStringType19();
                if (!tmp.isNil() && EncodingUtils.encAsciicompat(EncodingUtils.encGet(context, tmp))) {
                    EncodingUtils.parseModeEncoding(context, holder, tmp.asJavaString(), null);
                    EncodingUtils.SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(holder.getEnc2(), ecflags);
                    ecflags = EncodingUtils.econvPrepareOptions(context, opt, ecopts_p, ecflags);
                } else {
                    EncodingUtils.ioExtIntToEncs(context, holder, EncodingUtils.rbToEncoding(context, v1), null, 0);
                    EncodingUtils.SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(holder.getEnc2(), ecflags);
                }
            }
            // enc, enc2 should be set by now
        }

        int[] fmode_p = {openFile.getMode()};
        EncodingUtils.validateEncodingBinmode(context, fmode_p, ecflags, holder);
        openFile.setMode(fmode_p[0]);

        openFile.encs.enc = holder.enc;
        openFile.encs.enc2 = holder.enc2;
        openFile.encs.ecflags = ecflags;
        openFile.encs.ecopts = ecopts_p[0];

        openFile.clearCodeConversion();
    }

    // rb_io_s_open, 2014/5/16
    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject io = ((RubyClass)recv).newInstance(context, args, Block.NULL_BLOCK);

        return ensureYieldClose(context, io, block);
    }

    public static IRubyObject ensureYieldClose(ThreadContext context, IRubyObject port, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.runtime;
            try {
                return block.yield(context, port);
            } finally {
                ioClose(runtime, port);
            }
        }
        return port;
    }

    public static IRubyObject sysopen(IRubyObject recv, IRubyObject[] args, Block block) {
        return sysopen19(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    // rb_io_s_sysopen
    @JRubyMethod(name = "sysopen", required = 1, optional = 2, meta = true)
    public static IRubyObject sysopen19(ThreadContext context, IRubyObject recv, IRubyObject[] argv, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject fname, vmode, vperm;
        fname = vmode = vperm = context.nil;
        IRubyObject intmode;
        int oflags;
        ChannelFD fd;
        int perm;

        switch (argv.length) {
            case 3:
                vperm = argv[2];
            case 2:
                vmode = argv[1];
            case 1:
                fname = argv[0];
        }
        fname = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, fname));

        if (vmode.isNil())
            oflags = OpenFlags.O_RDONLY.intValue();
        else if (!(intmode = TypeConverter.checkIntegerType(runtime, vmode, "to_int")).isNil())
            oflags = RubyNumeric.num2int(intmode);
        else {
            vmode = vmode.convertToString();
            oflags = OpenFile.ioModestrOflags(runtime, vmode.toString());
        }
        if (vperm.isNil()) perm = 0666;
        else              perm = RubyNumeric.num2int(vperm);

        StringSupport.checkStringSafety(context.runtime, fname);
        fname = ((RubyString)fname).dupFrozen();
        fd = sysopen(runtime, fname.toString(), oflags, perm);
        return runtime.newFixnum(fd.bestFileno());
    }

    private static class Sysopen {
        String fname;
        int oflags;
        int perm;
        Errno errno;
    }

    // rb_sysopen
    protected static ChannelFD sysopen(Ruby runtime, String fname, int oflags, int perm) {
        ChannelFD fd;
        Sysopen data = new Sysopen();

        data.fname = fname;
        data.oflags = oflags;
        data.perm = perm;

        fd = sysopenInternal(runtime, data);
        if (fd == null) {
            if (data.errno == Errno.EMFILE || data.errno == Errno.ENFILE) {
                System.gc();
                data.errno = null;
                fd = sysopenInternal(runtime, data);
            }
            if (fd == null) {
                if (data.errno != null) {
                    throw runtime.newErrnoFromErrno(data.errno, fname.toString());
                } else {
                    throw runtime.newSystemCallError(fname.toString());
                }
            }
        }
        return fd;
    }

    // rb_sysopen_internal
    private static ChannelFD sysopenInternal(Ruby runtime, Sysopen data) {
        ChannelFD fd;
        // TODO: thread eventing as in MRI
        fd = sysopenFunc(runtime, data);
//        if (0 <= fd)
//            rb_update_max_fd(fd);
        return fd;
    }

    // sysopen_func
    private static ChannelFD sysopenFunc(Ruby runtime, Sysopen data) {
        return cloexecOpen(runtime, data);
    }

    // rb_cloexec_open
    private static ChannelFD cloexecOpen(Ruby runtime, Sysopen data)
    {
        Channel ret = null;
//        #ifdef O_CLOEXEC
//            /* O_CLOEXEC is available since Linux 2.6.23.  Linux 2.6.18 silently ignore it. */
//            flags |= O_CLOEXEC;
//        #elif defined O_NOINHERIT
//            flags |= O_NOINHERIT;
//        #endif
        PosixShim shim = new PosixShim(runtime);
        ret = shim.open(runtime.getCurrentDirectory(), data.fname, ModeFlags.createModeFlags(data.oflags), data.perm);
        if (ret == null) {
            data.errno = shim.errno;
            return null;
        }
        // TODO, if we need it?
//        rb_maygvl_fd_fix_cloexec(ret);
        return new ChannelFD(ret, runtime.getPosix(), runtime.getFilenoUtil());
    }

    // MRI: rb_io_autoclose_p
    public boolean isAutoclose() {
        OpenFile fptr;
        fptr = getOpenFileChecked();
        return fptr.isAutoclose();
    }

    // MRI: rb_io_set_autoclose
    public void setAutoclose(boolean autoclose) {
        OpenFile fptr;
        fptr = getOpenFileChecked();
        fptr.setAutoclose(autoclose);
    }

    @JRubyMethod(name = "autoclose?")
    public IRubyObject autoclose(ThreadContext context) {
        return context.runtime.newBoolean(isAutoclose());
    }

    @JRubyMethod(name = "autoclose=")
    public IRubyObject autoclose_set(ThreadContext context, IRubyObject autoclose) {
        setAutoclose(autoclose.isTrue());
        return context.nil;
    }

    // MRI: rb_io_binmode_m
    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        setAscii8bitBinmode();

        RubyIO write_io = GetWriteIO();
        if (write_io != this)
             write_io.setAscii8bitBinmode();

        return this;
    }

    // MRI: rb_io_binmode_p
    @JRubyMethod(name = "binmode?")
    public IRubyObject op_binmode(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, getOpenFileChecked().isBinmode());
    }

    // rb_io_syswrite
    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(ThreadContext context, IRubyObject str) {
       Ruby runtime = context.runtime;
        OpenFile fptr;
        long n;

        if (!(str instanceof RubyString))
            str = str.asString();

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkWritable(context);

            str.setFrozen(true);

            if (fptr.wbuf.len != 0) {
                runtime.getWarnings().warn("syswrite for buffered IO");
            }

            ByteList strByteList = ((RubyString) str).getByteList();
            n = OpenFile.writeInternal(context, fptr, fptr.fd(), strByteList.unsafeBytes(), strByteList.begin(), strByteList.getRealSize());

            if (n == -1) throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
        } finally {
            if (locked) fptr.unlock();
        }

        return runtime.newFixnum(n);
    }

    // MRI: rb_io_write_nonblock
    @JRubyMethod(name = "write_nonblock", required = 1, optional = 1)
    public IRubyObject write_nonblock(ThreadContext context, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        IRubyObject str;
        IRubyObject opts = context.nil;
        boolean no_exceptions = false;

        int argc = Arity.checkArgumentCount(context, argv, 1, 2);
        if (argc == 2) {
            opts = argv[1].convertToHash();
        }
        str = argv[0];

        if (!opts.isNil() && runtime.getFalse() == ((RubyHash)opts).op_aref(context, runtime.newSymbol("exception")))
            no_exceptions = true;

        return ioWriteNonblock(context, runtime, str, no_exceptions);
    }

    // MRI: io_write_nonblock
    private IRubyObject ioWriteNonblock(ThreadContext context, Ruby runtime, IRubyObject str, boolean no_exception) {
        OpenFile fptr;
        long n;

        if (!(str instanceof RubyString))
            str = str.asString();

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkWritable(context);

            if (fptr.io_fflush(context) < 0)
                throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());

            fptr.setNonblock(runtime);

            ByteList strByteList = ((RubyString) str).getByteList();
            n = fptr.posix.write(fptr.fd(), strByteList.unsafeBytes(), strByteList.begin(), strByteList.getRealSize(), true);

            if (n == -1) {
                if (fptr.posix.errno == Errno.EWOULDBLOCK || fptr.posix.errno == Errno.EAGAIN) {
                    if (no_exception) {
                        return runtime.newSymbol("wait_writable");
                    } else {
                        throw runtime.newErrnoEAGAINWritableError("write would block");
                    }
                }
                throw runtime.newErrnoFromErrno(fptr.posix.errno, fptr.getPath());
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return runtime.newFixnum(n);
    }

    public RubyIO GetWriteIO() {
        RubyIO writeIO;
        checkInitialized();
        writeIO = openFile.tiedIOForWriting;
        if (writeIO != null) {
            return writeIO;
        }
        return this;
    }

    private void checkInitialized() {
        if (openFile == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }
    
    /** io_write_m
     * 
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject str) {
        return write(context, str, false);
    }

    // io_write
    public IRubyObject write(ThreadContext context, IRubyObject str, boolean nosync) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        long n;
        IRubyObject tmp;

        RubyIO io = GetWriteIO();

        str = str.asString();
        tmp = TypeConverter.ioCheckIO(runtime, io);
        if (tmp.isNil()) {
	        /* port is not IO, call write method for it. */
            return io.callMethod(context, "write", str);
        }
        io = (RubyIO)tmp;
        if (((RubyString)str).size() == 0) return RubyFixnum.zero(runtime);

        fptr = io.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr = io.getOpenFileChecked();
            fptr.checkWritable(context);

            n = fptr.fwrite(context, str, nosync);
            if (n == -1) throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
        } finally {
            if (locked) fptr.unlock();
        }

        return RubyFixnum.newFixnum(runtime, n);
    }

    /** rb_io_addstr
     * 
     */
    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject op_append(ThreadContext context, IRubyObject anObject) {
        // Claims conversion is done via 'to_s' in docs.
        callMethod(context, "write", anObject);
        
        return this;
    }

    @JRubyMethod(name = "fileno", alias = "to_i")
    public RubyFixnum fileno(ThreadContext context) {
        return context.runtime.newFixnum(getOpenFileChecked().getFileno());
    }
    
    /** Returns the current line number.
     * 
     * @return the current line number.
     */
    @JRubyMethod(name = "lineno")
    public RubyFixnum lineno(ThreadContext context) {
        return context.runtime.newFixnum(getOpenFileChecked().getLineNumber());
    }

    /** Sets the current line number.
     * 
     * @param newLineNumber The new line number.
     */
    @JRubyMethod(name = "lineno=", required = 1)
    public RubyFixnum lineno_set(ThreadContext context, IRubyObject newLineNumber) {
        getOpenFileChecked().setLineNumber(RubyNumeric.fix2int(newLineNumber));

        return context.runtime.newFixnum(getOpenFileChecked().getLineNumber());
    }

    /** Returns the current sync mode.
     *
     * MRI: rb_io_sync
     * 
     * @return the current sync mode.
     */
    @JRubyMethod
    public RubyBoolean sync(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();
        fptr.lock();
        try {
            return (fptr.getMode() & OpenFile.SYNC) != 0 ? runtime.getTrue() : runtime.getFalse();
        } finally {
            fptr.unlock();
        }
    }
    
    /**
     * <p>Return the process id (pid) of the process this IO object
     * spawned.  If no process exists (popen was not called), then
     * nil is returned.  This is not how it appears to be defined
     * but ruby 1.8 works this way.</p>
     * 
     * @return the pid or nil
     */
    @JRubyMethod
    public IRubyObject pid(ThreadContext context) {
        OpenFile myOpenFile = getOpenFileChecked();
        
        if (myOpenFile.getProcess() == null) {
            return context.runtime.getNil();
        }
        
        // Of course this isn't particularly useful.
        long pid = myOpenFile.getPid();

        return context.runtime.newFixnum(pid);
    }

    // rb_io_pos
    @JRubyMethod(name = {"pos", "tell"})
    public RubyFixnum pos(ThreadContext context) {
        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            long pos = fptr.tell(context);
            if (pos < 0 && fptr.errno() != null) throw context.runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            pos -= fptr.rbuf.len;
            return context.runtime.newFixnum(pos);
        } finally {
            if (locked) fptr.unlock();
        }
    }

    // rb_io_set_pos
    @JRubyMethod(name = "pos=", required = 1)
    public RubyFixnum pos_set(ThreadContext context, IRubyObject offset) {
        OpenFile fptr;
        long pos;

        pos = offset.convertToInteger().getLongValue();
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            pos = fptr.seek(context, pos, PosixShim.SEEK_SET);
            if (pos < 0 && fptr.errno() != null) throw context.runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
        } finally {
            if (locked) fptr.unlock();
        }

        return context.runtime.newFixnum(pos);
    }
    
    /** Print some objects to the stream.
     * 
     */
    @JRubyMethod(rest = true, reads = FrameField.LASTLINE)
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        return print(context, this, args);
    }

    /**
     * Print some objects to the stream.
     *
     * MRI: rb_io_print
     */
    public static IRubyObject print(ThreadContext context, IRubyObject out, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int i;
        IRubyObject line;
        int argc = args.length;

        /* if no argument given, print `$_' */
        if (argc == 0) {
            argc = 1;
            line = context.getLastLine();
            args = new IRubyObject[]{line};
        }
        for (i=0; i<argc; i++) {
            IRubyObject outputFS = runtime.getGlobalVariables().get("$,");
            if (!outputFS.isNil() && i>0) {
                write(context, out, outputFS);
            }
            write(context, out, args[i]);
        }
        IRubyObject outputRS = runtime.getGlobalVariables().get("$\\");
        if (argc > 0 && !outputRS.isNil()) {
            write(context, out, outputRS);
        }

        return context.nil;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        write(context, this, RubyKernel.sprintf(context, this, args));
        return context.nil;
    }

    @JRubyMethod(required = 1)
    public IRubyObject putc(ThreadContext context, IRubyObject ch) {
        Ruby runtime = context.runtime;
        IRubyObject str;
        if (ch instanceof RubyString) {
            str = ((RubyString)ch).substr(runtime, 0, 1);
        }
        else {
            str = RubyString.newStringShared(runtime, RubyFixnum.SINGLE_CHAR_BYTELISTS19[RubyNumeric.num2chr(ch) & 0xFF]);
        }
        write(context, str);
        return ch;
    }

    public static IRubyObject putc(ThreadContext context, IRubyObject maybeIO, IRubyObject object) {
        if (maybeIO instanceof RubyIO) {
            ((RubyIO)maybeIO).putc(context, object);
        } else {
            byte c = RubyNumeric.num2chr(object);
            IRubyObject str = RubyString.newStringShared(context.runtime, RubyFixnum.SINGLE_CHAR_BYTELISTS19[c & 0xFF]);
            maybeIO.callMethod(context, "write", str);
        }

        return object;
    }

    public RubyFixnum seek(ThreadContext context, IRubyObject[] args) {
        int whence = PosixShim.SEEK_SET;
        
        if (args.length > 1) {
            whence = interpretSeekWhence(args[1]);
        }
        
        return doSeek(context, args[0], whence);
    }

    @JRubyMethod
    public RubyFixnum seek(ThreadContext context, IRubyObject arg0) {
        int whence = PosixShim.SEEK_SET;
        
        return doSeek(context, arg0, whence);
    }

    @JRubyMethod
    public RubyFixnum seek(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int whence = interpretSeekWhence(arg1);
        
        return doSeek(context, arg0, whence);
    }

    // rb_io_seek
    private RubyFixnum doSeek(ThreadContext context, IRubyObject offset, int whence) {
        OpenFile fptr;
        long pos;

        pos = RubyNumeric.num2long(offset);
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            pos = fptr.seek(context, pos, whence);
            if (pos < 0 && fptr.errno() != null) throw getRuntime().newErrnoFromErrno(fptr.errno(), fptr.getPath());
        } finally {
            if (locked) fptr.unlock();
        }

        return RubyFixnum.zero(context.runtime);
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    @JRubyMethod(required = 1, optional = 1)
    public RubyFixnum sysseek(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject offset = context.nil, ptrname = context.nil;
        int whence = PosixShim.SEEK_SET;
        OpenFile fptr;
        long pos;

        switch (args.length) {
            case 2:
                ptrname = args[1];
                whence = interpretSeekWhence(ptrname);
            case 1:
                offset = args[0];
        }
        pos = offset.convertToInteger().getLongValue();
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            if ((fptr.isReadable()) &&
                    (fptr.READ_DATA_BUFFERED() || fptr.READ_CHAR_PENDING())) {
                throw runtime.newIOError("sysseek for buffered IO");
            }
            if (fptr.isWritable() && fptr.wbuf.len != 0) {
                runtime.getWarnings().warn("sysseek for buffered IO");
            }
            fptr.errno(null);
            pos = fptr.posix.lseek(fptr.fd(), pos, whence);
            if (pos == -1 && fptr.errno() != null) throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
        } finally {
            if (locked) fptr.unlock();
        }

        return RubyFixnum.newFixnum(runtime, pos);
    }

    private static int interpretSeekWhence(IRubyObject vwhence) {
        if (vwhence instanceof RubySymbol) {
            if (vwhence.toString() == "SET")
                return PosixShim.SEEK_SET;
            if (vwhence.toString() == "CUR")
                return PosixShim.SEEK_CUR;
            if (vwhence.toString() == "END")
                return PosixShim.SEEK_END;
        }
        return (int)vwhence.convertToInteger().getLongValue();
    }

    // rb_io_rewind
    @JRubyMethod
    public RubyFixnum rewind(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

        fptr = getOpenFileChecked();
        boolean locked = fptr.lock();
        try {
            if (fptr.seek(context, 0L, 0) < 0 && fptr.errno() != null)
                throw context.runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            RubyArgsFile.ArgsFileData data = RubyArgsFile.ArgsFileData.getDataFrom(runtime.getArgsFile());
            if (this == data.currentFile) {
                data.currentLineNumber -= fptr.getLineNumber();
            }
            fptr.setLineNumber(0);
            if (fptr.readconv != null) {
                fptr.clearReadConversion();
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return RubyFixnum.zero(runtime);
    }

    // rb_io_fsync
    @JRubyMethod
    public RubyFixnum fsync(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();

        if (fptr.io_fflush(context) < 0)
            throw runtime.newSystemCallError("");

//        # ifndef _WIN32	/* already called in io_fflush() */
//        if ((int)rb_thread_io_blocking_region(nogvl_fsync, fptr, fptr->fd) < 0)
//            rb_sys_fail_path(fptr->pathv);
//        # endif
        return RubyFixnum.zero(runtime);
    }

    /** Sets the current sync mode.
     *
     * MRI: rb_io_set_sync
     * 
     * @param sync The new sync mode.
     */
    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject sync_set(IRubyObject sync) {
        setSync(sync.isTrue());

        return sync;
    }

    public void setSync(boolean sync) {
        RubyIO io = GetWriteIO();
        OpenFile fptr = io.getOpenFileChecked();
        fptr.setSync(sync);
    }

    public boolean getSync() {
        RubyIO io = GetWriteIO();
        OpenFile fptr = io.getOpenFileChecked();
        return fptr.isSync();
    }

    // rb_io_eof
    @JRubyMethod(name = {"eof?", "eof"})
    public RubyBoolean eof_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);

            if (fptr.READ_CHAR_PENDING()) return runtime.getFalse();
            if (fptr.READ_DATA_PENDING()) return runtime.getFalse();
            fptr.READ_CHECK(context);
            //        #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
            //        if (!NEED_READCONV(fptr) && NEED_NEWLINE_DECORATOR_ON_READ(fptr)) {
            //            return eof(fptr->fd) ? Qtrue : Qfalse;
            //        }
            //        #endif
            if (fptr.fillbuf(context) < 0) {
                return runtime.getTrue();
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return runtime.getFalse();
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public RubyBoolean tty_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        POSIX posix = runtime.getPosix();
        OpenFile fptr;

        fptr = getOpenFileChecked();

        fptr.lock();
        try {
            if (posix.isNative() && fptr.fd().realFileno != -1) {
                return posix.libc().isatty(fptr.getFileno()) == 0 ? runtime.getFalse() : runtime.getTrue();
            } else if (fptr.isStdio()) {
                // This is a bit of a hack for platforms where we can't do native stdio
                return runtime.getTrue();
            }
        } finally {
            fptr.unlock();
        }

        return runtime.getFalse();
    }

    // rb_io_init_copy
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject _io){
        RubyIO dest = this;
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        OpenFile fptr, orig;
        ChannelFD fd;
        RubyIO write_io;
        long pos;

        RubyIO io = TypeConverter.ioGetIO(runtime, _io);
        if (!OBJ_INIT_COPY(dest, io)) return dest;
        orig = io.getOpenFileChecked();
        fptr = dest.MakeOpenFile();

        // orig is the visible one here but we lock both anyway
        boolean locked1 = orig.lock();
        boolean locked2 = fptr.lock();
        try {
            io.flush(context);

            /* copy rb_io_t structure */
            fptr.setMode(orig.getMode() & ~OpenFile.PREP);
            fptr.encs = orig.encs;
            fptr.setProcess(orig.getProcess());
            fptr.setLineNumber(orig.getLineNumber());
            if (orig.getPath() != null) fptr.setPath(orig.getPath());
            fptr.setFinalizer(orig.getFinalizer());
            // TODO: not using pipe_finalize yet
            //        #if defined (__CYGWIN__) || !defined(HAVE_FORK)
            //        if (fptr.finalize == pipe_finalize)
            //            pipe_add_fptr(fptr);
            //        #endif

            fd = orig.fd().dup();
            fptr.setFD(fd);
            pos = orig.tell(context);
            if (0 <= pos)
                fptr.seek(context, pos, PosixShim.SEEK_SET);
        } finally {
            if (locked2) fptr.unlock();
            if (locked1) orig.unlock();
        }

        if (fptr.isBinmode()) {
            dest.setBinmode();
        }

        write_io = io.GetWriteIO();
        if (io != write_io) {
            write_io = (RubyIO)write_io.dup();
            fptr.tiedIOForWriting = write_io;
            dest.getInstanceVariables().setInstanceVariable("@tied_io_for_writing", write_io);
        }

        return dest;
    }
    
    @JRubyMethod(name = "closed?")
    public RubyBoolean closed_p(ThreadContext context) {
        return context.runtime.newBoolean(isClosed());
    }

    /**
     * Is this IO closed
     *
     * MRI: rb_io_closed
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        OpenFile fptr;
        RubyIO write_io;
        OpenFile write_fptr;

        write_io = GetWriteIO();
        if (this != write_io) {
            write_fptr = write_io.openFile;
            if (write_fptr != null && write_fptr.fd() != null) {
                return false;
            }
        }

        fptr = openFile;
        checkInitialized();
        return fptr.fd() != null ? false : true;
    }

    /** 
     * <p>Closes all open resources for the IO.  It also removes
     * it from our magical all open file descriptor pool.</p>
     * 
     * @return The IO.
     *
     * MRI: rb_io_close_m
     */
    @JRubyMethod
    public IRubyObject close() {
        Ruby runtime = getRuntime();

        openFile.checkClosed();
        return rbIoClose(runtime);
    }

    // io_close
    protected static IRubyObject ioClose(Ruby runtime, IRubyObject io) {
        ThreadContext context = runtime.getCurrentContext();
        IRubyObject closed = io.checkCallMethod(context, "closed?");
        if (closed != null && closed.isTrue()) return io;
        IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
        try {
            return io.checkCallMethod(context, "close");
        } catch (RaiseException re) {
            if (re.getMessage().contains(CLOSED_STREAM_MSG)) {
                // ignore
                runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
                return context.nil;
            } else {
                throw re;
            }
        }
    }
    
    // rb_io_close  
    protected IRubyObject rbIoClose(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        OpenFile fptr;
        RubyIO write_io;
        OpenFile write_fptr;

        write_io = GetWriteIO();
        if (this != write_io) {
            write_fptr = write_io.openFile;

            boolean locked = write_fptr.lock();
            try {
                if (write_fptr != null && write_fptr.fd() != null) {
                    write_fptr.cleanup(runtime, true);
                }
            } finally {
                if (locked) write_fptr.unlock();
            }
        }

        fptr = openFile;

        boolean locked = fptr.lock();
        try {
            if (fptr == null) return runtime.getNil();
            if (fptr.fd() == null) return runtime.getNil();

            // interrupt waiting threads
            fptr.interruptBlockingThreads();
            fptr.cleanup(runtime, false);

            if (fptr.getProcess() != null) {
                context.setLastExitStatus(context.nil);

                if (runtime.getPosix().isNative() && fptr.getProcess() instanceof POSIXProcess) {
                    // We do not need to nuke native-launched child process, since we now have full control
                    // over child process pipes.
                    IRubyObject processResult = RubyProcess.RubyStatus.newProcessStatus(runtime, ((POSIXProcess) fptr.getProcess()).status(), fptr.getPid());
                    context.setLastExitStatus(processResult);
                } else {
                    // If this is not a popen3/popen4 stream and it has a process, attempt to shut down that process
                    if (!popenSpecial) {
                        obliterateProcess(fptr.getProcess());
                        // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                        IRubyObject processResult = RubyProcess.RubyStatus.newProcessStatus(runtime, fptr.getProcess().exitValue() << 8, fptr.getPid());
                        context.setLastExitStatus(processResult);
                    }
                }
                fptr.setProcess(null);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return runtime.getNil();
    }

    // MRI: rb_io_close_write
    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        RubyIO write_io;

        write_io = GetWriteIO();
        fptr = write_io.getOpenFileChecked();
        boolean locked = fptr.lock();
        try {
            if (fptr.socketChannel() != null) {
                try {
                    fptr.socketChannel().shutdownOutput();
                } catch (IOException ioe) {
                    throw runtime.newErrnoFromErrno(Helpers.errnoFromException(ioe), fptr.getPath());
                }
                fptr.setMode(fptr.getMode() & ~OpenFile.WRITABLE);
                if (!fptr.isReadable())
                    return write_io.rbIoClose(runtime);
                return context.nil;
            }

            if (fptr.isReadable()) {
                throw runtime.newIOError("closing non-duplex IO for writing");
            }
        } finally {
            if (locked) fptr.unlock();
        }


        if (this != write_io) {
            fptr = getOpenFileChecked();

            locked = fptr.lock();
            try {
                fptr.tiedIOForWriting = null;
                fptr.setMode(fptr.getMode() & ~OpenFile.DUPLEX);
            } finally {
                if (locked) fptr.unlock();
            }
        }

        write_io.rbIoClose(runtime);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject close_read(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        RubyIO write_io;

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            if (fptr.socketChannel() != null) {
                try {
                    fptr.socketChannel().socket().shutdownInput();
                } catch (IOException ioe) {
                    throw runtime.newErrnoFromErrno(Helpers.errnoFromException(ioe), fptr.getPath());
                }
                fptr.setMode(fptr.getMode() & ~OpenFile.READABLE);
                if (!fptr.isWritable())
                    return rbIoClose(runtime);
                return context.nil;
            }

            write_io = GetWriteIO();
            if (this != write_io) {
                OpenFile wfptr;
                wfptr = write_io.getOpenFileChecked();

                boolean locked2 = wfptr.lock();
                try {
                    wfptr.setProcess(fptr.getProcess());
                    wfptr.setPid(fptr.getPid());
                    fptr.setProcess(null);
                    fptr.setPid(-1);
                    this.openFile = wfptr;
                    /* bind to write_io temporarily to get rid of memory/fd leak */
                    fptr.tiedIOForWriting = null;
                    fptr.setMode(fptr.getMode() & ~OpenFile.DUPLEX);
                    write_io.openFile = fptr;
                    fptr.cleanup(runtime, false);
                    /* should not finalize fptr because another thread may be reading it */
                    return context.nil;
                } finally {
                    if (locked2) wfptr.unlock();
                }
            }

            if (fptr.isWritable()) {
                throw runtime.newIOError("closing non-duplex IO for reading");
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return rbIoClose(runtime);
    }

    @JRubyMethod(name = "close_on_exec=", notImplemented = true)
    public IRubyObject close_on_exec_set(ThreadContext context, IRubyObject arg) {
        // TODO: rb_io_set_close_on_exec
        throw context.runtime.newNotImplementedError("close_on_exec=");
    }

    @JRubyMethod(name = "close_on_exec?", notImplemented = true)
    public IRubyObject close_on_exec_p(ThreadContext context) {
        // TODO: rb_io_close_on_exec_p
        throw context.runtime.newNotImplementedError("close_on_exec=");
    }

    /** Flushes the IO output stream.
     *
     * MRI: rb_io_flush
     *
     * @return The IO.
     */
    @JRubyMethod
    public RubyIO flush(ThreadContext context) {
        return flushRaw(context, true);
    }

    // rb_io_flush_raw
    protected RubyIO flushRaw(ThreadContext context, boolean sync) {
        OpenFile fptr;

        // not possible here
//        if (!RB_TYPE_P(io, T_FILE)) {
//            return rb_funcall(io, id_flush, 0);
//        }

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            if ((fptr.getMode() & OpenFile.WRITABLE) != 0) {
                if (fptr.io_fflush(context) < 0)
                    throw context.runtime.newErrnoFromErrno(fptr.errno(), "");
                //            #ifdef _WIN32
                //            if (sync && GetFileType((HANDLE)rb_w32_get_osfhandle(fptr->fd)) == FILE_TYPE_DISK) {
                //                rb_thread_io_blocking_region(nogvl_fsync, fptr, fptr->fd);
                //            }
                //            #endif
            }
            if ((fptr.getMode() & OpenFile.READABLE) != 0) {
                fptr.unread(context);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return io;
    }

    /** Read a line.
     * 
     */

    // rb_io_gets_m
    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context) {
        IRubyObject separator = prepareGetsSeparator(context, null, null);
        IRubyObject result = getline(context, separator);

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    // rb_io_gets_m
    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject arg) {
        IRubyObject separator = prepareGetsSeparator(context, arg, null);
        long limit = prepareGetsLimit(context, arg, null);

        IRubyObject result = getline(context, separator, limit);

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    // rb_io_gets_m
    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject rs, IRubyObject limit_arg) {
        rs = prepareGetsSeparator(context, rs, limit_arg);
        long limit = prepareGetsLimit(context, rs, limit_arg);
        IRubyObject result = getline(context, rs, limit);

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    private IRubyObject prepareGetsSeparator(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return prepareGetsSeparator(context, null, null);
            case 1:
                return prepareGetsSeparator(context, args[0], null);
            case 2:
                return prepareGetsSeparator(context, args[0], args[1]);
        }
        throw new RuntimeException("invalid size for gets args: " + args.length);
    }

    // MRI: prepare_getline_args, separator logic
    private IRubyObject prepareGetsSeparator(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        IRubyObject rs = runtime.getRecordSeparatorVar().get();
        if (arg0 != null && arg1 == null) { // argc == 1
            IRubyObject tmp = context.nil;

            if (arg0.isNil() || !(tmp = TypeConverter.checkStringType(runtime, arg0)).isNil()) {
                rs = tmp;
            }
        } else if (arg0 != null && arg1 != null) { // argc >= 2
            rs = arg0;
            if (!rs.isNil()) {
                rs = rs.convertToString();
            }
        }
        if (!rs.isNil()) {
            Encoding enc_rs, enc_io;

            OpenFile fptr = getOpenFileChecked();
            enc_rs = ((RubyString)rs).getEncoding();
            enc_io = fptr.readEncoding(runtime);
            if (enc_io != enc_rs &&
                    (((RubyString)rs).scanForCodeRange() != StringSupport.CR_7BIT ||
                            (((RubyString)rs).size() > 0 && !enc_io.isAsciiCompatible()))) {
                if (rs == runtime.getGlobalVariables().getDefaultSeparator()) {
                    rs = RubyString.newStringLight(runtime, 0, enc_io);
                    ((RubyString)rs).catAscii(NEWLINE_BYTES, 0, 1);
                }
                else {
                    throw runtime.newArgumentError("encoding mismatch: " + enc_io + " IO with " + enc_rs + " RS");
                }
            }
        }
        return rs;
    }

    private long prepareGetsLimit(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return prepareGetsLimit(context, null, null);
            case 1:
                return prepareGetsLimit(context, args[0], null);
            case 2:
                return prepareGetsLimit(context, args[0], args[1]);
        }
        throw new RuntimeException("invalid size for gets args: " + args.length);
    }

    // MRI: prepare_getline_args, limit logic
    private long prepareGetsLimit(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        IRubyObject lim = context.nil;
        if (arg0 != null && arg1 == null) { // argc == 1
            IRubyObject tmp = context.nil;

            if (arg0.isNil() || !(tmp = TypeConverter.checkStringType(runtime, arg0)).isNil()) {
                // only separator logic
            } else {
                lim = arg0;
            }
        } else if (arg0 != null && arg1 != null) { // argc >= 2
            lim = arg1;
        }
        return lim.isNil() ? -1 : lim.convertToInteger().getLongValue();
    }

    private IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return gets(context);
            case 1:
                return gets(context, args[0]);
            case 2:
                return gets(context, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context, args.length, 0, 2);
                return null; // not reached
        }
    }

    public boolean getBlocking() {
        return openFile.isBlocking();
    }

    public void setBlocking(boolean blocking) {
        openFile.setBlocking(getRuntime(), blocking);
    }

    @JRubyMethod(name = "fcntl")
    public IRubyObject fcntl(ThreadContext context, IRubyObject cmd) {
        // TODO: This version differs from ioctl by checking whether fcntl exists
        // and raising notimplemented if it doesn't; perhaps no difference for us?
        return ctl(context.runtime, cmd, null);
    }

    @JRubyMethod(name = "fcntl")
    public IRubyObject fcntl(ThreadContext context, IRubyObject cmd, IRubyObject arg) {
        // TODO: This version differs from ioctl by checking whether fcntl exists
        // and raising notimplemented if it doesn't; perhaps no difference for us?
        return ctl(context.runtime, cmd, arg);
    }

    @JRubyMethod(name = "ioctl", required = 1, optional = 1)
    public IRubyObject ioctl(ThreadContext context, IRubyObject[] args) {
        IRubyObject cmd = args[0];
        IRubyObject arg;
        
        if (args.length == 2) {
            arg = args[1];
        } else {
            arg = context.runtime.getNil();
        }

        return ctl(context.runtime, cmd, arg);
    }

    public IRubyObject ctl(Ruby runtime, IRubyObject cmd, IRubyObject arg) {
        long realCmd = cmd.convertToInteger().getLongValue();
        long nArg = 0;

        if (realCmd == Fcntl.F_GETFL.intValue()) {
            OpenFile myOpenFile = getOpenFileChecked();
            return runtime.newFixnum(OpenFile.ioFmodeOflags(myOpenFile.getMode()));
        }
        
        // FIXME: Arg may also be true, false, and nil and still be valid.  Strangely enough, 
        // protocol conversion is not happening in Ruby on this arg?
        if (arg == null || arg.isNil() || arg == runtime.getFalse()) {
            nArg = 0;
        } else if (arg instanceof RubyFixnum) {
            nArg = RubyFixnum.fix2long(arg);
        } else if (arg == runtime.getTrue()) {
            nArg = 1;
        } else {
            throw runtime.newNotImplementedError("JRuby does not support string for second fcntl/ioctl argument yet");
        }
        
        OpenFile fptr = getOpenFileChecked();

        // Fixme: Only F_SETFL and F_GETFL is current supported
        // FIXME: Only NONBLOCK flag is supported
        // FIXME: F_SETFL and F_SETFD are treated as the same thing here.  For the case of dup(fd) we
        //   should actually have F_SETFL only affect one (it is unclear how well we do, but this TODO
        //   is here to at least document that we might need to do more work here.  Mostly SETFL is
        //   for mode changes which should persist across fork() boundaries.  Since JVM has no fork
        //   this is not a problem for us.
        if (realCmd == FcntlLibrary.FD_CLOEXEC) {
            // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
            // And why the hell does webrick pass this in as a first argument!!!!!
        } else if (realCmd == Fcntl.F_SETFL.intValue() || realCmd == Fcntl.F_SETFD.intValue()) {
            if ((nArg & FcntlLibrary.FD_CLOEXEC) == FcntlLibrary.FD_CLOEXEC) {
                // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
            } else {
                boolean block = (nArg & ModeFlags.NONBLOCK) != ModeFlags.NONBLOCK;

                fptr.setBlocking(runtime, block);
            }
        } else if (realCmd == Fcntl.F_GETFL.intValue()) {
            return fptr.isBlocking() ? RubyFixnum.zero(runtime) : RubyFixnum.newFixnum(runtime, ModeFlags.NONBLOCK);
        } else {
            throw runtime.newNotImplementedError("JRuby only supports F_SETFL and F_GETFL with NONBLOCK for fcntl/ioctl");
        }
        
        return runtime.newFixnum(0);
    }

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @JRubyMethod(name = "puts")
    public IRubyObject puts(ThreadContext context) {
        return puts0(context, this);
    }

    @JRubyMethod(name = "puts")
    public IRubyObject puts(ThreadContext context, IRubyObject arg0) {
        return puts1(context, this, arg0);
    }

    @JRubyMethod(name = "puts")
    public IRubyObject puts(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return puts2(context, this, arg0, arg1);
    }

    @JRubyMethod(name = "puts")
    public IRubyObject puts(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return puts3(context, this, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        return puts(context, this, args);
    }

    public static IRubyObject puts0(ThreadContext context, IRubyObject maybeIO) {
        return writeSeparator(context, maybeIO);
    }

    public static IRubyObject puts1(ThreadContext context, IRubyObject maybeIO, IRubyObject arg0) {
        Ruby runtime = context.runtime;
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        putsSingle(context, runtime, maybeIO, arg0, separator);

        return context.nil;
    }

    public static IRubyObject puts2(ThreadContext context, IRubyObject maybeIO, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        putsSingle(context, runtime, maybeIO, arg0, separator);
        putsSingle(context, runtime, maybeIO, arg1, separator);

        return context.nil;
    }

    public static IRubyObject puts3(ThreadContext context, IRubyObject maybeIO, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.runtime;
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        putsSingle(context, runtime, maybeIO, arg0, separator);
        putsSingle(context, runtime, maybeIO, arg1, separator);
        putsSingle(context, runtime, maybeIO, arg2, separator);

        return context.nil;
    }

    public static IRubyObject puts(ThreadContext context, IRubyObject maybeIO, IRubyObject... args) {
        if (args.length == 0) {
            return writeSeparator(context, maybeIO);
        }

        return putsArray(context, maybeIO, args);
    }

    private static IRubyObject writeSeparator(ThreadContext context, IRubyObject maybeIO) {
        Ruby runtime = context.runtime;
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        write(context, maybeIO, separator);
        return runtime.getNil();
    }

    private static IRubyObject putsArray(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        for (int i = 0; i < args.length; i++) {
            putsSingle(context, runtime, maybeIO, args[i], separator);
        }

        return runtime.getNil();
    }

    private static final ByteList RECURSIVE_BYTELIST = ByteList.create("[...]");

    private static void putsSingle(ThreadContext context, Ruby runtime, IRubyObject maybeIO, IRubyObject arg, RubyString separator) {
        ByteList line;

        if (arg.isNil()) {
            line = getNilByteList(runtime);
        } else if (runtime.isInspecting(arg)) {
            line = RECURSIVE_BYTELIST;
        } else if (arg instanceof RubyArray) {
            inspectPuts(context, maybeIO, (RubyArray) arg);
            return;
        } else {
            line = arg.asString().getByteList();
        }

        write(context, maybeIO, line);

        if (line.length() == 0 || !line.endsWith(separator.getByteList())) {
            write(context, maybeIO, separator.getByteList());
        }
    }

    private static IRubyObject inspectPuts(ThreadContext context, IRubyObject maybeIO, RubyArray array) {
        try {
            context.runtime.registerInspecting(array);
            return putsArray(context, maybeIO, array.toJavaArray());
        } finally {
            context.runtime.unregisterInspecting(array);
        }
    }

    protected IRubyObject write(ThreadContext context, ByteList byteList) {
        return callMethod(context, "write", RubyString.newStringShared(context.runtime, byteList));
    }

    protected static IRubyObject write(ThreadContext context, IRubyObject maybeIO, ByteList byteList) {
        return maybeIO.callMethod(context, "write", RubyString.newStringShared(context.runtime, byteList));
    }

    public static IRubyObject write(ThreadContext context, IRubyObject maybeIO, IRubyObject str) {
        return maybeIO.callMethod(context, "write", str);
    }
    
    @Override
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        
        if (openFile == null) return super.inspect();
        
        String className = getMetaClass().getRealClass().getName();
        String path = openFile.getPath();
        String status = "";
        
        if (path == null) {
            if (openFile.fd() == null) {
                path = "";
                status = "(closed)";
            } else {
                path = "fd " + openFile.fd().bestFileno();
            }
        } else if (!openFile.isOpen()) {
            status = " (closed)";
        }
        
        String inspectStr = "#<" + className + ":" + path + status + ">";
        
        return runtime.newString(inspectStr);
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context) {
        IRubyObject line = gets(context);

        if (line.isNil()) {
            throw context.runtime.newEOFError();
        }
        
        return line;
    }

    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context, IRubyObject separator) {
        IRubyObject line = gets(context, separator);

        if (line.isNil()) {
            throw context.runtime.newEOFError();
        }

        return line;
    }

    /** Read a byte. On EOF returns nil.
     * 
     */
    public IRubyObject getc() {
        return getbyte(getRuntime().getCurrentContext());
    }

    // rb_io_readchar
    @JRubyMethod
    public IRubyObject readchar(ThreadContext context) {
        IRubyObject c = getc19(context);
        
        if (c.isNil()) {
            throw context.runtime.newEOFError();
        }
        return c;
    }

    // rb_io_getbyte
    @JRubyMethod
    public IRubyObject getbyte(ThreadContext context) {
        int c = getByte(context);

        if (c == -1) return context.nil;

        return RubyNumeric.int2fix(context.runtime, c & 0xff);
    }

    // rb_io_getbyte
    public int getByte(ThreadContext context) {
        int c;

        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkByteReadable(context);
            fptr.READ_CHECK(context);
            // TODO: tty flushing
            //        if (fptr->fd == 0 && (fptr->mode & FMODE_TTY) && RB_TYPE_P(rb_stdout, T_FILE)) {
            //            rb_io_t *ofp;
            //            GetOpenFile(rb_stdout, ofp);
            //            if (ofp->mode & FMODE_TTY) {
            //                rb_io_flush(rb_stdout);
            //            }
            //        }
            if (fptr.fillbuf(context) < 0) {
                return -1;
            }
            fptr.rbuf.off++;
            fptr.rbuf.len--;
            return fptr.rbuf.ptr[fptr.rbuf.off - 1] & 0xFF;
        } finally {
            if (locked) fptr.unlock();
        }
    }

    // rb_io_readbyte
    @JRubyMethod
    public IRubyObject readbyte(ThreadContext context) {
        IRubyObject c = getbyte(context);

        if (c.isNil()) {
            throw getRuntime().newEOFError();
        }
        return c;
    }

    // rb_io_getc
    @JRubyMethod(name = "getc")
    public IRubyObject getc19(ThreadContext context) {
        Ruby runtime = context.runtime;
        Encoding enc;

        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);

            enc = fptr.inputEncoding(runtime);
            fptr.READ_CHECK(context);
            return fptr.getc(context, enc);
        } finally {
            if (locked) fptr.unlock();
        }
    }

    // rb_io_ungetbyte
    @JRubyMethod
    public IRubyObject ungetbyte(ThreadContext context, IRubyObject b) {
        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkByteReadable(context);
            if (b.isNil()) return context.nil;
            if (b instanceof RubyFixnum) {
                byte cc = (byte) RubyNumeric.fix2int(b);
                b = RubyString.newStringNoCopy(context.runtime, new byte[]{cc});
            } else {
                b = b.convertToString();
            }
            fptr.ungetbyte(context, b);
        } finally {
            if (locked) fptr.unlock();
        }

        return context.nil;
    }

    // MRI: rb_io_ungetc
    @JRubyMethod
    public IRubyObject ungetc(ThreadContext context, IRubyObject c) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        int len;

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);
            if (c.isNil()) return c;
            if (c instanceof RubyFixnum) {
                c = EncodingUtils.encUintChr(context, (int) ((RubyFixnum) c).getLongValue(), fptr.readEncoding(runtime));
            } else if (c instanceof RubyBignum) {
                c = EncodingUtils.encUintChr(context, (int) ((RubyBignum) c).getLongValue(), fptr.readEncoding(runtime));
            } else {
                c = c.convertToString();
            }
            if (fptr.needsReadConversion()) {
                fptr.SET_BINARY_MODE();
                len = ((RubyString) c).size();
                //            #if SIZEOF_LONG > SIZEOF_INT
                //            if (len > INT_MAX)
                //                rb_raise(rb_eIOError, "ungetc failed");
                //            #endif
                fptr.makeReadConversion(context, (int) len);
                if (fptr.cbuf.capa - fptr.cbuf.len < len)
                    throw runtime.newIOError("ungetc failed");
                // shift cbuf back to 0
                if (fptr.cbuf.off < len) {
                    System.arraycopy(
                            fptr.cbuf.ptr, fptr.cbuf.off,
                            fptr.cbuf.ptr, fptr.cbuf.capa - fptr.cbuf.len, // this should be 0
                            fptr.cbuf.len);
                    fptr.cbuf.off = fptr.cbuf.capa - fptr.cbuf.len; // this should be 0 too
                }
                fptr.cbuf.off -= (int) len;
                fptr.cbuf.len += (int) len;
                ByteList cByteList = ((RubyString) c).getByteList();
                System.arraycopy(cByteList.unsafeBytes(), cByteList.begin(), fptr.cbuf.ptr, fptr.cbuf.off, len);
            } else {
                fptr.NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
                fptr.ungetbyte(context, c);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return context.nil;
    }

    @JRubyMethod(name = "read_nonblock", required = 1, optional = 2)
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        return doReadNonblock(context, args, true);
    }

    // MRI: io_read_nonblock
    public IRubyObject doReadNonblock(ThreadContext context, IRubyObject[] args, boolean useException) {
        Ruby runtime = context.runtime;
        IRubyObject ret;
        IRubyObject opts;
        boolean no_exception = !useException;

        opts = ArgsUtil.getOptionsArg(runtime, args);

        if (!opts.isNil() && runtime.getFalse() == ((RubyHash)opts).op_aref(context, runtime.newSymbol("exception")))
            no_exception = true;

        ret = getPartial(context, args, true, no_exception);

        if (ret.isNil()) {
            if (no_exception)
                return ret;
            else
                throw runtime.newEOFError();
        }
        return ret;
    }

    @JRubyMethod(name = "readpartial", required = 1, optional = 1)
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        IRubyObject value = getPartial(context, args, false, false);

        if (value.isNil()) {
            throw context.runtime.newEOFError();
        }

        return value;
    }

    // MRI: io_getpartial
    private IRubyObject getPartial(ThreadContext context, IRubyObject[] args, boolean nonblock, boolean noException) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        IRubyObject length, str;
        int n, len;

        switch (args.length) {
            case 3:
                length = args[0];
                str = args[1];
                args[2].convertToHash();
                break;
            case 2:
                length = args[0];
                str = TypeConverter.checkHashType(runtime, args[1]);
                str = str.isNil() ? args[1] : context.nil;
                break;
            case 1:
                length = args[0];
                str = context.nil;
                break;
            default:
                length = context.nil;
                str = context.nil;
        }

        if ((len = RubyNumeric.num2int(length)) < 0) {
            throw runtime.newArgumentError("negative length " + len + " given");
        }

        str = EncodingUtils.setStrBuf(runtime, str, len);
        str.setTaint(true);

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkByteReadable(context);

            if (len == 0)
                return str;

            if (!nonblock)
                fptr.READ_CHECK(context);
            ByteList strByteList = ((RubyString) str).getByteList();
            n = fptr.readBufferedData(strByteList.unsafeBytes(), strByteList.begin(), len);
            if (n <= 0) {
                again:
                while (true) {
                    if (nonblock) {
                        fptr.setNonblock(runtime);
                    }
                    str = EncodingUtils.setStrBuf(runtime, str, len);
                    strByteList = ((RubyString) str).getByteList();
                    //                arg.fd = fptr->fd;
                    //                arg.str_ptr = RSTRING_PTR(str);
                    //                arg.len = len;
                    //                rb_str_locktmp_ensure(str, read_internal_call, (VALUE)&arg);
                    //                n = arg.len;
                    n = OpenFile.readInternal(context, fptr, fptr.fd(), strByteList.unsafeBytes(), strByteList.begin(), len);
                    if (n < 0) {
                        if (!nonblock && fptr.waitReadable(context))
                            continue again;
                        if (nonblock && (fptr.errno() == Errno.EWOULDBLOCK || fptr.errno() == Errno.EAGAIN)) {
                            if (noException)
                                return runtime.newSymbol("wait_readable");
                            else
                                throw runtime.newErrnoEAGAINReadableError("read would block");
                        }
                        throw runtime.newEOFError(fptr.getPath());
                    }
                    break;
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }

        ((RubyString)str).setReadLength(n);

        if (n == 0)
            return context.nil;
        else
            return str;
    }

    // MRI: rb_io_sysread
    @JRubyMethod(name = "sysread", required = 1, optional = 1)
    public IRubyObject sysread(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject len, str;
        OpenFile fptr;
        int ilen, n;
//        struct read_internal_arg arg;

        len = args.length >= 1 ? args[0] : context.nil;
        str = args.length >= 2 ? args[1] : context.nil;
        ilen = RubyNumeric.num2int(len);

        str = EncodingUtils.setStrBuf(runtime, str, (int)ilen);
        if (ilen == 0) return str;

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkByteReadable(context);

            if (fptr.READ_DATA_BUFFERED()) {
                throw runtime.newIOError("sysread for buffered IO");
            }

            /*
             * MRI COMMENT:
             * FIXME: removing rb_thread_wait_fd() here changes sysread semantics
             * on non-blocking IOs.  However, it's still currently possible
             * for sysread to raise Errno::EAGAIN if another thread read()s
             * the IO after we return from rb_thread_wait_fd() but before
             * we call read()
             */
            context.getThread().select(fptr.channel(), fptr, SelectionKey.OP_READ);

            fptr.checkClosed();

            str = EncodingUtils.setStrBuf(runtime, str, ilen);
            ByteList strByteList = ((RubyString) str).getByteList();
            n = OpenFile.readInternal(context, fptr, fptr.fd(), strByteList.unsafeBytes(), strByteList.begin(), ilen);

            if (n == -1) {
                throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            }
        } finally {
            if (locked) fptr.unlock();
        }

        ((RubyString)str).setReadLength(n);
        if (n == 0 && ilen > 0) {
            throw runtime.newEOFError();
        }
        str.setTaint(true);

        return str;
    }

    // io_read
    public IRubyObject read(IRubyObject[] args) {
        ThreadContext context = getRuntime().getCurrentContext();
        
        switch (args.length) {
        case 0: return read(context);
        case 1: return read(context, args[0]);
        case 2: return read(context, args[0], args[1]);
        default: throw getRuntime().newArgumentError(args.length, 2);
        }
    }

    // io_read
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context) {
        return read(context, context.nil, context.nil);
    }

    // io_read
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject arg0) {
        return read(context, arg0, context.nil);
    }

    // io_read
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject length, IRubyObject str) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        int n, len;
        
        if (length.isNil()) {
            fptr = getOpenFileChecked();

            boolean locked = fptr.lock();
            try {
                fptr.checkCharReadable(context);
                return fptr.readAll(context, 0, str);
            } finally {
                if (locked) fptr.unlock();
            }
        }
        
        len = RubyNumeric.num2int(length);
        if (len < 0) {
            throw runtime.newArgumentError("negative length " + len + " given");
        }

        str = EncodingUtils.setStrBuf(runtime, str, len);

        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkByteReadable(context);
            if (len == 0) {
                ((RubyString)str).setReadLength(0);
                return str;
            }

            fptr.READ_CHECK(context);
            //        #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
            //        previous_mode = set_binary_mode_with_seek_cur(fptr);
            //        #endif
            n = fptr.fread(context, str, 0, len);
        } finally {
            if (locked) fptr.unlock();
        }

        ((RubyString)str).setReadLength(n);
//        #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
//        if (previous_mode == O_TEXT) {
//            setmode(fptr->fd, O_TEXT);
//        }
//        #endif
        if (n == 0) return context.nil;
        str.setTaint(true);

        return str;
    }
    
    /** Read a byte. On EOF throw EOFError.
     * 
     */
    public IRubyObject readchar() {
        return readchar(getRuntime().getCurrentContext());
    }
    
    @JRubyMethod
    public IRubyObject stat(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            int realFileno;
            fptr.checkClosed();
            if (runtime.getPosix().isNative() && (realFileno = fptr.fd().realFileno) != -1) {
                return RubyFileStat.newFileStat(runtime, realFileno);
            } else {
                // no real fd, stat the path
                return context.runtime.newFileStat(fptr.getPath(), false);
            }
        } finally {
            if (locked) fptr.unlock();
        }
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     *
     * MRI: rb_io_each_byte
     */
    public IRubyObject each_byteInternal(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_byte");
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            do {
                while (fptr.rbuf.len > 0) {
                    byte[] pBytes = fptr.rbuf.ptr;
                    int p = fptr.rbuf.off++;
                    fptr.rbuf.len--;
                    block.yield(context, runtime.newFixnum(pBytes[p] & 0xFF));
                    fptr.errno(null);
                }
                fptr.checkByteReadable(context);
                fptr.READ_CHECK(context);
            } while (fptr.fillbuf(context) >= 0);
        } finally {
            if (locked) fptr.unlock();
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject each_byte(ThreadContext context, Block block) {
        return block.isGiven() ? each_byteInternal(context, block) : enumeratorize(context.runtime, this, "each_byte");
    }

    // rb_io_bytes
    @JRubyMethod(name = "bytes")
    public IRubyObject bytes(ThreadContext context, Block block) {
        context.runtime.getWarnings().warn("IO#bytes is deprecated; use #each_byte instead");
        return each_byte(context, block);
    }

    // rb_io_each_char
    public IRubyObject each_charInternal(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        Encoding enc;
        IRubyObject c;

        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_char");
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);

            enc = fptr.inputEncoding(runtime);
            fptr.READ_CHECK(context);
            while (!(c = fptr.getc(context, enc)).isNil()) {
                block.yield(context, c);
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return this;
    }

    @JRubyMethod(name = "each_char")
    public IRubyObject each_char(ThreadContext context, Block block) {
        return each_charInternal(context, block);
    }

    @JRubyMethod(name = "chars")
    public IRubyObject chars(ThreadContext context, Block block) {
        context.runtime.getWarnings().warn("IO#chars is deprecated; use #each_char instead");
        return each_charInternal(context, block);
    }

    @JRubyMethod
    public IRubyObject codepoints(ThreadContext context, Block block) {
        context.runtime.getWarnings().warn("IO#codepoints is deprecated; use #each_codepoint instead");
        return eachCodePointCommon(context, block, "each_codepoint");
    }

    @JRubyMethod
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        return eachCodePointCommon(context, block, "each_codepoint");
    }

    // rb_io_each_codepoint
    private IRubyObject eachCodePointCommon(ThreadContext context, Block block, String methodName) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        Encoding enc;
        int c;
        int r, n;

        if (!block.isGiven()) return enumeratorize(context.runtime, this, methodName);
        fptr = getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            fptr.checkCharReadable(context);

            fptr.READ_CHECK(context);
            if (fptr.needsReadConversion()) {
                fptr.SET_BINARY_MODE();
                for (;;) {
                    fptr.makeReadConversion(context);
                    for (;;) {
                        if (fptr.cbuf.len != 0) {
                            if (fptr.encs.enc != null)
                                r = StringSupport.preciseLength(fptr.encs.enc, fptr.cbuf.ptr, fptr.cbuf.off, fptr.cbuf.off + fptr.cbuf.len);
                            else
                                r = StringSupport.CONSTRUCT_MBCLEN_CHARFOUND(1);
                            if (!StringSupport.MBCLEN_NEEDMORE_P(r))
                                break;
                            if (fptr.cbuf.len == fptr.cbuf.capa) {
                                throw runtime.newIOError("too long character");
                            }
                        }
                        if (fptr.moreChar(context) == OpenFile.MORE_CHAR_FINISHED) {
                            fptr.clearReadConversion();
                            /* ignore an incomplete character before EOF */
                            return this;
                        }
                    }
                    if (StringSupport.MBCLEN_INVALID_P(r)) {
                        throw runtime.newArgumentError("invalid byte sequence in " + fptr.encs.enc.toString());
                    }
                    n = StringSupport.MBCLEN_CHARFOUND_LEN(r);
                    if (fptr.encs.enc != null) {
                        c = StringSupport.codePoint(runtime, fptr.encs.enc, fptr.cbuf.ptr, fptr.cbuf.off, fptr.cbuf.off + fptr.cbuf.len);
                    }
                    else {
                        c = fptr.cbuf.ptr[fptr.cbuf.off] & 0xFF;
                    }
                    fptr.cbuf.off += n;
                    fptr.cbuf.len -= n;
                    block.yield(context, runtime.newFixnum(c & 0xFFFFFFFF));
                }
            }
            fptr.NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
            enc = fptr.inputEncoding(runtime);
            while (fptr.fillbuf(context) >= 0) {
                r = StringSupport.preciseLength(enc, fptr.rbuf.ptr, fptr.rbuf.off, fptr.rbuf.off + fptr.rbuf.len);
                if (StringSupport.MBCLEN_CHARFOUND_P(r) &&
                        (n = StringSupport.MBCLEN_CHARFOUND_LEN(r)) <= fptr.rbuf.len) {
                    c = StringSupport.codePoint(runtime, fptr.encs.enc, fptr.rbuf.ptr, fptr.rbuf.off, fptr.rbuf.off + fptr.rbuf.len);
                    fptr.rbuf.off += n;
                    fptr.rbuf.len -= n;
                    block.yield(context, runtime.newFixnum(c & 0xFFFFFFFF));
                } else if (StringSupport.MBCLEN_INVALID_P(r)) {
                    throw runtime.newArgumentError("invalid byte sequence in " + enc.toString());
                } else {
                    continue;
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }
        return this;
    }

    /** 
     * <p>Invoke a block for each line.</p>
     *
     * MRI: rb_io_each_line
     */
    private IRubyObject each_lineInternal(ThreadContext context, IRubyObject[] args, Block block, String name) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, name, args);

        Ruby runtime = context.runtime;
        IRubyObject separator = prepareGetsSeparator(context, args);

        ByteListCache cache = new ByteListCache();
        for (IRubyObject line = getline(context, separator); !line.isNil();
		        line = getline(context, separator, -1, cache)) {
            block.yield(context, line);
        }
        
        return this;
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each(final ThreadContext context, IRubyObject[]args, final Block block) {
        return each_lineInternal(context, args, block, "each");
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each_line(final ThreadContext context, IRubyObject[]args, final Block block) {
        return each_lineInternal(context, args, block, "each_line");
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(final ThreadContext context, Block block) {
        context.runtime.getWarnings().warn("IO#lines is deprecated; use #each_line instead");
        return each_lineInternal(context, NULL_ARRAY, block, "each_line");
    }

    @JRubyMethod(name = "readlines", optional = 2)
    public RubyArray readlines(ThreadContext context, IRubyObject[] args) {
        return readlinesCommon(context, args);
    }

    private RubyArray readlinesCommon(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        long limit = prepareGetsLimit(context, args);
        IRubyObject separator = prepareGetsSeparator(context, args);
        RubyArray result = runtime.newArray();
        IRubyObject line;

        while (! (line = getline(context, separator, limit, null)).isNil()) {
            result.append(line);
        }
        return result;
    }

    @JRubyMethod(name = "to_io")
    public RubyIO to_io() {
    	return this;
    }

    @Override
    public String toString() {
        return inspect().toString();
    }
    
    /* class methods for IO */

    // rb_io_s_foreach
    private static IRubyObject foreachInternal19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
       
        IRubyObject opt = ArgsUtil.getOptionsArg(context.runtime, args);
        IRubyObject io = openKeyArgs(context, recv, args, opt);
        if (io.isNil()) return io;

        // io_s_foreach

        IRubyObject[] methodArguments = processReadlinesMethodArguments(args);

        try {
            IRubyObject str;
            while (!(str = ((RubyIO)io).gets(context, methodArguments)).isNil()) {
                block.yield(context, str);
            }
        } finally {
            ((RubyIO)io).close();
            runtime.getGlobalVariables().clear("$_");
        }

        return context.nil;
    }

    @JRubyMethod(name = "foreach", required = 1, optional = 3, meta = true)
    public static IRubyObject foreach(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "foreach", args);

        return foreachInternal19(context, recv, args, block);
    }

    public static RubyIO convertToIO(ThreadContext context, IRubyObject obj) {
        return (RubyIO)TypeConverter.ioGetIO(context.runtime, obj);
    }
   
    @JRubyMethod(name = "select", required = 1, optional = 3, meta = true)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] argv) {
        IRubyObject read, write, except, _timeout;
        Long timeout;
        read = write = except = _timeout = context.nil;

        switch (argv.length) {
            case 4:
                _timeout = argv[3];
            case 3:
                except = argv[2];
            case 2:
                write = argv[1];
            case 1:
                read = argv[0];
        }
        if (_timeout.isNil()) {
            timeout = null;
        }
        else {
            double tmp = _timeout.convertToFloat().getDoubleValue();
            if (tmp < 0) throw context.runtime.newArgumentError("negative timeout");
            timeout = (long)(tmp * 1000); // ms
        }

        SelectExecutor args = new SelectExecutor(read, write, except, timeout);

        return args.go(context);
    }

    // MRI: rb_io_advise
    @JRubyMethod(required = 1, optional = 2)
    public IRubyObject advise(ThreadContext context, IRubyObject[] argv) {
        IRubyObject advice, offset, len;
        advice = offset = len = context.nil;
        int off, l;
        OpenFile fptr;

        switch (argv.length) {
            case 3:
                len = argv[2];
            case 2:
                offset = argv[1];
            case 1:
                advice = argv[0];
        }
        adviceArgCheck(context, advice);

        RubyIO io = GetWriteIO();
        fptr = io.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            off = offset.isNil() ? 0 : offset.convertToInteger().getIntValue();
            l = len.isNil() ? 0 : len.convertToInteger().getIntValue();

            // TODO: implement advise
            //        #ifdef HAVE_POSIX_FADVISE
            //        return do_io_advise(fptr, advice, off, l);
            //        #else
            //        ((void)off, (void)l);	/* Ignore all hint */
            return context.nil;
            //        #endif
        } finally {
            if (locked) fptr.unlock();
        }
    }

    // MRI: advice_arg_check
    static void adviceArgCheck(ThreadContext context, IRubyObject advice) {
        if (!(advice instanceof RubySymbol))
            throw context.runtime.newTypeError("advise must be a symbol");

        String adviceStr = advice.asJavaString();
        switch (adviceStr) {
            default:
                throw context.runtime.newNotImplementedError(rbInspect(context, advice).toString());

            case "normal":
            case "sequential":
            case "random":
            case "willneed":
            case "dontneed":
            case "noreuse":
                // ok
        }
    }
   
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return read19(context, recv, args, Block.NULL_BLOCK);
   }

    public static void failIfDirectory(Ruby runtime, RubyString pathStr) {
        if (RubyFileTest.directory_p(runtime, pathStr).isTrue()) {
            if (Platform.IS_WINDOWS) {
                throw runtime.newErrnoEACCESError(pathStr.asJavaString());
            } else {
                throw runtime.newErrnoEISDirError(pathStr.asJavaString());
            }
        }
    }

    // open_key_args
    private static IRubyObject openKeyArgs(ThreadContext context, IRubyObject recv, IRubyObject[] argv, IRubyObject opt) {
        Ruby runtime = context.runtime;
        IRubyObject path, v;
        
        path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, argv[0]));
        failIfDirectory(runtime, (RubyString)path); // only in JRuby
        // MRI increments args past 0 now, so remaining uses of args only see non-path args
        
        if (opt.isNil()) {
            return ioOpen(context, path, runtime.newFixnum(ModeFlags.RDONLY), runtime.newFixnum(0666), opt);
        }
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("open_args"));
        if (!v.isNil()) {
            IRubyObject args;
            int n;
            
            v = v.convertToArray();
            n = ((RubyArray)v).size() + 1;
            
            args = runtime.newArray(n);
            ((RubyArray)args).push_m19(new IRubyObject[]{path});
            ((RubyArray)args).concat19(v);
            
            return RubyKernel.open19(context, recv, ((RubyArray)args).toJavaArray(), Block.NULL_BLOCK);
        }
        
        return ioOpen(context, path, context.nil, context.nil, opt);
    }
    
    // rb_io_open
    public static IRubyObject ioOpen(ThreadContext context, IRubyObject filename, IRubyObject vmode, IRubyObject vperm, IRubyObject opt) {
        Ruby runtime = context.runtime;
        int[] oflags_p = {0}, fmode_p = {0};
        int perm;
        IRubyObject cmd;
        
        if ((filename instanceof RubyString) && ((RubyString) filename).isEmpty()) {
            throw context.getRuntime().newErrnoENOENTError();
        }

        Object pm = EncodingUtils.vmodeVperm(vmode, vperm);

        IOEncodable convconfig = new IOEncodable.ConvConfig();
        EncodingUtils.extractModeEncoding(context, convconfig, pm, opt, oflags_p, fmode_p);
        perm = (vperm(pm) == null || vperm(pm).isNil()) ? 0666 : RubyNumeric.num2int(vperm(pm));

        if (!(cmd = PopenExecutor.checkPipeCommand(context, filename)).isNil()) {
            return PopenExecutor.pipeOpen(context, cmd, OpenFile.ioOflagsModestr(runtime, oflags_p[0]), fmode_p[0], convconfig);
        } else {
            return ((RubyFile)context.runtime.getFile().allocate()).fileOpenGeneric(context, filename, oflags_p[0], fmode_p[0], convconfig, perm);
        }
    }

    /**
     *  options is a hash which can contain:
     *    encoding: string or encoding
     *    mode: string
     *    open_args: array of string
     */
    private static IRubyObject write19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject str, IRubyObject offset, RubyHash options) {
        Ruby runtime = context.runtime;
        RubyString pathStr = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, path));
        failIfDirectory(runtime, pathStr);

        RubyIO file = null;

        long mode = ModeFlags.CREAT;

        if (options == null || (options != null && options.isEmpty())) {
            if (offset.isNil()) {
                mode |= ModeFlags.WRONLY;
            } else {
                mode |= ModeFlags.RDWR;
            }

            file = (RubyIO) Helpers.invoke(context, runtime.getFile(), "new", path, RubyFixnum.newFixnum(runtime, mode));
        } else if (!options.containsKey(runtime.newSymbol("mode"))) {
            mode |= ModeFlags.WRONLY;
            file = (RubyIO) Helpers.invoke(context, runtime.getFile(), "new", path, RubyFixnum.newFixnum(runtime, mode), options); 
        } else {
            file = (RubyIO) Helpers.invoke(context, runtime.getFile(), "new", path, options);
        }

        try {
            if (!offset.isNil()) file.seek(context, offset);
            return file.write(context, str);
        } finally  {
            file.close();
        }
    }

    /**
     * binread is just like read, except it doesn't take options and it forces
     * mode to be "rb:ASCII-8BIT"
     *
     * @param context the current ThreadContext
     * @param recv the target of the call (IO or a subclass)
     * @param args arguments; path [, length [, offset]]
     * @return the binary contents of the given file, at specified length and offset
     */
    @JRubyMethod(meta = true, required = 1, optional = 2)
    public static IRubyObject binread(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject nil = runtime.getNil();
        IRubyObject path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, args[0]));
        IRubyObject length = nil;
        IRubyObject offset = nil;

        if (args.length > 2) {
            offset = args[2];
            length = args[1];
        } else if (args.length > 1) {
            length = args[1];
        }
        RubyIO file = (RubyIO) Helpers.invoke(context, runtime.getFile(), "new", path, runtime.newString("rb:ASCII-8BIT"));

        try {
            if (!offset.isNil()) {
                file.seek(context, offset);
            }
            return file.read(context, length);
        } finally  {
            file.close();
        }
    }

    // Enebo: annotation processing forced me to do pangea method here...
    @JRubyMethod(name = "read", meta = true, required = 1, optional = 3)
    public static IRubyObject read19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = context.runtime;
        IRubyObject nil = runtime.getNil();
        IRubyObject path = args[0];
        IRubyObject length = nil;
        IRubyObject offset = nil;
        IRubyObject options = nil;

        { // rb_scan_args logic, basically
            if (args.length > 3) {
                if (!(args[3] instanceof RubyHash)) throw runtime.newTypeError("Must be a hash");
                options = (RubyHash) args[3];
                offset = args[2];
                length = args[1];
            } else if (args.length > 2) {
                if (args[2] instanceof RubyHash) {
                    options = (RubyHash) args[2];
                } else {
                    offset = args[2];
                }
                length = args[1];
            } else if (args.length > 1) {
                if (args[1] instanceof RubyHash) {
                    options = (RubyHash) args[1];
                } else {
                    length = args[1];
                }
            }
            if (options == null) {
                options = RubyHash.newHash(runtime);
            }
        }

        RubyIO file = (RubyIO)openKeyArgs(context, recv, new IRubyObject[]{path, length, offset}, options);

        try {
            if (!offset.isNil()) {
                // protect logic around here in MRI?
                file.seek(context, offset);
            }
            return file.read(context, length);
        } finally  {
            file.close();
        }
    }

    // rb_io_s_binwrite
    @JRubyMethod(meta = true, required = 2, optional = 2)
    public static IRubyObject binwrite(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ioStaticWrite(context, recv, args, true);
    }

    // MRI: rb_io_s_write
    @JRubyMethod(name = "write", meta = true, required = 2, optional = 2)
    public static IRubyObject write(ThreadContext context, IRubyObject recv, IRubyObject[] argv) {
        return (ioStaticWrite(context, recv, argv, false));
    }

    // MRI: io_s_write
    public static IRubyObject ioStaticWrite(ThreadContext context, IRubyObject recv, IRubyObject[] argv, boolean binary) {
        Ruby runtime = context.runtime;
        IRubyObject string, offset, opt;
        string = offset = opt = context.nil;

        switch (argv.length) {
            case 4:
                opt = argv[3].convertToHash();
                offset = argv[2];
                string = argv[1];
                break;
            case 3:
                opt = TypeConverter.checkHashType(runtime, argv[2]);
                if (opt.isNil()) offset = argv[2];
                string = argv[1];
                break;
            case 2:
                string = argv[1];
                break;
            default:
                Arity.raiseArgumentError(runtime, argv.length, 2, 4);
        }

        if (opt.isNil()) opt = RubyHash.newHash(runtime);
        else opt = ((RubyHash)opt).dupFast(context);

        if (((RubyHash)opt).op_aref(context, runtime.newSymbol("mode")).isNil()) {
            int mode = OpenFlags.O_WRONLY.intValue()|OpenFlags.O_CREAT.intValue();
            if (OpenFlags.O_BINARY.defined()) {
                if (binary) mode |= OpenFlags.O_BINARY.intValue();
            }
            if (offset.isNil()) mode |= OpenFlags.O_TRUNC.intValue();
            ((RubyHash)opt).op_aset(runtime.newSymbol("mode"), runtime.newFixnum(mode));
        }
        IRubyObject _io = openKeyArgs(context, recv, argv, opt);

        if (_io.isNil()) return context.nil;

        RubyIO io = (RubyIO)_io;

        if (!OpenFlags.O_BINARY.defined()) {
            if (binary) io.binmode();
        }

        if (!offset.isNil()) {
            seekBeforeAccess(context, io, offset, PosixShim.SEEK_SET);
        }

        try {
            return io.write(context, string, false);
        } finally {
            ioClose(runtime, io);
        }
    }

    static IRubyObject seekBeforeAccess(ThreadContext context, RubyIO io, IRubyObject offset, int mode) {
        io.setBinmode();
        return io.doSeek(context, offset, mode);
    }

    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return readlines19(context, recv, args, unusedBlock);
    }

    // rb_io_s_readlines
    @JRubyMethod(name = "readlines", required = 1, optional = 3, meta = true)
    public static IRubyObject readlines19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        int argc = args.length;
        IRubyObject opt = ArgsUtil.getOptionsArg(context.runtime, args);
        IRubyObject io = openKeyArgs(context, recv, args, opt);
        if (io.isNil()) return io;
        
        IRubyObject[] methodArguments = processReadlinesMethodArguments(args);

        return readlinesCommon19(context, (RubyIO)io, methodArguments);
    }

    private static IRubyObject[] processReadlinesMethodArguments(IRubyObject[] args) {
        int count = args.length;
        IRubyObject[] methodArguments = IRubyObject.NULL_ARRAY;
        
        if(count >= 3 && (args[2] instanceof RubyFixnum || args[2].respondsTo("to_int"))) {
            methodArguments = new IRubyObject[]{args[1], args[2]};   
        } else if (count >= 2 && (args[1] instanceof RubyFixnum || args[1].respondsTo("to_int"))) {
            methodArguments = new IRubyObject[]{args[1]};  
        } else if (count >= 2 && !(args[1] instanceof RubyHash))  {
            methodArguments = new IRubyObject[]{args[1]};  
        }
        
        return methodArguments;
    }
    
    private static RubyArray readlinesCommon19(ThreadContext context, RubyIO file, IRubyObject[] newArguments) {
        try {
            return (RubyArray) file.callMethod(context, "readlines", newArguments);
        } finally {
            file.close();
        }
    }

    private void setupPopen(ModeFlags modes, POpenProcess process) throws RaiseException {
        Ruby runtime = getRuntime();
        openFile.setMode(modes.getOpenFileFlags() | OpenFile.SYNC);
        openFile.setProcess(process);

        if (openFile.isReadable()) {
            Channel inChannel;
            if (process.getInput() != null) {
                // NIO-based
                inChannel = process.getInput();
            } else {
                // Stream-based
                inChannel = Channels.newChannel(process.getInputStream());
            }

            ChannelFD main = new ChannelFD(inChannel, runtime.getPosix(), runtime.getFilenoUtil());

            openFile.setFD(main);
            openFile.setMode(OpenFile.READABLE);
        }

        if (openFile.isWritable() && process.hasOutput()) {
            Channel outChannel;
            if (process.getOutput() != null) {
                // NIO-based
                outChannel = process.getOutput();
            } else {
                outChannel = Channels.newChannel(process.getOutputStream());
            }

            ChannelFD pipe = new ChannelFD(outChannel, runtime.getPosix(), runtime.getFilenoUtil());

            RubyIO writeIO = new RubyIO(runtime, runtime.getIO());
            writeIO.initializeCommon(runtime.getCurrentContext(), pipe, runtime.newFixnum(OpenFlags.O_WRONLY), runtime.getNil());

            openFile.tiedIOForWriting = writeIO;
            setInstanceVariable("@tied_io_for_writing", writeIO);
        }
    }

    private static class Ruby19POpen {
        public final RubyString cmd;
        public final IRubyObject[] cmdPlusArgs;
        public final RubyHash env;
        
        public Ruby19POpen(Ruby runtime, IRubyObject[] args) {
            IRubyObject[] _cmdPlusArgs = null;
            RubyHash _env = null;
            IRubyObject _cmd;
            IRubyObject arg0 = args[0].checkArrayType();

            if (args[0] instanceof RubyHash) {
                // use leading hash as env
                if (args.length > 1) {
                    _env = (RubyHash)args[0];
                } else {
                    Arity.raiseArgumentError(runtime, 0, 1, 2);
                }

                if (Platform.IS_WINDOWS) {
                    String[] tokens = args[1].convertToString().toString().split(" ", 2);
                    String commandString = tokens[0].replace('/', '\\') +
                            (tokens.length > 1 ? ' ' + tokens[1] : "");
                    _cmd = runtime.newString(commandString);
                } else {
                    _cmd = args[1].convertToString();
                }
            } else if (args[0] instanceof RubyArray) {
                RubyArray arg0Ary = (RubyArray)arg0;
                if (arg0Ary.isEmpty()) throw runtime.newArgumentError("wrong number of arguments");
                if (arg0Ary.eltOk(0) instanceof RubyHash) {
                    // leading hash, use for env
                    _env = (RubyHash)arg0Ary.delete_at(0);
                }
                if (arg0Ary.isEmpty()) throw runtime.newArgumentError("wrong number of arguments");
                if (arg0Ary.size() > 1 && arg0Ary.eltOk(arg0Ary.size() - 1) instanceof RubyHash) {
                    // trailing hash, use for opts
                    _env = (RubyHash)arg0Ary.eltOk(arg0Ary.size() - 1);
                }
                _cmdPlusArgs = (IRubyObject[])arg0Ary.toJavaArray();

                if (Platform.IS_WINDOWS) {
                    String commandString = _cmdPlusArgs[0].convertToString().toString().replace('/', '\\');
                    _cmdPlusArgs[0] = runtime.newString(commandString);
                } else {
                    _cmdPlusArgs[0] = _cmdPlusArgs[0].convertToString();
                }
                _cmd = _cmdPlusArgs[0];
            } else {
                if (Platform.IS_WINDOWS) {
                    String[] tokens = args[0].convertToString().toString().split(" ", 2);
                    String commandString = tokens[0].replace('/', '\\') +
                            (tokens.length > 1 ? ' ' + tokens[1] : "");
                    _cmd = runtime.newString(commandString);
                } else {
                    _cmd = args[0].convertToString();
                }
            }

            this.cmd = (RubyString)_cmd;
            this.cmdPlusArgs = _cmdPlusArgs;
            this.env = _env;
        }
    }

    @JRubyMethod(name = "popen", required = 1, optional = 2, meta = true)
    public static IRubyObject popen(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        if (runtime.getPosix().isNative() && !Platform.IS_WINDOWS) {
            // new native popen logic
            return PopenExecutor.popen(context, args, (RubyClass)recv, block);
        }

        // old JDK popen logic
        IRubyObject pmode = null;
        RubyHash options = null;
        
        switch(args.length) {
            case 1:
                break;
            case 2:
                if (args[1] instanceof RubyHash) {
                    options = (RubyHash) args[1];
                } else {
                    pmode = args[1];
                }
                break;
            case 3:
                options = args[2].convertToHash();
                pmode = args[1];
                break;
        }
        
        RubyIO io = new RubyIO(runtime, (RubyClass) recv);
        
        io.MakeOpenFile();
        
        Object pm = vmodeVperm(pmode, runtime.newFixnum(0));
        int[] oflags_p = {0}, fmode_p = {0};
        EncodingUtils.extractModeEncoding(context, io, pm, options, oflags_p, fmode_p);
        ModeFlags modes = ModeFlags.createModeFlags(oflags_p[0]);
        
        // FIXME: Reprocessing logic twice for now...
        // for 1.9 mode, strip off the trailing options hash, if there
        if (args.length > 1 && args[args.length - 1] instanceof RubyHash) {
            options = (RubyHash)args[args.length - 1];
            IRubyObject[] newArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 0, newArgs, 0, args.length - 1);
            args = newArgs;
        }
        
        Ruby19POpen r19Popen = new Ruby19POpen(runtime, args);
        
        if ("-".equals(r19Popen.cmd.toString())) {
            throw runtime.newNotImplementedError("popen(\"-\") is unimplemented");
        }

        try {
            ShellLauncher.POpenProcess process;
            if (r19Popen.cmdPlusArgs == null) {
                process = ShellLauncher.popen(runtime, r19Popen.cmd, modes);
            } else {
                process = ShellLauncher.popen(runtime, r19Popen.cmdPlusArgs, r19Popen.env, modes);
            }

            // Yes, this is gross. java.lang.Process does not appear to be guaranteed
            // "ready" when we get it back from Runtime#exec, so we try to give it a
            // chance by waiting for 10ms before we proceed. Only doing this on 1.5
            // since Hotspot 1.6+ does not seem to exhibit the problem.
            if (System.getProperty("java.specification.version", "").equals("1.5")) {
                synchronized (process) {
                    try {
                        process.wait(100);
                    } catch (InterruptedException ie) {}
                }
            }

            checkPopenOptions(options);

            io.setupPopen(modes, process);

            if (block.isGiven()) {
                try {
                    return block.yield(context, io);
                } finally {
                    if (io.openFile.isOpen()) {
                        io.close();
                    }
                    // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, process.waitFor() << 8, ShellLauncher.getPidFromProcess(process)));
                }
            }
            return io;
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    public static IRubyObject pipe(ThreadContext context, IRubyObject recv) {
        return pipe19(context, recv);
    }

    public static IRubyObject pipe19(ThreadContext context, IRubyObject recv) {
        return pipe19(context, recv, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    public static IRubyObject pipe19(ThreadContext context, IRubyObject recv, IRubyObject modes) {
        return pipe19(context, recv, new IRubyObject[] {modes}, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "pipe", optional = 3, meta = true)
    public static IRubyObject pipe19(ThreadContext context, IRubyObject klass, IRubyObject[] argv, Block block) {
        Ruby runtime = context.runtime;
        int state;
        RubyIO r, w;
//        IRubyObject args[] = new IRubyObject[3]
        IRubyObject v1, v2;
        IRubyObject opt;
        v1 = v2 = opt = context.nil;
        OpenFile fptr, fptr2;
        int[] fmode_p = {0};
        IRubyObject ret;
        int argc = argv.length;

        switch (argc) {
            case 3:
                opt = argv[2].convertToHash();
                argc--;
                v2 = argv[1];
                v1 = argv[0];
                break;
            case 2:
                opt = TypeConverter.checkHashType(runtime, argv[1]);
                if (!opt.isNil()) {
                    argc--;
                } else {
                    v2 = argv[1];
                }
                v1 = argv[0];
                break;
            case 1:
                opt = TypeConverter.checkHashType(runtime, argv[0]);
                if (!opt.isNil()) {
                    argc--;
                } else {
                    v1 = argv[0];
                }
        }

        PosixShim posix = new PosixShim(runtime);
        Channel[] fds = posix.pipe();
        if (fds == null)
            throw runtime.newErrnoFromErrno(posix.errno, "opening pipe");

//        args[0] = klass;
//        args[1] = INT2NUM(pipes[0]);
//        args[2] = INT2FIX(O_RDONLY);
//        r = rb_protect(io_new_instance, (VALUE)args, &state);
//        if (state) {
//            close(pipes[0]);
//            close(pipes[1]);
//            rb_jump_tag(state);
//        }
        r = new RubyIO(runtime, (RubyClass)klass);
        r.initializeCommon(context, new ChannelFD(fds[0], runtime.getPosix(), runtime.getFilenoUtil()), runtime.newFixnum(OpenFlags.O_RDONLY), context.nil);
        fptr = r.getOpenFileChecked();

        r.setEncoding(context, v1, v2, opt);

//        args[1] = INT2NUM(pipes[1]);
//        args[2] = INT2FIX(O_WRONLY);
//        w = rb_protect(io_new_instance, (VALUE)args, &state);
//        if (state) {
//            close(pipes[1]);
//            if (!NIL_P(r)) rb_io_close(r);
//            rb_jump_tag(state);
//        }
        w = new RubyIO(runtime, (RubyClass)klass);
        w.initializeCommon(context, new ChannelFD(fds[1], runtime.getPosix(), runtime.getFilenoUtil()), runtime.newFixnum(OpenFlags.O_WRONLY), context.nil);
        fptr2 = w.getOpenFileChecked();
        fptr2.setSync(true);

        EncodingUtils.extractBinmode(runtime, opt, fmode_p);

        if (EncodingUtils.DEFAULT_TEXTMODE != 0) {
            if ((fptr.getMode() & OpenFile.TEXTMODE) != 0 && (fmode_p[0] & OpenFile.BINMODE) != 0) {
                fptr.setMode(fptr.getMode() & ~OpenFile.TEXTMODE);
                // TODO: setmode O_BINARY means what via NIO?
//                setmode(fptr->fd, O_BINARY);
            }
            if (Platform.IS_WINDOWS) { // #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
                if ((fptr.encs.ecflags & EncodingUtils.ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
                    fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
                }
            }
        }
        fptr.setMode(fptr.getMode() | fmode_p[0]);

        if (EncodingUtils.DEFAULT_TEXTMODE != 0) {
            if ((fptr2.getMode() & OpenFile.TEXTMODE) != 0 && (fmode_p[0] & OpenFile.BINMODE) != 0) {
                fptr2.setMode(fptr2.getMode() & ~OpenFile.TEXTMODE);
                // TODO: setmode O_BINARY means what via NIO?
//                setmode(fptr2->fd, O_BINARY);
            }
        }
        fptr2.setMode(fptr2.getMode() | fmode_p[0]);

        ret = runtime.newArray(r, w);
        if (block.isGiven()) {
            return ensureYieldClosePipes(context, ret, r, w, block);
        }
        return ret;
    }

    // MRI: rb_ensure(... pipe_pair_close ...)
    public static IRubyObject ensureYieldClosePipes(ThreadContext context, IRubyObject obj, RubyIO r, RubyIO w, Block block) {
        try {
            return block.yield(context, obj);
        } finally {
            pipePairClose(context.runtime, r, w);
        }
    }

    // MRI: pipe_pair_close
    private static void pipePairClose(Ruby runtime, RubyIO r, RubyIO w) {
        try {
            ioClose(runtime, r);
        } finally {
            ioClose(runtime, w);
        }
    }

    @JRubyMethod(name = "copy_stream", required = 2, optional = 2, meta = true)
    public static IRubyObject copy_stream(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        IRubyObject arg1 = args[0];
        IRubyObject arg2 = args[1];

        RubyInteger length = null;
        RubyInteger offset = null;

        RubyIO io1 = null;
        RubyIO io2 = null;

        RubyString read = null;

        if (args.length >= 3) {
            length = args[2].convertToInteger();
            if (args.length == 4) {
                offset = args[3].convertToInteger();
            }
        }

        if (arg1 instanceof RubyString) {
            io1 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {arg1}, Block.NULL_BLOCK);
        } else if (arg1 instanceof RubyIO) {
            io1 = (RubyIO) arg1;
        } else if (arg1.respondsTo("to_path")) {
            RubyString path = (RubyString) TypeConverter.convertToType19(arg1, runtime.getString(), "to_path");
            io1 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {path}, Block.NULL_BLOCK);
        } else if (arg1.respondsTo("read")) {
            if (length == null) {
                read = arg1.callMethod(context, "read", runtime.getNil()).convertToString();
            } else {
                read = arg1.callMethod(context, "read", length).convertToString();
            }
        } else {
            throw runtime.newArgumentError("Should be String or IO");
        }

        if (arg2 instanceof RubyString) {
            io2 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {arg2, runtime.newString("w")}, Block.NULL_BLOCK);
        } else if (arg2 instanceof RubyIO) {
            io2 = (RubyIO) arg2;
        } else if (arg2.respondsTo("to_path")) {
            RubyString path = (RubyString) TypeConverter.convertToType19(arg2, runtime.getString(), "to_path");
            io2 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {path, runtime.newString("w")}, Block.NULL_BLOCK);
        } else if (arg2.respondsTo("write")) {
            if (read == null) {
                if (length == null) {
                    read = io1.read(context, runtime.getNil()).convertToString();
                } else {
                    read = io1.read(context, length).convertToString();
                }
            }
            return arg2.callMethod(context, "write", read);
        } else {
            throw runtime.newArgumentError("Should be String or IO");
        }

        if (io1 == null) {
            IRubyObject size = io2.write(context, read);
            io2.flush(context);
            return size;
        }

        io2 = io2.GetWriteIO();

        if (!io1.openFile.isReadable()) throw runtime.newIOError("from IO is not readable");
        if (!io2.openFile.isWritable()) throw runtime.newIOError("to IO is not writable");

        // attempt to preserve position of original
        OpenFile fptr = io1.getOpenFileChecked();

        boolean locked = fptr.lock();
        try {
            long pos = fptr.tell(context);
            long size = 0;

            try {
                if (io1.openFile.fileChannel() == null) {
                    long remaining = length == null ? -1 : length.getLongValue();
                    long position = offset == null ? -1 : offset.getLongValue();
                    if (io2.openFile.fileChannel() == null) {
                        ReadableByteChannel from = io1.openFile.readChannel();
                        WritableByteChannel to = io2.openFile.writeChannel();

                        size = transfer(context, from, to, remaining, position);
                    } else {
                        ReadableByteChannel from = io1.openFile.readChannel();
                        FileChannel to = io2.openFile.fileChannel();

                        size = transfer(context, from, to, remaining, position);
                    }
                } else {
                    FileChannel from = io1.openFile.fileChannel();
                    WritableByteChannel to = io2.openFile.writeChannel();
                    long remaining = length == null ? from.size() : length.getLongValue();
                    long position = offset == null ? from.position() : offset.getLongValue();

                    size = transfer(from, to, remaining, position);
                }

                return context.runtime.newFixnum(size);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw runtime.newIOErrorFromException(ioe);
            } finally {
                if (offset != null) {
                    fptr.seek(context, pos, PosixShim.SEEK_SET);
                } else {
                    fptr.seek(context, pos + size, PosixShim.SEEK_SET);
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }
    }

    private static long transfer(ThreadContext context, ReadableByteChannel from, FileChannel to, long length, long position) throws IOException {
        // handle large files on 32-bit JVMs
        long chunkSize = 128 * 1024 * 1024;
        long transferred = 0;
        long bytes;
        long startPosition = to.position();

        if (position != -1) {
            if (from instanceof NativeSelectableChannel) {
                int ret = context.runtime.getPosix().lseek(((NativeSelectableChannel)from).getFD(), position, PosixShim.SEEK_SET);
                if (ret == -1) {
                    throw context.runtime.newErrnoFromErrno(Errno.valueOf(context.runtime.getPosix().errno()), from.toString());
                }
            }
        }
        if (length > 0) {
            while ((bytes = to.transferFrom(from, startPosition+transferred, Math.min(chunkSize, length))) > 0) {
                transferred += bytes;
                length -= bytes;
            }
        } else {
            while ((bytes = to.transferFrom(from, startPosition+transferred, chunkSize)) > 0) {
                transferred += bytes;
            }
        }
        // transforFrom does not change position of target
        to.position(startPosition + transferred);

        return transferred;
    }

    private static long transfer(FileChannel from, WritableByteChannel to, long remaining, long position) throws IOException {
        // handle large files on 32-bit JVMs
        long chunkSize = 128 * 1024 * 1024;
        long transferred = 0;

        if (remaining < 0) remaining = from.size();
        while (remaining > 0) {
            long count = Math.min(remaining, chunkSize);
            long n = from.transferTo(position, count, to);
            if (n == 0) {
                break;
            }
            
            position += n;
            remaining -= n;
            transferred += n;
        }

        return transferred;
    }

    private static long transfer(ThreadContext context, ReadableByteChannel from, WritableByteChannel to, long length, long position) throws IOException {
        int chunkSize = 8 * 1024;
        ByteBuffer buffer = ByteBuffer.allocateDirect(chunkSize);
        long transferred = 0;

        if (position != -1) {
            if (from instanceof NativeSelectableChannel) {
                int ret = context.runtime.getPosix().lseek(((NativeSelectableChannel)from).getFD(), position, PosixShim.SEEK_SET);
                if (ret == -1) {
                    throw context.runtime.newErrnoFromErrno(Errno.valueOf(context.runtime.getPosix().errno()), from.toString());
                }
            }
        }

        while (true) {
            context.pollThreadEvents();

            if (length > 0 && length < chunkSize) {
                // last read should limit to remaining length
                buffer.limit((int)length);
            }
            long n = from.read(buffer);

            if (n == -1) break;

            buffer.flip();
            to.write(buffer);
            buffer.clear();

            transferred += n;
            if (length > 0) {
                length -= n;
                if (length <= 0) break;
            }

            if (!from.isOpen()) break;
        }

        return transferred;
    }

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject tryConvert(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return arg.respondsTo("to_io") ? convertToIO(context, arg) : context.runtime.getNil();
    }

    private static ByteList getNilByteList(Ruby runtime) {
        return ByteList.EMPTY_BYTELIST;
    }
    
    /**
     * Add a thread to the list of blocking threads for this IO.
     * 
     * @param thread A thread blocking on this IO
     */
    public void addBlockingThread(RubyThread thread) {
        OpenFile fptr = openFile;
        if (openFile != null) openFile.addBlockingThread(thread);
    }
    
    /**
     * Remove a thread from the list of blocking threads for this IO.
     * 
     * @param thread A thread blocking on this IO
     */
    public void removeBlockingThread(RubyThread thread) {
        if (openFile != null) openFile.removeBlockingThread(thread);
    }
    
    /**
     * Fire an IOError in all threads blocking on this IO object
     */
    protected void interruptBlockingThreads() {
        if (openFile != null) openFile.interruptBlockingThreads();
    }

    /**
     * Caching reference to allocated byte-lists, allowing for internal byte[] to be
     * reused, rather than reallocated.
     *
     * Predominately used on {@link RubyIO#getline(Ruby, ByteList)} and variants.
     *
     * @author realjenius
     */
    private static class ByteListCache {
        private byte[] buffer = EMPTY_BYTE_ARRAY;
        public void release(ByteList l) {
            buffer = l.getUnsafeBytes();
        }

        public ByteList allocate(int size) {
            ByteList l = new ByteList(buffer, 0, size, false);
            return l;
        }
    }

    /**
     * See http://ruby-doc.org/core-1.9.3/IO.html#method-c-new for the format of modes in options
     */
    protected IOOptions updateIOOptionsFromOptions(ThreadContext context, RubyHash options, IOOptions ioOptions) {
        if (options == null || options.isNil()) return ioOptions;

        Ruby runtime = context.runtime;

        if (options.containsKey(runtime.newSymbol("mode"))) {
            ioOptions = parseIOOptions(options.fastARef(runtime.newSymbol("mode")));
        }

        // This duplicates the non-error behavior of MRI 1.9: the
        // :binmode option is ORed in with other options. It does
        // not obliterate what came before.

        if (options.containsKey(runtime.newSymbol("binmode")) &&
                options.fastARef(runtime.newSymbol("binmode")).isTrue()) {

            ioOptions = newIOOptions(runtime, ioOptions, ModeFlags.BINARY);
        }

        // This duplicates the non-error behavior of MRI 1.9: the
        // :binmode option is ORed in with other options. It does
        // not obliterate what came before.

        if (options.containsKey(runtime.newSymbol("binmode")) &&
                options.fastARef(runtime.newSymbol("binmode")).isTrue()) {

            ioOptions = newIOOptions(runtime, ioOptions, ModeFlags.BINARY);
        }

        if (options.containsKey(runtime.newSymbol("textmode")) &&
                options.fastARef(runtime.newSymbol("textmode")).isTrue()) {

            ioOptions = newIOOptions(runtime, ioOptions, ModeFlags.TEXT);
        }
        
        // TODO: Waaaay different than MRI.  They uniformly have all opening logic
        // do a scan of args before anything opens.  We do this logic in a less
        // consistent way.  We should consider re-impling all IO/File construction
        // logic.
        if (options.containsKey(runtime.newSymbol("open_args"))) {
            IRubyObject args = options.fastARef(runtime.newSymbol("open_args"));
            
            RubyArray openArgs = args.convertToArray();
            
            for (int i = 0; i < openArgs.size(); i++) {
                IRubyObject arg = openArgs.eltInternal(i);
                
                if (arg instanceof RubyString) { // Overrides all?
                    ioOptions = newIOOptions(runtime, arg.asJavaString());
                } else if (arg instanceof RubyFixnum) {
                    ioOptions = newIOOptions(runtime, ((RubyFixnum) arg).getLongValue());
                } else if (arg instanceof RubyHash) {
                    ioOptions = updateIOOptionsFromOptions(context, (RubyHash) arg, ioOptions);
                }
            }
        }

        EncodingUtils.ioExtractEncodingOption(context, this, options, null);

        return ioOptions;
    }

    private static final Set<String> UNSUPPORTED_SPAWN_OPTIONS = new HashSet<String>(Arrays.asList(new String[] {
            "unsetenv_others",
            "prgroup",
            "new_pgroup",
            "rlimit_resourcename",
            "chdir",
            "umask",
            "in",
            "out",
            "err",
            "close_others"
    }));

    private static final Set<String> ALL_SPAWN_OPTIONS = new HashSet<String>(Arrays.asList(new String[] {
            "unsetenv_others",
            "prgroup",
            "new_pgroup",
            "rlimit_resourcename",
            "chdir",
            "umask",
            "in",
            "out",
            "err",
            "close_others"
    }));

    /**
     * Warn when using exec with unsupported options.
     *
     * @param options
     */
    public static void checkExecOptions(IRubyObject options) {
        checkUnsupportedOptions(options, UNSUPPORTED_SPAWN_OPTIONS, "unsupported exec option");
        checkValidOptions(options, ALL_SPAWN_OPTIONS);
    }

    /**
     * Warn when using spawn with unsupported options.
     *
     * @param options
     */
    public static void checkSpawnOptions(IRubyObject options) {
        checkUnsupportedOptions(options, UNSUPPORTED_SPAWN_OPTIONS, "unsupported spawn option");
        checkValidOptions(options, ALL_SPAWN_OPTIONS);
    }

    /**
     * Warn when using spawn with unsupported options.
     *
     * @param options
     */
    public static void checkPopenOptions(IRubyObject options) {
        checkUnsupportedOptions(options, UNSUPPORTED_SPAWN_OPTIONS, "unsupported popen option");
    }

    /**
     * Warn when using unsupported options.
     *
     * @param options
     */
    private static void checkUnsupportedOptions(IRubyObject options, Set<String> unsupported, String error) {
        if (options == null || options.isNil() || !(options instanceof RubyHash)) return;

        RubyHash optsHash = (RubyHash)options;
        Ruby runtime = optsHash.getRuntime();

        for (String key : unsupported) {
            if (optsHash.containsKey(runtime.newSymbol(key))) {
                runtime.getWarnings().warn(error + ": " + key);
            }
        }
    }

    /**
     * Error when using unknown option.
     *
     * @param options
     */
    private static void checkValidOptions(IRubyObject options, Set<String> valid) {
        if (options == null || options.isNil() || !(options instanceof RubyHash)) return;

        RubyHash optsHash = (RubyHash)options;
        Ruby runtime = optsHash.getRuntime();

        for (Object opt : optsHash.keySet()) {
            if (opt instanceof RubySymbol || opt instanceof RubyFixnum || opt instanceof RubyArray || valid.contains(opt.toString())) {
                continue;
            }

            throw runtime.newTypeError("wrong exec option: " + opt);
        }
    }

    // MRI: check_exec_env, w/ check_exec_env_i body in-line
    public static RubyArray checkExecEnv(ThreadContext context, RubyHash hash) {
        Ruby runtime = context.runtime;
        RubyArray env = runtime.newArray();
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)hash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();
            ByteList k;

            k = StringSupport.checkEmbeddedNulls(runtime, key).getByteList();
            if (k.indexOf('=') != -1)
                throw runtime.newArgumentError("environment name contains a equal : " + k);

            if (!val.isNil())
                StringSupport.checkEmbeddedNulls(runtime, val);

            if (Platform.IS_WINDOWS) {
                key = ((RubyString)key).export(context);
            }
            if (!val.isNil()) val = ((RubyString)val).export(context);

            env.push(runtime.newArray(key, val));
        }

        return env;
    }
    
    /**
     * Try for around 1s to destroy the child process. This is to work around
     * issues on some JVMs where if you try to destroy the process too quickly
     * it may not be ready and may ignore the destroy. A subsequent waitFor
     * will then hang. This version tries to destroy and call exitValue
     * repeatedly for up to 1000 calls with 1ms delay between iterations, with
     * the intent that the target process ought to be "ready to die" fairly
     * quickly and we don't get stuck in a blocking waitFor call.
     *
     * @param process The process to obliterate
     */
    public static void obliterateProcess(Process process) {
        int i = 0;
        Object waitLock = new Object();
        while (true) {
            // only try 1000 times with a 1ms sleep between, so we don't hang
            // forever on processes that ignore SIGTERM. After that, not much
            // we can do...
            if (i >= 1000) {
                return;
            }

            // attempt to destroy (SIGTERM on UNIX, TerminateProcess on Windows)
            process.destroy();
            
            try {
                // get the exit value; succeeds if it has terminated, throws
                // IllegalThreadStateException if not.
                process.exitValue();
            } catch (IllegalThreadStateException itse) {
                // increment count and try again after a 1ms sleep
                i += 1;
                synchronized (waitLock) {
                    try {waitLock.wait(1);} catch (InterruptedException ie) {}
                }
                continue;
            }
            // success!
            break;
        }
    }

    public static ModeFlags newModeFlags(Ruby runtime, long mode) {
        return newModeFlags(runtime, (int) mode);
    }

    public static ModeFlags newModeFlags(Ruby runtime, int mode) {
        try {
            return new ModeFlags(mode);
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    public static ModeFlags newModeFlags(Ruby runtime, String mode) {
        try {
            return new ModeFlags(mode);
        } catch (InvalidValueException ive) {
            // This is used by File and StringIO, which seem to want an ArgumentError instead of EINVAL
            throw runtime.newArgumentError("illegal access mode " + mode);       
        }
    }

    public static IOOptions newIOOptions(Ruby runtime, ModeFlags modeFlags) {
        return new IOOptions(modeFlags);
    }

    public static IOOptions newIOOptions(Ruby runtime, long mode) {
        return newIOOptions(runtime, (int) mode);
    }

    public static IOOptions newIOOptions(Ruby runtime, int mode) {
        try {
            ModeFlags modeFlags = new ModeFlags(mode);
            return new IOOptions(modeFlags);
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    public static IOOptions newIOOptions(Ruby runtime, String mode) {
        try {
            return new IOOptions(runtime, mode);
        } catch (InvalidValueException ive) {
            // This is used by File and StringIO, which seem to want an ArgumentError instead of EINVAL
            throw runtime.newArgumentError("illegal access mode " + mode);
        }
    }

    public static IOOptions newIOOptions(Ruby runtime, IOOptions oldFlags, int orOflags) {
        try {
            return new IOOptions(new ModeFlags(oldFlags.getModeFlags().getFlags() | orOflags));
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    @Deprecated
    public IRubyObject readline(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? readline(context) : readline(context, args[0]);
    }

    @Override
    public void setEnc2(Encoding enc2) {
        openFile.encs.enc2 = enc2;
    }
    
    @Override
    public void setEnc(Encoding enc) {
        openFile.encs.enc = enc;
    }
    
    @Override
    public void setEcflags(int ecflags) {
        openFile.encs.ecflags = ecflags;
    }
    
    @Override
    public int getEcflags() {
        return openFile.encs.ecflags;
    }
    
    @Override
    public void setEcopts(IRubyObject ecopts) {
        openFile.encs.ecopts = ecopts;
    }
    
    @Override
    public IRubyObject getEcopts() {
        return openFile.encs.ecopts;
    }
    
    @Override
    public void setBOM(boolean bom) {
        openFile.setBOM(bom);
    }
    
    @Override
    public boolean getBOM() {
        return openFile.isBOM();
    }


    // MRI: rb_io_ascii8bit_binmode
    protected RubyIO setAscii8bitBinmode() {
        OpenFile fptr;

        fptr = getOpenFileChecked();
        fptr.ascii8bitBinmode(getRuntime());

        return this;
    }
    
    public OpenFile MakeOpenFile() {
        Ruby runtime = getRuntime();
        if (openFile != null) {
            rbIoClose(runtime);
            rb_io_fptr_finalize(runtime, openFile);
            openFile = null;
        }
        openFile = new OpenFile(runtime.getNil());
        runtime.addInternalFinalizer(openFile);
        return openFile;
    }

    private static int rb_io_fptr_finalize(Ruby runtime, OpenFile fptr) {
        if (fptr == null) return 0;
        fptr.setPath(null);;
        if (fptr.fd() != null)
            fptr.cleanup(runtime, true);
        fptr.write_lock = null;
        if (fptr.rbuf.ptr != null) {
            fptr.rbuf.ptr = null;
        }
        if (fptr.wbuf.ptr != null) {
            fptr.wbuf.ptr = null;
        }
        fptr.clearCodeConversion();
        return 1;
    }

    private static final byte[] NEWLINE_BYTES = {(byte)'\n'};

    @Deprecated
    public IRubyObject getline(Ruby runtime, ByteList separator) {
        return getline(runtime.getCurrentContext(), runtime.newString(separator), -1, null);
    }
    
    @Deprecated
    public IRubyObject getline(Ruby runtime, ByteList separator, long limit) {
        return getline(runtime.getCurrentContext(), runtime.newString(separator), limit, null);
    }

    @Deprecated
    private IRubyObject getline(ThreadContext context, IRubyObject separator, ByteListCache cache) {
        return getline(context, separator, -1, cache);
    }

    @Deprecated
    public IRubyObject getline(ThreadContext context, ByteList separator) {
        return getline(context, RubyString.newString(context.runtime, separator), -1, null);
    }

    @Deprecated
    public IRubyObject getline(ThreadContext context, ByteList separator, long limit) {
        return getline(context, RubyString.newString(context.runtime, separator), limit, null);
    }

    @Deprecated
    public IRubyObject lines19(final ThreadContext context, Block block) {
        return lines(context, block);
    }

    @Deprecated
    public IRubyObject each_char19(final ThreadContext context, final Block block) {
        return each_char(context, block);
    }

    @Deprecated
    public IRubyObject chars19(final ThreadContext context, final Block block) {
        return chars(context, block);
    }

    @Deprecated
    public RubyArray readlines19(ThreadContext context, IRubyObject[] args) {
        return readlines(context, args);
    }

    @Deprecated
    public RubyIO(Ruby runtime, STDIO stdio) {
        super(runtime, runtime.getIO());

        RubyIO tmp = null;
        switch (stdio) {
            case IN:
                tmp = prepStdio(runtime, runtime.getIn(), Channels.newChannel(runtime.getIn()), OpenFile.READABLE, runtime.getIO(), "<STDIN>");
                break;
            case OUT:
                tmp = prepStdio(runtime, runtime.getOut(), Channels.newChannel(runtime.getOut()), OpenFile.WRITABLE, runtime.getIO(), "<STDOUT>");
                break;
            case ERR:
                tmp = prepStdio(runtime, runtime.getIn(), Channels.newChannel(runtime.getErr()), OpenFile.WRITABLE | OpenFile.SYNC, runtime.getIO(), "<STDERR>");
                break;
        }

        this.openFile = tmp.openFile;
        tmp.openFile = null;
    }

    @Deprecated
    public RubyIO(Ruby runtime, RubyClass cls, ShellLauncher.POpenProcess process, RubyHash options, IOOptions ioOptions) {
        super(runtime, cls);

        ioOptions = updateIOOptionsFromOptions(runtime.getCurrentContext(), options, ioOptions);

        openFile = MakeOpenFile();

        setupPopen(ioOptions.getModeFlags(), process);
    }

    @Deprecated
    public static ModeFlags getIOModes(Ruby runtime, String modesString) {
        return newModeFlags(runtime, modesString);
    }

    @Deprecated
    public static int getIOModesIntFromString(Ruby runtime, String modesString) {
        try {
            return ModeFlags.getOFlagsFromString(modesString);
        } catch (InvalidValueException ive) {
            throw runtime.newArgumentError("illegal access mode");
        }
    }

    @Deprecated
    public static IRubyObject writeStatic(ThreadContext context, IRubyObject recv, IRubyObject[] argv, Block unusedBlock) {
        return write(context, recv, argv);
    }

    @Deprecated
    public static IRubyObject popen3(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return popen3_19(context, recv, args, block);
    }

    @Deprecated
    public static IRubyObject popen3_19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;

        // TODO: handle opts
        if (args.length > 0 && args[args.length - 1] instanceof RubyHash) {
            args = Arrays.copyOf(args, args.length - 1);
        }

        final POpenTuple tuple = popenSpecial(context, args);
        final long pid = ShellLauncher.getPidFromProcess(tuple.process);

        // array trick to be able to reference enclosing RubyThread
        final RubyThread[] waitThread = new RubyThread[1];
        waitThread[0] = new RubyThread(
                runtime,
                (RubyClass) runtime.getClassFromPath("Process::WaitThread"),
                new ThreadedRunnable() {

                    volatile Thread javaThread;

                    @Override
                    public Thread getJavaThread() {
                        return javaThread;
                    }

                    @Override
                    public void run() {
                        javaThread = Thread.currentThread();
                        RubyThread rubyThread;
                        // spin a bit until this happens; should almost never spin
                        while ((rubyThread = waitThread[0]) == null) {
                            Thread.yield();
                        }

                        ThreadContext context = runtime.getThreadService().registerNewThread(rubyThread);

                        rubyThread.op_aset(
                                runtime.newSymbol("pid"),
                                runtime.newFixnum(pid));

                        try {
                            int exitValue = tuple.process.waitFor();

                            // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                            RubyProcess.RubyStatus status = RubyProcess.RubyStatus.newProcessStatus(
                                    runtime,
                                    exitValue << 8,
                                    pid);

                            rubyThread.cleanTerminate(status);
                        } catch (Throwable t) {
                            rubyThread.exceptionRaised(t);
                        } finally {
                            rubyThread.dispose();
                        }
                    }

                });

        RubyArray yieldArgs = RubyArray.newArrayLight(runtime,
                tuple.output,
                tuple.input,
                tuple.error,
                waitThread[0]);

        if (block.isGiven()) {
            try {
                return block.yield(context, yieldArgs);
            } finally {
                cleanupPOpen(tuple);

                IRubyObject status = waitThread[0].join(IRubyObject.NULL_ARRAY);
                context.setLastExitStatus(status);
            }
        }

        return yieldArgs;
    }

    @Deprecated
    public static IRubyObject popen4(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        try {
            POpenTuple tuple = popenSpecial(context, args);

            RubyArray yieldArgs = RubyArray.newArrayLight(runtime,
                    runtime.newFixnum(ShellLauncher.getPidFromProcess(tuple.process)),
                    tuple.output,
                    tuple.input,
                    tuple.error);

            if (block.isGiven()) {
                try {
                    return block.yield(context, yieldArgs);
                } finally {
                    cleanupPOpen(tuple);
                    // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor() << 8, ShellLauncher.getPidFromProcess(tuple.process)));
                }
            }
            return yieldArgs;
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    @Deprecated
    private static void cleanupPOpen(POpenTuple tuple) {
        if (tuple.input.openFile.isOpen()) {
            tuple.input.close();
        }
        if (tuple.output.openFile.isOpen()) {
            tuple.output.close();
        }
        if (tuple.error.openFile.isOpen()) {
            tuple.error.close();
        }
    }

    @Deprecated
    private static class POpenTuple {
        public POpenTuple(RubyIO i, RubyIO o, RubyIO e, Process p) {
            input = i; output = o; error = e; process = p;
        }
        public final RubyIO input;
        public final RubyIO output;
        public final RubyIO error;
        public final Process process;
    }

    @Deprecated
    public static POpenTuple popenSpecial(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        try {
            ShellLauncher.POpenProcess process = ShellLauncher.popen3(runtime, args, false);
            RubyIO input = process.getInput() != null ?
                    new RubyIO(runtime, process.getInput()) :
                    new RubyIO(runtime, process.getInputStream());
            RubyIO output = process.getOutput() != null ?
                    new RubyIO(runtime, process.getOutput()) :
                    new RubyIO(runtime, process.getOutputStream());
            RubyIO error = process.getError() != null ?
                    new RubyIO(runtime, process.getError()) :
                    new RubyIO(runtime, process.getErrorStream());

            // ensure the OpenFile knows it's a process; see OpenFile#finalize
            input.getOpenFile().setProcess(process);
            output.getOpenFile().setProcess(process);
            error.getOpenFile().setProcess(process);

            // set all streams as popenSpecial streams, so we don't shut down process prematurely
            input.popenSpecial = true;
            output.popenSpecial = true;
            error.popenSpecial = true;

            // process streams are not seekable
//            input.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);
//            output.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);
//            error.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);

            return new POpenTuple(input, output, error, process);
//        } catch (BadDescriptorException e) {
//            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @Deprecated
    public IRubyObject doWriteNonblock(ThreadContext context, IRubyObject[] argv, boolean useException) {
        return write_nonblock(context, argv);
    }

    @Deprecated
    public static IRubyObject select_static(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        return select(context, runtime.getIO(), args);
    }
    
    protected OpenFile openFile;

    /**
     * If the stream is being used for popen, we don't want to destroy the process
     * when we close the stream.
     */
    protected boolean popenSpecial;
}
