/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005-2008 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
 * Copyright (C) 2006 Dave Brosius <dbrosius@mebigfatguy.com>
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
package org.jruby.util.io;

import jnr.constants.platform.OpenFlags;
import org.jruby.Ruby;

/**
 * This file represents the POSIX-like mode flags an open channel (as in a
 * ChannelDescriptor) can have. It provides the basic flags for read/write as
 * well as flags for create, truncate, and others. In addition, it provides
 * methods for querying specific flag settings and converting to two other
 * formats: a Java mode string and an OpenFile mode int.
 * 
 * Note: In MRI these are called oflags.
 * 
 * @see org.jruby.util.io.ChannelDescriptor
 * @see org.jruby.util.io.Stream
 * @see org.jruby.util.io.OpenFile
 */
public class ModeFlags implements Cloneable {
    /** read-only flag (default value if no other flags set) */
    public static final int RDONLY = OpenFlags.O_RDONLY.intValue();
    /** write-only flag */
    public static final int WRONLY = OpenFlags.O_WRONLY.intValue();
    /** read/write flag */
    public static final int RDWR = OpenFlags.O_RDWR.intValue();
    /** create flag, to specify non-existing file should be created */
    public static final int CREAT = OpenFlags.O_CREAT.intValue();
    /** exclusive access flag, to require locking the target file */
    public static final int EXCL = OpenFlags.O_EXCL.intValue();
    /** truncate flag, to truncate the target file to zero length */
    public static final int TRUNC = OpenFlags.O_TRUNC.intValue();
    /** append flag, to seek to the end of the file */
    public static final int APPEND = OpenFlags.O_APPEND.intValue();
    /** nonblock flag, to perform all operations non-blocking. Unused currently */
    public static final int NONBLOCK = OpenFlags.O_NONBLOCK.intValue();
    /** binary flag, to ensure no encoding changes are made while writing */
    public static final int BINARY = OpenFlags.O_BINARY.intValue();
    /** textmode flag, MRI has no equivalent but we use ModeFlags currently
     * to also capture what are oflags.
     */
    public static final int TEXT = 0x10000000; 
    /** accmode flag, used to mask the read/write mode */
    public static final int ACCMODE = RDWR | WRONLY | RDONLY;
    
    /** the set of flags for this ModeFlags instance */
    private final int flags;
    
    /**
     * Construct a new ModeFlags object with the default read-only flag.
     */
    public ModeFlags() {
    	flags = RDONLY;
    }
    
    /**
     * Construct a new ModeFlags object with the specified flags
     * 
     * @param flags The flags to use for this object
     * @throws org.jruby.util.io.InvalidValueException If the modes are invalid
     */
    public ModeFlags(long flags) throws InvalidValueException {
    	// TODO: Ruby does not seem to care about invalid numeric mode values
    	// I am not sure if ruby overflows here also...
        this.flags = (int)flags;
        
        if (isReadOnly() && ((flags & APPEND) != 0)) {
            // MRI 1.8 behavior: this combination of flags is not allowed
            throw new InvalidValueException();
        }
    }

    public ModeFlags(String flagString) throws InvalidValueException {
        this.flags = getOFlagsFromString(flagString);
    }

    public static int getOFlagsFromString(String modesString) throws InvalidValueException {
        int modes = 0;
        int length = modesString.length();

        if (length == 0) {
            throw new InvalidValueException();
        }

        switch (modesString.charAt(0)) {
            case 'r' :
                modes |= RDONLY;
                break;
            case 'a' :
                modes |= APPEND | WRONLY | CREAT;
                break;
            case 'w' :
                modes |= WRONLY | TRUNC | CREAT;
                break;
            default :
                throw new InvalidValueException();
        }

        ModifierLoop: for (int n = 1; n < length; n++) {
            switch (modesString.charAt(n)) {
                case 'b':
                    modes |= BINARY;
                    break;
                case '+':
                    modes = (modes & ~ACCMODE) | RDWR;
                    break;
                case 't' :
                    modes |= TEXT;
                    break;
                case ':':
                    break ModifierLoop;
                default:
                    throw new InvalidValueException();
            }
        }

        return modes;        
    }

    public static int getOFlagsFromString(Ruby runtime, String modesString) {
        try {
            return getOFlagsFromString(modesString);
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError("mode string: " + modesString);
        }
    }

    /**
     * Produce a Java IO mode string from the flags in this object.
     * 
     * @return A Java string suitable for opening files with RandomAccessFile
     */
    public String toJavaModeString() {
        // Do not open as 'rw' by default since a file with read-only permissions will fail on 'rw'
        if (isWritable() || isCreate() || isTruncate()) {
            // Java requires "w" for creating a file that does not exist
            return "rw";
        } else {
            return "r";
        }
    }
    
    /**
     * Whether the flags specify"read only".
     * 
     * @return true if read-only, false otherwise
     */
    public final boolean isReadOnly() {
        return ((flags & WRONLY) == 0) && ((flags & RDWR) == 0);
    }
    
