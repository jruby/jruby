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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import org.jruby.util.io.OpenFile;
import org.jruby.util.io.ChannelDescriptor;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.DirectoryAsFileException;
import org.jruby.util.io.Stream;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.JRubyFile;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.FileExistsException;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.PipeException;

/**
 * Ruby File class equivalent in java.
 **/
public class RubyFile extends RubyIO {
    private static final long serialVersionUID = 1L;
    
    public static final int LOCK_SH = 1;
    public static final int LOCK_EX = 2;
    public static final int LOCK_NB = 4;
    public static final int LOCK_UN = 8;

    private static final int FNM_NOESCAPE = 1;
    private static final int FNM_PATHNAME = 2;
    private static final int FNM_DOTMATCH = 4;
    private static final int FNM_CASEFOLD = 8;

    static final boolean IS_WINDOWS;
    static {
        String osname = System.getProperty("os.name");
        IS_WINDOWS = osname != null && osname.toLowerCase().indexOf("windows") != -1;
    }
    private static boolean startsWithDriveLetterOnWindows(String path) {
        return (path != null)
            && IS_WINDOWS && path.length() > 1
            && isWindowsDriveLetter(path.charAt(0))
            && path.charAt(1) == ':';
    }
    // adjusts paths started with '/', on windows.
    private static String adjustRootPathOnWindows(Ruby runtime, String path, String dir) {
        if (path == null) return path;
        if (IS_WINDOWS) {
            // MRI behavior on Windows: it treats '/' as a root of
            // a current drive (but only if SINGLE slash is present!):
            // E.g., if current work directory is
            // 'D:/home/directory', then '/' means 'D:/'.
            //
            // Basically, '/path' is treated as a *RELATIVE* path,
            // relative to the current drive. '//path' is treated
            // as absolute one.
            if (path.startsWith("/")) {
                if (path.length() > 1 && path.charAt(1) == '/') {
                    return path;
                }
                
                // First try to use drive letter from supplied dir value,
                // then try current work dir.
                if (!startsWithDriveLetterOnWindows(dir)) {
                    dir = runtime.getCurrentDirectory();
                }
                if (dir.length() >= 2) {
                    path = dir.substring(0, 2) + path;
                }
            }
        }
        return path;
    }

    protected String path;
    private FileLock currentLock;
    
    public RubyFile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    // XXX This constructor is a hack to implement the __END__ syntax.
    //     Converting a reader back into an InputStream doesn't generally work.
    public RubyFile(Ruby runtime, String path, final Reader reader) {
        this(runtime, path, new InputStream() {
            public int read() throws IOException {
                return reader.read();
            }
        });
    }
    
    public RubyFile(Ruby runtime, String path, InputStream in) {
        super(runtime, runtime.getFile());
        this.path = path;
        try {
            this.openFile.setMainStream(new ChannelStream(runtime, new ChannelDescriptor(Channels.newChannel(in), getNewFileno(), new FileDescriptor())));
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        }
        this.openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
        registerDescriptor(openFile.getMainStream().getDescriptor());
    }

