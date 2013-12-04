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

import org.jruby.runtime.Helpers;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.SelectBlob;
import jnr.constants.platform.Fcntl;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.Stream;
import org.jruby.util.io.IOOptions;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.ShellLauncher;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.PipeException;
import org.jruby.util.io.FileExistsException;
import org.jruby.util.io.DirectoryAsFileException;
import org.jruby.util.io.STDIO;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.ChannelDescriptor;

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.runtime.Arity;

import static org.jruby.CompatVersion.*;
import static org.jruby.RubyEnumerator.enumeratorize;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.internal.runtime.ThreadedRunnable;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.encoding.Transcoder;
import org.jruby.util.ShellLauncher.POpenProcess;
import org.jruby.util.io.IOEncodable;
import static org.jruby.util.StringSupport.isIncompleteChar;

/**
 * 
 * @author jpetersen
 */
@JRubyClass(name="IO", include="Enumerable")
public class RubyIO extends RubyObject implements IOEncodable {
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
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(Channels.newChannel(outputStream)), autoclose));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(OpenFile.WRITABLE | OpenFile.APPEND);
    }
    
    public RubyIO(Ruby runtime, InputStream inputStream) {
        super(runtime, runtime.getIO());
        
        if (inputStream == null) {
            throw runtime.newRuntimeError("Opening null stream");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(Channels.newChannel(inputStream))));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(OpenFile.READABLE);
    }
    
    public RubyIO(Ruby runtime, Channel channel) {
        super(runtime, runtime.getIO());
        
        // We only want IO objects with valid streams (better to error now). 
        if (channel == null) {
            throw runtime.newRuntimeError("Opening null channel");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(channel)));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
    }

    public RubyIO(Ruby runtime, ShellLauncher.POpenProcess process, IOOptions ioOptions) {
        this(runtime, runtime.getIO(), process, null, ioOptions);
    }
    
    @Deprecated
    public RubyIO(Ruby runtime, RubyClass cls, ShellLauncher.POpenProcess process, RubyHash options, IOOptions ioOptions) {
        super(runtime, cls);

        ioOptions = updateIOOptionsFromOptions(runtime.getCurrentContext(), (RubyHash) options, ioOptions);

        openFile = new OpenFile();
        
        setupPopen(ioOptions.getModeFlags(), process);
    }
    
    public RubyIO(Ruby runtime, STDIO stdio) {
        super(runtime, runtime.getIO());
        
        openFile = new OpenFile();
        ChannelDescriptor descriptor;
        Stream mainStream;

        switch (stdio) {
        case IN:
            // special constructor that accepts stream, not channel
            descriptor = new ChannelDescriptor(runtime.getIn(), newModeFlags(runtime, ModeFlags.RDONLY), FileDescriptor.in);
            runtime.putFilenoMap(0, descriptor.getFileno());
            mainStream = ChannelStream.open(runtime, descriptor);
            openFile.setMainStream(mainStream);
            break;
        case OUT:
            descriptor = new ChannelDescriptor(Channels.newChannel(runtime.getOut()), newModeFlags(runtime, ModeFlags.WRONLY | ModeFlags.APPEND), FileDescriptor.out);
            runtime.putFilenoMap(1, descriptor.getFileno());
            mainStream = ChannelStream.open(runtime, descriptor);
            openFile.setMainStream(mainStream);
            openFile.getMainStream().setSync(true);
            break;
        case ERR:
            descriptor = new ChannelDescriptor(Channels.newChannel(runtime.getErr()), newModeFlags(runtime, ModeFlags.WRONLY | ModeFlags.APPEND), FileDescriptor.err);
            runtime.putFilenoMap(2, descriptor.getFileno());
            mainStream = ChannelStream.open(runtime, descriptor);
            openFile.setMainStream(mainStream);
            openFile.getMainStream().setSync(true);
            break;
        }

        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
        // never autoclose stdio streams
        openFile.setAutoclose(false);
        openFile.setStdio(true);
    }
    
    public static RubyIO newIO(Ruby runtime, Channel channel) {
        return new RubyIO(runtime, channel);
    }
    
    public OpenFile getOpenFile() {
        return openFile;
    }
    
    protected OpenFile getOpenFileChecked() {
        openFile.checkClosed(getRuntime());
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
    public int getNativeTypeIndex() {
        return ClassIndex.FILE;
    }

    public static RubyClass createIOClass(Ruby runtime) {
        RubyClass ioClass = runtime.defineClass("IO", runtime.getObject(), IO_ALLOCATOR);

        ioClass.index = ClassIndex.IO;
        ioClass.setReifiedClass(RubyIO.class);

        ioClass.kindOf = new RubyModule.JavaClassKindOf(RubyIO.class);

        ioClass.includeModule(runtime.getEnumerable());
        
        // TODO: Implement tty? and isatty.  We have no real capability to
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.
        
        ioClass.defineAnnotatedMethods(RubyIO.class);

        // Constants for seek
        ioClass.setConstant("SEEK_SET", runtime.newFixnum(Stream.SEEK_SET));
        ioClass.setConstant("SEEK_CUR", runtime.newFixnum(Stream.SEEK_CUR));
        ioClass.setConstant("SEEK_END", runtime.newFixnum(Stream.SEEK_END));

        if (runtime.is1_9()) {
            ioClass.defineModuleUnder("WaitReadable");
            ioClass.defineModuleUnder("WaitWritable");
        }

        return ioClass;
    }

    public OutputStream getOutStream() {
        try {
            return getOpenFileChecked().getMainStreamSafe().newOutputStream();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
    }

    public InputStream getInStream() {
        try {
            return getOpenFileChecked().getMainStreamSafe().newInputStream();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
    }

    public Channel getChannel() {
        try {
            return getOpenFileChecked().getMainStreamSafe().getChannel();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
    }

    @Deprecated
    public Stream getHandler() throws BadDescriptorException {
        return getOpenFileChecked().getMainStreamSafe();
    }

    protected void reopenPath(Ruby runtime, IRubyObject[] args) {
        IRubyObject pathString;
        
        if (runtime.is1_9()) {
            pathString = RubyFile.get_path(runtime.getCurrentContext(), args[0]);
        } else {
            pathString = args[0].convertToString();
        }

        // TODO: check safe, taint on incoming string

        try {
            IOOptions modes;
            if (args.length > 1) {
                IRubyObject modeString = args[1].convertToString();
                modes = newIOOptions(runtime, modeString.toString());

                openFile.setMode(modes.getModeFlags().getOpenFileFlags());
            } else {
                modes = newIOOptions(runtime, "r");
            }

            String path = pathString.toString();

            // Ruby code frequently uses a platform check to choose "NUL:" on windows
            // but since that check doesn't work well on JRuby, we help it out

            openFile.setPath(path);

            if (openFile.getMainStream() == null) {
                try {
                    openFile.setMainStream(ChannelStream.fopen(runtime, path, modes.getModeFlags()));
                } catch (FileExistsException fee) {
                    throw runtime.newErrnoEEXISTError(path);
                }

                if (openFile.getPipeStream() != null) {
                    openFile.getPipeStream().fclose();
                    openFile.setPipeStream(null);
                }
            } else {
                // TODO: This is an freopen in MRI, this is close, but not quite the same
                openFile.getMainStreamSafe().freopen(runtime, path, newIOOptions(runtime, openFile.getModeAsString(runtime)).getModeFlags());
                
                if (openFile.getPipeStream() != null) {
                    // TODO: pipe handler to be reopened with path and "w" mode
                }
            }
        } catch (PipeException pe) {
            throw runtime.newErrnoEPIPEError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw runtime.newErrnoEINVALError();
        }
    }

    protected void reopenIO(Ruby runtime, RubyIO ios) {
        try {
            if (ios.openFile == this.openFile) return;

            OpenFile origFile = ios.getOpenFileChecked();
            OpenFile selfFile = getOpenFileChecked();

            long pos = 0;
            
            Stream origStream = origFile.getMainStreamSafe();
            ChannelDescriptor origDescriptor = origStream.getDescriptor();
            boolean origIsSeekable = origDescriptor.isSeekable();

            if (origFile.isReadable() && origIsSeekable) {
                pos = origStream.fgetpos();
            }

            if (origFile.getPipeStream() != null) {
                origFile.getPipeStream().fflush();
            } else if (origFile.isWritable()) {
                origStream.fflush();
            }

            if (selfFile.isWritable()) {
                selfFile.getWriteStreamSafe().fflush();
            }

            selfFile.setMode(origFile.getMode());
            selfFile.setProcess(origFile.getProcess());
            selfFile.setLineNumber(origFile.getLineNumber());
            selfFile.setPath(origFile.getPath());
            selfFile.setFinalizer(origFile.getFinalizer());

            Stream selfStream = selfFile.getMainStreamSafe();
            ChannelDescriptor selfDescriptor = selfStream.getDescriptor();
            boolean selfIsSeekable = selfDescriptor.isSeekable();

            // check if we're a stdio IO, and ensure we're not badly mutilated
            if (runtime.getFileno(selfDescriptor) >= 0 && runtime.getFileno(selfDescriptor) <= 2) {
                selfStream.clearerr();

                // dup2 new fd into self to preserve fileno and references to it
                origDescriptor.dup2Into(selfDescriptor);
                selfStream.setModes(origStream.getModes());
            } else {
                Stream pipeFile = selfFile.getPipeStream();
                selfStream.fclose();
                selfFile.setPipeStream(null);

                // TODO: turn off readable? am I reading this right?
                // This only seems to be used while duping below, since modes gets
                // reset to actual modes afterward
                //fptr->mode &= (m & FMODE_READABLE) ? ~FMODE_READABLE : ~FMODE_WRITABLE;

                if (pipeFile != null) {
                    selfFile.setMainStream(ChannelStream.fdopen(runtime, origDescriptor, origDescriptor.getOriginalModes()));
                    selfFile.setPipeStream(pipeFile);
                } else {
                    // only use internal fileno here, stdio is handled above
                    selfFile.setMainStream(
                            ChannelStream.open(
                            runtime,
                            origDescriptor.dup2(selfDescriptor.getFileno())));

                    // since we're not actually duping the incoming channel into our handler, we need to
                    // copy the original sync behavior from the other handler
                    selfFile.getMainStreamSafe().setSync(selfFile.getMainStreamSafe().isSync());
                }
            }

            // TODO: anything threads attached to original fd are notified of the close...
            // see rb_thread_fd_close

            if (origFile.isReadable() && pos >= 0) {
                if (selfIsSeekable) {
                    selfFile.seek(pos, Stream.SEEK_SET);
                }

                if (origIsSeekable) {
                    origFile.seek(pos, Stream.SEEK_SET);
                }
            }

            // only use internal fileno here, stdio is handled above
            if (selfFile.getPipeStream() != null && selfDescriptor.getFileno() != selfFile.getPipeStream().getDescriptor().getFileno()) {
                int fd = selfFile.getPipeStream().getDescriptor().getFileno();

                if (origFile.getPipeStream() == null) {
                    selfFile.getPipeStream().fclose();
                    selfFile.setPipeStream(null);
                } else if (fd != origFile.getPipeStream().getDescriptor().getFileno()) {
                    selfFile.getPipeStream().fclose();
                    ChannelDescriptor newFD2 = origFile.getPipeStream().getDescriptor().dup2(fd);
                    selfFile.setPipeStream(ChannelStream.fdopen(runtime, newFD2, newIOOptions(runtime, "w").getModeFlags()));
                }
            }

            if ((selfFile.getMode() & OpenFile.BINMODE) != 0) {
                selfFile.setBinmode();
            }

            // TODO: set our metaclass to target's class (i.e. scary!)

        } catch (IOException ex) { // TODO: better error handling
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newIOError("could not reopen: " + ex.getMessage());
        } catch (PipeException ex) {
            ex.printStackTrace();
            throw runtime.newIOError("could not reopen: " + ex.getMessage());
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    @JRubyMethod(name = "reopen", required = 1, optional = 1)
    public IRubyObject reopen(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
    	IRubyObject tmp = TypeConverter.convertToTypeWithCheck(args[0], runtime.getIO(), "to_io");
        
    	if (!tmp.isNil()) {
            reopenIO(runtime, (RubyIO) tmp);
        } else {
            reopenPath(runtime, args);
        }
        
        return this;
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

    /*
     * Ensure that separator is valid otherwise give it the default paragraph separator.
     */
    private ByteList separator(Ruby runtime) {
        return separator(runtime, runtime.getRecordSeparatorVar().get());
    }

    private ByteList separator(Ruby runtime, IRubyObject separatorValue) {
        ByteList separator = separatorValue.isNil() ? null :
            separatorValue.convertToString().getByteList();

        if (separator != null) {
            if (separator.getRealSize() == 0) return Stream.PARAGRAPH_DELIMETER;

            if (runtime.is1_9()) {
                if (separator.getEncoding() != getEnc()) {
                    separator = Transcoder.strConvEncOpts(runtime.getCurrentContext(), separator,
                            getEnc2(), getEnc(), 0, runtime.getNil());
                }
            }
        }

        return separator;
    }

    private ByteList getSeparatorFromArgs(Ruby runtime, IRubyObject[] args, int idx) {

        if (args.length > idx && args[idx] instanceof RubyFixnum) {
            return separator(runtime, runtime.getRecordSeparatorVar().get());
        }

        return separator(runtime, args.length > idx ? args[idx] : runtime.getRecordSeparatorVar().get());
    }

    private ByteList getSeparatorForGets(Ruby runtime, IRubyObject[] args) {
        return getSeparatorFromArgs(runtime, args, 0);
    }

    private IRubyObject getline(ThreadContext context, ByteList separator, ByteListCache cache) {
        return getline(context, separator, -1, cache);
    }

    public IRubyObject getline(ThreadContext context, ByteList separator) {
        return getline(context, separator, -1, null);
    }

    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     *
     */
    public IRubyObject getline(ThreadContext context, ByteList separator, long limit) {
        return getline(context, separator, limit, null);
    }

    private IRubyObject getline(ThreadContext context, ByteList separator, long limit, ByteListCache cache) {
        return getlineInner(context, separator, limit, cache);
    }
    
    private IRubyObject getlineEmptyString(Ruby runtime) {
        if (runtime.is1_9()) return RubyString.newEmptyString(runtime, getReadEncoding());

        return RubyString.newEmptyString(runtime);
    }
    
    private IRubyObject getlineAll(ThreadContext context, OpenFile myOpenFile) throws IOException, BadDescriptorException {
        Ruby runtime = context.runtime;
        RubyString str = (RubyString)readAll(context);

        if (str.getByteList().length() == 0) return runtime.getNil();
        incrementLineno(runtime, myOpenFile);
        
        return str;
    }
    
    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     * mri: rb_io_getline_1 (mostly)
     */
    private IRubyObject getlineInner(ThreadContext context, ByteList separator, long limit, ByteListCache cache) {
        Ruby runtime = context.runtime;
        
        try {
            boolean is19 = runtime.is1_9();
            
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            boolean isParagraph = separator == Stream.PARAGRAPH_DELIMETER;
            separator = isParagraph ? Stream.PARAGRAPH_SEPARATOR : separator;
            
            if (isParagraph) swallow('\n');
            
            if (separator == null && limit < 0) {
                return getlineAll(runtime.getCurrentContext(), myOpenFile);
            } else if (limit == 0) {
                return getlineEmptyString(runtime);
            } else if (separator != null && separator.length() == 1 && limit < 0 && 
                    (!is19 || (!needsReadConversion() && getReadEncoding().isAsciiCompatible()))) {
                return getlineFast(runtime, separator.get(0) & 0xFF, cache);
            } else {
                Stream readStream = myOpenFile.getMainStreamSafe();
                int c = -1;
                int n = -1;
                int newline = (separator != null) ? (separator.get(separator.length() - 1) & 0xFF) : -1;
                
                // FIXME: Change how we consume streams to match MRI (see append_line/more_char/fill_cbuf)
                // Awful hack.  MRI pre-transcodes lines into read-ahead whereas
                // we read a single line at a time PRE-transcoded.  To keep our
                // logic we need to do one additional transcode of the sep to
                // match the pre-transcoded encoding.  This is gross and we should
                // mimick MRI.
                if (is19 && separator != null && separator.getEncoding() != getInputEncoding()) {
                    separator = Transcoder.strConvEncOpts(runtime.getCurrentContext(), separator, separator.getEncoding(), getInputEncoding(), 0, context.nil);
                    newline = separator.get(separator.length() - 1) & 0xFF;
                }

                ByteList buf = cache != null ? cache.allocate(0) : new ByteList(0);
                try {
                    boolean update = false;
                    boolean limitReached = false;
                    
                    if (is19) makeReadConversion(context);
                    
                    while (true) {
                        do {
                            readCheck(readStream);
                            readStream.clearerr();

                            try {
                                runtime.getCurrentContext().getThread().beforeBlockingCall();
                                if (limit == -1) {
                                    n = readStream.getline(buf, (byte) newline);
                                } else {
                                    n = readStream.getline(buf, (byte) newline, limit);

                                    if (buf.length() > 0 && isIncompleteChar(buf.get(buf.length() - 1))) {
                                        buf.append((byte)readStream.fgetc());
                                    }

                                    limit -= n;
                                    if (limit <= 0) {
                                        update = limitReached = true;
                                        break;
                                    }
                                }

                                c = buf.length() > 0 ? buf.get(buf.length() - 1) & 0xff : -1;
                            } catch (EOFException e) {
                                n = -1;
                            } finally {
                                runtime.getCurrentContext().getThread().afterBlockingCall();
                            }
                            
                            // CRuby checks ferror(f) and retry getc for
                            // non-blocking IO.
                            if (n == 0) {
                                waitReadable(readStream);
                                continue;
                            } else if (n == -1) {
                                break;
                            }

                            update = true;
                        } while (c != newline); // loop until we see the nth separator char


                        // if we hit EOF or reached limit then we're done
                        if (n == -1 || limitReached) {
                            break;
                        }

                        // if we've found the last char of the separator,
                        // and we've found at least as many characters as separator length,
                        // and the last n characters of our buffer match the separator, we're done
                        if (c == newline && separator != null && buf.length() >= separator.length() &&
                                0 == ByteList.memcmp(buf.getUnsafeBytes(), buf.getBegin() + buf.getRealSize() - separator.length(), separator.getUnsafeBytes(), separator.getBegin(), separator.getRealSize())) {
                            break;
                        }
                    }
                    
                    if (is19 && readconv != null) buf = readconv.transcode(context, buf);
                    
                    if (isParagraph && c != -1) swallow('\n');
                    if (!update) return runtime.getNil();

                    incrementLineno(runtime, myOpenFile);

                    ByteList newBuf = cache != null ? new ByteList(buf) : buf;
                    RubyString str = RubyString.newString(runtime, newBuf);
                    
                    return ioEncStr(str);
                } finally {
                    if (cache != null) cache.release(buf);
                }
            }
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (EOFException e) {
            return runtime.getNil();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    // fptr->enc and codeconv->enc
    public Encoding getEnc() {
        return enc;
    }
    
    // mri: io_read_encoding
    public Encoding getReadEncoding() {
        return enc != null ? enc : EncodingUtils.defaultExternalEncoding(getRuntime());
    }
    
    // fptr->enc2 and codeconv->enc2
    public Encoding getEnc2() {
        return enc2;
    }
    
    // mri: io_input_encoding
    public Encoding getInputEncoding() {
        return enc2 != null ? enc2 : getReadEncoding();
    }

    private RubyString makeString(Ruby runtime, ByteList buffer, boolean isCached) {
        ByteList newBuf = isCached ? new ByteList(buffer) : buffer;
        if (runtime.is1_9()) newBuf.setEncoding(getReadEncoding());

        RubyString str = RubyString.newString(runtime, newBuf);
        str.setTaint(true);

        return str;
    }

    private void incrementLineno(Ruby runtime, OpenFile myOpenFile) {
        int lineno = myOpenFile.getLineNumber() + 1;
        myOpenFile.setLineNumber(lineno);
        runtime.setCurrentLine(lineno);
        RubyArgsFile.setCurrentLineNumber(runtime.getArgsFile(), lineno);
    }

    protected boolean swallow(int term) throws IOException, BadDescriptorException {
        Stream readStream = openFile.getMainStreamSafe();
        int c;
        
        do {
            readCheck(readStream);
            
            try {
                c = readStream.fgetc();
            } catch (EOFException e) {
                c = -1;
            }
            
            if (c != term) {
                readStream.ungetc(c);
                return true;
            }
        } while (c != -1);
        
        return false;
    }
    
    private static String vendor;
    static { String v = SafePropertyAccessor.getProperty("java.vendor") ; vendor = (v == null) ? "" : v; };
    private static String msgEINTR = "Interrupted system call";

    public static boolean restartSystemCall(Exception e) {
        return vendor.startsWith("Apple") && e.getMessage().equals(msgEINTR);
    }
    
    private IRubyObject getlineFast(Ruby runtime, int delim, ByteListCache cache) throws IOException, BadDescriptorException {
        Stream readStream = openFile.getMainStreamSafe();
        int c = -1;

        ByteList buf = cache != null ? cache.allocate(0) : new ByteList(0);
        try {
            boolean update = false;
            do {
                readCheck(readStream);
                readStream.clearerr();
                int n;
                try {
                    runtime.getCurrentContext().getThread().beforeBlockingCall();
                    n = readStream.getline(buf, (byte) delim);
                    c = buf.length() > 0 ? buf.get(buf.length() - 1) & 0xff : -1;
                } catch (EOFException e) {
                    n = -1;
                } finally {
                    runtime.getCurrentContext().getThread().afterBlockingCall();
                }

                // CRuby checks ferror(f) and retry getc for non-blocking IO.
                if (n == 0) {
                    waitReadable(readStream);
                    continue;
                } else if (n == -1) {
                    break;
                }
                
                update = true;
            } while (c != delim);

            if (!update) return runtime.getNil();
                
            incrementLineno(runtime, openFile);

            return makeString(runtime, buf, cache != null);
        } finally {
            if (cache != null) cache.release(buf);
        }
    }
    // IO class methods.

    @JRubyMethod(name = {"new", "for_fd"}, rest = true, meta = true)
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

    private IRubyObject initializeCommon19(ThreadContext context, int fileno, IRubyObject vmodeArg, IRubyObject opt) {
        Ruby runtime = context.runtime;
        
        int ofmode;
        int[] oflags_p = {ModeFlags.RDONLY};

        if(opt != null && !opt.isNil() && !(opt instanceof RubyHash) && !(opt.respondsTo("to_hash"))) {
            throw runtime.newArgumentError("last argument must be a hash!");
        }
        
        if (opt != null && !opt.isNil()) {
            opt = opt.convertToHash();
        }
        
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno(runtime.getFilenoExtMap(fileno));

            if (descriptor == null) throw runtime.newErrnoEBADFError();

            descriptor.checkOpen();
            
            IRubyObject[] pm = new IRubyObject[] { runtime.newFixnum(0), vmodeArg };
            int[] fmode_p = {0};
            EncodingUtils.extractModeEncoding(context, this, pm, opt, oflags_p, fmode_p);
            
            oflags_p[0] = descriptor.getOriginalModes().getFlags();

            ofmode = ModeFlags.getOpenFileFlagsFor(oflags_p[0]);
            if (pm[EncodingUtils.VMODE] == null || pm[EncodingUtils.VMODE].isNil()) {
                fmode_p[0] = ofmode;
            } else if (((~ofmode & fmode_p[0]) & OpenFile.READWRITE) != 0) {
                throw runtime.newErrnoEINVALError();
            }
            
            if (!opt.isNil() && ((RubyHash)opt).op_aref(context, runtime.newSymbol("autoclose")) == runtime.getFalse()) {
                setAutoclose(false);
            }
            
            // JRUBY-4650: Make sure we clean up the old data, if it's present.
            MakeOpenFile();

            ModeFlags modes = ModeFlags.createModeFlags(oflags_p[0]);
            
            openFile.setMode(fmode_p[0]);
            openFile.setMainStream(fdopen(descriptor, modes));
            clearCodeConversion();
            
//            io_check_tty(fp);
//            if (fileno(stdin) == fd)
//                fp - > stdio_file = stdin;
//            else if (fileno(stdout) == fd)
//                fp - > stdio_file = stdout;
//            else if (fileno(stderr) == fd)
//                fp - >stdio_file = stderr;
            
            if (hasBom) {
                EncodingUtils.ioSetEncodingByBOM(context, this);
            }
        } catch (BadDescriptorException ex) {
            throw context.runtime.newErrnoEBADFError();
        }

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, Block unused) {
        return initializeCommon19(context, RubyNumeric.fix2int(fileNumber), null, context.nil);
    }
    
    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, IRubyObject second, Block unused) {
        int fileno = RubyNumeric.fix2int(fileNumber);
        IRubyObject vmode = null;
        IRubyObject options = null;
        IRubyObject hashTest = TypeConverter.checkHashType(context.runtime, second);
        if (hashTest instanceof RubyHash) {
            options = hashTest;
        } else {
            options = context.nil;
            vmode = second;
        }

        return initializeCommon19(context, fileno, vmode, options);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, IRubyObject modeValue, IRubyObject options, Block unused) {
        int fileno = RubyNumeric.fix2int(fileNumber);

        return initializeCommon19(context, fileno, modeValue, options);
    }

    // No encoding processing
    protected IOOptions parseIOOptions(IRubyObject arg) {
        Ruby runtime = getRuntime();

        if (arg instanceof RubyFixnum) return newIOOptions(runtime, (int) RubyFixnum.fix2long(arg));

        return newIOOptions(runtime, newModeFlags(runtime, arg.convertToString().toString()));
    }

    // Encoding processing
    protected IOOptions parseIOOptions19(IRubyObject arg) {
        Ruby runtime = getRuntime();

        if (arg instanceof RubyFixnum) return newIOOptions(runtime, (int) RubyFixnum.fix2long(arg));

        String modeString = arg.convertToString().toString();
        try {
            return new IOOptions(runtime, modeString);
        } catch (InvalidValueException ive) {
            throw runtime.newArgumentError("invalid access mode " + modeString);
        }
    }

    @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = getRuntime();
        int argCount = args.length;
        IOOptions ioOptions;
        
        int fileno = RubyNumeric.fix2int(args[0]);
        
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno(runtime.getFilenoExtMap(fileno));
            
            if (descriptor == null) {
                throw runtime.newErrnoEBADFError();
            }
            
            descriptor.checkOpen();
            
            if (argCount == 2) {
                if (args[1] instanceof RubyFixnum) {
                    ioOptions = newIOOptions(runtime, RubyFixnum.fix2long(args[1]));
                } else {
                    ioOptions = newIOOptions(runtime, args[1].convertToString().toString());
                }
            } else {
                // use original modes
                ioOptions = newIOOptions(runtime, descriptor.getOriginalModes());
            }
            
            // JRUBY-4650: Make sure we clean up the old data, if it's present.
            MakeOpenFile();

            if (openFile.isOpen()) {
                // JRUBY-4650: Make sure we clean up the old data,
                // if it's present.
                openFile.cleanup(runtime, false);
            }

            openFile.setMode(ioOptions.getModeFlags().getOpenFileFlags());
        
            openFile.setMainStream(fdopen(descriptor, ioOptions.getModeFlags()));
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        }
        
        return this;
    }
    
    protected Stream fdopen(ChannelDescriptor existingDescriptor, ModeFlags modes) {
        Ruby runtime = getRuntime();

        // See if we already have this descriptor open.
        // If so then we can mostly share the handler (keep open
        // file, but possibly change the mode).
        
        if (existingDescriptor == null) {
            // redundant, done above as well
            
            // this seems unlikely to happen unless it's a totally bogus fileno
            // ...so do we even need to bother trying to create one?
            
            // IN FACT, we should probably raise an error, yes?
            throw runtime.newErrnoEBADFError();
            
//            if (mode == null) {
//                mode = "r";
//            }
//            
//            try {
//                openFile.setMainStream(streamForFileno(getRuntime(), fileno));
//            } catch (BadDescriptorException e) {
//                throw getRuntime().newErrnoEBADFError();
//            } catch (IOException e) {
//                throw getRuntime().newErrnoEBADFError();
//            }
//            //modes = new IOModes(getRuntime(), mode);
//            
//            registerStream(openFile.getMainStream());
        } else {
            // We are creating a new IO object that shares the same
            // IOHandler (and fileno).
            try {
                return ChannelStream.fdopen(runtime, existingDescriptor, modes);
            } catch (InvalidValueException ive) {
                throw runtime.newErrnoEINVALError();
            }
        }
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject external_encoding(ThreadContext context) {
        EncodingService encodingService = context.runtime.getEncodingService();
        
        if (enc2 != null) return encodingService.getEncoding(enc2);
        
        if (openFile.isWritable()) {
            return enc == null ? context.runtime.getNil() : encodingService.getEncoding(enc);
        }
        
        return encodingService.getEncoding(getReadEncoding());
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject internal_encoding(ThreadContext context) {
        if (enc2 == null) return context.nil;
        
        return context.runtime.getEncodingService().getEncoding(getReadEncoding());
    }

    @JRubyMethod(compat=RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingObj) {
        setEncoding(context, encodingObj, context.nil, context.nil);

        return context.nil;
    }

    @JRubyMethod(compat=RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding) {
        IRubyObject opt = TypeConverter.checkHashType(context.runtime, internalEncoding);
        if (!opt.isNil()) {
            setEncoding(context, encodingString, context.nil, opt);
        } else {
            setEncoding(context, encodingString, internalEncoding, context.nil);
        }

        return context.nil;
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding, IRubyObject options) {
        setEncoding(context, encodingString, internalEncoding, options);

        return context.nil;
    }
    
    // mri: io_encoding_set
    public void setEncoding(ThreadContext context, IRubyObject v1, IRubyObject v2, IRubyObject opt) {
        IOEncodable.ConvConfig holder = new IOEncodable.ConvConfig();
        int ecflags = this.ecflags;
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
        
        this.enc = holder.enc;
        this.enc2 = holder.enc2;
        this.ecflags = ecflags;
        this.ecopts = ecopts_p[0];

        clearCodeConversion();
    }

    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        RubyClass klass = (RubyClass)recv;
        
        RubyIO io = (RubyIO)klass.newInstance(context, args, block);

        if (block.isGiven()) {
            try {
                return block.yield(context, io);
            } finally {
                try {
                    io.getMetaClass().finvoke(context, io, "close", IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                } catch (RaiseException re) {
                    RubyException rubyEx = re.getException();
                    if (rubyEx.kind_of_p(context, runtime.getStandardError()).isTrue()) {
                        // MRI behavior: swallow StandardErorrs
                        runtime.getGlobalVariables().clear("$!");
                    } else {
                        throw re;
                    }
                }
            }
        }

        return io;
    }

    @JRubyMethod(required = 1, optional = 2, meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject sysopen(IRubyObject recv, IRubyObject[] args, Block block) {
        StringSupport.checkStringSafety(recv.getRuntime(), args[0]);
        IRubyObject pathString = args[0].convertToString();
        return sysopenCommon(recv, args, block, pathString);
    }
    
    @JRubyMethod(name = "sysopen", required = 1, optional = 2, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject sysopen19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString path = RubyFile.get_path(context, args[0]);
        StringSupport.checkStringSafety(context.runtime, path);

        return sysopenCommon(recv, args, block, path);
    }

    private static IRubyObject sysopenCommon(IRubyObject recv, IRubyObject[] args, Block block, IRubyObject pathString) {
        Ruby runtime = recv.getRuntime();
        String path = pathString.toString();

        IOOptions modes;
        int perms = -1; // -1 == don't set permissions

        if (args.length > 1 && !args[1].isNil()) {
            IRubyObject modeString = args[1].convertToString();
            modes = newIOOptions(runtime, modeString.toString());
        } else {
            modes = newIOOptions(runtime, "r");
        }

        if (args.length > 2 && !args[2].isNil()) {
            RubyInteger permsInt =
                args.length >= 3 ? args[2].convertToInteger() : null;
            perms = RubyNumeric.fix2int(permsInt);
        }

        int fileno = -1;
        try {
            ChannelDescriptor descriptor =
                ChannelDescriptor.open(runtime.getCurrentDirectory(),
                                       path, modes.getModeFlags(), perms, runtime.getPosix(),
                                       runtime.getJRubyClassLoader());
            // always a new fileno, so ok to use internal only
            fileno = descriptor.getFileno();
        }
        catch (FileNotFoundException fnfe) {
            throw runtime.newErrnoENOENTError(path);
        } catch (DirectoryAsFileException dafe) {
            throw runtime.newErrnoEISDirError(path);
        } catch (FileExistsException fee) {
            throw runtime.newErrnoEEXISTError(path);
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
        return runtime.newFixnum(fileno);
    }

    public boolean isAutoclose() {
        return openFile.isAutoclose();
    }

    public void setAutoclose(boolean autoclose) {
        openFile.setAutoclose(autoclose);
    }

    @JRubyMethod(name = "autoclose?", compat = RUBY1_9)
    public IRubyObject autoclose(ThreadContext context) {
        return context.runtime.newBoolean(isAutoclose());
    }

    @JRubyMethod(name = "autoclose=", compat = RUBY1_9)
    public IRubyObject autoclose_set(ThreadContext context, IRubyObject autoclose) {
        setAutoclose(autoclose.isTrue());
        return context.nil;
    }

    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        if (isClosed()) throw getRuntime().newIOError("closed stream");

        setAscii8bitBinmode();

        // missing logic:
        // write_io = GetWriteIO(io);
        // if (write_io != io)
        //     rb_io_ascii8bit_binmode(write_io);

        return this;
    }

    @JRubyMethod(name = "binmode?", compat = RUBY1_9)
    public IRubyObject op_binmode(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, openFile.isBinmode());
    }

    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(ThreadContext context, IRubyObject obj) {
       Ruby runtime = context.runtime;
        
        try {
            RubyString string = obj.asString();
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkWritable(runtime);
            
            Stream writeStream = myOpenFile.getWriteStream();
            
            if (myOpenFile.isWriteBuffered()) {
                runtime.getWarnings().warn(ID.SYSWRITE_BUFFERED_IO, "syswrite for buffered IO");
            }
            
            if (!writeStream.getDescriptor().isWritable()) {
                myOpenFile.checkClosed(runtime);
            }
            
            context.getThread().beforeBlockingCall();
            int read = writeStream.getDescriptor().write(string.getByteList());
            
            if (read == -1) {
                // TODO? I think this ends up propagating from normal Java exceptions
                // sys_fail(openFile.getPath())
            }
            
            return runtime.newFixnum(read);
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            if (e.getMessage().equals("Broken pipe")) {
                throw runtime.newErrnoEPIPEError();
            }
            if (e.getMessage().equals("Connection reset by peer")) {
                throw runtime.newErrnoEPIPEError();
            }
            throw runtime.newSystemCallError(e.getMessage());
        } finally {
            context.getThread().afterBlockingCall();
        }
    }

    @JRubyMethod(name = "write_nonblock", required = 1)
    public IRubyObject write_nonblock(ThreadContext context, IRubyObject obj) {
        return doWriteNonblock(context, obj, true);
    }
    
    public IRubyObject doWriteNonblock(ThreadContext context, IRubyObject obj, boolean useException) {
        Ruby runtime = context.runtime;

        OpenFile myOpenFile = getOpenFileChecked();

        try {
            myOpenFile.checkWritable(context.runtime);
            RubyString str = obj.asString();
            if (str.getByteList().length() == 0) {
                return context.runtime.newFixnum(0);
            }

            if (myOpenFile.isWriteBuffered()) {
                context.runtime.getWarnings().warn(ID.SYSWRITE_BUFFERED_IO, "write_nonblock for buffered IO");
            }

            ChannelStream stream = (ChannelStream)myOpenFile.getWriteStream();

            int written = stream.writenonblock(str.getByteList());
            if (written == 0) {
                if (useException) {
                    if (runtime.is1_9()) {
                        throw runtime.newErrnoEAGAINWritableError("");
                    } else {
                        throw runtime.newErrnoEWOULDBLOCKError();
                    }
                } else {
                    return runtime.fastNewSymbol("wait_writable");
                }
            }

            return context.runtime.newFixnum(written);
        } catch (IOException ex) {
            throw context.runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw context.runtime.newErrnoEBADFError();
        } catch (InvalidValueException ex) {
            throw context.runtime.newErrnoEINVALError();
        }
    }
    
    /** io_write
     * 
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.runtime;
        
        RubyString str = obj.asString();

        // TODO: Ruby reuses this logic for other "write" behavior by checking if it's an IO and calling write again
        
        if (str.getByteList().length() == 0) {
            return runtime.newFixnum(0);
        }

        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkWritable(runtime);

            context.getThread().beforeBlockingCall();
            int written = fwrite(str);

            if (written == -1) {
                // TODO: sys fail
            }

            // if not sync, we switch to write buffered mode
            if (!myOpenFile.isSync()) {
                myOpenFile.setWriteBuffered();
            }

            return runtime.newFixnum(written);
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } finally {
            context.getThread().afterBlockingCall();
        }
    }
    
    private boolean waitWritable(Stream stream) {
        Channel ch = stream.getChannel();
        if (ch instanceof SelectableChannel) {
            getRuntime().getCurrentContext().getThread().select(ch, this, SelectionKey.OP_WRITE);
            return true;
        }
        return false;
    }

    private boolean waitReadable(Stream stream) {
        if (stream.readDataBuffered()) {
            return true;
        }
        Channel ch = stream.getChannel();
        if (ch instanceof SelectableChannel) {
            getRuntime().getCurrentContext().getThread().select(ch, this, SelectionKey.OP_READ);
            return true;
        }
        return false;
    }

    protected int fwrite(RubyString buffer) {
        int n, r, l, offset = 0;
        boolean eagain = false;
        Stream writeStream = openFile.getWriteStream();
        
        if (getRuntime().is1_9()) {
            buffer = (RubyString)doWriteConversion(getRuntime().getCurrentContext(), buffer);
        }

        int len = buffer.size();

        if ((n = len) <= 0) return n;

        // console() can detect underlying windows codepage so we will just write to it
        // and hope it is legible.
        if (Platform.IS_WINDOWS && tty_p(getRuntime().getCurrentContext()).isTrue()) {
            System.console().printf("%s", buffer.asJavaString());
            return len;
        }

        try {
            if (openFile.isSync()) {
                openFile.fflush(writeStream);

                // TODO: why is this guarded?
    //            if (!rb_thread_fd_writable(fileno(f))) {
    //                rb_io_check_closed(fptr);
    //            }
               
                while(offset<len) {
                    l = n;

                    // TODO: Something about pipe buffer length here

                    r = writeStream.getDescriptor().write(buffer.getByteList(),offset,l);

                    if(r == len) {
                        return len; //Everything written
                    }

                    if (0 <= r) {
                        offset += r;
                        n -= r;
                        eagain = true;
                    }

                    if(eagain && waitWritable(writeStream)) {
                        openFile.checkClosed(getRuntime());
                        if(offset >= buffer.size()) {
                            return -1;
                        }
                        eagain = false;
                    } else {
                        return -1;
                    }
                }


                // TODO: all this stuff...some pipe logic, some async thread stuff
    //          retry:
    //            l = n;
    //            if (PIPE_BUF < l &&
    //                !rb_thread_critical &&
    //                !rb_thread_alone() &&
    //                wsplit_p(fptr)) {
    //                l = PIPE_BUF;
    //            }
    //            TRAP_BEG;
    //            r = write(fileno(f), RSTRING(str)->ptr+offset, l);
    //            TRAP_END;
    //            if (r == n) return len;
    //            if (0 <= r) {
    //                offset += r;
    //                n -= r;
    //                errno = EAGAIN;
    //            }
    //            if (rb_io_wait_writable(fileno(f))) {
    //                rb_io_check_closed(fptr);
    //                if (offset < RSTRING(str)->len)
    //                    goto retry;
    //            }
    //            return -1L;
            }

            // TODO: handle errors in buffered write by retrying until finished or file is closed
            return writeStream.fwrite(buffer.getByteList());
    //        while (errno = 0, offset += (r = fwrite(RSTRING(str)->ptr+offset, 1, n, f)), (n -= r) > 0) {
    //            if (ferror(f)
    //            ) {
    //                if (rb_io_wait_writable(fileno(f))) {
    //                    rb_io_check_closed(fptr);
    //                    clearerr(f);
    //                    if (offset < RSTRING(str)->len)
    //                        continue;
    //                }
    //                return -1L;
    //            }
    //        }

//            return len - n;
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        }
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
        Ruby runtime = context.runtime;
        // map to external fileno
        try {
            return runtime.newFixnum(runtime.getFileno(getOpenFileChecked().getMainStreamSafe().getDescriptor()));
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
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
     * @return the current sync mode.
     */
    @JRubyMethod(name = "sync")
    public RubyBoolean sync(ThreadContext context) {
        try {
            return context.runtime.newBoolean(getOpenFileChecked().getMainStreamSafe().isSync());
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
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
    @JRubyMethod(name = "pid")
    public IRubyObject pid(ThreadContext context) {
        OpenFile myOpenFile = getOpenFileChecked();
        
        if (myOpenFile.getProcess() == null) {
            return context.runtime.getNil();
        }
        
        // Of course this isn't particularly useful.
        long pid = myOpenFile.getPid();

        return context.runtime.newFixnum(pid);
    }
    
    @JRubyMethod(name = {"pos", "tell"})
    public RubyFixnum pos(ThreadContext context) {
        try {
            return context.runtime.newFixnum(getOpenFileChecked().getMainStreamSafe().fgetpos());
        } catch (InvalidValueException ex) {
            throw context.runtime.newErrnoEINVALError();
        } catch (BadDescriptorException bde) {
            throw context.runtime.newErrnoEBADFError();
        } catch (PipeException e) {
            throw context.runtime.newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }
    }
    
    @JRubyMethod(name = "pos=", required = 1)
    public RubyFixnum pos_set(ThreadContext context, IRubyObject newPosition) {
        long offset = RubyNumeric.num2long(newPosition);

        if (offset < 0) {
            throw context.runtime.newSystemCallError("Negative seek offset");
        }
        
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            myOpenFile.getMainStreamSafe().lseek(offset, Stream.SEEK_SET);
        
            myOpenFile.getMainStreamSafe().clearerr();
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.runtime.newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.runtime.newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        return context.runtime.newFixnum(offset);
    }
    
    /** Print some objects to the stream.
     * 
     */
    @JRubyMethod(name = "print", rest = true, reads = FrameField.LASTLINE)
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        return context.runtime.is1_9() ?
                print19(context, this, args) :
                print(context, this, args);
    }

    /** Print some objects to the stream.
     *
     */
    public static IRubyObject print(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { context.getLastLine() };
        }

        Ruby runtime = context.runtime;
        IRubyObject fs = runtime.getGlobalVariables().get("$,");
        IRubyObject rs = runtime.getGlobalVariables().get("$\\");

        for (int i = 0; i < args.length; i++) {
            if (i > 0 && !fs.isNil()) {
                write(context, maybeIO, fs);
            }
            if (args[i].isNil()) {
                write(context, maybeIO, runtime.newString("nil"));
            } else {
                write(context, maybeIO, args[i]);
            }
        }
        if (args.length > 0 && !rs.isNil()) {
            write(context, maybeIO, rs);
        }

        return context.nil;
    }

    /** Print some objects to the stream.
     *
     */
    public static IRubyObject print19(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { context.getLastLine() };
        }

        Ruby runtime = context.runtime;
        IRubyObject fs = runtime.getGlobalVariables().get("$,");
        IRubyObject rs = runtime.getGlobalVariables().get("$\\");

        for (int i = 0; i < args.length; i++) {
            if (!fs.isNil() && i > 0) {
                write(context, maybeIO, fs);
            }
            write(context, maybeIO, args[i]);
        }
        if (args.length > 0 && !rs.isNil()) {
            write(context, maybeIO, rs);
        }

        return context.nil;
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        callMethod(context, "write", RubyKernel.sprintf(context, this, args));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "putc", required = 1)
    public IRubyObject putc(ThreadContext context, IRubyObject object) {
        return putc(context, this, object);
    }

    public static IRubyObject putc(ThreadContext context, IRubyObject maybeIO, IRubyObject object) {
        int c = RubyNumeric.num2chr(object);
        if (maybeIO instanceof RubyIO) {
            // FIXME we should probably still be dyncalling 'write' here
            RubyIO io = (RubyIO)maybeIO;
            try {
                OpenFile myOpenFile = io.getOpenFileChecked();
                myOpenFile.checkWritable(context.runtime);
                Stream writeStream = myOpenFile.getWriteStream();
                writeStream.fputc(c);
                if (myOpenFile.isSync()) myOpenFile.fflush(writeStream);
            } catch (IOException ex) {
                throw context.runtime.newIOErrorFromException(ex);
            } catch (BadDescriptorException e) {
                throw context.runtime.newErrnoEBADFError();
            } catch (InvalidValueException ex) {
                throw context.runtime.newErrnoEINVALError();
            }
        } else {
            maybeIO.callMethod(context, "write",
                    RubyString.newStringNoCopy(context.runtime, new byte[] {(byte)c}));
        }

        return object;
    }

    public RubyFixnum seek(ThreadContext context, IRubyObject[] args) {
        long offset = RubyNumeric.num2long(args[0]);
        int whence = Stream.SEEK_SET;
        
        if (args.length > 1) {
            whence = RubyNumeric.fix2int(args[1].convertToInteger());
        }
        
        return doSeek(context, offset, whence);
    }

    @JRubyMethod(name = "seek")
    public RubyFixnum seek(ThreadContext context, IRubyObject arg0) {
        long offset = RubyNumeric.num2long(arg0);
        int whence = Stream.SEEK_SET;
        
        return doSeek(context, offset, whence);
    }

    @JRubyMethod(name = "seek")
    public RubyFixnum seek(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        long offset = RubyNumeric.num2long(arg0);
        int whence = RubyNumeric.fix2int(arg1.convertToInteger());
        
        return doSeek(context, offset, whence);
    }
    
    private RubyFixnum doSeek(ThreadContext context, long offset, int whence) {
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            myOpenFile.seek(offset, whence);
        
            myOpenFile.getMainStreamSafe().clearerr();
        } catch (BadDescriptorException ex) {
            throw context.runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.runtime.newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.runtime.newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        return RubyFixnum.zero(context.runtime);
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    @JRubyMethod(name = "sysseek", required = 1, optional = 1)
    public RubyFixnum sysseek(ThreadContext context, IRubyObject[] args) {
        long offset = RubyNumeric.num2long(args[0]);
        long pos;
        int whence = Stream.SEEK_SET;
        
        if (args.length > 1) {
            whence = RubyNumeric.fix2int(args[1].convertToInteger());
        }
        
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            
            if (myOpenFile.isReadable() && myOpenFile.isReadBuffered()) {
                throw context.runtime.newIOError("sysseek for buffered IO");
            }
            if (myOpenFile.isWritable() && myOpenFile.isWriteBuffered()) {
                context.runtime.getWarnings().warn(ID.SYSSEEK_BUFFERED_IO, "sysseek for buffered IO");
            }
            
            pos = myOpenFile.getMainStreamSafe().getDescriptor().lseek(offset, whence);
        
            myOpenFile.getMainStreamSafe().clearerr();
        } catch (BadDescriptorException ex) {
            throw context.runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.runtime.newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.runtime.newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        return context.runtime.newFixnum(pos);
    }

    @JRubyMethod(name = "rewind")
    public RubyFixnum rewind(ThreadContext context) {
        OpenFile myOpenfile = getOpenFileChecked();
        
        try {
            myOpenfile.getMainStreamSafe().lseek(0L, Stream.SEEK_SET);
            myOpenfile.getMainStreamSafe().clearerr();
            
            // TODO: This is some goofy global file value from MRI..what to do?
//            if (io == current_file) {
//                gets_lineno -= fptr->lineno;
//            }
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.runtime.newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.runtime.newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        // Must be back on first line on rewind.
        myOpenfile.setLineNumber(0);

        return RubyFixnum.zero(context.runtime);
    }
    
    @JRubyMethod(name = "fsync")
    public RubyFixnum fsync(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkWritable(runtime);
        
            Stream writeStream = myOpenFile.getWriteStream();

            writeStream.fflush();
            writeStream.sync();

        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }

        return RubyFixnum.zero(runtime);
    }

    /** Sets the current sync mode.
     * 
     * @param newSync The new sync mode.
     */
    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject sync_set(IRubyObject newSync) {
        try {
            getOpenFileChecked().setSync(newSync.isTrue());
            getOpenFileChecked().getMainStreamSafe().setSync(newSync.isTrue());
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }

        return this;
    }

    @JRubyMethod(name = {"eof?", "eof"})
    public RubyBoolean eof_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStreamSafe().feof()) {
                return runtime.getTrue();
            }
            
            if (myOpenFile.getMainStreamSafe().readDataBuffered()) {
                return runtime.getFalse();
            }
            
            readCheck(myOpenFile.getMainStreamSafe());
            waitReadable(myOpenFile.getMainStreamSafe());
            
            myOpenFile.getMainStreamSafe().clearerr();
            
            int c = myOpenFile.getMainStreamSafe().fgetc();
            
            if (c != -1) {
                myOpenFile.getMainStreamSafe().ungetc(c);
                return runtime.getFalse();
            }
            
            myOpenFile.checkClosed(runtime);
            
            myOpenFile.getMainStreamSafe().clearerr();
            
            return runtime.getTrue();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public RubyBoolean tty_p(ThreadContext context) {
        try {
            return context.runtime.newBoolean(
                    context.runtime.getPosix().isatty(
                            getOpenFileChecked().getMainStreamSafe().getDescriptor().getFileDescriptor()));
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1)
    @Override
    public IRubyObject initialize_copy(IRubyObject original){
        Ruby runtime = getRuntime();
        
        if (this == original) return this;

        RubyIO originalIO = (RubyIO) TypeConverter.convertToTypeWithCheck(original, runtime.getIO(), "to_io");
        
        OpenFile originalFile = originalIO.getOpenFileChecked();
        
        MakeOpenFile();
        OpenFile newFile = openFile;
        
        try {
            if (originalFile.getPipeStream() != null) {
                originalFile.getPipeStream().fflush();
                originalFile.getMainStreamSafe().lseek(0, Stream.SEEK_CUR);
            } else if (originalFile.isWritable()) {
                originalFile.getMainStreamSafe().fflush();
            } else {
                originalFile.getMainStreamSafe().lseek(0, Stream.SEEK_CUR);
            }

            newFile.setMode(originalFile.getMode());
            newFile.setProcess(originalFile.getProcess());
            newFile.setLineNumber(originalFile.getLineNumber());
            newFile.setPath(originalFile.getPath());
            newFile.setFinalizer(originalFile.getFinalizer());
            
            IOOptions modes;
            if (newFile.isReadable()) {
                if (newFile.isWritable()) {
                    if (newFile.getPipeStream() != null) {
                        modes = newIOOptions(runtime, ModeFlags.RDONLY);
                    } else {
                        modes = newIOOptions(runtime, ModeFlags.RDWR);
                    }
                } else {
                    modes = newIOOptions(runtime, ModeFlags.RDONLY);
                }
            } else {
                if (newFile.isWritable()) {
                    modes = newIOOptions(runtime, ModeFlags.WRONLY);
                } else {
                    modes = newIOOptions(runtime, originalFile.getMainStreamSafe().getModes());
                }
            }
            
            ChannelDescriptor descriptor = originalFile.getMainStreamSafe().getDescriptor().dup();

            newFile.setMainStream(ChannelStream.fdopen(runtime, descriptor, modes.getModeFlags()));

            newFile.getMainStream().setSync(originalFile.getMainStreamSafe().isSync());
            if (originalFile.getMainStreamSafe().isBinmode()) newFile.getMainStream().setBinmode();
            
            // TODO: the rest of this...seeking to same position is unnecessary since we share a channel
            // but some of this may be needed?
            
//    fseeko(fptr->f, ftello(orig->f), SEEK_SET);
//    if (orig->f2) {
//	if (fileno(orig->f) != fileno(orig->f2)) {
//	    fd = ruby_dup(fileno(orig->f2));
//	}
//	fptr->f2 = rb_fdopen(fd, "w");
//	fseeko(fptr->f2, ftello(orig->f2), SEEK_SET);
//    }
//    if (fptr->mode & FMODE_BINMODE) {
//	rb_io_binmode(dest);
//    }
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newIOError("could not init copy: " + ex);
        } catch (PipeException ex) {
            throw runtime.newIOError("could not init copy: " + ex);
        } catch (InvalidValueException ex) {
            throw runtime.newIOError("could not init copy: " + ex);
        }
        
        return this;
    }
    
    @JRubyMethod(name = "closed?")
    public RubyBoolean closed_p(ThreadContext context) {
        return context.runtime.newBoolean(isClosed());
    }

    /**
     * Is this IO closed
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        return (openFile.getMainStream() == null && openFile.getPipeStream() == null);
    }

    /** 
     * <p>Closes all open resources for the IO.  It also removes
     * it from our magical all open file descriptor pool.</p>
     * 
     * @return The IO.
     */
    @JRubyMethod(name = "close")
    public IRubyObject close() {
        Ruby runtime = getRuntime();
        
        openFile.checkClosed(runtime);
        return ioClose(runtime);
    }
    
    // rb_io_close  
    protected IRubyObject ioClose(Ruby runtime) {
        if (openFile == null) return runtime.getNil();
        
        interruptBlockingThreads();

        /* FIXME: Why did we go to this trouble and not use these descriptors?
        ChannelDescriptor main, pipe;
        if (openFile.getPipeStream() != null) {
            pipe = openFile.getPipeStream().getDescriptor();
        } else {
            if (openFile.getMainStream() == null) {
                return runtime.getNil();
            }
            pipe = null;
        }
        
        main = openFile.getMainStream().getDescriptor(); */
        
        // cleanup, raising errors if any
        openFile.cleanup(runtime, true);
        
        // TODO: notify threads waiting on descriptors/IO? probably not...

        // If this is not a popen3/popen4 stream and it has a process, attempt to shut down that process
        if (!popenSpecial && openFile.getProcess() != null) {
            obliterateProcess(openFile.getProcess());
            IRubyObject processResult = RubyProcess.RubyStatus.newProcessStatus(runtime, openFile.getProcess().exitValue(), openFile.getPid());
            runtime.getCurrentContext().setLastExitStatus(processResult);
        }
        
        return runtime.getNil();
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject close_write(ThreadContext context) {
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            if (myOpenFile.getPipeStream() == null && myOpenFile.isReadable()) {
                throw context.runtime.newIOError("closing non-duplex IO for writing");
            }
            
            if (myOpenFile.getPipeStream() == null) {
                close();
            } else{
                myOpenFile.getPipeStream().fclose();
                myOpenFile.setPipeStream(null);
                myOpenFile.setMode(myOpenFile.getMode() & ~OpenFile.WRITABLE);
                // TODO
                // n is result of fclose; but perhaps having a SysError below is enough?
                // if (n != 0) rb_sys_fail(fptr->path);
            }
        } catch (BadDescriptorException bde) {
            throw context.runtime.newErrnoEBADFError();
        } catch (IOException ioe) {
            // hmmmm
        }
        return this;
    }

    @JRubyMethod(name = "close_read")
    public IRubyObject close_read(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            if (myOpenFile.getPipeStream() == null && myOpenFile.isWritable()) {
                throw runtime.newIOError("closing non-duplex IO for reading");
            }
            
            if (myOpenFile.getPipeStream() == null) {
                close();
            } else{
                myOpenFile.getMainStreamSafe().fclose();
                myOpenFile.setMode(myOpenFile.getMode() & ~OpenFile.READABLE);
                myOpenFile.setMainStream(myOpenFile.getPipeStream());
                myOpenFile.setPipeStream(null);
                // TODO
                // n is result of fclose; but perhaps having a SysError below is enough?
                // if (n != 0) rb_sys_fail(fptr->path);
            }
        } catch (BadDescriptorException bde) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException ioe) {
            // I believe Ruby bails out with a "bug" if closing fails
            throw runtime.newIOErrorFromException(ioe);
        }
        return this;
    }

    /** Flushes the IO output stream.
     * 
     * @return The IO.
     */
    @JRubyMethod(name = "flush")
    public RubyIO flush() {
        try { 
            getOpenFileChecked().getWriteStream().fflush();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        }

        return this;
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject gets(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject result = getline(context, separator(runtime, runtime.getRecordSeparatorVar().get()));

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject gets(ThreadContext context, IRubyObject separatorArg) {
        Ruby runtime = context.runtime;
        IRubyObject result = getline(context, separator(runtime, separatorArg));

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject result = getline(context, separator(runtime));

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        ByteList separator;
        long limit = -1;
        IRubyObject test = TypeConverter.checkIntegerType(runtime, arg, "to_int");
        if (test instanceof RubyInteger) {
            limit = RubyInteger.fix2long(test);
            separator = separator(runtime);
        } else {
            separator = separator(runtime, arg);
        }

        IRubyObject result = getline(context, separator, limit);

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context, IRubyObject separator, IRubyObject limit_arg) {
        Ruby runtime = context.runtime;
        long limit = limit_arg.isNil() ? -1 : RubyNumeric.fix2long(TypeConverter.checkIntegerType(runtime, limit_arg, "to_int"));
        IRubyObject result = getline(context, separator(runtime, separator), limit);

        if (!result.isNil()) context.setLastLine(result);

        return result;
    }

    public boolean getBlocking() {
        try {
            return ((ChannelStream) openFile.getMainStreamSafe()).isBlocking();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
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
            return runtime.newFixnum(myOpenFile.getMainStream().getModes().getFcntlFileFlags());
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
        
        OpenFile myOpenFile = getOpenFileChecked();

        // Fixme: Only F_SETFL and F_GETFL is current supported
        // FIXME: Only NONBLOCK flag is supported
        // FIXME: F_SETFL and F_SETFD are treated as the same thing here.  For the case of dup(fd) we
        //   should actually have F_SETFL only affect one (it is unclear how well we do, but this TODO
        //   is here to at least document that we might need to do more work here.  Mostly SETFL is
        //   for mode changes which should persist across fork() boundaries.  Since JVM has no fork
        //   this is not a problem for us.
        try {
            if (realCmd == FcntlLibrary.FD_CLOEXEC) {
                // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
                // And why the hell does webrick pass this in as a first argument!!!!!
            } else if (realCmd == Fcntl.F_SETFL.intValue() || realCmd == Fcntl.F_SETFD.intValue()) {
                if ((nArg & FcntlLibrary.FD_CLOEXEC) == FcntlLibrary.FD_CLOEXEC) {
                    // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
                } else {
                    boolean block = (nArg & ModeFlags.NONBLOCK) != ModeFlags.NONBLOCK;

                    myOpenFile.getMainStreamSafe().setBlocking(block);
                }
            } else if (realCmd == Fcntl.F_GETFL.intValue()) {
                return myOpenFile.getMainStreamSafe().isBlocking() ? RubyFixnum.zero(runtime) : RubyFixnum.newFixnum(runtime, ModeFlags.NONBLOCK);
            } else {
                throw runtime.newNotImplementedError("JRuby only supports F_SETFL and F_GETFL with NONBLOCK for fcntl/ioctl");
            }
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
        
        return runtime.newFixnum(0);
    }
    
    private static final ByteList NIL_BYTELIST = ByteList.create("nil");
    private static final ByteList RECURSIVE_BYTELIST = ByteList.create("[...]");
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

    protected IRubyObject write(ThreadContext context, ByteList byteList) {
        return callMethod(context, "write", RubyString.newStringShared(context.runtime, byteList));
    }

    protected static IRubyObject write(ThreadContext context, IRubyObject maybeIO, ByteList byteList) {
        return maybeIO.callMethod(context, "write", RubyString.newStringShared(context.runtime, byteList));
    }

    public static IRubyObject write(ThreadContext context, IRubyObject maybeIO, IRubyObject str) {
        return maybeIO.callMethod(context, "write", str);
    }

    private static IRubyObject inspectPuts(ThreadContext context, IRubyObject maybeIO, RubyArray array) {
        try {
            context.runtime.registerInspecting(array);
            return putsArray(context, maybeIO, array.toJavaArray());
        } finally {
            context.runtime.unregisterInspecting(array);
        }
    }
    
    @Override
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        
        if (!runtime.is1_9()) return super.inspect();
        if (openFile == null) return super.inspect();
        
        Stream stream = openFile.getMainStream();
        String className = getMetaClass().getRealClass().getName();
        String path = openFile.getPath();
        String status = "";
        
        if (path == null) {
            if (stream == null) {
                path = "";
                status = "(closed)";
            } else {
                path = "fd " + runtime.getFileno(stream.getDescriptor());
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
    @JRubyMethod(name = {"getc", "getbyte"}, compat = RUBY1_8)
    public IRubyObject getc() {
        int c = getcCommon();

        if (c == -1) {
            // CRuby checks ferror(f) and retry getc for non-blocking IO
            // read. We checks readability first if possible so retry should
            // not be needed I believe.
            return getRuntime().getNil();
        }

        return getRuntime().newFixnum(c);
    }
    
    @JRubyMethod(name = "readchar", compat = RUBY1_9)
    public IRubyObject readchar19(ThreadContext context) {
        IRubyObject value = getc19(context);
        
        if (value.isNil()) {
            throw context.runtime.newEOFError();
        }
        
        return value;
    }

    @JRubyMethod(name = "getbyte", compat = RUBY1_9)
    public IRubyObject getbyte19(ThreadContext context) {
        return getc(); // Yes 1.8 getc is 1.9 getbyte
    }
    
    @JRubyMethod
    public IRubyObject readbyte(ThreadContext context) {
        int c = getcCommon();
        if (c == -1) {
            throw getRuntime().newEOFError();
        }
        
        return context.runtime.newFixnum(c);
    }
    
    // io_getc, transcoded portion
    private IRubyObject getcTranscoded(ThreadContext context, Stream stream) throws IOException, 
            BadDescriptorException, InvalidValueException {
        SET_BINARY_MODE();
        makeReadConversion(context);
        
        Encoding read = getInputEncoding(); // MRI has readencoding
        int cr = 0;
        IRubyObject str;
        ByteList bytes = new ByteList();
        int firstByte;
        int r = 0;
        boolean done = false;
        
        // keep going
        while (!done) {
            firstByte = stream.fgetc();
            
            if (firstByte == -1) {
                // can't read anymore
                break;
            }
            
            bytes.append((byte)firstByte);

            r = StringSupport.preciseLength(read, bytes.getUnsafeBytes(), 0, bytes.getRealSize());
            if (!StringSupport.MBCLEN_NEEDMORE_P(r)) {
                // logic from fill_buf, which transcodes while buffering from IO
                bytes = readconv.econvStrConvert(context, bytes, false);
            
                if (bytes.length() == 0) continue;
                
                break;
            }
            // Missing: logic for too-long character
        }
        
        // done with reading, check what we have
        if (StringSupport.MBCLEN_INVALID_P(r)) {
            r = StringSupport.length(read, bytes.getUnsafeBytes(), 0, bytes.getRealSize());
            // put back bytes we don't want to keep
            for (int i = bytes.getRealSize(); i > r; i--) {
                stream.ungetc(bytes.get(i));
            }
            bytes.setRealSize(r);
            cr = StringSupport.CR_BROKEN;
        } else {
            cr = StringSupport.CR_VALID;
            if (StringSupport.MBCLEN_CHARFOUND_LEN(r) == 1 && read.isAsciiCompatible() && Encoding.isAscii(bytes.get(0))) {
                cr = StringSupport.CR_7BIT;
            }
        }
        
        str = context.runtime.newString(bytes);
        str = ioEncStr(str);
        ((RubyString)str).setCodeRange(cr);
        
        return str;
    }
    
    // io_enc_str
    private IRubyObject ioEncStr(IRubyObject str) {
        str.setTaint(true);
        if (getRuntime().is1_9()) ((EncodingCapable)str).setEncoding(getReadEncoding());
        return str;
    }
    
    // get a char directly without needing to transcode
    // io_getc, untranscoded logic
    private IRubyObject getcDirect(ThreadContext context, Stream stream, Encoding enc) throws InvalidValueException, 
            BadDescriptorException, IOException {
        ByteList bytes = null;
        boolean shared = false;
        int cr = 0;
        
        int firstByte = stream.fgetc();

        if (firstByte == -1) {
            // CRuby checks ferror(f) and retry getc for non-blocking IO
            // read. We checks readability first if possible so retry should
            // not be needed I believe.
            return context.runtime.getNil();
        }

        if (enc.isAsciiCompatible() && Encoding.isAscii((byte) firstByte)) {
            if (enc == ASCIIEncoding.INSTANCE) {
                bytes = RubyInteger.SINGLE_CHAR_BYTELISTS[(int) firstByte];
                shared = true;
            } else {
                bytes = new ByteList(new byte[]{(byte) firstByte}, enc, false);
                shared = false;
                cr = StringSupport.CR_7BIT;
            }
        } else {
            // potential MBC
            int len = enc.length((byte) firstByte);
            byte[] byteAry = new byte[len];

            byteAry[0] = (byte) firstByte;
            for (int i = 1; i < len; i++) {
                int c = (byte) stream.fgetc();
                if (c == -1) {
                    bytes = new ByteList(byteAry, 0, i - 1, enc, false);
                    cr = StringSupport.CR_BROKEN;
                }
                byteAry[i] = (byte) c;
            }

            if (bytes == null) {
                cr = StringSupport.CR_VALID;
                bytes = new ByteList(byteAry, enc, false);
            }
        }

        if (shared) return RubyString.newStringShared(context.runtime, bytes, cr);

        return RubyString.newStringNoCopy(context.runtime, bytes, enc, cr);
    }    
    
    @JRubyMethod(name = "getc", compat = RUBY1_9)
    public IRubyObject getc19(ThreadContext context) {
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(context.runtime);
            myOpenFile.setReadBuffered();

            Stream stream = myOpenFile.getMainStreamSafe();
            
            readCheck(stream);
            waitReadable(stream);
            stream.clearerr();
            
            return ioGetc(context, stream);
        } catch (InvalidValueException ex) {
            throw context.runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        } catch (EOFException e) {
            throw context.runtime.newEOFError();
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }
    }
    
    // io_getc
    private IRubyObject ioGetc(ThreadContext context, Stream stream) throws IOException, BadDescriptorException, InvalidValueException {
        if (needsReadConversion()) {
            return getcTranscoded(context, stream);
        }
        
        return getcDirect(context, stream, getInputEncoding());
    }
    
    private void SET_BINARY_MODE() {
        openFile.getMainStream().setBinmode();
    }

    public int getcCommon() {
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(getRuntime());
            myOpenFile.setReadBuffered();

            Stream stream = myOpenFile.getMainStreamSafe();
            
            readCheck(stream);
            waitReadable(stream);
            stream.clearerr();
            
            return myOpenFile.getMainStreamSafe().fgetc();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        }
    }
    
    // MRI: NEED_NEWLINE_DECORATOR_ON_READ_CHECK
    private void readCheck(Stream stream) {
        if (!stream.readDataBuffered()) {
            openFile.checkClosed(getRuntime());
        }
    }
    
    /** 
     * <p>Pushes char represented by int back onto IOS.</p>
     * 
     * @param number to push back
     */
    @JRubyMethod(name = "ungetc", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject ungetc(IRubyObject number) {
        OpenFile myOpenFile = getOpenFileChecked();
        
        if (!myOpenFile.isReadBuffered()) {
            throw getRuntime().newIOError("unread stream");
        }

        ungetcCommon((int)number.convertToInteger().getLongValue());

        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"ungetc", "ungetbyte"}, required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject ungetc19(IRubyObject character) {
        Ruby runtime = getRuntime();

        if(character.isNil()) {
            return runtime.getNil();
        }

        if (character instanceof RubyFixnum) {
            int c = (int)character.convertToInteger().getLongValue();
            ungetcCommon(c);
        } else if (character instanceof RubyString || character.respondsTo("to_str")) {
            RubyString str = (RubyString) character.callMethod(runtime.getCurrentContext(), "to_str");
            if (str.isEmpty()) return runtime.getNil();

            byte[] bytes = str.getBytes();
            for(int i = bytes.length - 1; i >= 0; i-- ) {
                int c =  bytes[i];
                ungetcCommon(c);
            }

        } else {
            throw runtime.newTypeError(character, runtime.getFixnum());
        }

        return runtime.getNil();
    }

    public void ungetcCommon(int ch) {
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            myOpenFile.checkReadable(getRuntime());
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStreamSafe().ungetc(ch) == -1 && ch != -1) {
                throw getRuntime().newIOError("ungetc failed");
            }
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = "read_nonblock", required = 1, optional = 1)
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        return doReadNonblock(context, args, true);
    }
    
    public IRubyObject doReadNonblock(ThreadContext context, IRubyObject[] args, boolean useException) {
        IRubyObject value = getPartial(context, args, true);

        if (value.isNil()) {
            throw context.runtime.newEOFError();
        }

        if (value instanceof RubyString) {
            RubyString str = (RubyString) value;
            if (str.isEmpty()) {
                Ruby ruby = context.runtime;

                if (useException) {
                    if (ruby.is1_9()) {
                        throw ruby.newErrnoEAGAINReadableError("");
                    } else {
                        throw ruby.newErrnoEAGAINError("");
                    }
                } else {
                    return ruby.fastNewSymbol("wait_readable");
                }
            }
        }

        return value;
    }

    @JRubyMethod(name = "readpartial", required = 1, optional = 1)
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        IRubyObject value = getPartial(context, args, false);

        if (value.isNil()) {
            throw context.runtime.newEOFError();
        }

        return value;
    }

    // implements io_getpartial in io.c
    private IRubyObject getPartial(ThreadContext context, IRubyObject[] args, boolean isNonblocking) {
        Ruby runtime = context.runtime;

        // Length to read
        int length = RubyNumeric.fix2int(args[0]);
        if (length < 0) throw runtime.newArgumentError("negative length " + length + " given");

        // String/Buffer to read it into
        IRubyObject stringArg = args.length > 1 ? args[1] : runtime.getNil();
        RubyString string = stringArg.isNil() ? RubyString.newEmptyString(runtime) : stringArg.convertToString();
        string.empty();
        string.setTaint(true);
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            myOpenFile.checkReadable(runtime);
            
            if (length == 0) {
                return string;
            }

            if (!(myOpenFile.getMainStreamSafe() instanceof ChannelStream)) { // cryptic for the uninitiated...
                throw runtime.newNotImplementedError("readpartial only works with Nio based handlers");
            }
            ChannelStream stream = (ChannelStream) myOpenFile.getMainStreamSafe();

            // We don't check RubyString modification since JRuby doesn't have
            // GIL. Other threads are free to change anytime.

            ByteList buf = null;
            if (isNonblocking) {
                buf = stream.readnonblock(length);
            } else {
                while ((buf == null || buf.length() == 0) && !stream.feof()) {
                    waitReadable(stream);
                    buf = stream.readpartial(length);
                }
            }
            boolean empty = buf == null || buf.length() == 0;
            ByteList newBuf = empty ? ByteList.EMPTY_BYTELIST.dup() : buf;
            
            string.view(newBuf);

            if (stream.feof() && empty) return runtime.getNil();

            return string;
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (EOFException e) {
            throw runtime.newEOFError(e.getMessage());
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = "sysread", required = 1, optional = 1)
    public IRubyObject sysread(ThreadContext context, IRubyObject[] args) {
        int len = (int)RubyNumeric.num2long(args[0]);
        if (len < 0) throw getRuntime().newArgumentError("Negative size");

        try {
            RubyString str;
            ByteList buffer;
            if (args.length == 1 || args[1].isNil()) {
                if (len == 0) {
                    return RubyString.newEmptyString(getRuntime());
                }
                
                buffer = new ByteList(len);
                str = RubyString.newString(getRuntime(), buffer);
            } else {
                str = args[1].convertToString();
                str.modify(len);
                
                if (len == 0) {
                    return str;
                }
                
                buffer = str.getByteList();
                buffer.length(0);
            }
            
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkReadable(getRuntime());
            
            if (myOpenFile.getMainStreamSafe().readDataBuffered()) {
                throw getRuntime().newIOError("sysread for buffered IO");
            }
            
            // TODO: Ruby locks the string here
            
            waitReadable(myOpenFile.getMainStreamSafe());
            myOpenFile.checkClosed(getRuntime());

            // We don't check RubyString modification since JRuby doesn't have
            // GIL. Other threads are free to change anytime.

            int bytesRead = myOpenFile.getMainStreamSafe().getDescriptor().read(len, str.getByteList());
            
            // TODO: Ruby unlocks the string here

            if (bytesRead == -1 || (bytesRead == 0 && len > 0)) {
                throw getRuntime().newEOFError();
            }
            
            str.setTaint(true);
            
            return str;
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
    	} catch (IOException e) {
            synthesizeSystemCallError(e);
            return null;
        }
    }

    /**
     * Java does not give us enough information for specific error conditions
     * so we are reduced to divining them through string matches...
     */
    // TODO: Should ECONNABORTED get thrown earlier in the descriptor itself or is it ok to handle this late?
    // TODO: Should we include this into Errno code somewhere do we can use this from other places as well?
    private void synthesizeSystemCallError(IOException e) {
        String errorMessage = e.getMessage();
        // All errors to sysread should be SystemCallErrors, but on a closed stream
        // Ruby returns an IOError.  Java throws same exception for all errors so
        // we resort to this hack...
        if ("File not open".equals(errorMessage)) {
            throw getRuntime().newIOError(e.getMessage());
        } else if ("An established connection was aborted by the software in your host machine".equals(errorMessage)) {
            throw getRuntime().newErrnoECONNABORTEDError();
        } else if ("Connection reset by peer".equals(e.getMessage())
                || "An existing connection was forcibly closed by the remote host".equals(e.getMessage())) {
            throw getRuntime().newErrnoECONNRESETError();
        }

        throw getRuntime().newSystemCallError(e.getMessage());
    }
    
    public IRubyObject read(IRubyObject[] args) {
        ThreadContext context = getRuntime().getCurrentContext();
        
        switch (args.length) {
        case 0: return read(context);
        case 1: return read(context, args[0]);
        case 2: return read(context, args[0], args[1]);
        default: throw getRuntime().newArgumentError(args.length, 2);
        }
    }
    
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();
            return readAll(context);
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (EOFException ex) {
            throw getRuntime().newEOFError();
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        }
    }
    
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject arg0) {
        if (arg0.isNil()) {
            return read(context);
        }
        
        OpenFile myOpenFile = getOpenFileChecked();
        
        int length = RubyNumeric.num2int(arg0);
        
        if (length < 0) {
            throw getRuntime().newArgumentError("negative length " + length + " given");
        }
        
        RubyString str = RubyString.newEmptyString(getRuntime());

        return readNotAll(context, myOpenFile, length, str);
    }
    
    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        OpenFile myOpenFile = getOpenFileChecked();
        
        if (arg0.isNil()) {
            try {
                myOpenFile.checkReadable(getRuntime());
                myOpenFile.setReadBuffered();
                if (arg1.isNil()) {
                    return readAll(context);
                } else {
                    return readAll(arg1.convertToString());
                }
            } catch (InvalidValueException ex) {
                throw getRuntime().newErrnoEINVALError();
            } catch (EOFException ex) {
                throw getRuntime().newEOFError();
            } catch (IOException ex) {
                throw getRuntime().newIOErrorFromException(ex);
            } catch (BadDescriptorException ex) {
                throw getRuntime().newErrnoEBADFError();
            }
        }
        
        int length = RubyNumeric.num2int(arg0);
        
        if (length < 0) {
            throw getRuntime().newArgumentError("negative length " + length + " given");
        }

        if (arg1.isNil()) {
            return readNotAll(context, myOpenFile, length);
        } else {
            // this readNotAll empties the string for us
            return readNotAll(context, myOpenFile, length, arg1.convertToString());
        }
    }
    
    // implements latter part of io_read in io.c
    private IRubyObject readNotAll(ThreadContext context, OpenFile myOpenFile, int length, RubyString str) {
        Ruby runtime = context.runtime;
        str.empty();

        try {
            ByteList newBuffer = readNotAllCommon(context, myOpenFile, length);

            if (emptyBufferOrEOF(newBuffer, myOpenFile)) {
                return runtime.getNil();
            }

            str.setValue(newBuffer);
            str.setTaint(true);

            return str;
        } catch (EOFException ex) {
            throw runtime.newEOFError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        }
    }

    // implements latter part of io_read in io.c
    private IRubyObject readNotAll(ThreadContext context, OpenFile myOpenFile, int length) {
        Ruby runtime = context.runtime;

        try {
            ByteList newBuffer = readNotAllCommon(context, myOpenFile, length);

            if (emptyBufferOrEOF(newBuffer, myOpenFile)) {
                return runtime.getNil();
            }

            RubyString str = RubyString.newString(runtime, newBuffer);
            str.setTaint(true);

            return str;
        } catch (EOFException ex) {
            throw runtime.newEOFError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        }
    }

    private ByteList readNotAllCommon(ThreadContext context, OpenFile myOpenFile, int length) {
        Ruby runtime = context.runtime;

        try {
            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStreamSafe().feof()) {
                return null;
            }

            // READ_CHECK from MRI io.c
            readCheck(myOpenFile.getMainStreamSafe());

            ByteList newBuffer = fread(context.getThread(), length);

            return newBuffer;
        } catch (EOFException ex) {
            throw runtime.newEOFError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        }
    }

    protected static boolean emptyBufferOrEOF(ByteList buffer, OpenFile myOpenFile) throws BadDescriptorException, IOException {
        if (buffer == null) {
            return true;
        } else if (buffer.length() == 0) {
            if (myOpenFile.getMainStreamSafe() == null) {
                return true;
            }

            if (myOpenFile.getMainStreamSafe().feof()) {
                return true;
            }
        }
        return false;
    }
    
    // implements read_all() in io.c
    protected RubyString readAll(RubyString str) throws BadDescriptorException, EOFException, IOException {
        Ruby runtime = getRuntime();

        // TODO: handle writing into original buffer better
        ByteList buf = readAllCommon(runtime);
        
        if (buf == null) {
            str.empty();
        } else {
            str.setValue(buf);
        }
        str.setTaint(true);
        return str;
    }

    protected IRubyObject readAll(ThreadContext context) throws BadDescriptorException, EOFException, IOException {
        Ruby runtime = getRuntime();
        
        if (runtime.is1_9() && needsReadConversion()) {
            openFile.setBinmode();
            
            makeReadConversion(context);

            // TODO: handle writing into original buffer better
            ByteList buf = readAllCommon(runtime);

            if (readconv != null) buf = readconv.transcode(runtime.getCurrentContext(), buf);
            
            clearReadConversion();
            return ioEncStr(runtime.newString(buf));
        }

        // TODO: handle writing into original buffer better
        
        ByteList buf = readAllCommon(runtime);

        RubyString str;
        if (buf == null) {
            str = RubyString.newEmptyString(runtime);
        } else {
            str = makeString(runtime, buf, false);
        }
        str.setTaint(true);
        return str;
    }

    // mri: read_all
    protected ByteList readAllCommon(Ruby runtime) throws BadDescriptorException, EOFException, IOException {
        ByteList buf = null;
        ChannelDescriptor descriptor = openFile.getMainStreamSafe().getDescriptor();
        try {
            // ChannelStream#readall knows what size should be allocated at first. Just use it.
            if (descriptor.isSeekable() && descriptor.getChannel() instanceof FileChannel) {
                buf = openFile.getMainStreamSafe().readall();
            } else {
                RubyThread thread = runtime.getCurrentContext().getThread();
                try {
                    while (true) {
                        // TODO: ruby locks the string here
                        Stream stream = openFile.getMainStreamSafe();
                        readCheck(stream);
                        openFile.checkReadable(runtime);
                        ByteList read = fread(thread, ChannelStream.BUFSIZE);
                            
                        // TODO: Ruby unlocks the string here
                        if (read.length() == 0) {
                            break;
                        }
                        if (buf == null) {
                            buf = read;
                        } else {
                            buf.append(read);
                        }
                    }
                } catch (InvalidValueException ex) {
                    throw runtime.newErrnoEINVALError();
                }
            }
        } catch (NonReadableChannelException ex) {
            throw runtime.newIOError("not opened for reading");
        }

        return buf;
    }

    // implements io_fread in io.c
    private ByteList fread(RubyThread thread, int length) throws IOException, BadDescriptorException {
        Stream stream = openFile.getMainStreamSafe();
        int rest = length;
        waitReadable(stream);
        ByteList buf = blockingFRead(stream, thread, length);
        if (buf != null) {
            rest -= buf.length();
        }
        while (rest > 0) {
            waitReadable(stream);
            openFile.checkClosed(getRuntime());
            stream.clearerr();
            ByteList newBuffer = blockingFRead(stream, thread, rest);
            if (newBuffer == null) {
                // means EOF
                break;
            }
            int len = newBuffer.length();
            if (len == 0) {
                // TODO: warn?
                // rb_warning("nonblocking IO#read is obsolete; use IO#readpartial or IO#sysread")
                continue;
            }
            if (buf == null) {
                buf = newBuffer;
            } else {
                buf.append(newBuffer);
            }
            rest -= len;
        }
        if (buf == null) {
            return ByteList.EMPTY_BYTELIST.dup();
        } else {
            return buf;
        }
    }

    private ByteList blockingFRead(Stream stream, RubyThread thread, int length) throws IOException, BadDescriptorException {
        try {
            thread.beforeBlockingCall();
            return stream.fread(length);
        } finally {
            thread.afterBlockingCall();
        }
    }
    
    /** Read a byte. On EOF throw EOFError.
     * 
     */
    @JRubyMethod(name = "readchar", compat = RUBY1_8)
    public IRubyObject readchar() {
        IRubyObject c = getc();
        
        if (c.isNil()) throw getRuntime().newEOFError();
        
        return c;
    }
    
    @JRubyMethod
    public IRubyObject stat(ThreadContext context) {
        openFile.checkClosed(context.runtime);
        try {
            return context.runtime.newFileStat(getOpenFileChecked().getMainStreamSafe().getDescriptor().getFileDescriptor());
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    public IRubyObject each_byteInternal(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        
    	try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            while (true) {
                myOpenFile.checkReadable(runtime);
                myOpenFile.setReadBuffered();
                waitReadable(myOpenFile.getMainStream());
                
                int c = myOpenFile.getMainStreamSafe().fgetc();
                
                // CRuby checks ferror(f) and retry getc for
                // non-blocking IO.
                if (c == -1) {
                    break;
                }
                
                assert c < 256;
                block.yield(context, getRuntime().newFixnum(c));
            }

            return this;
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (EOFException e) {
            return runtime.getNil();
    	} catch (IOException e) {
    	    throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod
    public IRubyObject each_byte(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_byteInternal(context, block) : enumeratorize(context.runtime, this, "each_byte");
    }

    @JRubyMethod(name = "bytes")
    public IRubyObject bytes(final ThreadContext context) {
        return enumeratorize(context.runtime, this, "each_byte");
    }

    @JRubyMethod(name = "lines", compat = CompatVersion.RUBY1_8)
    public IRubyObject lines(final ThreadContext context, Block block) {
        return enumeratorize(context.runtime, this, "each_line");
    }

    @JRubyMethod(name = "lines", compat = CompatVersion.RUBY1_9)
    public IRubyObject lines19(final ThreadContext context, Block block) {
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, this, "each_line");
        }
        return each_lineInternal(context, NULL_ARRAY, block);
    }

    public IRubyObject each_charInternal(final ThreadContext context, final Block block) {
        Ruby runtime = context.runtime;
        IRubyObject ch;

        while(!(ch = getc()).isNil()) {
            byte c = (byte)RubyNumeric.fix2int(ch);
            int n = runtime.getKCode().getEncoding().length(c);
            RubyString str = runtime.newString();
            if (runtime.is1_9()) str.setEncoding(getReadEncoding());
            str.setTaint(true);
            str.cat(c);

            while(--n > 0) {
                if((ch = getc()).isNil()) {
                    block.yield(context, str);
                    return this;
                }
                c = (byte)RubyNumeric.fix2int(ch);
                str.cat(c);
            }
            block.yield(context, str);
        }
        return this;
    }

    public IRubyObject each_charInternal19(final ThreadContext context, final Block block) {
        IRubyObject ch;

        while(!(ch = getc19(context)).isNil()) {
            block.yield(context, ch);
        }
        return this;
    }

    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "each_char");
    }

    @JRubyMethod(name = "each_char", compat = RUBY1_9)
    public IRubyObject each_char19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal19(context, block) : enumeratorize(context.runtime, this, "each_char");
    }

    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject chars(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "chars");
    }

    @JRubyMethod(name = "chars", compat = RUBY1_9)
    public IRubyObject chars19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal19(context, block) : enumeratorize(context.runtime, this, "chars");
    }

    @JRubyMethod
    public IRubyObject codepoints(final ThreadContext context, final Block block) {
        return eachCodePointCommon(context, block, "codepoints");
    }

    @JRubyMethod
    public IRubyObject each_codepoint(final ThreadContext context, final Block block) {
        return eachCodePointCommon(context, block, "each_codepoint");
    }

    private IRubyObject eachCodePointCommon(final ThreadContext context, final Block block, final String methodName) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorize(runtime, this, methodName);
        IRubyObject ch;

        while(!(ch = getc()).isNil()) {
            block.yield(context, ch);
        }
        return this;
    }

    /** 
     * <p>Invoke a block for each line.</p>
     */
    public RubyIO each_lineInternal(ThreadContext context, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        ByteList separator = getSeparatorForGets(runtime, args);

        ByteListCache cache = new ByteListCache();
        for (IRubyObject line = getline(context, separator); !line.isNil(); 
		line = getline(context, separator, cache)) {
            block.yield(context, line);
        }
        
        return this;
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each(final ThreadContext context, IRubyObject[]args, final Block block) {
        return block.isGiven() ? each_lineInternal(context, args, block) : enumeratorize(context.runtime, this, "each", args);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each_line(final ThreadContext context, IRubyObject[]args, final Block block) {
        return block.isGiven() ? each_lineInternal(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
    }

    @JRubyMethod(name = "readlines", optional = 1, compat = RUBY1_8)
    public RubyArray readlines(ThreadContext context, IRubyObject[] args) {
        return readlinesCommon(context, args);
    }
    
    @JRubyMethod(name = "readlines", optional = 2, compat = RUBY1_9)
    public RubyArray readlines19(ThreadContext context, IRubyObject[] args) {
        return readlinesCommon(context, args);
    }
    
    private RubyArray readlinesCommon(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        long limit = getLimitFromArgs(args);
        ByteList separator = getSeparatorFromArgs(runtime, args, 0);
        RubyArray result = runtime.newArray();
        IRubyObject line;

        while (! (line = getline(context, separator, limit, null)).isNil()) {
            result.append(line);
        }
        return result;
    }
    
    private long getLimitFromArgs(IRubyObject[] args) {
        long limit = -1;

        if (args.length > 1) {
            limit = RubyNumeric.num2long(args[1]);
        } else if (args.length > 0 && args[0] instanceof RubyFixnum) {
            limit = RubyNumeric.num2long(args[0]);
        }

        return limit;
    }

    @JRubyMethod(name = "to_io")
    public RubyIO to_io() {
    	return this;
    }

    @Override
    public String toString() {
        try {
            return "RubyIO(" + openFile.getMode() + ", " + getRuntime().getFileno(openFile.getMainStreamSafe().getDescriptor()) + ")";
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
    }
    
    /* class methods for IO */
    
    /** rb_io_s_foreach
    *
    */
    private static IRubyObject foreachInternal(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject filename = args[0].convertToString();

        RubyIO io = newFile(context, runtime.getFile(), new IRubyObject[] { filename });

        ByteListCache cache = new ByteListCache();
        if (!io.isNil()) {
            try {
                ByteList separator = io.getSeparatorFromArgs(runtime, args, 1);
                IRubyObject str = io.getline(context, separator, cache);
                while (!str.isNil()) {
                    block.yield(context, str);
                    str = io.getline(context, separator, cache);
                }
            } finally {
                io.close();
            }
        }
       
        return runtime.getNil();
    }

    /** rb_io_s_foreach
    *
    */
    private static IRubyObject foreachInternal19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
       
        IRubyObject opt = ArgsUtil.getOptionsArg(context.runtime, args);
        IRubyObject io = openKeyArgs(context, recv, args, opt);
        if (io.isNil()) return io;
        
        IRubyObject[] methodArguments = processReadlinesMethodArguments(args);

        ByteListCache cache = new ByteListCache();
        if (!io.isNil()) {
            try {

                long limit = ((RubyIO)io).getLimitFromArgs(methodArguments);
                ByteList separator = ((RubyIO)io).getSeparatorFromArgs(runtime, methodArguments, 0);
                
                IRubyObject str = ((RubyIO)io).getline(context, separator, limit ,cache);
                while (!str.isNil()) {
                    block.yield(context, str);
                    str = ((RubyIO)io).getline(context, separator, limit ,cache);
                }
            } finally {
                ((RubyIO)io).close();
                runtime.getGlobalVariables().clear("$_");
            }
        }

        return runtime.getNil();
    }
    
    @JRubyMethod(required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject foreach(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "foreach", args);

        return foreachInternal(context, recv, args, block);
    }

    @JRubyMethod(name = "foreach", required = 1, optional = 3, meta = true, compat = RUBY1_9)
    public static IRubyObject foreach19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "foreach", args);

        return foreachInternal19(context, recv, args, block);
    }

    public static RubyIO convertToIO(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyIO) return (RubyIO)obj;
        return (RubyIO)TypeConverter.convertToType(obj, context.runtime.getIO(), "to_io");
    }
   
    @JRubyMethod(name = "select", required = 1, optional = 3, meta = true)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return select_static(context, context.runtime, args);
    }

    public static IRubyObject select_static(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        return new SelectBlob().goForIt(context, runtime, args);
    }
   
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
        case 0:
            throw context.runtime.newArgumentError(0, 1);
        case 1: return readStatic(context, recv, args[0]);
        case 2: return readStatic(context, recv, args[0], args[1]);
        case 3: return readStatic(context, recv, args[0], args[1], args[2]);
        default:
            throw context.runtime.newArgumentError(args.length, 3);
        }
   }

    private static RubyIO newFile(ThreadContext context, IRubyObject recv, IRubyObject... args) {
       return (RubyIO) RubyKernel.open(context, recv, args, Block.NULL_BLOCK);
    }

    private static RubyIO newFile19(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        return (RubyIO) RubyKernel.open19(context, recv, args, Block.NULL_BLOCK);
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

    @Deprecated
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject path, Block unusedBlock) {
        return readStatic(context, recv, path);
    }
    @Deprecated
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length) {
        return readStatic(context, recv, path, length);
    }
    @Deprecated
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length, IRubyObject offset) {
        return readStatic(context, recv, path, length, offset);
    }
   
    @JRubyMethod(name = "read", meta = true, compat = RUBY1_8)
    public static IRubyObject readStatic(ThreadContext context, IRubyObject recv, IRubyObject path) {
        StringSupport.checkStringSafety(context.runtime, path);
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.runtime;
        failIfDirectory(runtime, pathStr);
        RubyIO file = newFile(context, recv, pathStr);

       try {
           return file.read(context);
       } finally {
           file.close();
       }
    }
   
    @JRubyMethod(name = "read", meta = true, compat = RUBY1_8)
    public static IRubyObject readStatic(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length) {
        StringSupport.checkStringSafety(context.runtime, path);
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.runtime;
        failIfDirectory(runtime, pathStr);
        RubyIO file = newFile(context, recv, pathStr);
       
        try {
            return !length.isNil() ? file.read(context, length) : file.read(context);
        } finally  {
            file.close();
        }
    }

    @JRubyMethod(name = "read", meta = true, compat = RUBY1_8)
    public static IRubyObject readStatic(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length, IRubyObject offset) {
        StringSupport.checkStringSafety(context.runtime, path);
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.runtime;
        failIfDirectory(runtime, pathStr);
        RubyIO file = newFile(context, recv, pathStr);

        try {
            if (!offset.isNil()) file.seek(context, offset);
            return !length.isNil() ? file.read(context, length) : file.read(context);
        } finally  {
            file.close();
        }
    }

    /**
     *  options is a hash which can contain:
     *    encoding: string or encoding
     *    mode: string
     *    open_args: array of string
     */
    private static IRubyObject read19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length, IRubyObject offset, IRubyObject options) {
        RubyIO file = (RubyIO)openKeyArgs(context, recv, new IRubyObject[]{path, length, offset}, options);

        try {
            if (!offset.isNil()) file.seek(context, offset);
            return !length.isNil() ? file.read(context, length) : file.read(context);
        } finally  {
            file.close();
        }
    }
    
    // open_key_args
    private static IRubyObject openKeyArgs(ThreadContext context, IRubyObject recv, IRubyObject[] argv, IRubyObject opt) {
        Ruby runtime = context.runtime;
        IRubyObject path, v;
        
        path = RubyFile.get_path(context, argv[0]);
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
        int[] oflags_p = {0}, fmode_p = {0};
        int perm;
        IRubyObject cmd;
        
        IRubyObject[] pm = {vperm, vmode};
        
        RubyFile file = (RubyFile)context.runtime.getFile().allocate();
        EncodingUtils.extractModeEncoding(context, file, pm, opt, oflags_p, fmode_p);
        perm = (pm[EncodingUtils.PERM] == null || pm[EncodingUtils.PERM].isNil()) ? 0666 : RubyNumeric.num2int(pm[EncodingUtils.PERM]);
    
        if (!(cmd = checkPipeCommand(context, filename)).isNil()) {
            return (RubyIO) RubyKernel.open19(context, context.runtime.getIO(), new IRubyObject[]{filename, vmode, opt}, Block.NULL_BLOCK);
            // TODO: lots of missing logic for pipe opening
        } else {
            return file.fileOpenGeneric(context, filename, oflags_p[0], fmode_p[0], file, perm);
        }
    }
    
    public static IRubyObject checkPipeCommand(ThreadContext context, IRubyObject filenameOrCommand) {
        RubyString filenameStr = filenameOrCommand.convertToString();
        ByteList filenameByteList = filenameStr.getByteList();
        
        if (EncodingUtils.encAscget(
                filenameByteList.getUnsafeBytes(),
                filenameByteList.getBegin(),
                filenameByteList.getBegin() + filenameByteList.getRealSize(),
                null,
                filenameByteList.getEncoding()) == '|') {
            return filenameStr.makeShared19(context.runtime, 0, 1).infectBy(filenameOrCommand);
        }
        return context.nil;
    }

    /**
     *  options is a hash which can contain:
     *    encoding: string or encoding
     *    mode: string
     *    open_args: array of string
     */
    private static IRubyObject write19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject str, IRubyObject offset, RubyHash options) {
        RubyString pathStr = RubyFile.get_path(context, path);
        Ruby runtime = context.runtime;
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
    @JRubyMethod(meta = true, required = 1, optional = 2, compat = RUBY1_9)
    public static IRubyObject binread(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject nil = context.runtime.getNil();
        IRubyObject path = RubyFile.get_path(context, args[0]);
        IRubyObject length = nil;
        IRubyObject offset = nil;
        Ruby runtime = context.runtime;

        if (args.length > 2) {
            offset = args[2];
            length = args[1];
        } else if (args.length > 1) {
            length = args[1];
        }
        RubyIO file = (RubyIO) Helpers.invoke(context, runtime.getFile(), "new", path, runtime.newString("rb:ASCII-8BIT"));

        try {
            if (!offset.isNil()) file.seek(context, offset);
            return !length.isNil() ? file.read(context, length) : file.read(context);
        } finally  {
            file.close();
        }
    }

    // Enebo: annotation processing forced me to do pangea method here...
    @JRubyMethod(name = "read", meta = true, required = 1, optional = 3, compat = RUBY1_9)
    public static IRubyObject read19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = context.runtime;
        IRubyObject nil = runtime.getNil();
        IRubyObject path = args[0];
        IRubyObject length = nil;
        IRubyObject offset = nil;
        IRubyObject options = nil;
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

        return read19(context, recv, path, length, offset, options);
    }

    @JRubyMethod(meta = true, required = 2, optional = 2, compat = RUBY1_9)
    public static IRubyObject binwrite(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject path = args[0];
        IRubyObject str = args[1];
        RubyInteger offset = null;
        RubyHash options = null;
        Ruby runtime = context.runtime;

        if (args.length > 2) {
            if (args[2] instanceof RubyHash) {
                options = args[2].convertToHash();
            } else {
                offset = args[2].convertToInteger();
            }
        }

        if (args.length > 3) {
            options = args[3].convertToHash();
        }

        RubyIO file = null;

        long mode = ModeFlags.CREAT | ModeFlags.BINARY;
        if (options == null || (options != null && options.isEmpty())) {

            if (offset == null) {
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
            if (offset != null) file.seek(context, offset);
            return file.write(context, str);
        } finally  {
            file.close();
        }
    }

    @JRubyMethod(name = "write", meta = true, required = 2, optional = 2, compat = RUBY1_9)
    public static IRubyObject writeStatic(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        IRubyObject nil = context.nil;
        IRubyObject path = args[0];
        IRubyObject str = args[1];
        IRubyObject offset = nil;
        RubyHash options = null;
        if (args.length > 3) {
            if (!(args[3] instanceof RubyHash)) {
                throw context.runtime.newTypeError("Must be a hash");
            }
            options = (RubyHash) args[3];
            offset = args[2];
        } else if (args.length > 2) {
            if (args[2] instanceof RubyHash) {
                options = (RubyHash) args[2];
            } else {
                offset = args[2];
            }
        }

        return write19(context, recv, path, str, offset, (RubyHash) options);
    }
    
    @JRubyMethod(name = "readlines", required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        int count = args.length;

        IRubyObject[] fileArguments = new IRubyObject[]{ args[0].convertToString() };
        IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
        
        return readlinesCommon(context, recv, fileArguments, separatorArguments);
    }

    // rb_io_s_readlines
    @JRubyMethod(name = "readlines", required = 1, optional = 3, meta = true, compat = RUBY1_9)
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
    
    private static RubyArray readlinesCommon(ThreadContext context, IRubyObject recv, IRubyObject[] openFileArguments , IRubyObject[] methodArguments) {
        RubyIO file = (RubyIO) RubyKernel.open(context, recv, openFileArguments, Block.NULL_BLOCK);
        try {
            return (RubyArray) file.callMethod("readlines", methodArguments);
        } finally {
            file.close();
        }
    }
    
    private static RubyArray readlinesCommon19(ThreadContext context, RubyIO file, IRubyObject[] newArguments) {
        try {
            return (RubyArray) file.callMethod(context, "readlines", newArguments);
        } finally {
            file.close();
        }
    }
   
    @JRubyMethod(name = "popen", required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject popen(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        IRubyObject cmdObj;
        if (Platform.IS_WINDOWS) {
            String[] tokens = args[0].convertToString().toString().split(" ", 2);
            String commandString = tokens[0].replace('/', '\\') +
                    (tokens.length > 1 ? ' ' + tokens[1] : "");
            cmdObj = runtime.newString(commandString);
        } else {
            cmdObj = args[0].convertToString();
        }

        if ("-".equals(cmdObj.toString())) {
            throw runtime.newNotImplementedError("popen(\"-\") is unimplemented");
        }

        try {
            IOOptions ioOptions;
            if (args.length == 1) {
                ioOptions = newIOOptions(runtime, ModeFlags.RDONLY);
            } else if (args[1] instanceof RubyFixnum) {
                ioOptions = newIOOptions(runtime, RubyFixnum.num2int(args[1]));
            } else {
                ioOptions = newIOOptions(runtime, args[1].convertToString().toString());
            }

            ShellLauncher.POpenProcess process = ShellLauncher.popen(runtime, cmdObj, ioOptions.getModeFlags());

            // Yes, this is gross. java.lang.Process does not appear to be guaranteed
            // "ready" when we get it back from Runtime#exec, so we try to give it a
            // chance by waiting for 10ms before we proceed. Only doing this on 1.5
            // since Hotspot 1.6+ does not seem to exhibit the problem.
            if (System.getProperty("java.specification.version", "").equals("1.5")) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {}
            }

            RubyIO io = new RubyIO(runtime, process, ioOptions);
            if (recv instanceof RubyClass) {
                io.setMetaClass((RubyClass) recv);
            }

            if (block.isGiven()) {
                try {
                    return block.yield(context, io);
                } finally {
                    if (io.openFile.isOpen()) {
                        io.close();
                    }
                }
            }
            return io;
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    private void setupPopen(ModeFlags modes, POpenProcess process) throws RaiseException {
        openFile.setMode(modes.getOpenFileFlags() | OpenFile.SYNC);
        openFile.setProcess(process);

        try {
            if (openFile.isReadable()) {
                Channel inChannel;
                if (process.getInput() != null) {
                    // NIO-based
                    inChannel = process.getInput();
                } else {
                    // Stream-based
                    inChannel = Channels.newChannel(process.getInputStream());
                }
                
                ChannelDescriptor main = new ChannelDescriptor(
                        inChannel);
                main.setCanBeSeekable(false);
                
                openFile.setMainStream(ChannelStream.open(getRuntime(), main));
            }
            
            if (openFile.isWritable() && process.hasOutput()) {
                Channel outChannel;
                if (process.getOutput() != null) {
                    // NIO-based
                    outChannel = process.getOutput();
                } else {
                    outChannel = Channels.newChannel(process.getOutputStream());
                }

                ChannelDescriptor pipe = new ChannelDescriptor(
                        outChannel);
                pipe.setCanBeSeekable(false);
                
                if (openFile.getMainStream() != null) {
                    openFile.setPipeStream(ChannelStream.open(getRuntime(), pipe));
                } else {
                    openFile.setMainStream(ChannelStream.open(getRuntime(), pipe));
                }
            }
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
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

    @JRubyMethod(name = "popen", required = 1, optional = 2, meta = true, compat = RUBY1_9)
    public static IRubyObject popen19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

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
        
        IRubyObject[] pm = new IRubyObject[] { runtime.newFixnum(0), pmode };
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
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, process.waitFor(), ShellLauncher.getPidFromProcess(process)));
                }
            }
            return io;
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }
   
    @JRubyMethod(rest = true, meta = true, compat = RUBY1_8)
    public static IRubyObject popen3(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        try {
            POpenTuple tuple = popenSpecial(context, args);

            RubyArray yieldArgs = RubyArray.newArrayLight(runtime,
                    tuple.output,
                    tuple.input,
                    tuple.error);
            
            if (block.isGiven()) {
                try {
                    return block.yield(context, yieldArgs);
                } finally {
                    cleanupPOpen(tuple);
                    context.setLastExitStatus(
                            RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor(), ShellLauncher.getPidFromProcess(tuple.process)));
                }
            }
            return yieldArgs;
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }
   
    @JRubyMethod(name = "popen3", rest = true, meta = true, compat = RUBY1_9)
    public static IRubyObject popen3_19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;

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
                    
                    RubyProcess.RubyStatus status = RubyProcess.RubyStatus.newProcessStatus(
                            runtime,
                            exitValue,
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

    @JRubyMethod(rest = true, meta = true)
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
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor(), ShellLauncher.getPidFromProcess(tuple.process)));
                }
            }
            return yieldArgs;
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

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

    private static class POpenTuple {
        public POpenTuple(RubyIO i, RubyIO o, RubyIO e, Process p) {
            input = i; output = o; error = e; process = p;
        }
        public final RubyIO input;
        public final RubyIO output;
        public final RubyIO error;
        public final Process process;
    }

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
            input.getOpenFile().getMainStreamSafe().getDescriptor().
              setCanBeSeekable(false);
            output.getOpenFile().getMainStreamSafe().getDescriptor().
              setCanBeSeekable(false);
            error.getOpenFile().getMainStreamSafe().getDescriptor().
              setCanBeSeekable(false);

            return new POpenTuple(input, output, error, process);
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = "pipe", meta = true, compat = RUBY1_8)
    public static IRubyObject pipe(ThreadContext context, IRubyObject recv) {
        // TODO: This isn't an exact port of MRI's pipe behavior, so revisit
        Ruby runtime = context.runtime;
        try {
            Pipe pipe = Pipe.open();

            RubyIO source = new RubyIO(runtime, pipe.source());
            RubyIO sink = new RubyIO(runtime, pipe.sink());

            sink.openFile.getMainStreamSafe().setSync(true);
            return runtime.newArrayNoCopy(new IRubyObject[]{source, sink});
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "pipe", meta = true, compat = RUBY1_9)
    public static IRubyObject pipe19(ThreadContext context, IRubyObject recv) {
        return pipe19(context, recv, context.nil, context.nil);
    }

    @JRubyMethod(name = "pipe", meta = true, compat = RUBY1_9)
    public static IRubyObject pipe19(ThreadContext context, IRubyObject recv, IRubyObject modes) {
        return pipe19(context, recv, modes, context.nil);
    }

    @JRubyMethod(name = "pipe", meta = true, compat = RUBY1_9)
    public static IRubyObject pipe19(ThreadContext context, IRubyObject recv, IRubyObject modes, IRubyObject options) {
        Ruby runtime = context.runtime;
        try {
            Pipe pipe = Pipe.open();

            RubyIO source = new RubyIO(runtime, pipe.source());
            source.setEncoding(context, modes, context.nil, options);
            RubyIO sink = new RubyIO(runtime, pipe.sink());

            sink.openFile.getMainStreamSafe().setSync(true);
            return runtime.newArrayNoCopy(new IRubyObject[]{source, sink});
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }
    
    @JRubyMethod(name = "copy_stream", required = 2, optional = 2, meta = true, compat = RUBY1_9)
    public static IRubyObject copy_stream(ThreadContext context, IRubyObject recv, 
            IRubyObject[] args) {
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

        try {
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
                io2.flush();
                return size;
            }

            if (!io1.openFile.isReadable()) throw runtime.newIOError("from IO is not readable");
            if (!io2.openFile.isWritable()) throw runtime.newIOError("to IO is not writable");

            ChannelDescriptor d1 = io1.openFile.getMainStreamSafe().getDescriptor();
            ChannelDescriptor d2 = io2.openFile.getWriteStreamSafe().getDescriptor();

            try {
                long size = 0;
                if (!d1.isSeekable()) {
                    if (!d2.isSeekable()) {
                        ReadableByteChannel from = (ReadableByteChannel) d1.getChannel();
                        WritableByteChannel to = (WritableByteChannel) d2.getChannel();

                        size = transfer(context, from, to);
                    } else {
                        ReadableByteChannel from = (ReadableByteChannel) d1.getChannel();
                        FileChannel to = (FileChannel) d2.getChannel();

                        size = transfer(from, to);
                    }
                } else {
                    FileChannel from = (FileChannel) d1.getChannel();
                    WritableByteChannel to = (WritableByteChannel) d2.getChannel();
                    long remaining = length == null ? from.size() : length.getLongValue();
                    long position = offset == null? from.position() : offset.getLongValue();                    

                    size = transfer(from, to, remaining, position);
                    
                    if (offset == null) from.position(from.position() + size);
                }

                return context.runtime.newFixnum(size);
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
    }

    private static long transfer(ReadableByteChannel from, FileChannel to) throws IOException {
        long transferred = 0;
        long bytes;
        long startPosition = to.position();
        while ((bytes = to.transferFrom(from, startPosition+transferred, 4196)) > 0) {
            transferred += bytes;
        }

        return transferred;
    }

    private static long transfer(FileChannel from, WritableByteChannel to, long remaining, long position) throws IOException {
        // handle large files on 32-bit JVMs
        long chunkSize = 128 * 1024 * 1024;
        long transferred = 0;
        
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

    private static long transfer(ThreadContext context, ReadableByteChannel from, WritableByteChannel to) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024);
        long transferred = 0;

        while (from.isOpen()) {
            context.pollThreadEvents();

            long n = from.read(buffer);

            if (n == -1) break;

            buffer.flip();
            to.write(buffer);
            buffer.clear();

            transferred += n;
        }

        return transferred;
    }

    @JRubyMethod(name = "try_convert", meta = true, compat = RUBY1_9)
    public static IRubyObject tryConvert(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return arg.respondsTo("to_io") ? convertToIO(context, arg) : context.runtime.getNil();
    }

    private static ByteList getNilByteList(Ruby runtime) {
        return runtime.is1_9() ? ByteList.EMPTY_BYTELIST : NIL_BYTELIST;
    }
    
    /**
     * Add a thread to the list of blocking threads for this IO.
     * 
     * @param thread A thread blocking on this IO
     */
    public synchronized void addBlockingThread(RubyThread thread) {
        if (blockingThreads == null) {
            blockingThreads = new ArrayList<RubyThread>(1);
        }
        blockingThreads.add(thread);
    }
    
    /**
     * Remove a thread from the list of blocking threads for this IO.
     * 
     * @param thread A thread blocking on this IO
     */
    public synchronized void removeBlockingThread(RubyThread thread) {
        if (blockingThreads == null) {
            return;
        }
        for (int i = 0; i < blockingThreads.size(); i++) {
            if (blockingThreads.get(i) == thread) {
                // not using remove(Object) here to avoid the equals() call
                blockingThreads.remove(i);
            }
        }
    }
    
    /**
     * Fire an IOError in all threads blocking on this IO object
     */
    protected synchronized void interruptBlockingThreads() {
        if (blockingThreads == null) {
            return;
        }
        for (int i = 0; i < blockingThreads.size(); i++) {
            RubyThread thread = blockingThreads.get(i);
            
            // raise will also wake the thread from selection
            thread.raise(new IRubyObject[] {getRuntime().newIOError("stream closed").getException()}, Block.NULL_BLOCK);
        }
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
            ioOptions = parseIOOptions19(options.fastARef(runtime.newSymbol("mode")));
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

    // mri: io_strip_bom
    public Encoding encodingFromBOM() {
        return EncodingUtils.ioStripBOM(this);
    }

    private static final Set<String> UNSUPPORTED_SPAWN_OPTIONS = new HashSet<String>(Arrays.asList(new String[] {
            "unsetenv_others",
            "prgroup",
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
            if (opt instanceof RubySymbol || opt instanceof RubyFixnum || valid.contains(opt.toString())) {
                continue;
            }

            throw runtime.newTypeError("wrong exec option: " + opt);
        }
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

    public boolean writeDataBuffered() {
        return openFile.getMainStream().writeDataBuffered();
    }

    @Deprecated
    public void registerDescriptor(ChannelDescriptor descriptor, boolean isRetained) {
    }

    @Deprecated
    public void registerDescriptor(ChannelDescriptor descriptor) {
    }

    @Deprecated
    public void unregisterDescriptor(int aFileno) {
    }

    @Deprecated
    public ChannelDescriptor getDescriptorByFileno(int aFileno) {
        return ChannelDescriptor.getDescriptorByFileno(aFileno);
    }

    @Deprecated
    public static int getNewFileno() {
        return ChannelDescriptor.getNewFileno();
    }

    @Deprecated
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? gets(context) : gets(context, args[0]);
    }

    @Deprecated
    public IRubyObject readline(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? readline(context) : readline(context, args[0]);
    }
    
    // MRI: do_writeconv
    private IRubyObject doWriteConversion(ThreadContext context, IRubyObject str) {
        Ruby runtime = context.runtime;
        if (!needsWriteConversion(context)) return str;
        
        IRubyObject commonEncoding = context.nil;
        openFile.setBinmode(); // In MRI this does not affect flags like we do in OpenFile
        makeWriteConversion(context);
        
        if (writeconv != null) {
            if (!writeconvAsciicompat.isNil()) {
                commonEncoding = writeconvAsciicompat;
//            } else if (/*MODE_BTMODE(DEFAULT_TEXTMODE,0,1) && */ EncodingUtils.econvAsciicompatEncoding(enc) == null) {
//                throw runtime.newArgumentError("ASCII incompatible string written for text mode IO without encoding conversion:" + str.getEncoding());
            }
        } else {
            if (enc2 != null) {
                commonEncoding = runtime.getEncodingService().convertEncodingToRubyEncoding(enc2);
            } else if (enc != runtime.getEncodingService().getAscii8bitEncoding()) {
                commonEncoding = runtime.getEncodingService().convertEncodingToRubyEncoding(enc);
            }
        }
        
        if (!commonEncoding.isNil()) {
            str = EncodingUtils.rbStrEncode(context, str, commonEncoding, writeconvPreEcflags, writeconvPreEcopts);
        }
        
        if (writeconv != null) {
            str = runtime.newString(writeconv.econvStrConvert(context, ((RubyString)str).getByteList(), false));
        }
        
        // TODO: win32 logic
        
        return str;
    }
    
    // MRI: NEED_READCONV (FIXME: Windows has slightly different version)
    private boolean needsReadConversion() {
        return (enc2 != null || (ecflags & ~EncodingUtils.ECONV_CRLF_NEWLINE_DECORATOR) != 0);
    }
    
    // MRI: NEED_WRITECONV (FIXME: Windows has slightly different version)
    private boolean needsWriteConversion(ThreadContext context) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        
        return (enc != null && enc != ascii8bit) || openFile.isTextMode() || 
                (ecflags & ((EncodingUtils.ECONV_DECORATOR_MASK & ~EncodingUtils.ECONV_CRLF_NEWLINE_DECORATOR)|EncodingUtils.ECONV_STATEFUL_DECORATOR_MASK)) != 0;
    }
    
    // MRI: make_readconv
    // Missing flags and doubling readTranscoder as transcoder and whether transcoder has been initializer (ick).
    private void makeReadConversion(ThreadContext context) {
        if (readconv != null) return;
        
        int ecflags;
        IRubyObject ecopts;
        byte[] sname, dname;
        ecflags = this.ecflags & ~EncodingUtils.ECONV_NEWLINE_DECORATOR_WRITE_MASK;
        ecopts = this.ecopts;
        
        if (enc2 != null) {
            sname = enc2.getName();
            dname = enc.getName();
        } else {
            sname = dname = EMPTY_BYTE_ARRAY;
        }
        
        readconv = EncodingUtils.econvOpenOpts(context, sname, dname, ecflags, ecopts);
        
        if (readconv == null) {
            throw EncodingUtils.econvOpenExc(context, sname, dname, ecflags);
        }
        
        // rest of MRI code sets up read/write buffers
    }
    
    // MRI: make_writeconv
    private void makeWriteConversion(ThreadContext context) {
        if (writeconvInitialized) return;
        
        byte[] senc;
        byte[] denc;
        Encoding enc;
        int ecflags;
        IRubyObject ecopts;
        
        writeconvInitialized = true;
        
        ecflags = this.ecflags & ~EncodingUtils.ECONV_NEWLINE_DECORATOR_READ_MASK;
        ecopts = this.ecopts;

        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        if (this.enc == null || (this.enc == ascii8bit && enc2 == null)) {
            /* no encoding conversion */
            writeconvPreEcflags = 0;
            writeconvPreEcopts = context.nil;
            writeconv = EncodingUtils.econvOpenOpts(context, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, ecflags, ecopts);
            if (writeconv == null) {
                throw EncodingUtils.econvOpenExc(context, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, ecflags);
            }
            writeconvAsciicompat = context.nil;
        }
        else {
            enc = this.enc2 != null ? this.enc2 : this.enc;
            Encoding tmpEnc = EncodingUtils.econvAsciicompatEncoding(enc);
            senc = tmpEnc == null ? null : tmpEnc.getName();
            if (senc == null && (this.ecflags & EncodingUtils.ECONV_STATEFUL_DECORATOR_MASK) == 0) {
                /* single conversion */
                writeconvPreEcflags = ecflags;
                writeconvPreEcopts = ecopts;
                writeconv = null;
                writeconvAsciicompat = context.nil;
            }
            else {
                /* double conversion */
                writeconvPreEcflags = ecflags & ~EncodingUtils.ECONV_STATEFUL_DECORATOR_MASK;
                writeconvPreEcopts = ecopts;
                if (senc != null) {
                    denc = enc.getName();
                    writeconvAsciicompat = RubyString.newString(context.runtime, senc);
                }
                else {
                    senc = denc = EMPTY_BYTE_ARRAY;
                    writeconvAsciicompat = RubyString.newString(context.runtime, enc.getName());
                }
                ecflags = this.ecflags & (EncodingUtils.ECONV_ERROR_HANDLER_MASK|EncodingUtils.ECONV_STATEFUL_DECORATOR_MASK);
                ecopts = this.ecopts;
                writeconv = EncodingUtils.econvOpenOpts(context, senc, denc, ecflags, ecopts);
                if (writeconv == null) {
                    throw EncodingUtils.econvOpenExc(context, senc, denc, ecflags);
                }
            }
        }
    }
    
    private void clearReadConversion() {
        readconv = null;
    }
    
    private void clearCodeConversion() {
        readconv = null;
        writeconv = null;
    }
    
    @Override
    public void setEnc2(Encoding enc2) {
        this.enc2 = enc2;
    }
    
    @Override
    public void setEnc(Encoding enc) {
        this.enc = enc;
    }
    
    @Override
    public void setEcflags(int ecflags) {
        this.ecflags = ecflags;
    }
    
    @Override
    public int getEcflags() {
        return ecflags;
    }
    
    @Override
    public void setEcopts(IRubyObject ecopts) {
        this.ecopts = ecopts;
    }
    
    @Override
    public IRubyObject getEcopts() {
        return ecopts;
    }
    
    @Override
    public void setBOM(boolean bom) {
        this.hasBom = bom;
    }
    
    @Override
    public boolean getBOM() {
        return hasBom;
    }

    // MRI: rb_io_ascii8bit_binmode
    protected void setAscii8bitBinmode() {
        Encoding ascii8bit = getRuntime().getEncodingService().getAscii8bitEncoding();

        if (readconv != null) {
            readconv = null;
        }
        if (writeconv != null) {
            writeconv = null;
        }
        openFile.setBinmode();
        openFile.clearTextMode();
        enc = ascii8bit;
        enc2 = null;
        ecflags = 0;
        ecopts = getRuntime().getNil();
        clearCodeConversion();
    }
    
    protected void MakeOpenFile() {
        if (openFile != null) {
            Ruby runtime = getRuntime();
            ioClose(runtime);
            openFile.finalize(runtime, false);
            openFile = null;
        }
        openFile = new OpenFile();
    }

    @Deprecated
    public IRubyObject getline(Ruby runtime, ByteList separator) {
        return getline(runtime.getCurrentContext(), separator, -1, null);
    }
    
    @Deprecated
    public IRubyObject getline(Ruby runtime, ByteList separator, long limit) {
        return getline(runtime.getCurrentContext(), separator, limit, null);
    }
    
    protected Transcoder readconv = null;
    protected boolean writeconvInitialized = false;
    protected Transcoder writeconv = null;
    protected OpenFile openFile;
    protected List<RubyThread> blockingThreads;
    
    /**
     * readEncoding/writeEncoding deserve a paragraph explanation.  In spite
     * of appearing to be a better name than enc/enc as is used in MRI, it is
     * probably a wash.  readEncoding represents the encoding we want the string
     * to be.  If writeEncoding is not null this represents the source encoding
     * to use.
     * 
     * Reading:
     * So if we are reading and there is no writeEncoding then we assume that
     * the io is already readEncoding and read it as such.  If both are set
     * then we assume readEncoding is external encoding and we transcode to
     * writeEncoding (internal).
     * 
     * Writing:
     * If writeEncoding is null then we write the bytes as readEncoding.  If
     * writeEncoding is set then we convert from writeEncoding to readEncoding.
     * 
     * Note: This naming is clearly wrong, but it is no worse then enc/enc2 so
     * I did not feel the need to fix it.
     */
    protected Encoding enc; // MRI:enc
    protected Encoding enc2; // MRI:enc2
    protected int ecflags;
    protected IRubyObject ecopts = getRuntime().getNil();
    protected int writeconvPreEcflags;
    protected IRubyObject writeconvPreEcopts = ecopts;
    protected IRubyObject writeconvAsciicompat = ecopts;
    
    /**
     * If the stream is being used for popen, we don't want to destroy the process
     * when we close the stream.
     */
    protected boolean popenSpecial;
    protected boolean hasBom = false;
}