    /**
     * Whether the flags specify "readable", either read/write or read-only.
     * 
     * @return true if readable, false otherwise
     */
    public boolean isReadable() {
        return ((flags & RDWR) != 0) || isReadOnly();
    }

    /**
     * Whether the flags specify "binary" mode for reads and writes.
     * 
     * @return true if binary mode, false otherwise
     */
    public boolean isBinary() {
        return (flags & BINARY) != 0;
    }

    /**
     * Whether the flags specify "text" mode for reads and writes.
     *
     * @return true if text mode, false otherwise
     */
    public boolean isText() {
        return (flags & TEXT) != 0;
    }
    
    /**
     * Whether the flags specify to create nonexisting files.
     * 
     * @return true if nonexisting files should be created, false otherwise
     */
    public boolean isCreate() {
        return (flags & CREAT) != 0;
    }

    /**
     * Whether the flags specify "writable", either read/write or write-only
     * 
     * @return true if writable, false otherwise
     */
    public boolean isWritable() {
    	return (flags & RDWR) != 0 || (flags & WRONLY) != 0;
    }
    
    /**
     * Whether the flags specify exclusive access.
     * 
     * @return true if exclusive, false otherwise
     */
    public boolean isExclusive() {
        return (flags & EXCL) != 0;
    }
    
    /**
     * Whether the flags specify to append to existing files.
     * 
     * @return true if append, false otherwise
     */
    public boolean isAppendable() {
    	return (flags & APPEND) != 0;
    }

    /**
     * Whether the flags specify to truncate the target file.
     * 
     * @return true if truncate, false otherwise
     */
    public boolean isTruncate() {
    	return (flags & TRUNC) != 0;
    }

    /**
     * Check whether the target set of flags is a superset of this one; used to
     * ensure that a file is not re-opened with more privileges than it already
     * had.
     * 
     * @param superset The ModeFlags object which should be a superset of this one
     * @return true if the object is a superset, false otherwise
     */
    public boolean isSubsetOf(ModeFlags superset) {
    // TODO: Make sure all appropriate open flags are added to this check.
        if ((!superset.isReadable() && isReadable()) ||
            (!superset.isWritable() && isWritable()) ||
            !superset.isAppendable() && isAppendable()) {
            
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ModeFlags(" + flags + "): ");
        
        if (isAppendable()) buf.append("APPENDABLE ");
        if (isBinary()) buf.append("BINARY ");
        if (isCreate()) buf.append("CREATE ");
        if (isExclusive()) buf.append("EXCLUSIVE ");
        if (isReadOnly()) buf.append("READONLY ");
        if (isText()) buf.append("TEXT ");
        if (isTruncate()) buf.append("TRUNCATE ");
        if (isWritable()) {
            if (isReadable()) {
                buf.append("RDWR");
            } else {
                buf.append("WRITABLE ");
            }
        }
        
        return buf.toString();
    }

    /**
     * Return a value that, when passed to our constructor,
     * would create a copy of this instance.
     *
     * @return an int of the private flags variable.
     */
    public int getFlags() {
        return flags;
    }


    // MRI: rb_io_oflags_fmode
    /**
     * With the provided open flags parameter what fmode values should be
     * set (fmode for us is represented by OpenFile).
     */
    public static int getOpenFileFlagsFor(int flags) {
        int fmodeFlags;

        int readWrite = flags & 3;
        if (readWrite == RDONLY) {
            fmodeFlags = OpenFile.READABLE;
        } else if (readWrite == WRONLY) {
            fmodeFlags = OpenFile.WRITABLE;
        } else {
            fmodeFlags = OpenFile.READWRITE;
        }

        if ((flags & APPEND) != 0) {
            fmodeFlags |= OpenFile.APPEND;
        }
        if ((flags & CREAT) != 0) {
            fmodeFlags |= OpenFile.CREATE;
        }
        if ((flags & BINARY) == BINARY) {
            fmodeFlags |= OpenFile.BINMODE;
        }

        // This is unique to us to keep bridge betweeen mode_flags and oflags
        if ((flags & TEXT) == TEXT) {
            fmodeFlags |= OpenFile.TEXTMODE;
        }
        
        return fmodeFlags;
    }

    /**
     * Convert the flags in this object to a set of flags appropriate for the
     * OpenFile structure and logic therein.
     * 
     * @return an int of flags appropriate for OpenFile
     */    
    public int getOpenFileFlags() {
        return getOpenFileFlagsFor(flags);
    }
    /**
     * Convert the flags in this object to a set of flags appropriate for the
     * fcntl.
     *
     * @return an int of flags appropriate for fcntl
     */
    public int getFcntlFileFlags() {
        int fcntlFlags;

        int readWrite = flags & 3;
        if (readWrite == RDONLY) {
            fcntlFlags = 0;
        } else if (readWrite == WRONLY) {
            fcntlFlags = 1;
        } else {
            fcntlFlags = 2;
        }

        return fcntlFlags;
    }
    
    public static ModeFlags createModeFlags(int oflags) {
        try {
            return new ModeFlags(oflags);
        } catch (InvalidValueException e) {
            return new ModeFlags();
        }             
    }
}