    private static ObjectAllocator FILE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new RubyFile(runtime, klass);
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };
    
    public static RubyClass createFileClass(Ruby runtime) {
        RubyClass fileClass = runtime.defineClass("File", runtime.getIO(), FILE_ALLOCATOR);
        runtime.setFile(fileClass);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFile.class);   
        RubyString separator = runtime.newString("/");
        
        fileClass.kindOf = new RubyModule.KindOf() {
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyFile;
                }
            };

        separator.freeze();
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);
        
        if (File.separatorChar == '\\') {
            RubyString altSeparator = runtime.newString("\\");
            altSeparator.freeze();
            fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        } else {
            fileClass.defineConstant("ALT_SEPARATOR", runtime.getNil());
        }
        
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze();
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);
        
        // TODO: These were missing, so we're not handling them elsewhere?
        // FIXME: The old value, 32786, didn't match what IOModes expected, so I reference
        // the constant here. THIS MAY NOT BE THE CORRECT VALUE.
        fileClass.fastSetConstant("BINARY", runtime.newFixnum(ModeFlags.BINARY));
        fileClass.fastSetConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
        fileClass.fastSetConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
        fileClass.fastSetConstant("FNM_SYSCASE", runtime.newFixnum(FNM_CASEFOLD));
        fileClass.fastSetConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
        fileClass.fastSetConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
        
        // Create constants for open flags
        fileClass.fastSetConstant("RDONLY", runtime.newFixnum(ModeFlags.RDONLY));
        fileClass.fastSetConstant("WRONLY", runtime.newFixnum(ModeFlags.WRONLY));
        fileClass.fastSetConstant("RDWR", runtime.newFixnum(ModeFlags.RDWR));
        fileClass.fastSetConstant("CREAT", runtime.newFixnum(ModeFlags.CREAT));
        fileClass.fastSetConstant("EXCL", runtime.newFixnum(ModeFlags.EXCL));
        fileClass.fastSetConstant("NOCTTY", runtime.newFixnum(ModeFlags.NOCTTY));
        fileClass.fastSetConstant("TRUNC", runtime.newFixnum(ModeFlags.TRUNC));
        fileClass.fastSetConstant("APPEND", runtime.newFixnum(ModeFlags.APPEND));
        fileClass.fastSetConstant("NONBLOCK", runtime.newFixnum(ModeFlags.NONBLOCK));
        
        // Create constants for flock
        fileClass.fastSetConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        fileClass.fastSetConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        fileClass.fastSetConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        fileClass.fastSetConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        // Create Constants class
        RubyModule constants = fileClass.defineModuleUnder("Constants");
        
        // TODO: These were missing, so we're not handling them elsewhere?
        constants.fastSetConstant("BINARY", runtime.newFixnum(32768));
        constants.fastSetConstant("FNM_NOESCAPE", runtime.newFixnum(1));
        constants.fastSetConstant("FNM_CASEFOLD", runtime.newFixnum(8));
        constants.fastSetConstant("FNM_DOTMATCH", runtime.newFixnum(4));
        constants.fastSetConstant("FNM_PATHNAME", runtime.newFixnum(2));
        
        // Create constants for open flags
        constants.fastSetConstant("RDONLY", runtime.newFixnum(ModeFlags.RDONLY));
        constants.fastSetConstant("WRONLY", runtime.newFixnum(ModeFlags.WRONLY));
        constants.fastSetConstant("RDWR", runtime.newFixnum(ModeFlags.RDWR));
        constants.fastSetConstant("CREAT", runtime.newFixnum(ModeFlags.CREAT));
        constants.fastSetConstant("EXCL", runtime.newFixnum(ModeFlags.EXCL));
        constants.fastSetConstant("NOCTTY", runtime.newFixnum(ModeFlags.NOCTTY));
        constants.fastSetConstant("TRUNC", runtime.newFixnum(ModeFlags.TRUNC));
        constants.fastSetConstant("APPEND", runtime.newFixnum(ModeFlags.APPEND));
        constants.fastSetConstant("NONBLOCK", runtime.newFixnum(ModeFlags.NONBLOCK));
        
        // Create constants for flock
        constants.fastSetConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        constants.fastSetConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        constants.fastSetConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        constants.fastSetConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        runtime.getFileTest().extend_object(fileClass);
        
        fileClass.defineAnnotatedMethods(RubyFile.class);
        fileClass.dispatcher = callbackFactory.createDispatcher(fileClass);
        
        return fileClass;
    }
    
    public void openInternal(String newPath, ModeFlags newModes) {
        this.path = newPath;
        this.openFile.setMode(newModes.getOpenFileFlags());
        
        // Ruby code frequently uses a platform check to choose "NUL:" on windows
        // but since that check doesn't work well on JRuby, we help it out
        if ("/dev/null".equals(path) && SafePropertyAccessor.getProperty("os.name").contains("Windows")) {
            path = "NUL:";
        }
        
        try {
            if(newPath.startsWith("file:")) {
                String filePath = path.substring(5, path.indexOf("!"));
                String internalPath = path.substring(path.indexOf("!") + 2);

                java.util.jar.JarFile jf;
                try {
                    jf = new java.util.jar.JarFile(filePath);
                } catch(IOException e) {
                    throw getRuntime().newErrnoENOENTError(
                                                           "File not found - " + newPath);
                }

                java.util.zip.ZipEntry zf = jf.getEntry(internalPath);
                
                if(zf == null) {
                    throw getRuntime().newErrnoENOENTError(
                                                           "File not found - " + newPath);
                }

                InputStream is = jf.getInputStream(zf);
                openFile.setMainStream(
                        new ChannelStream(getRuntime(), new ChannelDescriptor(Channels.newChannel(is), getNewFileno(), new FileDescriptor())));
            } else {
                try {
                    openFile.setMainStream(ChannelStream.fopen(getRuntime(), newPath, newModes));
                } catch (BadDescriptorException e) {
                    throw getRuntime().newErrnoEBADFError();
                } catch (FileExistsException fee) {
                    throw getRuntime().newErrnoEEXISTError(fee.getMessage());
                }
            }
            
            registerDescriptor(openFile.getMainStream().getDescriptor());
        } catch (PipeException e) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (DirectoryAsFileException e) {
            throw getRuntime().newErrnoEISDirError();
        } catch (FileNotFoundException e) {
            // FNFException can be thrown in both cases, when the file
            // is not found, or when permission is denied.
            if (Ruby.isSecurityRestricted() || new File(newPath).exists()) {
                throw getRuntime().newErrnoEACCESError(
                        "Permission denied - " + newPath);
            }
            throw getRuntime().newErrnoENOENTError(
                    "File not found - " + newPath);
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        } catch (SecurityException se) {
            throw getRuntime().newIOError(se.getMessage());
        }
    }
    
    @JRubyMethod
    public IRubyObject close() {
        // Make sure any existing lock is released before we try and close the file
        if (currentLock != null) {
            try {
                currentLock.release();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        return super.close();
    }

    @JRubyMethod(required = 1)
    public IRubyObject flock(IRubyObject lockingConstant) {
        assert openFile.getMainStream().getDescriptor().isSeekable();
        
        FileChannel fileChannel = (FileChannel)openFile.getMainStream().getDescriptor().getChannel();
        int lockMode = RubyNumeric.num2int(lockingConstant);

        // Exclusive locks in Java require the channel to be writable, otherwise
        // an exception is thrown (terminating JRuby execution).
        // But flock behavior of MRI is that it allows
        // exclusive locks even on non-writable file. So we convert exclusive
        // lock to shared lock if the channel is not writable, to better match
        // the MRI behavior.
        if (!openFile.isWritable() && (lockMode & LOCK_EX) > 0) {
            lockMode = (lockMode ^ LOCK_EX) | LOCK_SH;
        }

        try {
            switch (lockMode) {
                case LOCK_UN:
                case LOCK_UN | LOCK_NB:
                    if (currentLock != null) {
                        currentLock.release();
                        currentLock = null;

                        return getRuntime().newFixnum(0);
                    }
                    break;
                case LOCK_EX:
                    if (currentLock != null) {
                        currentLock.release();
                        currentLock = null;
                    }
                    currentLock = fileChannel.lock();
                    if (currentLock != null) {
                        return getRuntime().newFixnum(0);
                    }

                    break;
                case LOCK_EX | LOCK_NB:
                    if (currentLock != null) {
                        currentLock.release();
                        currentLock = null;
                    }
                    currentLock = fileChannel.tryLock();
                    if (currentLock != null) {
                        return getRuntime().newFixnum(0);
                    }

                    break;
                case LOCK_SH:
                    if (currentLock != null) {
                        currentLock.release();
                        currentLock = null;
                    }

                    currentLock = fileChannel.lock(0L, Long.MAX_VALUE, true);
                    if (currentLock != null) {
                        return getRuntime().newFixnum(0);
                    }

                    break;
                case LOCK_SH | LOCK_NB:
                    if (currentLock != null) {
                        currentLock.release();
                        currentLock = null;
                    }

                    currentLock = fileChannel.tryLock(0L, Long.MAX_VALUE, true);
                    if (currentLock != null) {
                        return getRuntime().newFixnum(0);
                    }

                    break;
                default:
            }
        } catch (IOException ioe) {
            if (getRuntime().getDebug().isTrue()) {
                ioe.printStackTrace(System.err);
            }
            // Return false here
        } catch (java.nio.channels.OverlappingFileLockException ioe) {
            if (getRuntime().getDebug().isTrue()) {
                ioe.printStackTrace(System.err);
            }
            // Return false here
        }

        return getRuntime().getFalse();
    }

    @JRubyMethod(required = 1, optional = 2, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (openFile == null) {
            throw getRuntime().newRuntimeError("reinitializing File");
        }
        
        if (args.length > 0 && args.length < 3) {
            IRubyObject fd = TypeConverter.convertToTypeWithCheck(args[0], getRuntime().getFixnum(), MethodIndex.TO_INT, "to_int");
            if (!fd.isNil()) {
                args[0] = fd;
                return super.initialize(args, block);
            }
        }

        return openFile(args);
    }
    
    private IRubyObject openFile(IRubyObject args[]) {
        IRubyObject filename = args[0].convertToString();
        getRuntime().checkSafeString(filename);
        
        path = filename.toString();
        
        String modeString;
        ModeFlags modes;
        int perm;
        
        try {
            if ((args.length > 1 && args[1] instanceof RubyFixnum) || (args.length > 2 && !args[2].isNil())) {
                if (args[1] instanceof RubyFixnum) {
                    modes = new ModeFlags(RubyNumeric.num2int(args[1]));
                } else {
                    modeString = args[1].convertToString().toString();
                    modes = getIOModes(getRuntime(), modeString);
                }
                if (args.length > 2 && !args[2].isNil()) {
                    perm = RubyNumeric.num2int(args[2]);
                } else {
                    perm = 438; // 0666
                }

                sysopenInternal(path, modes, perm);
            } else {
                modeString = "r";
                if (args.length > 1) {
                    if (!args[1].isNil()) {
                        modeString = args[1].convertToString().toString();
                    }
                }
                openInternal(path, modeString);
            }
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } finally {}
        
        return this;
    }
    
    private void sysopenInternal(String path, ModeFlags modes, int perm) throws InvalidValueException {
        openFile = new OpenFile();
        
        openFile.setPath(path);
        openFile.setMode(modes.getOpenFileFlags());
        
        ChannelDescriptor descriptor = sysopen(path, modes, perm);
        openFile.setMainStream(fdopen(descriptor, modes));
        
        registerDescriptor(descriptor);
    }
    
    private void openInternal(String path, String modeString) throws InvalidValueException {
        openFile = new OpenFile();

        openFile.setMode(getIOModes(getRuntime(), modeString).getOpenFileFlags());
        openFile.setPath(path);
        openFile.setMainStream(fopen(path, modeString));
        
        registerDescriptor(openFile.getMainStream().getDescriptor());
    }
    
    private ChannelDescriptor sysopen(String path, ModeFlags modes, int perm) throws InvalidValueException {
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.open(
                    getRuntime().getCurrentDirectory(),
                    path,
                    modes,
                    perm,
                    getRuntime().getPosix());

            // TODO: check if too many open files, GC and try again

            return descriptor;
        } catch (FileNotFoundException fnfe) {
            throw getRuntime().newErrnoENOENTError();
        } catch (DirectoryAsFileException dafe) {
            throw getRuntime().newErrnoEISDirError();
        } catch (FileExistsException fee) {
            throw getRuntime().newErrnoEEXISTError("file exists: " + path);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }
    
    private Stream fopen(String path, String modeString) {
        try {
            Stream stream = ChannelStream.fopen(
                    getRuntime(),
                    path,
                    getIOModes(getRuntime(), modeString));
            
            if (stream == null) {
                // TODO
    //            if (errno == EMFILE || errno == ENFILE) {
    //                rb_gc();
    //                file = fopen(fname, mode);
    //            }
    //            if (!file) {
    //                rb_sys_fail(fname);
    //            }
            }

            // Do we need to be in SETVBUF mode for buffering to make sense? This comes up elsewhere.
    //    #ifdef USE_SETVBUF
    //        if (setvbuf(file, NULL, _IOFBF, 0) != 0)
    //            rb_warn("setvbuf() can't be honoured for %s", fname);
    //    #endif
    //    #ifdef __human68k__
    //        fmode(file, _IOTEXT);
    //    #endif
            return stream;
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (FileNotFoundException ex) {
            throw getRuntime().newErrnoENOENTError();
        } catch (DirectoryAsFileException ex) {
            throw getRuntime().newErrnoEISDirError();
        } catch (FileExistsException ex) {
            throw getRuntime().newErrnoEEXISTError(path);
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        }
    }

    @JRubyMethod(required = 1)
    public IRubyObject chmod(IRubyObject arg) {
        int mode = (int) arg.convertToInteger().getLongValue();

        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }

        return getRuntime().newFixnum(getRuntime().getPosix().chmod(path, mode));
    }

    @JRubyMethod(required = 2)
    public IRubyObject chown(IRubyObject arg1, IRubyObject arg2) {
        int owner = (int) arg1.convertToInteger().getLongValue();
        int group = (int) arg2.convertToInteger().getLongValue();
        
        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }

        return getRuntime().newFixnum(getRuntime().getPosix().chown(path, owner, group));
    }

    @JRubyMethod
    public IRubyObject atime() {
        return getRuntime().newFileStat(path, false).atime();
    }

    @JRubyMethod
    public IRubyObject ctime() {
        return getRuntime().newFileStat(path, false).ctime();
    }

    @JRubyMethod(required = 1)
    public IRubyObject lchmod(IRubyObject arg) {
        int mode = (int) arg.convertToInteger().getLongValue();

        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }

        return getRuntime().newFixnum(getRuntime().getPosix().lchmod(path, mode));
    }

    @JRubyMethod(required = 2)
    public IRubyObject lchown(IRubyObject arg1, IRubyObject arg2) {
        int owner = (int) arg1.convertToInteger().getLongValue();
        int group = (int) arg2.convertToInteger().getLongValue();
        
        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }

        return getRuntime().newFixnum(getRuntime().getPosix().lchown(path, owner, group));
    }

    @JRubyMethod
    public IRubyObject lstat() {
        return getRuntime().newFileStat(path, true);
    }
    
    @JRubyMethod
    public IRubyObject mtime() {
        return getLastModified(getRuntime(), path);
    }

    @JRubyMethod
    public RubyString path() {
        return getRuntime().newString(path);
    }

    @JRubyMethod
    public IRubyObject stat() {
        openFile.checkClosed(getRuntime());
        return getRuntime().newFileStat(path, false);
    }

    @JRubyMethod(required = 1)
    public IRubyObject truncate(IRubyObject arg) {
        RubyInteger newLength = arg.convertToInteger();
        if (newLength.getLongValue() < 0) {
            throw getRuntime().newErrnoEINVALError("invalid argument: " + path);
        }
        try {
            openFile.checkWritable(getRuntime());
            openFile.getMainStream().ftruncate(newLength.getLongValue());
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (PipeException e) {
            throw getRuntime().newErrnoESPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (IOException e) {
            // Should we do anything?
        }

        return RubyFixnum.zero(getRuntime());
    }

    public String toString() {
        return "RubyFile(" + path + ", " + openFile.getMode() + ", " + openFile.getMainStream().getDescriptor().getFileno() + ")";
    }

    // TODO: This is also defined in the MetaClass too...Consolidate somewhere.
    private static ModeFlags getModes(Ruby runtime, IRubyObject object) throws InvalidValueException {
        if (object instanceof RubyString) {
            return getIOModes(runtime, ((RubyString) object).toString());
        } else if (object instanceof RubyFixnum) {
            return new ModeFlags(((RubyFixnum) object).getLongValue());
        }

        throw runtime.newTypeError("Invalid type for modes");
    }

    @JRubyMethod
    public IRubyObject inspect() {
        StringBuffer val = new StringBuffer();
        val.append("#<File:").append(path);
        if(!openFile.isOpen()) {
            val.append(" (closed)");
        }
        val.append(">");
        return getRuntime().newString(val.toString());
    }
    
    /* File class methods */
    
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject basename(IRubyObject recv, IRubyObject[] args) {
        String name = RubyString.stringValue(args[0]).toString();

        // MRI-compatible basename handling for windows drive letter paths
        if (IS_WINDOWS) {
            if (name.length() > 1 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
                switch (name.length()) {
                case 2:
                    return recv.getRuntime().newString("").infectBy(args[0]);
                case 3:
                    return recv.getRuntime().newString(name.substring(2)).infectBy(args[0]);
                default:
                    switch (name.charAt(2)) {
                    case '/':
                    case '\\':
                        break;
                    default:
                        // strip c: away from relative-pathed name
                        name = name.substring(2);
                        break;
                    }
                    break;
                }
            }
        }

        while (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
            name = name.substring(0, name.length() - 1);
        }
        
        // Paths which end in "/" or "\\" must be stripped off.
        int slashCount = 0;
        int length = name.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (c != '/' && c != '\\') {
                break;
            }
            slashCount++;
        }
        if (slashCount > 0 && length > 1) {
            name = name.substring(0, name.length() - slashCount);
        }
        
        int index = name.lastIndexOf('/');
        if (index == -1) {
            // XXX actually only on windows...
            index = name.lastIndexOf('\\');
        }
        
        if (!name.equals("/") && index != -1) {
            name = name.substring(index + 1);
        }
        
        if (args.length == 2) {
            String ext = RubyString.stringValue(args[1]).toString();
            if (".*".equals(ext)) {
                index = name.lastIndexOf('.');
                if (index > 0) {  // -1 no match; 0 it is dot file not extension
                    name = name.substring(0, index);
                }
            } else if (name.endsWith(ext)) {
                name = name.substring(0, name.length() - ext.length());
            }
        }
        return recv.getRuntime().newString(name).infectBy(args[0]);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject chmod(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            boolean result = 0 == runtime.getPosix().chmod(filename.toString(), (int)mode.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 3, rest = true, meta = true)
    public static IRubyObject chown(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        int count = 0;
        RubyInteger owner = args[0].convertToInteger();
        RubyInteger group = args[1].convertToInteger();
        for (int i = 2; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            boolean result = 0 == runtime.getPosix().chown(filename.toString(), (int)owner.getLongValue(), (int)group.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject dirname(IRubyObject recv, IRubyObject arg) {
        RubyString filename = RubyString.stringValue(arg);
        String jfilename = filename.toString();
        String name = jfilename.replace('\\', '/');
        int minPathLength = 1;
        boolean trimmedSlashes = false;

        boolean startsWithDriveLetterOnWindows
                = startsWithDriveLetterOnWindows(name);

        if (startsWithDriveLetterOnWindows) {
            minPathLength = 3;
        }

        while (name.length() > minPathLength && name.charAt(name.length() - 1) == '/') {
            trimmedSlashes = true;
            name = name.substring(0, name.length() - 1);
        }

        String result;
        if (startsWithDriveLetterOnWindows && name.length() == 2) {
            if (trimmedSlashes) {
                // C:\ is returned unchanged
                result = jfilename.substring(0, 3);
            } else {
                result = jfilename.substring(0, 2) + '.';
            }
        } else {
            //TODO deal with UNC names
            int index = name.lastIndexOf('/');
            if (index == -1) {
                if (startsWithDriveLetterOnWindows) {
                    return recv.getRuntime().newString(
                            jfilename.substring(0, 2) + ".");
                } else {
                    return recv.getRuntime().newString(".");
                }
            }
            if (index == 0) return recv.getRuntime().newString("/");

            if (startsWithDriveLetterOnWindows && index == 2) {
                // Include additional path separator
                // (so that dirname of "C:\file.txt" is  "C:\", not "C:")
                index++;
            }

            result = jfilename.substring(0, index);
        }

        char endChar;
        // trim trailing slashes
        while (result.length() > minPathLength) {
            endChar = result.charAt(result.length() - 1);
            if (endChar == '/' || endChar == '\\') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }

        return recv.getRuntime().newString(result).infectBy(filename);
    }

    private static boolean isWindowsDriveLetter(char c) {
           return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }


    /**
     * Returns the extension name of the file. An empty string is returned if 
     * the filename (not the entire path) starts or ends with a dot.
     * @param recv
     * @param arg Path to get extension name of
     * @return Extension, including the dot, or an empty string
     */
    @JRubyMethod(required = 1, meta = true)
           public static IRubyObject extname(IRubyObject recv, IRubyObject arg) {
                   IRubyObject baseFilename = basename(recv, new IRubyObject[] { arg });
                   String filename = RubyString.stringValue(baseFilename).toString();
                   String result = "";

                   int dotIndex = filename.lastIndexOf(".");
                   if (dotIndex > 0  && dotIndex != (filename.length() - 1)) {
                           // Dot is not at beginning and not at end of filename. 
                           result = filename.substring(dotIndex);
                   }

                   return recv.getRuntime().newString(result);
           }

    /**
     * Converts a pathname to an absolute pathname. Relative paths are 
     * referenced from the current working directory of the process unless 
     * a second argument is given, in which case it will be used as the 
     * starting point. If the second argument is also relative, it will 
     * first be converted to an absolute pathname.
     * @param recv
     * @param args 
     * @return Resulting absolute path as a String
     */
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject expand_path(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        String relativePath = RubyString.stringValue(args[0]).toString();

        boolean isAbsoluteWithFilePrefix = relativePath.startsWith("file:");

        String cwd = null;
        
        // Handle ~user paths 
        relativePath = expandUserPath(recv, relativePath);
        
        // If there's a second argument, it's the path to which the first 
        // argument is relative.
        if (args.length == 2 && !args[1].isNil()) {
            
            String cwdArg = RubyString.stringValue(args[1]).toString();
            
            // Handle ~user paths.
            cwd = expandUserPath(recv, cwdArg);

            cwd = adjustRootPathOnWindows(runtime, cwd, null);

            boolean startsWithSlashNotOnWindows = (cwd != null)
                    && !IS_WINDOWS && cwd.length() > 0
                    && cwd.charAt(0) == '/';

            // TODO: better detection when path is absolute or not.
            // If the path isn't absolute, then prepend the current working
            // directory to the path.
            if (!startsWithSlashNotOnWindows && !startsWithDriveLetterOnWindows(cwd)) {
                cwd = new File(runtime.getCurrentDirectory(), cwd)
                    .getAbsolutePath();
            }
        } else {
            // If there's no second argument, simply use the working directory 
            // of the runtime.
            cwd = runtime.getCurrentDirectory();
        }
        
        // Something wrong we don't know the cwd...
        // TODO: Is this behavior really desirable? /mov
        if (cwd == null) return runtime.getNil();
        
        /* The counting of slashes that follows is simply a way to adhere to 
         * Ruby's UNC (or something) compatibility. When Ruby's expand_path is 
         * called with "//foo//bar" it will return "//foo/bar". JRuby uses 
         * java.io.File, and hence returns "/foo/bar". In order to retain 
         * java.io.File in the lower layers and provide full Ruby 
         * compatibility, the number of extra slashes must be counted and 
         * prepended to the result.
         */ 

        // TODO: special handling on windows for some corner cases
//        if (IS_WINDOWS) {
//            if (relativePath.startsWith("//")) {
//                if (relativePath.length() > 2 && relativePath.charAt(2) != '/') {
//                    int nextSlash = relativePath.indexOf('/', 3);
//                    if (nextSlash != -1) {
//                        return runtime.newString(
//                                relativePath.substring(0, nextSlash)
//                                + canonicalize(relativePath.substring(nextSlash)));
//                    } else {
//                        return runtime.newString(relativePath);
//                    }
//                }
//            }
//        }

        // Find out which string to check.
        String padSlashes = "";
        if (!IS_WINDOWS) {
            if (relativePath.length() > 0 && relativePath.charAt(0) == '/') {
                padSlashes = countSlashes(relativePath);
            } else if (cwd.length() > 0 && cwd.charAt(0) == '/') {
                padSlashes = countSlashes(cwd);
            }
        }
        
        JRubyFile path;
        
        if (relativePath.length() == 0) {
            path = JRubyFile.create(relativePath, cwd);
        } else {
            relativePath = adjustRootPathOnWindows(runtime, relativePath, cwd);
            path = JRubyFile.create(cwd, relativePath);
        }
        
        String tempResult = padSlashes + canonicalize(path.getAbsolutePath());

        if(isAbsoluteWithFilePrefix) {
            tempResult = tempResult.substring(tempResult.indexOf("file:"));
        }

        return runtime.newString(tempResult);
    }
    
    /**
     * This method checks a path, and if it starts with ~, then it expands 
     * the path to the absolute path of the user's home directory. If the 
     * string does not begin with ~, then the string is simply retuned 
     * unaltered.
     * @param recv
     * @param path Path to check
     * @return Expanded path
     */
    private static String expandUserPath( IRubyObject recv, String path ) {
        
        int pathLength = path.length();

        if (pathLength >= 1 && path.charAt(0) == '~') {
            // Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
            int userEnd = path.indexOf('/');
            
            if (userEnd == -1) {
                if (pathLength == 1) {
                    // Single '~' as whole path to expand
                    path = RubyDir.getHomeDirectoryPath(recv).toString();
                } else {
                    // No directory delimeter.  Rest of string is username
                    userEnd = pathLength;
                }
            }
            
            if (userEnd == 1) {
                // '~/...' as path to expand
                path = RubyDir.getHomeDirectoryPath(recv).toString() +
                        path.substring(1);
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = path.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(recv, user);
                
                if (dir.isNil()) {
                    Ruby runtime = recv.getRuntime();
                    throw runtime.newArgumentError("user " + user + " does not exist");
                }
                
                path = "" + dir +
                        (pathLength == userEnd ? "" : path.substring(userEnd));
            }
        }
        return path;
    }
    
    /**
     * Returns a string consisting of <code>n-1</code> slashes, where 
     * <code>n</code> is the number of slashes at the beginning of the input 
     * string.
     * @param stringToCheck
     * @return
     */
    private static String countSlashes( String stringToCheck ) {
        
        // Count number of extra slashes in the beginning of the string.
        int slashCount = 0;
        for (int i = 0; i < stringToCheck.length(); i++) {
            if (stringToCheck.charAt(i) == '/') {
                slashCount++;
            } else {
                break;
            }
        }

        // If there are N slashes, then we want N-1.
        if (slashCount > 0) {
            slashCount--;
        }
        
        // Prepare a string with the same number of redundant slashes so that 
        // we easily can prepend it to the result.
        byte[] slashes = new byte[slashCount];
        for (int i = 0; i < slashCount; i++) {
            slashes[i] = '/';
        }
        return new String(slashes); 
        
    }

    private static String canonicalize(String path) {
        return canonicalize(null, path);
    }

    private static String canonicalize(String canonicalPath, String remaining) {
        if (remaining == null) {
            if ("".equals(canonicalPath)) {
                return "/";
            } else {
                // compensate for missing slash after drive letter on windows
                if (startsWithDriveLetterOnWindows(canonicalPath)
                        && canonicalPath.length() == 2) {
                    canonicalPath += "/";
                }
            }
            return canonicalPath;
        }

        String child;
        int slash = remaining.indexOf('/');
        if (slash == -1) {
            child = remaining;
            remaining = null;
        } else {
            child = remaining.substring(0, slash);
            remaining = remaining.substring(slash + 1);
        }

        if (child.equals(".")) {
            // skip it
            if (canonicalPath != null && canonicalPath.length() == 0 ) canonicalPath += "/";
        } else if (child.equals("..")) {
            if (canonicalPath == null) throw new IllegalArgumentException("Cannot have .. at the start of an absolute path");
            int lastDir = canonicalPath.lastIndexOf('/');
            if (lastDir == -1) {
                if (startsWithDriveLetterOnWindows(canonicalPath)) {
                   // do nothing, we should not delete the drive letter
                } else {
                    canonicalPath = "";
                }
            } else {
                canonicalPath = canonicalPath.substring(0, lastDir);
            }
        } else if (canonicalPath == null) {
            canonicalPath = child;
        } else {
            canonicalPath += "/" + child;
        }

        return canonicalize(canonicalPath, remaining);
    }

    /**
     * Returns true if path matches against pattern The pattern is not a regular expression;
     * instead it follows rules similar to shell filename globbing. It may contain the following
     * metacharacters:
     *   *:  Glob - match any sequence chars (re: .*).  If like begins with '.' then it doesn't.
     *   ?:  Matches a single char (re: .).
     *   [set]:  Matches a single char in a set (re: [...]).
     *
     */
    @JRubyMethod(name = {"fnmatch", "fnmatch?"}, required = 2, optional = 1, meta = true)
    public static IRubyObject fnmatch(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int flags;
        if (args.length == 3) {
            flags = RubyNumeric.num2int(args[2]);
        } else {
            flags = 0;
        }

        ByteList pattern = args[0].convertToString().getByteList();
        ByteList path = args[1].convertToString().getByteList();

        if (org.jruby.util.Dir.fnmatch(pattern.bytes, pattern.begin, pattern.begin+pattern.realSize, 
                                       path.bytes, path.begin, path.begin+path.realSize, flags) == 0) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }
    
    @JRubyMethod(name = "ftype", required = 1, meta = true)
    public static IRubyObject ftype(IRubyObject recv, IRubyObject filename) {
        return recv.getRuntime().newFileStat(filename.convertToString().toString(), true).ftype();
    }

    private static String inspectJoin(IRubyObject recv, RubyArray parent, RubyArray array) {
        Ruby runtime = recv.getRuntime();

        // If already inspecting, there is no need to register/unregister again.
        if (runtime.isInspecting(parent)) {
            return join(recv, array).toString();
        }

        try {
            runtime.registerInspecting(parent);
            return join(recv, array).toString();
        } finally {
            runtime.unregisterInspecting(parent);
        }
    }

    private static RubyString join(IRubyObject recv, RubyArray ary) {
        IRubyObject[] args = ary.toJavaArray();
        boolean isTainted = false;
        StringBuffer buffer = new StringBuffer();
        Ruby runtime = recv.getRuntime();
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].isTaint()) {
                isTainted = true;
            }
            String element;
            if (args[i] instanceof RubyString) {
                element = args[i].toString();
            } else if (args[i] instanceof RubyArray) {
                if (runtime.isInspecting(args[i])) {
                    element = "[...]";
                } else {
                    element = inspectJoin(recv, ary, ((RubyArray)args[i]));
                }
            } else {
                element = args[i].convertToString().toString();
            }
            
            chomp(buffer);
            if (i > 0 && !element.startsWith("/") && !element.startsWith("\\")) {
                buffer.append("/");
            }
            buffer.append(element);
        }
        
        RubyString fixedStr = RubyString.newString(recv.getRuntime(), buffer.toString());
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }
    
    /*
     * Fixme:  This does not have exact same semantics as RubyArray.join, but they
     * probably could be consolidated (perhaps as join(args[], sep, doChomp)).
     */
    @JRubyMethod(rest = true, meta = true)
    public static RubyString join(IRubyObject recv, IRubyObject[] args) {
        return join(recv, RubyArray.newArrayNoCopyLight(recv.getRuntime(), args));
    }

    private static void chomp(StringBuffer buffer) {
        int lastIndex = buffer.length() - 1;
        
        while (lastIndex >= 0 && (buffer.lastIndexOf("/") == lastIndex || buffer.lastIndexOf("\\") == lastIndex)) {
            buffer.setLength(lastIndex);
            lastIndex--;
        }
    }
    
    @JRubyMethod(name = "lstat", required = 1, meta = true)
    public static IRubyObject lstat(IRubyObject recv, IRubyObject filename) {
        String f = filename.convertToString().toString();
        if(f.startsWith("file:")) {
            f = f.substring(5, f.indexOf("!"));
        }
        return recv.getRuntime().newFileStat(f, true);
    }

    @JRubyMethod(name = "stat", required = 1, meta = true)
    public static IRubyObject stat(IRubyObject recv, IRubyObject filename) {
        String f = filename.convertToString().toString();
        if(f.startsWith("file:")) {
            f = f.substring(5, f.indexOf("!"));
        }
        return recv.getRuntime().newFileStat(f, false);
    }

    @JRubyMethod(name = "atime", required = 1, meta = true)
    public static IRubyObject atime(IRubyObject recv, IRubyObject filename) {
        String f = filename.convertToString().toString();
        if(f.startsWith("file:")) {
            f = f.substring(5, f.indexOf("!"));
        }
        return recv.getRuntime().newFileStat(f, false).atime();
    }

    @JRubyMethod(name = "ctime", required = 1, meta = true)
    public static IRubyObject ctime(IRubyObject recv, IRubyObject filename) {
        String f = filename.convertToString().toString();
        if(f.startsWith("file:")) {
            f = f.substring(5, f.indexOf("!"));
        }
        return recv.getRuntime().newFileStat(f, false).ctime();
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject lchmod(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            boolean result = 0 == runtime.getPosix().lchmod(filename.toString(), (int)mode.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 3, rest = true, meta = true)
    public static IRubyObject lchown(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        int count = 0;
        RubyInteger owner = args[0].convertToInteger();
        RubyInteger group = args[1].convertToInteger();
        for (int i = 2; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            boolean result = 0 == runtime.getPosix().lchown(filename.toString(), (int)owner.getLongValue(), (int)group.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject link(IRubyObject recv, IRubyObject from, IRubyObject to) {
        RubyString fromStr = RubyString.stringValue(from);
        RubyString toStr = RubyString.stringValue(to);
        try {
            if (recv.getRuntime().getPosix().link(
                    fromStr.toString(),toStr.toString()) == -1) {
                // FIXME: When we get JNA3 we need to properly write this to errno.
                throw recv.getRuntime().newErrnoEEXISTError("File exists - " 
                               + fromStr + " or " + toStr);
            }
        } catch (java.lang.UnsatisfiedLinkError ule) {
            throw recv.getRuntime().newNotImplementedError(
                    "link() function is unimplemented on this machine");
        }
        
        return recv.getRuntime().newFixnum(0);
    }

    @JRubyMethod(name = "mtime", required = 1, meta = true)
    public static IRubyObject mtime(IRubyObject recv, IRubyObject filename) {
        return getLastModified(recv.getRuntime(), filename.convertToString().toString());
    }
    
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, boolean tryToYield, Block block) {
        Ruby runtime = recv.getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        
        RubyFile file;

        if (args[0] instanceof RubyInteger) { // open with file descriptor
            file = new RubyFile(runtime, (RubyClass) recv);
            file.initialize(args, Block.NULL_BLOCK);            
        } else {
            RubyString pathString = RubyString.stringValue(args[0]);
            runtime.checkSafeString(pathString);
            String path = pathString.toString();

            ModeFlags modes;
            try {
                modes = args.length >= 2 ? getModes(runtime, args[1]) : new ModeFlags(ModeFlags.RDONLY);
            } catch (InvalidValueException ex) {
                throw runtime.newErrnoEINVALError();
            }
            file = new RubyFile(runtime, (RubyClass) recv);

            RubyInteger fileMode = args.length >= 3 ? args[2].convertToInteger() : null;

            file.openInternal(path, modes);

            if (fileMode != null) chmod(recv, new IRubyObject[] {fileMode, pathString});
        }
        
        if (tryToYield && block.isGiven()) {
            try {
                return block.yield(tc, file);
            } finally {
                if (file.openFile.isOpen()) {
                    file.close();
                }
            }
        }
        
        return file;
    }

    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject rename(IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        Ruby runtime = recv.getRuntime();
        RubyString oldNameString = RubyString.stringValue(oldName);
        RubyString newNameString = RubyString.stringValue(newName);
        runtime.checkSafeString(oldNameString);
        runtime.checkSafeString(newNameString);
        JRubyFile oldFile = JRubyFile.create(runtime.getCurrentDirectory(), oldNameString.toString());
        JRubyFile newFile = JRubyFile.create(runtime.getCurrentDirectory(), newNameString.toString());
        
        if (!oldFile.exists() || !newFile.getParentFile().exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + oldNameString + 
                    " or " + newNameString);
        }

        JRubyFile dest = JRubyFile.create(runtime.getCurrentDirectory(), newNameString.toString());

        if (oldFile.renameTo(dest)) {  // rename is successful
            return RubyFixnum.zero(runtime);
        }

        // rename via Java API call wasn't successful, let's try some tricks, similar to MRI 

        if (newFile.exists()) {
            recv.getRuntime().getPosix().chmod(newNameString.toString(), 0666);
            newFile.delete();
        }

        if (oldFile.renameTo(dest)) { // try to rename one more time
            return RubyFixnum.zero(runtime);
        }

        throw runtime.newErrnoEACCESError("Permission denied - " + oldNameString + " or " + 
                newNameString);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static RubyArray split(IRubyObject recv, IRubyObject arg) {
        RubyString filename = RubyString.stringValue(arg);
        
        return filename.getRuntime().newArray(dirname(recv, filename),
                basename(recv, new IRubyObject[] { filename }));
    }
    
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject symlink(IRubyObject recv, IRubyObject from, IRubyObject to) {
        RubyString fromStr = RubyString.stringValue(from);
        RubyString toStr = RubyString.stringValue(to);
        try {
            if (recv.getRuntime().getPosix().symlink(
                    fromStr.toString(), toStr.toString()) == -1) {
                // FIXME: When we get JNA3 we need to properly write this to errno.
                throw recv.getRuntime().newErrnoEEXISTError("File exists - " 
                               + fromStr + " or " + toStr);
            }
        } catch (java.lang.UnsatisfiedLinkError ule) {
            throw recv.getRuntime().newNotImplementedError(
                    "symlink() function is unimplemented on this machine");
        }
        
        return recv.getRuntime().newFixnum(0);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject readlink(IRubyObject recv, IRubyObject path) {
        try {
            String realPath = recv.getRuntime().getPosix().readlink(path.toString());
        
            if (!RubyFileTest.exist_p(recv, path).isTrue()) {
                throw recv.getRuntime().newErrnoENOENTError("No such file or directory - " + path);
            }
        
            if (!RubyFileTest.symlink_p(recv, path).isTrue()) {
                throw recv.getRuntime().newErrnoEINVALError("invalid argument - " + path);
            }
        
            if (realPath == null) {
                //FIXME: When we get JNA3 we need to properly write this to errno.
            }

            return recv.getRuntime().newString(realPath);
        } catch (IOException e) {
            throw recv.getRuntime().newIOError(e.getMessage());
        }
    }

    // Can we produce IOError which bypasses a close?
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject truncate(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = recv.getRuntime();
        RubyString filename = arg1.convertToString(); // TODO: SafeStringValue here
        RubyInteger newLength = arg2.convertToInteger(); 
        
        if (!new File(filename.getByteList().toString()).exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + filename.getByteList().toString());
        }

        if (newLength.getLongValue() < 0) {
            throw runtime.newErrnoEINVALError("invalid argument: " + filename);
        }
        
        IRubyObject[] args = new IRubyObject[] { filename, runtime.newString("r+") };
        RubyFile file = (RubyFile) open(recv, args, false, null);
        file.truncate(newLength);
        file.close();
        
        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod(meta = true, optional = 1)
    public static IRubyObject umask(IRubyObject recv, IRubyObject[] args) {
        int oldMask = 0;
        if (args.length == 0) {
            oldMask = recv.getRuntime().getPosix().umask(0);
        } else if (args.length == 1) {
            oldMask = recv.getRuntime().getPosix().umask((int) args[0].convertToInteger().getLongValue()); 
        } else {
            recv.getRuntime().newArgumentError("wrong number of arguments");
        }
        
        return recv.getRuntime().newFixnum(oldMask);
    }

    /**
     * This method does NOT set atime, only mtime, since Java doesn't support anything else.
     */
    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject utime(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        // Ignore access_time argument since Java does not support it.
        
        long mtime;
        if (args[1] instanceof RubyTime) {
            mtime = ((RubyTime) args[1]).getJavaDate().getTime();
        } else if (args[1] instanceof RubyNumeric) {
            mtime = RubyNumeric.num2long(args[1]);
        } else if (args[1] == runtime.getNil()) {
            mtime = System.currentTimeMillis();
        } else {
            RubyTime time = (RubyTime) TypeConverter.convertToType(args[1], runtime.getTime(), MethodIndex.NO_INDEX,"to_time", true);
            mtime = time.getJavaDate().getTime();
        }
        
        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = RubyString.stringValue(args[i]);
            runtime.checkSafeString(filename);
            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(),filename.toString());
            
            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(" No such file or directory - \"" + filename + "\"");
            }
            
            fileToTouch.setLastModified(mtime);
        }
        
        return runtime.newFixnum(args.length - 2);
    }
    
    @JRubyMethod(name = {"unlink", "delete"}, rest = true, meta = true)
    public static IRubyObject unlink(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        for (int i = 0; i < args.length; i++) {
            RubyString filename = RubyString.stringValue(args[i]);
            runtime.checkSafeString(filename);
            JRubyFile lToDelete = JRubyFile.create(runtime.getCurrentDirectory(),filename.toString());
            
            boolean isSymlink = RubyFileTest.symlink_p(recv, filename).isTrue();
            // Broken symlinks considered by exists() as non-existing,
            // so we need to check for symlinks explicitly.
            if (!lToDelete.exists() && !isSymlink) {
                throw runtime.newErrnoENOENTError(" No such file or directory - \"" + filename + "\"");
            }
            
            if (!lToDelete.delete()) return runtime.getFalse();
        }
        
        return runtime.newFixnum(args.length);
    }

    // Fast path since JNA stat is about 10x slower than this
    private static IRubyObject getLastModified(Ruby runtime, String path) {
        JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), path);
        
        if (!file.exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + path);
        }
        
        return runtime.newTime(file.lastModified());
    }
}
