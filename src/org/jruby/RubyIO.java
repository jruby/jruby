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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jcodings.exception.EncodingException;
import com.kenai.constantine.platform.Fcntl;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodings.Encoding;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.libraries.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.Stream;
import org.jruby.util.io.ModeFlags;
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

import org.jruby.util.io.SelectorFactory;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.javasupport.util.RuntimeHelpers;

import static org.jruby.CompatVersion.*;
import static org.jruby.RubyEnumerator.enumeratorize;

/**
 * 
 * @author jpetersen
 */
@JRubyClass(name="IO", include="Enumerable")
public class RubyIO extends RubyObject {
    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public RubyIO(Ruby runtime, RubyClass type) {
        super(runtime, type);
        
        openFile = new OpenFile();
    }

    public RubyIO(Ruby runtime, OutputStream outputStream) {
        super(runtime, runtime.getIO());
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw runtime.newRuntimeError("Opening null stream");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(Channels.newChannel(outputStream))));
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
            throw runtime.newRuntimeError("Opening null channelpo");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(channel)));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
    }

    public RubyIO(Ruby runtime, ShellLauncher.POpenProcess process, ModeFlags modes) {
    	super(runtime, runtime.getIO());
        
        openFile = new OpenFile();
        
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
    
    public RubyIO(Ruby runtime, STDIO stdio) {
        super(runtime, runtime.getIO());
        
        openFile = new OpenFile();
        ChannelDescriptor descriptor;

        try {
            switch (stdio) {
            case IN:
                // special constructor that accepts stream, not channel
                descriptor = new ChannelDescriptor(runtime.getIn(), new ModeFlags(ModeFlags.RDONLY), FileDescriptor.in);
                runtime.putFilenoMap(0, descriptor.getFileno());
                openFile.setMainStream(
                        ChannelStream.open(
                            runtime, 
                            descriptor));
                break;
            case OUT:
                descriptor = new ChannelDescriptor(Channels.newChannel(runtime.getOut()), new ModeFlags(ModeFlags.WRONLY | ModeFlags.APPEND), FileDescriptor.out);
                runtime.putFilenoMap(1, descriptor.getFileno());
                openFile.setMainStream(
                        ChannelStream.open(
                            runtime, 
                            descriptor));
                openFile.getMainStream().setSync(true);
                break;
            case ERR:
                descriptor = new ChannelDescriptor(Channels.newChannel(runtime.getErr()), new ModeFlags(ModeFlags.WRONLY | ModeFlags.APPEND), FileDescriptor.err);
                runtime.putFilenoMap(2, descriptor.getFileno());
                openFile.setMainStream(
                        ChannelStream.open(
                            runtime, 
                            descriptor));
                openFile.getMainStream().setSync(true);
                break;
            }
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
        // never autoclose stdio streams
        openFile.setAutoclose(false);
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

        ioClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyIO;
            }
        };

        ioClass.includeModule(runtime.getEnumerable());
        
        // TODO: Implement tty? and isatty.  We have no real capability to
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.
        
        ioClass.defineAnnotatedMethods(RubyIO.class);

        // Constants for seek
        ioClass.fastSetConstant("SEEK_SET", runtime.newFixnum(Stream.SEEK_SET));
        ioClass.fastSetConstant("SEEK_CUR", runtime.newFixnum(Stream.SEEK_CUR));
        ioClass.fastSetConstant("SEEK_END", runtime.newFixnum(Stream.SEEK_END));

        if (runtime.is1_9()) {
            ioClass.defineModuleUnder("WaitReadable");
            ioClass.defineModuleUnder("WaitWritable");
        }

        return ioClass;
    }

    public OutputStream getOutStream() {
        return getHandler().newOutputStream();
    }

    public InputStream getInStream() {
        return getHandler().newInputStream();
    }

    public Channel getChannel() {
        return getHandler().getChannel();
    }
    
    public Stream getHandler() {
        return getOpenFileChecked().getMainStream();
    }

    protected void reopenPath(Ruby runtime, IRubyObject[] args) {
        if (runtime.is1_9() && !(args[0] instanceof RubyString) && args[0].respondsTo("to_path")) {
            args[0] = args[0].callMethod(runtime.getCurrentContext(), "to_path");
        }
        IRubyObject pathString = args[0].convertToString();

        // TODO: check safe, taint on incoming string

        try {
            ModeFlags modes;
            if (args.length > 1) {
                IRubyObject modeString = args[1].convertToString();
                modes = getIOModes(runtime, modeString.toString());

                openFile.setMode(modes.getOpenFileFlags());
            } else {
                modes = getIOModes(runtime, "r");
            }

            String path = pathString.toString();

            // Ruby code frequently uses a platform check to choose "NUL:" on windows
            // but since that check doesn't work well on JRuby, we help it out

            openFile.setPath(path);

            if (openFile.getMainStream() == null) {
                try {
                    openFile.setMainStream(ChannelStream.fopen(runtime, path, modes));
                } catch (FileExistsException fee) {
                    throw runtime.newErrnoEEXISTError(path);
                }

                if (openFile.getPipeStream() != null) {
                    openFile.getPipeStream().fclose();
                    openFile.setPipeStream(null);
                }
            } else {
                // TODO: This is an freopen in MRI, this is close, but not quite the same
                openFile.getMainStream().freopen(runtime, path, getIOModes(runtime, openFile.getModeAsString(runtime)));
                
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

            OpenFile originalFile = ios.getOpenFileChecked();
            OpenFile selfFile = getOpenFileChecked();

            long pos = 0;
            if (originalFile.isReadable()) {
                pos = originalFile.getMainStream().fgetpos();
            }

            if (originalFile.getPipeStream() != null) {
                originalFile.getPipeStream().fflush();
            } else if (originalFile.isWritable()) {
                originalFile.getMainStream().fflush();
            }

            if (selfFile.isWritable()) {
                selfFile.getWriteStream().fflush();
            }

            selfFile.setMode(originalFile.getMode());
            selfFile.setProcess(originalFile.getProcess());
            selfFile.setLineNumber(originalFile.getLineNumber());
            selfFile.setPath(originalFile.getPath());
            selfFile.setFinalizer(originalFile.getFinalizer());

            ChannelDescriptor selfDescriptor = selfFile.getMainStream().getDescriptor();
            ChannelDescriptor originalDescriptor = originalFile.getMainStream().getDescriptor();

            // confirm we're not reopening self's channel
            if (selfDescriptor.getChannel() != originalDescriptor.getChannel()) {
                // check if we're a stdio IO, and ensure we're not badly mutilated
                if (runtime.getFileno(selfDescriptor) >= 0 && runtime.getFileno(selfDescriptor) <= 2) {
                    selfFile.getMainStream().clearerr();

                    // dup2 new fd into self to preserve fileno and references to it
                    originalDescriptor.dup2Into(selfDescriptor);
                } else {
                    Stream pipeFile = selfFile.getPipeStream();
                    int mode = selfFile.getMode();
                    selfFile.getMainStream().fclose();
                    selfFile.setPipeStream(null);

                    // TODO: turn off readable? am I reading this right?
                    // This only seems to be used while duping below, since modes gets
                    // reset to actual modes afterward
                    //fptr->mode &= (m & FMODE_READABLE) ? ~FMODE_READABLE : ~FMODE_WRITABLE;

                    if (pipeFile != null) {
                        selfFile.setMainStream(ChannelStream.fdopen(runtime, originalDescriptor, new ModeFlags()));
                        selfFile.setPipeStream(pipeFile);
                    } else {
                        // only use internal fileno here, stdio is handled above
                        selfFile.setMainStream(
                                ChannelStream.open(
                                runtime,
                                originalDescriptor.dup2(selfDescriptor.getFileno())));

                        // since we're not actually duping the incoming channel into our handler, we need to
                        // copy the original sync behavior from the other handler
                        selfFile.getMainStream().setSync(selfFile.getMainStream().isSync());
                    }
                    selfFile.setMode(mode);
                }

                // TODO: anything threads attached to original fd are notified of the close...
                // see rb_thread_fd_close

                if (originalFile.isReadable() && pos >= 0) {
                    selfFile.seek(pos, Stream.SEEK_SET);
                    originalFile.seek(pos, Stream.SEEK_SET);
                }
            }

            // only use internal fileno here, stdio is handled above
            if (selfFile.getPipeStream() != null && selfDescriptor.getFileno() != selfFile.getPipeStream().getDescriptor().getFileno()) {
                int fd = selfFile.getPipeStream().getDescriptor().getFileno();

                if (originalFile.getPipeStream() == null) {
                    selfFile.getPipeStream().fclose();
                    selfFile.setPipeStream(null);
                } else if (fd != originalFile.getPipeStream().getDescriptor().getFileno()) {
                    selfFile.getPipeStream().fclose();
                    ChannelDescriptor newFD2 = originalFile.getPipeStream().getDescriptor().dup2(fd);
                    selfFile.setPipeStream(ChannelStream.fdopen(runtime, newFD2, getIOModes(runtime, "w")));
                }
            }

            // TODO: restore binary mode
            //            if (fptr->mode & FMODE_BINMODE) {
            //                rb_io_binmode(io);
            //            }

            // TODO: set our metaclass to target's class (i.e. scary!)

        } catch (IOException ex) { // TODO: better error handling
            throw runtime.newIOError("could not reopen: " + ex.getMessage());
        } catch (BadDescriptorException ex) {
            throw runtime.newIOError("could not reopen: " + ex.getMessage());
        } catch (PipeException ex) {
            throw runtime.newIOError("could not reopen: " + ex.getMessage());
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    @JRubyMethod(name = "reopen", required = 1, optional = 1)
    public IRubyObject reopen(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
    	IRubyObject tmp = TypeConverter.convertToTypeWithCheck(args[0], runtime.getIO(), "to_io");
        
    	if (!tmp.isNil()) {
            reopenIO(runtime, (RubyIO) tmp);
        } else {
            reopenPath(runtime, args);
        }
        
        return this;
    }
    
    public static ModeFlags getIOModes(Ruby runtime, String modesString) throws InvalidValueException {
        return new ModeFlags(getIOModesIntFromString(runtime, modesString));
    }
        
    public static int getIOModesIntFromString(Ruby runtime, String modesString) {
        int modes = 0;
        int length = modesString.length();

        if (length == 0) {
            throw runtime.newArgumentError("illegal access mode");
        }

        switch (modesString.charAt(0)) {
        case 'r' :
            modes |= ModeFlags.RDONLY;
            break;
        case 'a' :
            modes |= ModeFlags.APPEND | ModeFlags.WRONLY | ModeFlags.CREAT;
            break;
        case 'w' :
            modes |= ModeFlags.WRONLY | ModeFlags.TRUNC | ModeFlags.CREAT;
            break;
        default :
            throw runtime.newArgumentError("illegal access mode " + modesString);
        }

        ModifierLoop: for (int n = 1; n < length; n++) {
            switch (modesString.charAt(n)) {
            case 'b':
                modes |= ModeFlags.BINARY;
                break;
            case '+':
                modes = (modes & ~ModeFlags.ACCMODE) | ModeFlags.RDWR;
                break;
            case 't' :
                // FIXME: add text mode to mode flags
                break;
            case ':':
                break ModifierLoop;
            default:
                throw runtime.newArgumentError("illegal access mode " + modesString);
            }
        }

        return modes;
    }

    /*
     * Ensure that separator is valid otherwise give it the default paragraph separator.
     */
    private static ByteList separator(Ruby runtime) {
        return separator(runtime.getRecordSeparatorVar().get());
    }

    private static ByteList separator(IRubyObject separatorValue) {
        ByteList separator = separatorValue.isNil() ? null :
            separatorValue.convertToString().getByteList();

        if (separator != null && separator.getRealSize() == 0) separator = Stream.PARAGRAPH_DELIMETER;

        return separator;
    }

    private static ByteList getSeparatorFromArgs(Ruby runtime, IRubyObject[] args, int idx) {
        return separator(args.length > idx ? args[idx] : runtime.getRecordSeparatorVar().get());
    }

    private ByteList getSeparatorForGets(Ruby runtime, IRubyObject[] args) {
        return getSeparatorFromArgs(runtime, args, 0);
    }

    private IRubyObject getline(Ruby runtime, ByteList separator, ByteListCache cache) {
        return getline(runtime, separator, -1, cache);
    }

    public IRubyObject getline(Ruby runtime, ByteList separator) {
        return getline(runtime, separator, -1, null);
    }


    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     *
     */
    public IRubyObject getline(Ruby runtime, ByteList separator, long limit) {
        return getline(runtime, separator, limit, null);
    }
    
    /**
     * getline using logic of gets.  If limit is -1 then read unlimited amount.
     *
     */
    private IRubyObject getline(Ruby runtime, ByteList separator, long limit, ByteListCache cache) {
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            boolean isParagraph = separator == Stream.PARAGRAPH_DELIMETER;
            separator = isParagraph ? Stream.PARAGRAPH_SEPARATOR : separator;
            
            if (isParagraph) swallow('\n');
            
            if (separator == null && limit < 0) {
                RubyString str = readAll();
                if (str.getByteList().length() == 0) {
                    return runtime.getNil();
                }
                incrementLineno(runtime, myOpenFile);
                return str;
            } else if (limit == 0) {
                return RubyString.newEmptyString(runtime, externalEncoding);
            } else if (separator.length() == 1 && limit < 0) {
                return getlineFast(runtime, separator.get(0) & 0xFF, cache);
            } else {
                Stream readStream = myOpenFile.getMainStream();
                int c = -1;
                int n = -1;
                int newline = separator.get(separator.length() - 1) & 0xFF;

                ByteList buf = cache != null ? cache.allocate(0) : new ByteList(0);
                try {
                    boolean update = false;
                    boolean limitReached = false;

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
                        if (n == -1 || limitReached) break;

                        // if we've found the last char of the separator,
                        // and we've found at least as many characters as separator length,
                        // and the last n characters of our buffer match the separator, we're done
                        if (c == newline && buf.length() >= separator.length() &&
                                0 == ByteList.memcmp(buf.getUnsafeBytes(), buf.getBegin() + buf.getRealSize() - separator.length(), separator.getUnsafeBytes(), separator.getBegin(), separator.getRealSize())) {
                            break;
                        }
                    }
                    
                    if (isParagraph && c != -1) swallow('\n');
                    if (!update) return runtime.getNil();

                    incrementLineno(runtime, myOpenFile);

                    return makeString(runtime, buf, cache != null);
                }
                finally {
                    if (cache != null) cache.release(buf);
                }
            }
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (EOFException e) {
            return runtime.getNil();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    private Encoding getExternalEncoding(Ruby runtime) {
        return externalEncoding != null ? externalEncoding : runtime.getDefaultExternalEncoding();
    }

    private RubyString makeString(Ruby runtime, ByteList buffer, boolean isCached) {
        ByteList newBuf = isCached ? new ByteList(buffer) : buffer;
        Encoding encoding = getExternalEncoding(runtime);

        if (encoding != null) newBuf.setEncoding(encoding);

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
        Stream readStream = openFile.getMainStream();
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
        Stream readStream = openFile.getMainStream();
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
            context.getRuntime().getWarnings().warn(
                    ID.BLOCK_NOT_ACCEPTED,
                    className + "::new() does not take block; use " + className + "::open() instead",
                    className + "::open()");
        }
        
        return klass.newInstance(context, args, block);
    }

    private IRubyObject initializeCommon19(int fileno, ModeFlags modes) {
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno(getRuntime().getFilenoExtMap(fileno));

            if (descriptor == null) throw getRuntime().newErrnoEBADFError();

            descriptor.checkOpen();

            if (modes == null) modes = descriptor.getOriginalModes();

            if (openFile.isOpen()) {
                // JRUBY-4650: Make sure we clean up the old data,
                // if it's present.
                openFile.cleanup(getRuntime(), false);
            }

            openFile.setMode(modes.getOpenFileFlags());
            openFile.setMainStream(fdopen(descriptor, modes));
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException ive) {
            throw getRuntime().newErrnoEINVALError();
        }

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, Block unusedBlock) {
        return initializeCommon19(RubyNumeric.fix2int(fileNumber), null);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, IRubyObject second, Block unusedBlock) {
        int fileno = RubyNumeric.fix2int(fileNumber);
        ModeFlags modes;
        if (second instanceof RubyHash) {
            modes = parseOptions(context, second, null);
        } else {
            modes = parseModes19(context, second);
        }

        return initializeCommon19(fileno, modes);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject fileNumber, IRubyObject modeValue, IRubyObject options, Block unusedBlock) {
        int fileno = RubyNumeric.fix2int(fileNumber);
        ModeFlags modes = parseModes19(context, modeValue);

        modes = parseOptions(context, options, modes);
        return initializeCommon19(fileno, modes);
    }

    protected ModeFlags parseModes(IRubyObject arg) {
        try {
            if (arg instanceof RubyFixnum) return new ModeFlags(RubyFixnum.fix2long(arg));

            return getIOModes(getRuntime(), arg.convertToString().toString());
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
    }

    protected ModeFlags parseModes19(ThreadContext context, IRubyObject arg) {
        ModeFlags modes = parseModes(arg);

        if (arg instanceof RubyString) {
            parseEncodingFromString(context, arg, 1);
        }

        return modes;
    }

    private void parseEncodingFromString(ThreadContext context, IRubyObject arg, int initialPosition) {
        RubyString modes19 = arg.convertToString();
        if (modes19.toString().contains(":")) {
            Ruby runtime = context.getRuntime();

            IRubyObject[] fullEncoding = modes19.split(context, RubyString.newString(runtime, ":")).toJavaArray();

            IRubyObject externalEncodingOption = fullEncoding[initialPosition];
            RubyString dash = runtime.newString("-");
            if (dash.eql(externalEncodingOption)) {
                externalEncodingOption = runtime.getEncodingService().getDefaultExternal();
            }

            if (fullEncoding.length > (initialPosition + 1)) {
                IRubyObject internalEncodingOption = fullEncoding[initialPosition + 1];
                if (dash.eql(internalEncodingOption)) {
                    internalEncodingOption = runtime.getEncodingService().getDefaultInternal();
                }
                set_encoding(context, externalEncodingOption, internalEncodingOption);
            } else {
                set_encoding(context, externalEncodingOption);
            }
        }
    }

    @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        int argCount = args.length;
        ModeFlags modes;
        
        int fileno = RubyNumeric.fix2int(args[0]);
        
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno(getRuntime().getFilenoExtMap(fileno));
            
            if (descriptor == null) {
                throw getRuntime().newErrnoEBADFError();
            }
            
            descriptor.checkOpen();
            
            if (argCount == 2) {
                if (args[1] instanceof RubyFixnum) {
                    modes = new ModeFlags(RubyFixnum.fix2long(args[1]));
                } else {
                    modes = getIOModes(getRuntime(), args[1].convertToString().toString());
                }
            } else {
                // use original modes
                modes = descriptor.getOriginalModes();
            }

            if (openFile.isOpen()) {
                // JRUBY-4650: Make sure we clean up the old data,
                // if it's present.
                openFile.cleanup(getRuntime(), false);
            }

            openFile.setMode(modes.getOpenFileFlags());
        
            openFile.setMainStream(fdopen(descriptor, modes));
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException ive) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        return this;
    }
    
    protected Stream fdopen(ChannelDescriptor existingDescriptor, ModeFlags modes) throws InvalidValueException {
        // See if we already have this descriptor open.
        // If so then we can mostly share the handler (keep open
        // file, but possibly change the mode).
        
        if (existingDescriptor == null) {
            // redundant, done above as well
            
            // this seems unlikely to happen unless it's a totally bogus fileno
            // ...so do we even need to bother trying to create one?
            
            // IN FACT, we should probably raise an error, yes?
            throw getRuntime().newErrnoEBADFError();
            
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
            return ChannelStream.fdopen(getRuntime(), existingDescriptor, modes);
        }
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject external_encoding(ThreadContext context) {
        return externalEncoding != null ?
            context.getRuntime().getEncodingService().getEncoding(externalEncoding) :
            context.getRuntime().getNil();
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject internal_encoding(ThreadContext context) {
        return internalEncoding != null ?
            context.getRuntime().getEncodingService().getEncoding(internalEncoding) :
            context.getRuntime().getNil();
    }

    @JRubyMethod(compat=RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString) {
        setExternalEncoding(context, encodingString);
        return context.getRuntime().getNil();
    }

    @JRubyMethod(compat=RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding) {
        setExternalEncoding(context, encodingString);
        setInternalEncoding(context, internalEncoding);
        return context.getRuntime().getNil();
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject encodingString, IRubyObject internalEncoding, IRubyObject options) {
        setExternalEncoding(context, encodingString);
        setInternalEncoding(context, internalEncoding);
        return context.getRuntime().getNil();
    }

    private void setExternalEncoding(ThreadContext context, IRubyObject encoding) {
        externalEncoding = getEncodingCommon(context, encoding);
    }


    private void setInternalEncoding(ThreadContext context, IRubyObject encoding) {
        Encoding internalEncodingOption = getEncodingCommon(context, encoding);

        if (internalEncodingOption == externalEncoding) {
            context.getRuntime().getWarnings().warn("Ignoring internal encoding " + encoding
                    + ": it is identical to external encoding " + external_encoding(context));
        } else {
            internalEncoding = internalEncodingOption;
        }
    }

    private Encoding getEncodingCommon(ThreadContext context, IRubyObject encoding) {
        if (encoding instanceof RubyEncoding) return ((RubyEncoding) encoding).getEncoding();
        
        return context.getRuntime().getEncodingService().getEncodingFromObject(encoding);
    }

    @JRubyMethod(required = 1, optional = 2, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
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
        IRubyObject pathString = args[0].convertToString();
        return sysopenCommon(recv, args, block, pathString);
    }
    
    @JRubyMethod(name = "sysopen", required = 1, optional = 2, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject sysopen19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject pathString;
        if (!(args[0] instanceof RubyString) && args[0].respondsTo("to_path")) {
            pathString = args[0].callMethod(context, "to_path");
        } else {
            pathString = args[0].convertToString();
        }
        return sysopenCommon(recv, args, block, pathString);
    }

    private static IRubyObject sysopenCommon(IRubyObject recv, IRubyObject[] args, Block block, IRubyObject pathString) {
        Ruby runtime = recv.getRuntime();
        runtime.checkSafeString(pathString);
        String path = pathString.toString();

        ModeFlags modes = null;
        int perms = -1; // -1 == don't set permissions
        try {
            if (args.length > 1) {
                IRubyObject modeString = args[1].convertToString();
                modes = getIOModes(runtime, modeString.toString());
            } else {
                modes = getIOModes(runtime, "r");
            }
            if (args.length > 2) {
                RubyInteger permsInt =
                    args.length >= 3 ? args[2].convertToInteger() : null;
                perms = RubyNumeric.fix2int(permsInt);
            }
        } catch (InvalidValueException e) {
            throw runtime.newErrnoEINVALError();
        }

        int fileno = -1;
        try {
            ChannelDescriptor descriptor =
                ChannelDescriptor.open(runtime.getCurrentDirectory(),
                                       path, modes, perms, runtime.getPosix(),
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

    @JRubyMethod
    public IRubyObject autoclose(ThreadContext context) {
        return context.runtime.newBoolean(isAutoclose());
    }

    @JRubyMethod(name = "autoclose=")
    public IRubyObject autoclose_set(ThreadContext context, IRubyObject autoclose) {
        setAutoclose(autoclose.isTrue());
        return context.nil;
    }

    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        if (isClosed()) throw getRuntime().newIOError("closed stream");

        Ruby runtime = getRuntime();
        if (getExternalEncoding(runtime) == USASCIIEncoding.INSTANCE) {
            externalEncoding = ASCIIEncoding.INSTANCE;
        }
        openFile.setBinmode();
        return this;
    }

    @JRubyMethod(name = "binmode?", compat = RUBY1_9)
    public IRubyObject op_binmode(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), openFile.isBinmode());
    }

    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        
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
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newSystemCallError(e.getMessage());
        } finally {
            context.getThread().afterBlockingCall();
        }
    }
    
    @JRubyMethod(name = "write_nonblock", required = 1)
    public IRubyObject write_nonblock(ThreadContext context, IRubyObject obj) {
        // MRI behavior: always check whether the file is writable
        // or not, even if we are to write 0 bytes.
        OpenFile myOpenFile = getOpenFileChecked();

        try {
            myOpenFile.checkWritable(context.getRuntime());
            RubyString str = obj.asString();
            if (str.getByteList().length() == 0) {
                return context.getRuntime().newFixnum(0);
            }

            if (myOpenFile.isWriteBuffered()) {
                context.getRuntime().getWarnings().warn(ID.SYSWRITE_BUFFERED_IO, "write_nonblock for buffered IO");
            }
            int written = myOpenFile.getWriteStream().getDescriptor().write(str.getByteList());
            return context.getRuntime().newFixnum(written);
        } catch (IOException ex) {
            throw context.getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        }  catch (PipeException ex) {
            throw context.getRuntime().newErrnoEPIPEError();
        }
    }
    
    /** io_write
     * 
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        
        runtime.secure(4);
        
        RubyString str = obj.asString();

        // TODO: Ruby reuses this logic for other "write" behavior by checking if it's an IO and calling write again
        
        if (str.getByteList().length() == 0) {
            return runtime.newFixnum(0);
        }

        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkWritable(runtime);

            context.getThread().beforeBlockingCall();
            int written = fwrite(str.getByteList());

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
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
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

    protected int fwrite(ByteList buffer) {
        int n, r, l, offset = 0;
        boolean eagain = false;
        Stream writeStream = openFile.getWriteStream();

        int len = buffer.length();
        
        if ((n = len) <= 0) return n;

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

                    r = writeStream.getDescriptor().write(buffer,offset,l);

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
                        if(offset >= buffer.length()) {
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
            return writeStream.fwrite(buffer);
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
        Ruby runtime = context.getRuntime();
        // map to external fileno
        return runtime.newFixnum(runtime.getFileno(getOpenFileChecked().getMainStream().getDescriptor()));
    }
    
    /** Returns the current line number.
     * 
     * @return the current line number.
     */
    @JRubyMethod(name = "lineno")
    public RubyFixnum lineno(ThreadContext context) {
        return context.getRuntime().newFixnum(getOpenFileChecked().getLineNumber());
    }

    /** Sets the current line number.
     * 
     * @param newLineNumber The new line number.
     */
    @JRubyMethod(name = "lineno=", required = 1)
    public RubyFixnum lineno_set(ThreadContext context, IRubyObject newLineNumber) {
        getOpenFileChecked().setLineNumber(RubyNumeric.fix2int(newLineNumber));

        return context.getRuntime().newFixnum(getOpenFileChecked().getLineNumber());
    }

    /** Returns the current sync mode.
     * 
     * @return the current sync mode.
     */
    @JRubyMethod(name = "sync")
    public RubyBoolean sync(ThreadContext context) {
        return context.getRuntime().newBoolean(getOpenFileChecked().getMainStream().isSync());
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
            return context.getRuntime().getNil();
        }
        
        // Of course this isn't particularly useful.
        long pid = myOpenFile.getPid();
        
        return context.getRuntime().newFixnum(pid); 
    }
    
    @JRubyMethod(name = {"pos", "tell"})
    public RubyFixnum pos(ThreadContext context) {
        try {
            return context.getRuntime().newFixnum(getOpenFileChecked().getMainStream().fgetpos());
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (BadDescriptorException bde) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.getRuntime().newIOError(e.getMessage());
        }
    }
    
    @JRubyMethod(name = "pos=", required = 1)
    public RubyFixnum pos_set(ThreadContext context, IRubyObject newPosition) {
        long offset = RubyNumeric.num2long(newPosition);

        if (offset < 0) {
            throw context.getRuntime().newSystemCallError("Negative seek offset");
        }
        
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            myOpenFile.getMainStream().lseek(offset, Stream.SEEK_SET);
        } catch (BadDescriptorException e) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.getRuntime().newIOError(e.getMessage());
        }
        
        myOpenFile.getMainStream().clearerr();
        
        return context.getRuntime().newFixnum(offset);
    }
    
    /** Print some objects to the stream.
     * 
     */
    @JRubyMethod(name = "print", rest = true, reads = FrameField.LASTLINE)
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        return print(context, this, args);
    }

    /** Print some objects to the stream.
     *
     */
    public static IRubyObject print(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { context.getCurrentScope().getLastLine(context.getRuntime()) };
        }

        Ruby runtime = context.getRuntime();
        IRubyObject fs = runtime.getGlobalVariables().get("$,");
        IRubyObject rs = runtime.getGlobalVariables().get("$\\");

        for (int i = 0; i < args.length; i++) {
            if (i > 0 && !fs.isNil()) {
                maybeIO.callMethod(context, "write", fs);
            }
            if (args[i].isNil()) {
                maybeIO.callMethod(context, "write", runtime.newString("nil"));
            } else {
                maybeIO.callMethod(context, "write", args[i]);
            }
        }
        if (!rs.isNil()) {
            maybeIO.callMethod(context, "write", rs);
        }

        return runtime.getNil();
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        callMethod(context, "write", RubyKernel.sprintf(context, this, args));
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "putc", required = 1, backtrace = true)
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
                myOpenFile.checkWritable(context.getRuntime());
                Stream writeStream = myOpenFile.getWriteStream();
                writeStream.fputc(c);
                if (myOpenFile.isSync()) myOpenFile.fflush(writeStream);
            } catch (IOException ex) {
                throw context.getRuntime().newIOErrorFromException(ex);
            } catch (BadDescriptorException e) {
                throw context.getRuntime().newErrnoEBADFError();
            } catch (InvalidValueException ex) {
                throw context.getRuntime().newErrnoEINVALError();
            } catch (PipeException ex) {
                throw context.getRuntime().newErrnoEPIPEError();
            }
        } else {
            maybeIO.callMethod(context, "write",
                    RubyString.newStringNoCopy(context.getRuntime(), new byte[] {(byte)c}));
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
        } catch (BadDescriptorException ex) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.getRuntime().newIOError(e.getMessage());
        }
        
        myOpenFile.getMainStream().clearerr();
        
        return RubyFixnum.zero(context.getRuntime());
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
                throw context.getRuntime().newIOError("sysseek for buffered IO");
            }
            if (myOpenFile.isWritable() && myOpenFile.isWriteBuffered()) {
                context.getRuntime().getWarnings().warn(ID.SYSSEEK_BUFFERED_IO, "sysseek for buffered IO");
            }
            
            pos = myOpenFile.getMainStream().getDescriptor().lseek(offset, whence);
        } catch (BadDescriptorException ex) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.getRuntime().newIOError(e.getMessage());
        }
        
        myOpenFile.getMainStream().clearerr();
        
        return context.getRuntime().newFixnum(pos);
    }

    @JRubyMethod(name = "rewind")
    public RubyFixnum rewind(ThreadContext context) {
        OpenFile myOpenfile = getOpenFileChecked();
        
        try {
            myOpenfile.getMainStream().lseek(0L, Stream.SEEK_SET);
            myOpenfile.getMainStream().clearerr();
            
            // TODO: This is some goofy global file value from MRI..what to do?
//            if (io == current_file) {
//                gets_lineno -= fptr->lineno;
//            }
        } catch (BadDescriptorException e) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw context.getRuntime().newIOError(e.getMessage());
        }

        // Must be back on first line on rewind.
        myOpenfile.setLineNumber(0);
        
        return RubyFixnum.zero(context.getRuntime());
    }
    
    @JRubyMethod(name = "fsync")
    public RubyFixnum fsync(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            myOpenFile.checkWritable(runtime);
        
            Stream writeStream = myOpenFile.getWriteStream();

            writeStream.fflush();
            writeStream.sync();

        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
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
        getOpenFileChecked().setSync(newSync.isTrue());
        getOpenFileChecked().getMainStream().setSync(newSync.isTrue());

        return this;
    }

    @JRubyMethod(name = {"eof?", "eof"})
    public RubyBoolean eof_p(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStream().feof()) {
                return runtime.getTrue();
            }
            
            if (myOpenFile.getMainStream().readDataBuffered()) {
                return runtime.getFalse();
            }
            
            readCheck(myOpenFile.getMainStream());
            waitReadable(myOpenFile.getMainStream());
            
            myOpenFile.getMainStream().clearerr();
            
            int c = myOpenFile.getMainStream().fgetc();
            
            if (c != -1) {
                myOpenFile.getMainStream().ungetc(c);
                return runtime.getFalse();
            }
            
            myOpenFile.checkClosed(runtime);
            
            myOpenFile.getMainStream().clearerr();
            
            return runtime.getTrue();
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public RubyBoolean tty_p(ThreadContext context) {
        return context.getRuntime().newBoolean(context.getRuntime().getPosix().isatty(getOpenFileChecked().getMainStream().getDescriptor().getFileDescriptor()));
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1)
    @Override
    public IRubyObject initialize_copy(IRubyObject original){
        Ruby runtime = getRuntime();
        
        if (this == original) return this;

        RubyIO originalIO = (RubyIO) TypeConverter.convertToTypeWithCheck(original, runtime.getIO(), "to_io");
        
        OpenFile originalFile = originalIO.getOpenFileChecked();
        OpenFile newFile = openFile;
        
        try {
            if (originalFile.getPipeStream() != null) {
                originalFile.getPipeStream().fflush();
                originalFile.getMainStream().lseek(0, Stream.SEEK_CUR);
            } else if (originalFile.isWritable()) {
                originalFile.getMainStream().fflush();
            } else {
                originalFile.getMainStream().lseek(0, Stream.SEEK_CUR);
            }

            newFile.setMode(originalFile.getMode());
            newFile.setProcess(originalFile.getProcess());
            newFile.setLineNumber(originalFile.getLineNumber());
            newFile.setPath(originalFile.getPath());
            newFile.setFinalizer(originalFile.getFinalizer());
            
            ModeFlags modes;
            if (newFile.isReadable()) {
                if (newFile.isWritable()) {
                    if (newFile.getPipeStream() != null) {
                        modes = new ModeFlags(ModeFlags.RDONLY);
                    } else {
                        modes = new ModeFlags(ModeFlags.RDWR);
                    }
                } else {
                    modes = new ModeFlags(ModeFlags.RDONLY);
                }
            } else {
                if (newFile.isWritable()) {
                    modes = new ModeFlags(ModeFlags.WRONLY);
                } else {
                    modes = originalFile.getMainStream().getModes();
                }
            }
            
            ChannelDescriptor descriptor = originalFile.getMainStream().getDescriptor().dup();

            newFile.setMainStream(ChannelStream.fdopen(runtime, descriptor, modes));

            newFile.getMainStream().setSync(originalFile.getMainStream().isSync());
            
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
            throw runtime.newIOError("could not init copy: " + ex);
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
        return context.getRuntime().newBoolean(isClosed());
    }

    /**
     * Is this IO closed
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        return openFile.getMainStream() == null && openFile.getPipeStream() == null;
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
        
        if (runtime.getSafeLevel() >= 4 && isTaint()) {
            throw runtime.newSecurityError("Insecure: can't close");
        }
        
        openFile.checkClosed(runtime);
        return close2(runtime);
    }
        
    protected IRubyObject close2(Ruby runtime) {
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
        
        if (openFile.getProcess() != null) {
            obliterateProcess(openFile.getProcess());
            IRubyObject processResult = RubyProcess.RubyStatus.newProcessStatus(runtime, openFile.getProcess().exitValue());
            runtime.getCurrentContext().setLastExitStatus(processResult);
        }
        
        return runtime.getNil();
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject close_write(ThreadContext context) {
        try {
            if (context.getRuntime().getSafeLevel() >= 4 && isTaint()) {
                throw context.getRuntime().newSecurityError("Insecure: can't close");
            }
            
            OpenFile myOpenFile = getOpenFileChecked();
            
            if (myOpenFile.getPipeStream() == null && myOpenFile.isReadable()) {
                throw context.getRuntime().newIOError("closing non-duplex IO for writing");
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
            throw context.getRuntime().newErrnoEBADFError();
        } catch (IOException ioe) {
            // hmmmm
        }
        return this;
    }

    @JRubyMethod(name = "close_read")
    public IRubyObject close_read(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        try {
            if (runtime.getSafeLevel() >= 4 && isTaint()) {
                throw runtime.newSecurityError("Insecure: can't close");
            }
            
            OpenFile myOpenFile = getOpenFileChecked();
            
            if (myOpenFile.getPipeStream() == null && myOpenFile.isWritable()) {
                throw runtime.newIOError("closing non-duplex IO for reading");
            }
            
            if (myOpenFile.getPipeStream() == null) {
                close();
            } else{
                myOpenFile.getMainStream().fclose();
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
            throw getRuntime().newIOError(e.getMessage());
        }

        return this;
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject gets(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        IRubyObject result = getline(runtime, separator(runtime.getRecordSeparatorVar().get()));

        if (!result.isNil()) context.getCurrentScope().setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject gets(ThreadContext context, IRubyObject separatorArg) {
        IRubyObject result = getline(context.getRuntime(), separator(separatorArg));

        if (!result.isNil()) context.getCurrentScope().setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        IRubyObject result = getline(runtime, separator(runtime));

        if (!result.isNil()) context.getCurrentScope().setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context, IRubyObject arg) {
        ByteList separator;
        long limit = -1;
        if (arg instanceof RubyInteger) {
            limit = RubyInteger.fix2long(arg);
            separator = separator(context.getRuntime());
        } else {
            separator = separator(arg);
        }

        IRubyObject result = getline(context.getRuntime(), separator, limit);

        if (!result.isNil()) context.getCurrentScope().setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets19(ThreadContext context, IRubyObject separator, IRubyObject limit_arg) {
        long limit = limit_arg.isNil() ? -1 : RubyNumeric.fix2long(limit_arg);
        IRubyObject result = getline(context.getRuntime(), separator(separator), limit);

        if (!result.isNil()) context.getCurrentScope().setLastLine(result);

        return result;
    }

    public boolean getBlocking() {
        return ((ChannelStream) openFile.getMainStream()).isBlocking();
    }

    @JRubyMethod(name = "fcntl")
    public IRubyObject fcntl(ThreadContext context, IRubyObject cmd) {
        // TODO: This version differs from ioctl by checking whether fcntl exists
        // and raising notimplemented if it doesn't; perhaps no difference for us?
        return ctl(context.getRuntime(), cmd, null);
    }

    @JRubyMethod(name = "fcntl")
    public IRubyObject fcntl(ThreadContext context, IRubyObject cmd, IRubyObject arg) {
        // TODO: This version differs from ioctl by checking whether fcntl exists
        // and raising notimplemented if it doesn't; perhaps no difference for us?
        return ctl(context.getRuntime(), cmd, arg);
    }

    @JRubyMethod(name = "ioctl", required = 1, optional = 1)
    public IRubyObject ioctl(ThreadContext context, IRubyObject[] args) {
        IRubyObject cmd = args[0];
        IRubyObject arg;
        
        if (args.length == 2) {
            arg = args[1];
        } else {
            arg = context.getRuntime().getNil();
        }
        
        return ctl(context.getRuntime(), cmd, arg);
    }

    public IRubyObject ctl(Ruby runtime, IRubyObject cmd, IRubyObject arg) {
        long realCmd = cmd.convertToInteger().getLongValue();
        long nArg = 0;
        
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
        if (realCmd == FcntlLibrary.FD_CLOEXEC) {
            // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
            // And why the hell does webrick pass this in as a first argument!!!!!
        } else if (realCmd == Fcntl.F_SETFL.value() || realCmd == Fcntl.F_SETFD.value()) {
            if ((nArg & FcntlLibrary.FD_CLOEXEC) == FcntlLibrary.FD_CLOEXEC) {
                // Do nothing.  FD_CLOEXEC has no meaning in JVM since we cannot really exec.
            } else {
                try {
                    boolean block = (nArg & ModeFlags.NONBLOCK) != ModeFlags.NONBLOCK;

                    myOpenFile.getMainStream().setBlocking(block);
                } catch (IOException e) {
                    throw runtime.newIOError(e.getMessage());
                }
            }
        } else if (realCmd == Fcntl.F_GETFL.value()) {
            return myOpenFile.getMainStream().isBlocking() ? RubyFixnum.zero(runtime) : RubyFixnum.newFixnum(runtime, ModeFlags.NONBLOCK);
        } else {
            throw runtime.newNotImplementedError("JRuby only supports F_SETFL and F_GETFL with NONBLOCK for fcntl/ioctl");
        }
        
        return runtime.newFixnum(0);
    }
    
    private static final ByteList NIL_BYTELIST = ByteList.create("nil");
    private static final ByteList RECURSIVE_BYTELIST = ByteList.create("[...]");

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        return puts(context, this, args);
    }

    public static IRubyObject puts(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        if (args.length == 0) {
            return writeSeparator(context, maybeIO);
        }

        return putsArray(context, maybeIO, args);
    }

    private static IRubyObject writeSeparator(ThreadContext context, IRubyObject maybeIO) {
        Ruby runtime = context.getRuntime();
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        write(context, maybeIO, separator.getByteList());
        return runtime.getNil();
    }

    private static IRubyObject putsArray(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        assert runtime.getGlobalVariables().getDefaultSeparator() instanceof RubyString;
        RubyString separator = (RubyString) runtime.getGlobalVariables().getDefaultSeparator();

        for (int i = 0; i < args.length; i++) {
            ByteList line;

            if (args[i].isNil()) {
                line = getNilByteList(runtime);
            } else if (runtime.isInspecting(args[i])) {
                line = RECURSIVE_BYTELIST;
            } else if (args[i] instanceof RubyArray) {
                inspectPuts(context, maybeIO, (RubyArray) args[i]);
                continue;
            } else {
                line = args[i].asString().getByteList();
            }

            write(context, maybeIO, line);

            if (line.length() == 0 || !line.endsWith(separator.getByteList())) {
                write(context, maybeIO, separator.getByteList());
            }
        }
        return runtime.getNil();
    }

    protected void write(ThreadContext context, ByteList byteList) {
        callMethod(context, "write", RubyString.newStringShared(context.getRuntime(), byteList));
    }

    protected static void write(ThreadContext context, IRubyObject maybeIO, ByteList byteList) {
        maybeIO.callMethod(context, "write", RubyString.newStringShared(context.getRuntime(), byteList));
    }

    private static IRubyObject inspectPuts(ThreadContext context, IRubyObject maybeIO, RubyArray array) {
        try {
            context.getRuntime().registerInspecting(array);
            return putsArray(context, maybeIO, array.toJavaArray());
        } finally {
            context.getRuntime().unregisterInspecting(array);
        }
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context) {
        IRubyObject line = gets(context);

        if (line.isNil()) throw context.getRuntime().newEOFError();
        
        return line;
    }

    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context, IRubyObject separator) {
        IRubyObject line = gets(context, separator);

        if (line.isNil()) throw context.getRuntime().newEOFError();

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

    private ByteList fromEncodedBytes(Ruby runtime, Encoding enc, int value) {
        int n;
        try {
            n = value < 0 ? 0 : enc.codeToMbcLength(value);
        } catch (EncodingException ee) {
            n = 0;
        }

        if (n <= 0) throw runtime.newRangeError(this.toString() + " out of char range");

        ByteList bytes = new ByteList(n);
        enc.codeToMbc(value, bytes.getUnsafeBytes(), 0);
        bytes.setRealSize(n);
        return bytes;
    }
    
    @JRubyMethod(name = "readchar", compat = RUBY1_9)
    public IRubyObject readchar19(ThreadContext context) {
        IRubyObject value = getc19(context);
        
        if (value.isNil()) throw context.getRuntime().newEOFError();
        
        return value;
    }

    @JRubyMethod(name = "getbyte", compat = RUBY1_9)
    public IRubyObject getbyte19(ThreadContext context) {
        return getc(); // Yes 1.8 getc is 1.9 getbyte
    }

    @JRubyMethod(name = "getc", compat = RUBY1_9)
    public IRubyObject getc19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        int c = getcCommon();

        if (c == -1) {
            // CRuby checks ferror(f) and retry getc for non-blocking IO
            // read. We checks readability first if possible so retry should
            // not be needed I believe.
            return runtime.getNil();
        }

        Encoding enc = getExternalEncoding(runtime);
        // TODO: This should be optimized like RubyInteger.chr is for ascii values
        return RubyString.newStringNoCopy(runtime, fromEncodedBytes(runtime, enc, (int) c), enc, 0);
    }

    public int getcCommon() {
        try {
            OpenFile myOpenFile = getOpenFileChecked();

            myOpenFile.checkReadable(getRuntime());
            myOpenFile.setReadBuffered();

            Stream stream = myOpenFile.getMainStream();
            
            readCheck(stream);
            waitReadable(stream);
            stream.clearerr();
            
            return myOpenFile.getMainStream().fgetc();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    
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

        return ungetcCommon(number, myOpenFile);
    }

    @JRubyMethod(name = "ungetc", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject ungetc19(IRubyObject number) {
        Ruby runtime = getRuntime();
        OpenFile myOpenFile = getOpenFileChecked();

        if (!myOpenFile.isReadBuffered()) {
            return runtime.getNil();
        }

        if (number instanceof RubyString) {
            RubyString str = (RubyString) number;
            if (str.isEmpty()) return runtime.getNil();

            int c =  str.getEncoding().mbcToCode(str.getBytes(), 0, 1);
            number = runtime.newFixnum(c);
        }

        return ungetcCommon(number, myOpenFile);
    }

    private IRubyObject ungetcCommon(IRubyObject number, OpenFile myOpenFile) {
        int ch = RubyNumeric.fix2int(number);

        try {
            myOpenFile.checkReadable(getRuntime());
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStream().ungetc(ch) == -1 && ch != -1) {
                throw getRuntime().newIOError("ungetc failed");
            }
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "read_nonblock", required = 1, optional = 1, backtrace = true)
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        IRubyObject value = getPartial(context, args, true);

        if (value.isNil()) throw context.getRuntime().newEOFError();

        if (value instanceof RubyString) {
            RubyString str = (RubyString) value;
            if (str.isEmpty()) {
                Ruby ruby = context.getRuntime();
                RaiseException eagain = ruby.newErrnoEAGAINError("");

                // FIXME: *oif* 1.9 actually does this
                if (ruby.is1_9()) {
                    eagain.getException().extend(new IRubyObject[] {ruby.getIO().getConstant("WaitReadable")});
                }

                throw eagain;
            }
        }

        return value;
    }

    @JRubyMethod(name = "readpartial", required = 1, optional = 1)
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        IRubyObject value = getPartial(context, args, false);

        if (value.isNil()) throw context.getRuntime().newEOFError();

        return value;
    }

    // implements io_getpartial in io.c
    private IRubyObject getPartial(ThreadContext context, IRubyObject[] args, boolean isNonblocking) {
        Ruby runtime = context.getRuntime();

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

            if (!(myOpenFile.getMainStream() instanceof ChannelStream)) { // cryptic for the uninitiated...
                throw runtime.newNotImplementedError("readpartial only works with Nio based handlers");
            }
            ChannelStream stream = (ChannelStream) myOpenFile.getMainStream();

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
            if (externalEncoding != null) { // TODO: Encapsulate into something more central (when adding trancoding)
                newBuf.setEncoding(externalEncoding);
            }
            string.view(newBuf);

            if (stream.feof() && empty) return runtime.getNil();

            return string;
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException e) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (EOFException e) {
            throw runtime.newEOFError(e.getMessage());
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
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
            
            if (myOpenFile.getMainStream().readDataBuffered()) {
                throw getRuntime().newIOError("sysread for buffered IO");
            }
            
            // TODO: Ruby locks the string here
            
            waitReadable(myOpenFile.getMainStream());
            myOpenFile.checkClosed(getRuntime());

            // We don't check RubyString modification since JRuby doesn't have
            // GIL. Other threads are free to change anytime.

            int bytesRead = myOpenFile.getMainStream().getDescriptor().read(len, str.getByteList());
            
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
        } catch (PipeException e) {
            throw getRuntime().newErrnoEPIPEError();
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
        Ruby runtime = context.getRuntime();
        OpenFile myOpenFile = getOpenFileChecked();
        
        try {
            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();
            return readAll();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
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
                    return readAll();
                } else {
                    return readAll(arg1.convertToString());
                }
            } catch (PipeException ex) {
                throw getRuntime().newErrnoEPIPEError();
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
        Ruby runtime = context.getRuntime();
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
        Ruby runtime = context.getRuntime();

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
        Ruby runtime = context.getRuntime();

        try {
            myOpenFile.checkReadable(runtime);
            myOpenFile.setReadBuffered();

            if (myOpenFile.getMainStream().feof()) {
                return null;
            }

            // READ_CHECK from MRI io.c
            readCheck(myOpenFile.getMainStream());

            context.getThread().beforeBlockingCall();
            ByteList newBuffer = fread(length);

            return newBuffer;
        } catch (EOFException ex) {
            throw runtime.newEOFError();
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        } finally {
            context.getThread().afterBlockingCall();
        }
    }

    protected static boolean emptyBufferOrEOF(ByteList buffer, OpenFile myOpenFile) throws BadDescriptorException, IOException {
        if (buffer == null) {
            return true;
        } else if (buffer.length() == 0) {
            if (myOpenFile.getMainStream() == null) {
                return true;
            }

            if (myOpenFile.getMainStream().feof()) {
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

    // implements read_all() in io.c
    protected RubyString readAll() throws BadDescriptorException, EOFException, IOException {
        Ruby runtime = getRuntime();

        // TODO: handle writing into original buffer better
        ByteList buf = readAllCommon(runtime);

        RubyString str;
        if (buf == null) {
            str = RubyString.newEmptyString(runtime);
        } else {
            str = RubyString.newString(runtime, buf);
        }
        str.setTaint(true);
        return str;
    }

    protected ByteList readAllCommon(Ruby runtime) throws BadDescriptorException, EOFException, IOException {
        ByteList buf = null;
        ChannelDescriptor descriptor = openFile.getMainStream().getDescriptor();
        try {
            // ChannelStream#readall knows what size should be allocated at first. Just use it.
            if (descriptor.isSeekable() && descriptor.getChannel() instanceof FileChannel) {
                buf = openFile.getMainStream().readall();
            } else if (descriptor == null) {
                buf = null;
            } else {
                RubyThread thread = runtime.getCurrentContext().getThread();
                try {
                    while (true) {
                        // TODO: ruby locks the string here
                        readCheck(openFile.getMainStream());
                        openFile.checkReadable(runtime);
                        thread.beforeBlockingCall();
                        ByteList read = fread(ChannelStream.BUFSIZE);
                        thread.afterBlockingCall();
                            
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
                } catch (PipeException ex) {
                    throw runtime.newErrnoEPIPEError();
                } catch (InvalidValueException ex) {
                    throw runtime.newErrnoEINVALError();
                } finally {
                    thread.afterBlockingCall();
                }
            }
        } catch (NonReadableChannelException ex) {
            throw runtime.newIOError("not opened for reading");
        }

        return buf;
    }

    // implements io_fread in io.c
    private ByteList fread(int length) throws IOException, BadDescriptorException {
        Stream stream = openFile.getMainStream();
        int rest = length;
        ByteList buf = stream.fread(length);
        if (buf != null) {
            rest -= buf.length();
        }
        while (rest > 0) {
            waitReadable(stream);
            openFile.checkClosed(getRuntime());
            stream.clearerr();
            ByteList newBuffer = stream.fread(rest);
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
        openFile.checkClosed(context.getRuntime());
        return context.getRuntime().newFileStat(getOpenFileChecked().getMainStream().getDescriptor().getFileDescriptor());
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    public IRubyObject each_byteInternal(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        
    	try {
            OpenFile myOpenFile = getOpenFileChecked();
            
            while (true) {
                myOpenFile.checkReadable(runtime);
                myOpenFile.setReadBuffered();
                waitReadable(myOpenFile.getMainStream());
                
                int c = myOpenFile.getMainStream().fgetc();
                
                // CRuby checks ferror(f) and retry getc for
                // non-blocking IO.
                if (c == -1) {
                    break;
                }
                
                assert c < 256;
                block.yield(context, getRuntime().newFixnum(c));
            }

            return this;
        } catch (PipeException ex) {
            throw runtime.newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (EOFException e) {
            return runtime.getNil();
    	} catch (IOException e) {
    	    throw runtime.newIOError(e.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject each_byte(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_byteInternal(context, block) : enumeratorize(context.getRuntime(), this, "each_byte");
    }

    @JRubyMethod(name = "bytes")
    public IRubyObject bytes(final ThreadContext context) {
        return enumeratorize(context.getRuntime(), this, "each_byte");
    }

    @JRubyMethod(name = "lines", compat = CompatVersion.RUBY1_8)
    public IRubyObject lines(final ThreadContext context, Block block) {
        return enumeratorize(context.getRuntime(), this, "each_line");
    }

    @JRubyMethod(name = "lines", compat = CompatVersion.RUBY1_9)
    public IRubyObject lines19(final ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), this, "each_line");
        return each_lineInternal(context, NULL_ARRAY, block);
    }

    public IRubyObject each_charInternal(final ThreadContext context, final Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject ch;

        while(!(ch = getc()).isNil()) {
            byte c = (byte)RubyNumeric.fix2int(ch);
            int n = runtime.getKCode().getEncoding().length(c);
            RubyString str = runtime.newString();
            if (externalEncoding != null) str.setEncoding(externalEncoding);
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

    @JRubyMethod
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.getRuntime(), this, "each_char");
    }

    @JRubyMethod
    public IRubyObject chars(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.getRuntime(), this, "chars");
    }

    @JRubyMethod
    public IRubyObject codepoints(final ThreadContext context, final Block block) {
        return eachCodePointCommon(context, block, "codepoints");
    }

    @JRubyMethod
    public IRubyObject each_codepoint(final ThreadContext context, final Block block) {
        return eachCodePointCommon(context, block, "each_codepoint");
    }

    private IRubyObject eachCharCommon(final ThreadContext context, final Block block, final String methodName) {
        return block.isGiven() ? each_char(context, block) : enumeratorize(context.getRuntime(), this, methodName);
    }

    private IRubyObject eachCodePointCommon(final ThreadContext context, final Block block, final String methodName) {
        Ruby runtime = context.getRuntime();
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
        Ruby runtime = context.getRuntime();
        ByteList separator = getSeparatorForGets(runtime, args);

        ByteListCache cache = new ByteListCache();
        for (IRubyObject line = getline(runtime, separator); !line.isNil(); 
		line = getline(runtime, separator, cache)) {
            block.yield(context, line);
        }
        
        return this;
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each(final ThreadContext context, IRubyObject[]args, final Block block) {
        return block.isGiven() ? each_lineInternal(context, args, block) : enumeratorize(context.getRuntime(), this, "each", args);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each_line(final ThreadContext context, IRubyObject[]args, final Block block) {
        return block.isGiven() ? each_lineInternal(context, args, block) : enumeratorize(context.getRuntime(), this, "each_line", args);
    }

    @JRubyMethod(optional = 1)
    public RubyArray readlines(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        IRubyObject[] separatorArgs = args.length > 0 ? new IRubyObject[] { args[0] } : IRubyObject.NULL_ARRAY;
        ByteList separator = getSeparatorForGets(runtime, separatorArgs);
        RubyArray result = runtime.newArray();
        IRubyObject line;
        
        while (! (line = getline(runtime, separator)).isNil()) {
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
        return "RubyIO(" + openFile.getMode() + ", " + getRuntime().getFileno(openFile.getMainStream().getDescriptor()) + ")";
    }
    
    /* class methods for IO */
    
    /** rb_io_s_foreach
    *
    */
    public static IRubyObject foreachInternal(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject filename = args[0].convertToString();
        runtime.checkSafeString(filename);

        RubyIO io = (RubyIO)RubyFile.open(context, runtime.getFile(), new IRubyObject[] { filename }, Block.NULL_BLOCK);
        
        ByteListCache cache = new ByteListCache();
        if (!io.isNil()) {
            try {
                ByteList separator = getSeparatorFromArgs(runtime, args, 1);
                IRubyObject str = io.getline(runtime, separator, cache);
                while (!str.isNil()) {
                    block.yield(context, str);
                    str = io.getline(runtime, separator, cache);
                    if (runtime.is1_9()) {
                        separator = getSeparatorFromArgs(runtime, args, 1);
                    }
                }
            } finally {
                io.close();
            }
        }
       
        return runtime.getNil();
    }
    
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject foreach(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), recv, "foreach", args);

        if (!(args[0] instanceof RubyString) && args[0].respondsTo("to_path")) {
            args[0] = args[0].callMethod(context, "to_path");
        }
        return foreachInternal(context, recv, args, block);
    }

    private static RubyIO convertToIO(ThreadContext context, IRubyObject obj) {
        return (RubyIO)TypeConverter.convertToType(obj, context.getRuntime().getIO(), "to_io");
    }
   
    private static boolean registerSelect(ThreadContext context, Selector selector, IRubyObject obj, RubyIO ioObj, int ops) throws IOException {
       Channel channel = ioObj.getChannel();
       if (channel == null || !(channel instanceof SelectableChannel)) {
           return false;
       }
       
       ((SelectableChannel) channel).configureBlocking(false);
       int real_ops = ((SelectableChannel) channel).validOps() & ops;
       SelectionKey key = ((SelectableChannel) channel).keyFor(selector);
       
       if (key == null) {
           ((SelectableChannel) channel).register(selector, real_ops, obj);
       } else {
           key.interestOps(key.interestOps()|real_ops);
       }
       
       return true;
    }
   
    @JRubyMethod(name = "select", required = 1, optional = 3, meta = true)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return select_static(context, context.getRuntime(), args);
    }

    private static void checkArrayType(Ruby runtime, IRubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            throw runtime.newTypeError("wrong argument type "
                    + obj.getMetaClass().getName() + " (expected Array)");
        }
    }

    public static IRubyObject select_static(ThreadContext context, Ruby runtime, IRubyObject[] args) {
        Selector selector = null;
       try {
            Set pending = new HashSet();
            Set unselectable_reads = new HashSet();
            Set unselectable_writes = new HashSet();
            Map<RubyIO, Boolean> blocking = new HashMap();
            
            selector = SelectorFactory.openWithRetryFrom(context.getRuntime(), SelectorProvider.provider());
            if (!args[0].isNil()) {
                // read
                checkArrayType(runtime, args[0]);
                for (Iterator i = ((RubyArray)args[0]).getList().iterator(); i.hasNext();) {
                    IRubyObject obj = (IRubyObject)i.next();
                    RubyIO ioObj = convertToIO(context, obj);

                    // save blocking state
                    if (ioObj.getChannel() instanceof SelectableChannel) blocking.put(ioObj, ((SelectableChannel)ioObj.getChannel()).isBlocking());
                    
                    if (registerSelect(context, selector, obj, ioObj, SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) {
                        if (ioObj.writeDataBuffered()) {
                            pending.add(obj);
                        }
                    } else {
                        if ((ioObj.openFile.getMode() & OpenFile.READABLE) != 0) {
                            unselectable_reads.add(obj);
                        }
                    }
                }
            }

            if (args.length > 1 && !args[1].isNil()) {
                // write
                checkArrayType(runtime, args[1]);
                for (Iterator i = ((RubyArray)args[1]).getList().iterator(); i.hasNext();) {
                    IRubyObject obj = (IRubyObject)i.next();
                    RubyIO ioObj = convertToIO(context, obj);

                    // save blocking state
                    if (!blocking.containsKey(ioObj) && ioObj.getChannel() instanceof SelectableChannel) blocking.put(ioObj, ((SelectableChannel)ioObj.getChannel()).isBlocking());

                    if (!registerSelect(context, selector, obj, ioObj, SelectionKey.OP_WRITE)) {
                        if ((ioObj.openFile.getMode() & OpenFile.WRITABLE) != 0) {
                            unselectable_writes.add(obj);
                        }
                    }
                }
            }

            if (args.length > 2 && !args[2].isNil()) {
                checkArrayType(runtime, args[2]);
            // Java's select doesn't do anything about this, so we leave it be.
            }

            final boolean has_timeout = (args.length > 3 && !args[3].isNil());
            long timeout = 0;
            if (has_timeout) {
                IRubyObject timeArg = args[3];
                if (timeArg instanceof RubyFloat) {
                    timeout = Math.round(((RubyFloat)timeArg).getDoubleValue() * 1000);
                } else if (timeArg instanceof RubyFixnum) {
                    timeout = Math.round(((RubyFixnum)timeArg).getDoubleValue() * 1000);
                } else { // TODO: MRI also can hadle Bignum here
                    throw runtime.newTypeError("can't convert " + timeArg.getMetaClass().getName() + " into time interval");
                }

                if (timeout < 0) {
                    throw runtime.newArgumentError("negative timeout given");
                }
            }

            if (pending.isEmpty() && unselectable_reads.isEmpty() && unselectable_writes.isEmpty()) {
                if (has_timeout) {
                    if (timeout == 0) {
                        selector.selectNow();
                    } else {
                        selector.select(timeout);
                    }
                } else {
                    selector.select();
                }
            } else {
                selector.selectNow();
            }

            List r = new ArrayList();
            List w = new ArrayList();
            List e = new ArrayList();
            for (Iterator i = selector.selectedKeys().iterator(); i.hasNext();) {
                SelectionKey key = (SelectionKey)i.next();
                try {
                    int interestAndReady = key.interestOps() & key.readyOps();
                    if ((interestAndReady & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT | SelectionKey.OP_CONNECT)) != 0) {
                        r.add(key.attachment());
                        pending.remove(key.attachment());
                    }
                    if ((interestAndReady & (SelectionKey.OP_WRITE)) != 0) {
                        w.add(key.attachment());
                    }
                } catch (CancelledKeyException cke) {
                    // TODO: is this the right thing to do?
                    pending.remove(key.attachment());
                    e.add(key.attachment());
                }
            }
            r.addAll(pending);
            r.addAll(unselectable_reads);
            w.addAll(unselectable_writes);

            // make all sockets blocking as configured again
            selector.close(); // close unregisters all channels, so we can safely reset blocking modes
            for (Map.Entry blockingEntry : blocking.entrySet()) {
                SelectableChannel channel = (SelectableChannel)((RubyIO)blockingEntry.getKey()).getChannel();
                synchronized (channel.blockingLock()) {
                    channel.configureBlocking((Boolean)blockingEntry.getValue());
                }
            }

            if (r.isEmpty() && w.isEmpty() && e.isEmpty()) {
                return runtime.getNil();
            }

            List ret = new ArrayList();

            ret.add(RubyArray.newArray(runtime, r));
            ret.add(RubyArray.newArray(runtime, w));
            ret.add(RubyArray.newArray(runtime, e));

            return RubyArray.newArray(runtime, ret);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        } finally {
            if (selector != null) {
                try {
                    selector.close();
                } catch (Exception e) {
                }
            }
        }
   }
   
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
        case 0: throw context.getRuntime().newArgumentError(0, 1);
        case 1: return readStatic(context, recv, args[0]);
        case 2: return readStatic(context, recv, args[0], args[1]);
        case 3: return readStatic(context, recv, args[0], args[1], args[2]);
        default: throw context.getRuntime().newArgumentError(args.length, 3);
        }
   }

    private static RubyIO newFile(ThreadContext context, IRubyObject recv, IRubyObject... args) {
       return (RubyIO) RubyKernel.open(context, recv, args, Block.NULL_BLOCK);
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
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.getRuntime();
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
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.getRuntime();
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
        RubyString pathStr = path.convertToString();
        Ruby runtime = context.getRuntime();
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
    private static IRubyObject read19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length, IRubyObject offset, RubyHash options) {
        // FIXME: process options

        RubyString pathStr = RubyFile.get_path(context, path);
        Ruby runtime = context.getRuntime();
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
        IRubyObject nil = context.getRuntime().getNil();
        IRubyObject path = args[0];
        IRubyObject length = nil;
        IRubyObject offset = nil;
        Ruby runtime = context.runtime;

        if (args.length > 2) {
            offset = args[2];
            length = args[1];
        } else if (args.length > 1) {
            length = args[1];
        }
        RubyIO file = (RubyIO)RuntimeHelpers.invoke(context, runtime.getFile(), "new", path, runtime.newString("rb:ASCII-8BIT"));

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
        IRubyObject nil = context.getRuntime().getNil();
        IRubyObject path = args[0];
        IRubyObject length = nil;
        IRubyObject offset = nil;
        RubyHash options = null;
        if (args.length > 3) {
            if (!(args[3] instanceof RubyHash)) throw context.getRuntime().newTypeError("Must be a hash");
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

        return read19(context, recv, path, length, offset, (RubyHash) options);
    }

    @JRubyMethod(name = "readlines", required = 1, optional = 1, meta = true)
    public static RubyArray readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        int count = args.length;

        IRubyObject[] fileArguments = new IRubyObject[]{ args[0].convertToString() };
        IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
        RubyIO file = (RubyIO) RubyKernel.open(context, recv, fileArguments, Block.NULL_BLOCK);
        try {
            return file.readlines(context, separatorArguments);
        } finally {
            file.close();
        }
    }
   
    @JRubyMethod(name = "popen", required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject popen(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        int mode;

        IRubyObject cmdObj = null;
        if (Platform.IS_WINDOWS) {
            String[] tokens = args[0].convertToString().toString().split(" ", 2);
            String commandString = tokens[0].replace('/', '\\') +
                    (tokens.length > 1 ? ' ' + tokens[1] : "");
            cmdObj = runtime.newString(commandString);
        } else {
            cmdObj = args[0].convertToString();
        }
        runtime.checkSafeString(cmdObj);

        if ("-".equals(cmdObj.toString())) {
            throw runtime.newNotImplementedError("popen(\"-\") is unimplemented");
        }

        try {
            if (args.length == 1) {
                mode = ModeFlags.RDONLY;
            } else if (args[1] instanceof RubyFixnum) {
                mode = RubyFixnum.num2int(args[1]);
            } else {
                mode = getIOModesIntFromString(runtime, args[1].convertToString().toString());
            }

            ModeFlags modes = new ModeFlags(mode);

            ShellLauncher.POpenProcess process = ShellLauncher.popen(runtime, cmdObj, modes);

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
            
            RubyIO io = new RubyIO(runtime, process, modes);

            if (block.isGiven()) {
                try {
                    return block.yield(context, io);
                } finally {
                    if (io.openFile.isOpen()) {
                        io.close();
                    }
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, (process.waitFor())));
                }
            }
            return io;
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    @JRubyMethod(name = "popen", required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject popen19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        int mode;

        IRubyObject[] cmdPlusArgs = null;
        RubyHash env = null;
        RubyHash opts = null;
        IRubyObject cmdObj = null;
        IRubyObject arg0 = args[0].checkArrayType();
        
        if (!arg0.isNil()) {
            List argList = new ArrayList(Arrays.asList(((RubyArray)arg0).toJavaArray()));
            if (argList.isEmpty()) throw runtime.newArgumentError("wrong number of arguments");
            if (argList.get(0) instanceof RubyHash) {
                // leading hash, use for env
                env = (RubyHash)argList.remove(0);
            }
            if (argList.isEmpty()) throw runtime.newArgumentError("wrong number of arguments");
            if (argList.size() > 1 && argList.get(argList.size() - 1) instanceof RubyHash) {
                // trailing hash, use for opts
                env = (RubyHash)argList.get(argList.size() - 1);
            }
            cmdPlusArgs = (IRubyObject[])argList.toArray(new IRubyObject[argList.size()]);

            if (Platform.IS_WINDOWS) {
                String commandString = cmdPlusArgs[0].convertToString().toString().replace('/', '\\');
                cmdPlusArgs[0] = runtime.newString(commandString);
            } else {
                cmdPlusArgs[0] = cmdPlusArgs[0].convertToString();
            }
            cmdObj = cmdPlusArgs[0];
        } else {
            if (Platform.IS_WINDOWS) {
                String[] tokens = args[0].convertToString().toString().split(" ", 2);
                String commandString = tokens[0].replace('/', '\\') +
                        (tokens.length > 1 ? ' ' + tokens[1] : "");
                cmdObj = runtime.newString(commandString);
            } else {
                cmdObj = args[0].convertToString();
            }
        }
        
        runtime.checkSafeString(cmdObj);

        if ("-".equals(cmdObj.toString())) {
            throw runtime.newNotImplementedError("popen(\"-\") is unimplemented");
        }

        try {
            if (args.length == 1) {
                mode = ModeFlags.RDONLY;
            } else if (args[1] instanceof RubyFixnum) {
                mode = RubyFixnum.num2int(args[1]);
            } else {
                mode = getIOModesIntFromString(runtime, args[1].convertToString().toString());
            }

            ModeFlags modes = new ModeFlags(mode);

            ShellLauncher.POpenProcess process;
            if (cmdPlusArgs == null) {
                process = ShellLauncher.popen(runtime, cmdObj, modes);
            } else {
                process = ShellLauncher.popen(runtime, cmdPlusArgs, env, modes);
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

            RubyIO io = new RubyIO(runtime, process, modes);

            if (block.isGiven()) {
                try {
                    return block.yield(context, io);
                } finally {
                    if (io.openFile.isOpen()) {
                        io.close();
                    }
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, (process.waitFor())));
                }
            }
            return io;
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }
   
    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject popen3(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();

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
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor()));
                }
            }
            return yieldArgs;
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject popen4(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();

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
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor()));
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
        Ruby runtime = context.getRuntime();

        try {
            ShellLauncher.POpenProcess process = ShellLauncher.popen3(runtime, args);
            RubyIO input = process.getInput() != null ?
                new RubyIO(runtime, process.getInput()) :
                new RubyIO(runtime, process.getInputStream());
            RubyIO output = process.getOutput() != null ?
                new RubyIO(runtime, process.getOutput()) :
                new RubyIO(runtime, process.getOutputStream());
            RubyIO error = process.getError() != null ?
                new RubyIO(runtime, process.getError()) :
                new RubyIO(runtime, process.getErrorStream());

            input.getOpenFile().getMainStream().getDescriptor().
              setCanBeSeekable(false);
            output.getOpenFile().getMainStream().getDescriptor().
              setCanBeSeekable(false);
            error.getOpenFile().getMainStream().getDescriptor().
              setCanBeSeekable(false);

            return new POpenTuple(input, output, error, process);
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    // NIO based pipe
    @JRubyMethod(name = "pipe", meta = true)
    public static IRubyObject pipe(ThreadContext context, IRubyObject recv) {
        // TODO: This isn't an exact port of MRI's pipe behavior, so revisit
       Ruby runtime = context.getRuntime();
       try {
           Pipe pipe = Pipe.open();

           RubyIO source = new RubyIO(runtime, pipe.source());
           RubyIO sink = new RubyIO(runtime, pipe.sink());

           sink.openFile.getMainStream().setSync(true);
           return runtime.newArrayNoCopy(new IRubyObject[] { source, sink });
       } catch (IOException ioe) {
           throw runtime.newIOErrorFromException(ioe);
       }
   }
    
    @JRubyMethod(name = "copy_stream", meta = true, compat = RUBY1_9)
    public static IRubyObject copy_stream(ThreadContext context, IRubyObject recv, 
            IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.getRuntime();

        RubyIO io1 = null;
        RubyIO io2 = null;

        try {
            if (arg1 instanceof RubyString) {
                io1 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {arg1}, Block.NULL_BLOCK);
            } else if (arg1 instanceof RubyIO) {
                io1 = (RubyIO) arg1;
            } else {
                throw runtime.newTypeError("Should be String or IO");
            }

            if (arg2 instanceof RubyString) {
                io2 = (RubyIO) RubyFile.open(context, runtime.getFile(), new IRubyObject[] {arg2, runtime.newString("w")}, Block.NULL_BLOCK);
            } else if (arg2 instanceof RubyIO) {
                io2 = (RubyIO) arg2;
            } else {
                throw runtime.newTypeError("Should be String or IO");
            }

            ChannelDescriptor d1 = io1.openFile.getMainStream().getDescriptor();
            if (!d1.isSeekable()) {
                throw context.getRuntime().newTypeError("only supports file-to-file copy");
            }
            ChannelDescriptor d2 = io2.openFile.getMainStream().getDescriptor();
            if (!d2.isSeekable()) {
                throw context.getRuntime().newTypeError("only supports file-to-file copy");
            }

            FileChannel f1 = (FileChannel)d1.getChannel();
            FileChannel f2 = (FileChannel)d2.getChannel();

            try {
                long size = f1.size();

                f1.transferTo(f2.position(), size, f2);

                return context.getRuntime().newFixnum(size);
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        } finally {
            try {
                if (io1 != null) {
                    io1.close();
                }
            } finally {
                if (io2 != null) {
                    io2.close();
                }
            }
        }
    }

    @JRubyMethod(name = "try_convert", meta = true, backtrace = true, compat = RUBY1_9)
    public static IRubyObject tryConvert(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return arg.respondsTo("to_io") ? convertToIO(context, arg) : context.getRuntime().getNil();
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
        private byte[] buffer = new byte[0];
        public void release(ByteList l) {
            buffer = l.getUnsafeBytes();
        }

        public ByteList allocate(int size) {
            ByteList l = new ByteList(buffer, 0, size, false);
            return l;
        }
    }

    /**
     *
     *  ==== Options
     *  <code>opt</code> can have the following keys
     *  :mode ::
     *    same as <code>mode</code> parameter
     *  :external_encoding ::
     *    external encoding for the IO. "-" is a
     *    synonym for the default external encoding.
     *  :internal_encoding ::
     *    internal encoding for the IO.
     *    "-" is a synonym for the default internal encoding.
     *    If the value is nil no conversion occurs.
     *  :encoding ::
     *    specifies external and internal encodings as "extern:intern".
     *  :textmode ::
     *    If the value is truth value, same as "b" in argument <code>mode</code>.
     *  :binmode ::
     *    If the value is truth value, same as "t" in argument <code>mode</code>.
     *
     *  Also <code>opt</code> can have same keys in <code>String#encode</code> for
     *  controlling conversion between the external encoding and the internal encoding.
     *
     */
    protected ModeFlags parseOptions(ThreadContext context, IRubyObject options, ModeFlags modes) {
        Ruby runtime = context.getRuntime();

        RubyHash rubyOptions = (RubyHash) options;

        IRubyObject internalEncodingOption = rubyOptions.fastARef(runtime.newSymbol("internal_encoding"));
        IRubyObject externalEncodingOption = rubyOptions.fastARef(runtime.newSymbol("external_encoding"));
        RubyString dash = runtime.newString("-");
        if (externalEncodingOption != null && !externalEncodingOption.isNil()) {
            if (dash.eql(externalEncodingOption)) {
                externalEncodingOption = runtime.getEncodingService().getDefaultExternal();
            }
            setExternalEncoding(context, externalEncodingOption);
        }

        if (internalEncodingOption != null && !internalEncodingOption.isNil()) {
            if (dash.eql(internalEncodingOption)) {
                internalEncodingOption = runtime.getEncodingService().getDefaultInternal();
            }
            setInternalEncoding(context, internalEncodingOption);
        }

        IRubyObject encoding = rubyOptions.fastARef(runtime.newSymbol("encoding"));
        if (encoding != null && !encoding.isNil()) {
            if (externalEncodingOption != null && !externalEncodingOption.isNil()) {
                runtime.getWarnings().warn("Ignoring encoding parameter '"+ encoding +"': external_encoding is used");
            } else if (internalEncodingOption != null && !internalEncodingOption.isNil()) {
                runtime.getWarnings().warn("Ignoring encoding parameter '"+ encoding +"': internal_encoding is used");
            } else {
                parseEncodingFromString(context, encoding, 0);
            }
        }

        if (rubyOptions.containsKey(runtime.newSymbol("mode"))) {
            modes = parseModes19(context, rubyOptions.fastARef(runtime.newSymbol("mode")).asString());
        }

//      FIXME: check how ruby 1.9 handles this

//        if (rubyOptions.containsKey(runtime.newSymbol("textmode")) &&
//                rubyOptions.fastARef(runtime.newSymbol("textmode")).isTrue()) {
//            try {
//                modes = getIOModes(runtime, "t");
//            } catch (InvalidValueException e) {
//                throw getRuntime().newErrnoEINVALError();
//            }
//        }
//
//        if (rubyOptions.containsKey(runtime.newSymbol("binmode")) &&
//                rubyOptions.fastARef(runtime.newSymbol("binmode")).isTrue()) {
//            try {
//                modes = getIOModes(runtime, "b");
//            } catch (InvalidValueException e) {
//                throw getRuntime().newErrnoEINVALError();
//            }
//        }

        return modes;
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
     * @param runtime The Ruby runtime, for raising an error
     * @param process The process to obliterate
     */
    public static void obliterateProcess(Process process) {
        int i = 0;
        Object waitLock = new Object();
        while (true) {
            // only try 1000 times with a 1ms sleep between, so we don't hang
            // forever on processes that ignore SIGTERM
            if (i >= 1000) {
                throw new RuntimeException("could not shut down process: " + process);
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
    public boolean writeDataBuffered() {
        return openFile.getMainStream().writeDataBuffered();
    }

    @Deprecated
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? gets(context) : gets(context, args[0]);
    }

    @Deprecated
    public IRubyObject readline(ThreadContext context, IRubyObject[] args) {
        return args.length == 0 ? readline(context) : readline(context, args[0]);
    }
    
    protected OpenFile openFile;
    protected List<RubyThread> blockingThreads;
    protected Encoding externalEncoding;
    protected Encoding internalEncoding;
}
