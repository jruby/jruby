package org.jruby.util.string;
import java.io.Serializable;
import java.util.Hashtable;

/**
 * Ustr - rhymes with Wooster.
 * Implements a string, with three design goals:
 *
 * <ol>
 * <li>Correct implementation of Unicode semantics.</li>
 * <li>Support for as many of java's String and StringBuffer methods as
 *    is reasonable.</li>
 * <li>Support for the familiar null-terminated-string primitives
 *    of the C programming language: strcpy() and friends.</li></ol>
 *
 * <p>A Ustr is a fairly thin wrapper around a byte[] array, which
 * contains null-terminated UTF8-encoded text.</p>
 *
 * <p><b>Note</b> that in the context of a Ustr, "index" always means how
 * many Unicode characters you are into the Ustr's text, while "offset"
 * always mean how many bytes you are into its UTF8 encoded form.</p>
 *
 * <p>Similarly, "char" and "String" always refer to the Java constructs,
 * while "character" always means a Unicode character, always identified
 * by a Java int.</p>
 *
 * <p>If any of the Ustr methods are passed an integer alleged to represent
 * a Unicode character whose value is not a valid code point, i.e. is either
 * negative or greater than 0x10ffff, the method will throw a UstrException,
 * which extends RuntimeException and is thus not checked at compile-time.</p>
 *
 * <p>For any method that copies characters and might overrun a buffer, a
 * "safe" version is provided, starting with an extra <code>s</code>, e.g.
 * <code>sstrcopy</code> and <code>sstrcat</code>. These versions always
 * arrange that the copied string not overrun the provided buffer, which
 * will be properly null-terminated.</p>
 *
 * @see org.jruby.util.string.UstrException
 */
public class Ustr
        implements Comparable, Serializable {
    private static final long serialVersionUID = -7263880042540200296L;


    // the number of bytes of UTF8, indexed by the value of the first byte
    private static final byte[] encLength = {
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
        2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
        3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,
        4,  4,  4,  4,  4,  4,  4,  4, -1, -1, -1, -1, -1, -1, -1, -1
    };
    
    
    private static Hashtable interns = new Hashtable();
    
    /**
     * A null-terminated byte array containing the string in UTF-8 form.  All
     * Ustr operations count on null-termination.  The byte array may
     * be much bigger than the contained string
     */
    public byte[] s;       // UTF-encoded text
    
    /**
     * Where in the array <code>s</code> the string starts.  You can
     *  have lots of different Ustrs co-existing in a single byte array.
     */
    public int base = 0;   // of the start of the string
    
    /**
     * To keep track of a single character position within the string;
     * this is used by the <code>nextChar</code> and <code>appendChar</code>
     * methods.
     */
    public int offset = 0; // for iterating, relative to base
    
    /**
     * Creates an empty Ustr with no buffer
     */
    public Ustr() {
        base = offset = 0;
    }
    /**
     * Creates an empty Ustr, with a null termination at the front.
     *
     * @param length length of the buffer, in bytes
     */
    public Ustr(int length) {
        s = new byte[length];
        base = offset = 0;
        s[0] = 0;
    }
    /**
     * Wraps a Ustr around a buffer.  Does not do null termination, so you
     * can pass in a buffer already containing a string.
     *
     * @param bytes the buffer
     */
    public Ustr(byte[] bytes) {
        s = bytes;
        base = offset = 0;
    }
    /**
     * Wraps a Ustr around a position in a buffer.   Does not do null
     * termination, so you can pass in a buffer already containing a string.
     *
     * @param bytes the buffer
     * @param start where in the buffer the strings starts
     */
    public Ustr(byte[] bytes, int start) {
        s = bytes;
        base = offset = start;
    }
    /**
     * Makes a Ustr which is a copy of another Ustr
     *
     * @param from the Ustr to copy
     */
    public Ustr(Ustr from) {
        s = new byte[from.strlen() + 1];
        base = offset = 0;
        strcpy(from);
    }
    /**
     * Makes a Ustr from a char[] array.  The Ustr is null-terminated, but
     * no space is allocated beyond what's needed.
     *
     * @param chars the char array
     */
    public Ustr(char [] chars) {
        
        int size = 0;
        for (int i = 0; i < chars.length; i++) {
            char utf16 = chars[i];
            // this works because surrogate characters will be counted as 2
            //  each, and anything in the astral planes takes 4 bytes.
            size += bytesInChar(utf16);
        }
        s = new byte[size + 1];
        base = 0;
        prepareAppend();
        int i = 0;
        while (i < chars.length) {
            int val = chars[i];
            if (val < 0xd800 || val > 0xdfff)
                ; // no-op
            
            // argh, surrogates.
            else {
                if (val > 0xdbff)
                    throw new UstrException("Mangled surrogate pair");
                
                i++;
                if (i == chars.length)
                    throw new UstrException("Mangled surrogate pair");
                
                int val2 = chars[i];
                if (val2 < 0xdc00 || val2 > 0xdfff)
                    throw new UstrException("Mangled surrogate pair");
                
                val &= 0x3ff;
                val <<= 10;
                val2 &= 0x3ff;
                val |= val2;
                val += 0x10000;
            }
            i++;
            appendChar(val);
        }
        s[s.length - 1] = 0;
    }

    /**
     * Makes a Ustr from an int[] array, where each int is the value of
     * a Unicode character.  Throws a UstrException if one of the ints
     * is not a Unicode codepoint (negative or >0x10ffff).
     *
     * @param ints the int array
     * @throws UstrException
     *
     */
    public Ustr(int [] ints) {
        int bufsiz = 0;
        
        for (int j = 0; j < ints.length; j++) {
            int i = ints[j];
            if (i < 0)
                throw new UstrException("Negative character value");
            if (i > 0x10ffff)
                throw new UstrException("Character out of Unicode range");
            
            bufsiz += bytesInChar(i);
            
        }
        s = new byte[bufsiz + 1];
        base = offset = 0;
        
        for (int j = 0; j < ints.length; j++) {
            int i = ints[j];
            appendChar(i);
        }
    }
    
    /**
     * Makes a Ustr from an object, based on its <code>toString()</code>.
     * Most commonly used with a String argument.  The Ustr is null-terminated,
     * but no space is allocated beyond what's needed.  Throws a UstrException
     * if the environment doesn't support the UTF8 encoding.
     *
     * @param o the Object
     * @throws UstrException
     */
    public Ustr(Object o) {
        byte[] inbytes;
        
        base = offset = 0;
        try {
            inbytes = o.toString().getBytes("UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new UstrException("UTF8 not supported!?!?");
        }
        
        // because we need one more byte than getBytes provides
        s = new byte[inbytes.length + 1];
        for (int i = 0; i < inbytes.length; i++)
            s[i] = inbytes[i];
        
        s[inbytes.length] = 0;
    }
    /**
     * Makes a Ustr from an object, based on its <code>toString()</code>,
     * leaving room for growth. Most commonly used with a String argument.
     * The Ustr is null-terminated.
     *
     * @param space How large a buffer to allocate
     * @param o     The object
     */
    public Ustr(int space, Object o) {
        s = new byte[space];
        base = offset = 0;
        byte [] b;
        
        try {
            b = o.toString().getBytes("UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF8 not supported!?!?");
        }
        
        for (int i = 0; i < b.length; i++)
            s[i] = b[i];
        
        s[b.length] = 0;
    }
    
    /**
     * Empty a Ustr by setting its first byte to 0.
     */
    public void init() {
        s[base] = 0;
        offset = base;
    }
    
    /**
     * Supports the <code>Comparable</code> interface.  The ordering is that of
     * native Unicode code points and probably not culturally appropriate
     * anywhere.
     *
     * @param other the object compared
     * @return -1, 0, or 1 as you'd expect.
     */
    public int compareTo(Object other) {
        Ustr o = (other instanceof Ustr) ? (Ustr) other : new Ustr(other);
        return strcmp(s, base, o.s, o.base);
    }
    
    /**
     * Generates a Java String representing the Ustr.  Throws a UstrException
     * if the Java environment doesn't support the UTF8 encoding.
     *
     * @return the String.
     * @throws UstrException
     */
    public String toString() {
        try {
            return new String(s, base, strlen(), "UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new UstrException("UTF8 not supported!?!?");
        }
    }
    
    // per-Unicode-character operations
    //
    /**
     * Length of a Ustr in Unicode characters (not bytes).
     *
     * @return the number of Unicode characters.
     */
    public int length() {
        int saveOffset = offset;
        int l = 0;
        for (prepareNext(); nextChar() != 0; l++)
            ; // empty
        offset = saveOffset;
        return l;
    }
    /**
     * Number of Unicode characters stored starting at some offset in a byte
     * array.  Assumes UTF-8 encoding and null termination.
     *
     * @param b      the byte array
     * @param offset where to start counting
     * @return       the number of unicode characters.
     */
    public static int length(byte [] b, int offset) {
        return (new Ustr(b, offset)).length();
    }
    /**
     * Number of Unicode characters stored in a byte array.  Assumes UTF-8
     * encoding and null termination.
     *
     * @param b the byte array
     * @return  the number of Unicode characters.
     */
    public static int length(byte [] b) {
        return length(b, 0);
    }
    
    /**
     * Number of Unicode characters stored in a Java string.
     * if <code>s</code> is a String, <code>s.length()</code> and
     * <code>Ustr.length(s)</code> will be the same except when <code>s</code>
     * contains non-BMP characters.
     *
     * @param str the string
     * @return    the number of Unicode characters
     */
    public static int length(String str) {
        return (new Ustr(str)).length();
    }
    
    /**
     * Set up for <code>appendChar</code>.  Points the <code>offset</code>
     * field at the buffer's null terminator.
     */
    public void prepareAppend() {
        offset = strlen();
    }
    /**
     * Append one Unicode character to a Ustr.  Assumes that the
     * <code>offset</code> points to the null-termination,
     * where the character ought to go, updates that field and applies
     * another null termination.  You could change the value of
     * <code>offset</code> and start "appending" into the middle of a Ustr
     * if that's what you wanted.  This generates the UTF-8 bytes from
     * the input characters.
     * <p>If the character is less than 128, one byte of buffer is used.
     * If less than 0x8000, two bytes.  If less than 2**16, three bytes.
     * If less than 0x10ffff, four bytes.  If greater than 0x10ffff, or
     * negative, you get an exception.</p>
     *
     * @param c the character to be appended.
     */
    public void appendChar(int c) {
        offset = appendChar(c, s, offset);
    }
    
    /**
     * Writes one Unicode character into a UTF-8 encoded byte array at
     * a given offset, and null-terminates it.  Throws a UstrException if
     * the 'c' argument is not a Unicode codepoint (negative or >0x10ffff)
     *
     * @param        c the Unicode character
     * @param        s the array
     * @param offset the offset to write at
     * @return       the offset of the null byte after the encoded character
     * @throws       UstrException
     */
    public static int appendChar(int c, byte [] s, int offset) {
        if (c < 0)
            throw new UstrException("Appended negative character");
        if (c < 128)
            s[offset++] = (byte) c;
        else if (c <= 0x7ff) {
            s[offset++] = (byte) (  (c >> 6) | 0xc0);
            s[offset++] = (byte) ((c & 0x3f) | 0x80);
        } else if (c <= 0xffff) {
            s[offset++] = (byte) (        (c >> 12) | 0xe0);
            s[offset++] = (byte) (((c >> 6) & 0x3f) | 0x80);
            s[offset++] = (byte) (       (c & 0x3f) | 0x80);
        } else if (c <= 0x10ffff) {
            s[offset++] = (byte) (         (c >> 18) | 0xf0);
            s[offset++] = (byte) (((c >> 12) & 0x3f) | 0x80);
            s[offset++] = (byte) ( ((c >> 6) & 0x3f) | 0x80);
            s[offset++] = (byte) (        (c & 0x3f) | 0x80);
        } else
            throw new UstrException("Appended character > 0x10ffff");
        s[offset] = 0;
        return offset;
    }
    
    /**
     * Set up for <code>nextChar()</code>.  Points the <code>offset</code>
     * field at the start of the buffer.
     */
    public void prepareNext() {
        offset = base;
    }
    /**
     * Retrieve one Unicode character from a Ustr and advance the working
     * offset.  Assumes the working offset is sanely located.
     *
     * @return the Unicode character, 0 signaling the end of the string
     */
    public int nextChar() {
        if (s[offset] == 0)
            return 0;
        if ((s[offset] & 0x80) == 0)
            return (int) s[offset++];
        if ((s[offset] & 0xe0) == 0xc0) {
            // 110w wwww 10zz zzzz
            // xxxx xwww wwzz zzzz
            int c = (s[offset++] & 0x1f) << 6;
            c |= s[offset++] & 0x3f;
            return c;
        }
        if ((s[offset] & 0xf0) == 0xe0) {
            // 1110 wwww 10zz zzzz 10xx xxxx
            // wwww zzzz zzxx xxxx
            int c = (s[offset++] & 0xf) << 12;
            c |= (s[offset++] & 0x3f) << 6;
            c |= s[offset++] & 0x3f;
            return c;
        }
        // 1111 0www 10zz zzzz 10xx xxxx 10yy yyyy
        // wwwwzz zzzzxxxx xxyyyyyy
        int c = (s[offset++] & 0x7) << 18;
        c |= (s[offset++] & 0x3f) << 12;
        c |= (s[offset++] & 0x3f) << 6;
        c |= s[offset++] & 0x3f;
        return c;
    }
    
    // Strlen variants
    //
    /**
     * The length in bytes of a Ustr's UTF representation.  Assumes
     * null-termination.
     *
     * @return the number of bytes
     */
    public int strlen() {
        return strlen(s, base);
    }
    /**
     * The length in bytes of a null-terminated byte array
     *
     * @param b the array
     * @return  the number of bytes
     */
    public static int strlen(byte [] b) {
        int i = 0;
        while (b[i] != 0)
            i++;
        return i;
    }
    /**
     * The length in bytes of a null-terminated sequence starting at some
     * offset in a byte array.
     *
     * @param b    the byte array
     * @param base the byte offset to start counting at
     * @return     the number of bytes
     */
    public static int strlen(byte [] b, int base) {
        int i = base;
        while (b[i] != 0)
            i++;
        return i - base;
    }
    
    // Strcpy variants
    //
    /**
     * Copy a null-terminated byte array.
     *
     * @param to   destination array
     * @param from source array
     * @return     the destination array
     */
    public static byte [] strcpy(byte [] to, byte [] from) {
        return strcpy(to, 0, from, 0);
    }
    /**
     * Copy null-terminated byte arrays with control over offsets.
     *
     * @param to    destination array
     * @param tbase starting offset in destination array
     * @param from  source array
     * @param fbase starting offset in source array
     * @return      the destination array
     */
    public static byte [] strcpy(byte [] to, int tbase, byte [] from, int fbase) {
        while (from[fbase] != 0)
            to[tbase++] = from[fbase++];
        to[tbase] = 0;
        
        return to;
    }
    /**
     * Copy in the contents of another Ustr.  Does not change the offset.
     *
     * @param from source Ustr
     * @return     this Ustr
     */
    public Ustr strcpy(Ustr from) {
        strcpy(s, base, from.s, from.base);
        return this;
    }
    
    /**
     * Copy in the String representation of an Object.  Does not change the
     * offset.
     *
     * @param o the source object
     * @return  this Ustr
     */
    public Ustr strcpy(Object o) {
        strcpy(new Ustr(o));
        return this;
    }
    /**
     * Copy in the contents of a null-terminated byte array.  Does not change
     * the offset.
     *
     * @param from the byte array
     * @return     this Ustr
     */
    public Ustr strcpy(byte[] from) {
        strcpy(s, from);
        return this;
    }
    /**
     * Copy in the contents at some offset in a null-terminated byte array.
     * Does not change the offset.
     *
     * @param from    the source byte array
     * @param boffset where to start copying in the source array
     * @return        this Ustr
     */
    public Ustr strcpy(byte[] from, int boffset) {
        strcpy(s, 0, from, boffset);
        return this;
    }
    /**
     *
     * Load a null-terminated UTF-8 encoding of a String into a byte array at
     * the front.
     *
     * @param b      the byte array
     * @param s      the String
     *
     * @return the byte array
     */
    public static byte [] strcpy(byte [] b, String s) {
        return strcpy(b, 0, s);
    }
    
    /**
     * Load a null-terminated UTF-8 encoding of a String into a byte array.
     *
     * @param b      the byte array
     * @param offset where in the byte array to load
     * @param s      the String
     *
     * @return the byte array
     */
    public static byte [] strcpy(byte [] b, int offset, String s) {
        byte [] sbytes;
        
        try { sbytes = s.getBytes("UTF8"); } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF8 not supported!?!?"); }
        
        for (int i = 0; i < sbytes.length; i++)
            b[offset + i] = sbytes[i];
        b[offset + sbytes.length] = 0;
        return b;
    }
    
    
    // safe versions
    // could check for to.length myself, but since Java is necessarily
    //  doing this for me each time around the loop, why bother?
    //
    /**
     * Safely append one Ustr to another.
     *
     * @param from the Ustr to be appended
     * @return     this
     */
    public Ustr sstrcat(Ustr from) {
        sstrcat(s, base, from.s, from.base);
        return this;
    }
    
    /**
     * Safely append one null-terminated byte array to another.  Destination
     * buffer will not be overrun.
     *
     * @param to   dest array
     * @param from source array
     * @return     dest array
     */
    public byte [] sstrcat(byte [] to, byte[] from) {
        return sstrcat(to, 0, from, 0);
    }
    /**
     * Safely append one null-terminated byte array to another with control
     * over offsets.  Destination buffer will not be overrun.
     *
     * @param to    dest array
     * @param tbase base of dest array
     * @param from  source array
     * @param fbase base of source array
     * @return to
     */
    public static byte [] sstrcat(byte [] to, int tbase, byte [] from, int fbase) {
        // don't want to catch if the dest string is malformed
        while (to[tbase] != 0)
            tbase++;
        
        try {
            while (from[fbase] != 0)
                to[tbase++] = from[fbase++];
            to[tbase] = 0;
            
            return to;
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            if (tbase >= to.length)
                to[to.length - 1] = 0;
            else
                throw e;
        }
        return to;
    }
    
    /**
     * Safely copy null-terminated byte arrays with control over offsets.
     * Destination buffer will not be overrun.
     *
     * @param to    destination array
     * @param tbase starting offset in destination array
     * @param from  source array
     * @param fbase starting offset in source array`
     * @return      the destination array
     */
    public static byte [] sstrcpy(byte [] to, int tbase, byte [] from, int fbase) {
        try {
            while (from[fbase] != 0)
                to[tbase++] = from[fbase++];
            to[tbase] = 0;
        }
        
        catch (java.lang.ArrayIndexOutOfBoundsException e) {
            // if the buffer's too short
            if (tbase >= to.length)
                to[to.length - 1] = 0;
            
            // otherwise there's some problem with the source string, we
            //  shouldn't catch it
            else
                throw e;
        }
        return to;
    }
    /**
     * Safely copy a null-terminated byte array.  The destination buffer will not
     * be overrun.
     *
     * @param to   destination array
     * @param from source array
     * @return     the destination array
     */
    public static byte [] sstrcpy(byte [] to, byte [] from) {
        return sstrcpy(to, 0, from, 0);
    }
    
    /**
     * Safely copy in the contents of another Ustr.  Does not change the offset.
     * The destination buffer will not be overrun.
     *
     * @param from source Ustr
     * @return     this Ustr
     */
    public Ustr sstrcpy(Ustr from) {
        sstrcpy(s, base, from.s, from.base);
        return this;
    }
    
    /**
     * Copy one null-terminated array to the end of another, with
     * starting offsets for each
     *
     * @param to    destination array
     * @param tbase  base pos of destination
     * @param from  source array
     * @param fbase base pos of source
     * @return      destination
     */
    public static byte [] strcat(byte [] to, int tbase, byte [] from, int fbase) {
        while (to[tbase] != 0)
            tbase++;
        
        while (from[fbase] != 0)
            to[tbase++] = from[fbase++];
        to[tbase] = 0;
        
        return to;
    }
    
    /**
     * Copy one null-terminated byte array to the end of another.
     *
     * @param to   destination array
     * @param from source array
     * @return     the destionation array
     */
    public static byte [] strcat(byte [] to, byte [] from) {
        return strcat(to, 0, from, 0);
    }
    
    /**
     * Append the contents of another Ustr to the end of this one
     *
     * @param  other the other Ustr
     * @return       this Ustr
     */
    public Ustr strcat(Ustr other) {
        strcat(s, other.s);
        return this;
    }
    
    /**
     * Compare two null-terminated byte arrays.  The ordering is that of
     * native Unicode code points and probably not culturally appropriate
     * anywhere.
     *
     * @param s1 first byte array
     * @param s2 second byte array
     * @return   a negative number, zero, or a positive number depending
     * on whether s1 is lexically less than, equal to, or greater than s2.   */
    public static int strcmp(byte [] s1, byte [] s2) {
        return strcmp(s1, 0, s2, 0);
    }
    /**
     * Compare sections of two null-terminated byte arrays.  The ordering is
     * that of
     * native Unicode code points and probably not culturally appropriate
     * anywhere.
     *
     * @param s1     first byte array
     * @param s1base byte offset in first array to start comparing
     * @param s2     second byte array
     * @param s2base byte offset in second array to start comparing
     * @return       a negative number, zero, or a positive number depending on
     * whether s1 is lexically less than, equal to, or greater than s2.
     */
    public static int strcmp(byte [] s1, int s1base, byte [] s2, int s2base) {
        
        Ustr u1 = new Ustr(s1, s1base);
        Ustr u2 = new Ustr(s2, s2base);
        
        int c1 = u1.nextChar();
        int c2 = u2.nextChar();
        
        while (c1 != 0 && c2 != 0 && c1 == c2) {
            c1 = u1.nextChar();
            c2 = u2.nextChar();
        }
        
        return c1 - c2;
    }
    /**
     * Compare two Ustrs.  The ordering is that of
     * native Unicode code points and probably not culturally appropriate
     * anywhere.
     *
     * @param other the other Ustr
     * @return   a negative number, zero, or a positive number depending on
     * whether the other is lexically less than, equal to, or greater than this.
     */
    public int strcmp(Ustr other) {
        return strcmp(s, base, other.s, other.base);
    }
    
    /**
     * Compare a Ustr to an object's String representation.  The ordering
     * is that of native Unicode code points and probably not culturally
     * appropriate anywhere.
     *
     * @param other the other Object
     * @return   a negative number, zero, or a positive number depending on
     * whether the other is lexically less than, equal to, or greater than this.
     */
    public int strcmp(Object other) {
        return strcmp(new Ustr(other));
    }
    
    /**
     * Locate a Unicode character in a Ustr.  Returns null if not
     * found; if the character is zero, finds the offset of the null termination.
     *
     * @param c the character, as an integer
     * @return  a Ustr with the same buffer, starting at the matching character,
     * or null if it's not found.
     */
    public Ustr strchr(int c) {
        int where = strchr(s, c);
        return (where == -1) ? null : new Ustr(s, where);
    }
    
    /**
     * Find the offset where a Unicode character starts in a null-terminated
     * UTF-encoded byte array.
     * Returns -1 if not found; if the character is zero, finds the index of
     * the null termination.
     *
     * @param b UTF-encoded null-terminated byte array
     * @return  the offset in the string, or -1
     */
    public static int strchr(byte [] b, int c) {
        byte [] cbytes = new byte[10];
        appendChar(c, cbytes, 0);
        return strstr(b, cbytes);
    }
    
    /**
     * Locate the last occurrence of a Unicode character in a Ustr.
     * If found, returns a Ustr built around the same buffer as
     * this, with the base set to the matching location.  If not found, null
     *
     * @param c the character, as an integer
     * @return  a Ustr with the base set to the match, or null
     */
    public Ustr strrchr(int c) {
        int where = strrchr(s, c);
        return (where == -1) ? null : new Ustr(s, where);
    }
    
    /**
     * Find the index of the last appearance of a Unicode character in a
     * null-terminated UTF-encoded byte array.
     * Returns -1 if not found.
     *
     * @param b the byte array
     * @param c the integer
     * @return  the offset where the last occurence of c starts, or -1
     */
    public static int strrchr(byte [] b, int c) {
        byte [] cbytes = new byte[10];
        appendChar(c, cbytes, 0);
        
        int where = b.length - strlen(cbytes);
        while (where >= 0) {
            int i;
            for (i = 0; cbytes[i] != 0; i++)
                if (b[where + i] != cbytes[i])
                    break;
            if (cbytes[i] == 0)
                return where;
            where--;
        }
        return -1;
    }
    
    /**
     * Locate a substring in a string.  Returns a Ustr built around the same
     * buffer, but starting at the matching position, or null if no match
     * is found.
     *
     * @param little the substring to be located
     * @return       matching Ustr, or null
     */
    public Ustr strstr(Ustr little) {
        int where = strstr(s, little.s);
        return (where == -1) ? null : new Ustr(s, where);
    }
    
    /**
     * locate a substring in a byte array.  Returns the offset of the substring
     * if it matches, otherwise -1.
     *
     * @param big    the array to search in
     * @param little the array to search for
     * @return       the index of the match, or -1
     */
    public static int strstr(byte [] big, byte [] little) {
        // should BoyerMooreify this...
        
        for (int bi = 0; big[bi] != 0; bi++) {
            int li;
            for (li = 0; little[li] != 0; li++)
                if (big[bi + li] != little[li])
                    break;
            if (little[li] == 0)
                return bi;
        }
        return -1;
    }
    
    /////////////////////////////////////////////////////////////////
    // From here on down the methods are those from java.lang.String
    /////////////////////////////////////////////////////////////////
    
    /**
     * Returns a Ustr generated from the char array.
     *
     * @param data the char array
     * @return     a new Ustr
     */
    static Ustr copyValueOf(char [] data) {
        return new Ustr(data);
    }
    
    /**
     * Returns a Ustr generated from a piece of the char array.
     *
     * @param data   the char array
     * @param offset where to start generating from
     * @param count  how many java chars to use
     * @return       a new Ustr
     */
    static Ustr copyValueOf(char [] data, int offset, int count) {
        char [] chunk = new char[count];
        for (int i = 0; i < count; i++)
            chunk[i] = data[offset + i];
        return new Ustr(chunk);
    }
    
    /**
     * find the Unicode character at some index in a Ustr.  Throws an
     * IndexOutOfBounds exception if appropriate.
     *
     * @param at the index
     * @return   the Unicode character, as an integer
     */
    public int charAt(int at)
            throws IndexOutOfBoundsException {
        if (at < 0)
            throw new IndexOutOfBoundsException("Negative Ustr charAt");
        int c = 0;
        offset = 0;
        prepareNext();
        do {
            c = nextChar();
            at--;
        } while (c != 0 && at >= 0);
        
        if (at > 0)
            throw new IndexOutOfBoundsException("Ustr charAt too large");
        return c;
    }
    
    /**
     * Append a String to the end of this.
     *
     * @param str the string
     * @return a  a new Ustr which contains the concatenation
     */
    public Ustr concat(String str) {
        Ustr us = new Ustr(str);
        return concat(us);
    }
    
    /**
     * Append a Ustr to the end of this.
     *
     * @param us the ustr to append
     * @return   a new ustr
     */
    public Ustr concat(Ustr us) {
        Ustr ret = new Ustr(strlen() + us.strlen() + 1);
        ret.strcpy(this);
        ret.strcat(us);
        return ret;
    }
    
    /**
     * Test if this Ustr ends with the specified suffix (a Ustr).
     *
     * @param suffix the possible suffix.
     * @return       true or false.
     */
    public boolean endsWith(Ustr suffix) {
        int start = strlen() - suffix.strlen();
        if (start < 0)
            return false;
        //      can't use strcmp because we're just seeing if the byte encodings end the same
        // irrespective of the Unicode chars they encode
        int i = 0;
        while (s[base + start + i] != 0 && suffix.s[suffix.base + i] != 0 &&
               s[base + start + i] == suffix.s[suffix.base + i])
            i++;
        
        return (s[base + start + i] == suffix.s[suffix.base + i]);
    }
    
    /**
     * Test if this Ustr ends with specified suffix (a String).
     *
     * @param suffix the possible suffix
     * @return       true or false
     */
    public boolean endsWith(String suffix) {
        return endsWith(new Ustr(suffix));
    }
    
    /**
     * Compares this Ustr to another object.
     *
     * @param anObject the other object
     * @return         true or false
     */
    public boolean equals(Object anObject) {
        return (compareTo(anObject) == 0);
    }
    
    /**
     * Convert this Ustr into bytes according to the platform's default
     * character encoding, storing the result in a new byte array.
     *
     * @return a new byte array
     */
    public byte [] getBytes() {
        return toString().getBytes();
    }
    
    /**
     * Convert this Ustr into bytes according to the specified
     * character encoding, storing the result into a new byte array.
     *
     * @param enc the encoding to use in generating bytes
     * @return    the new byte array
     */
    public byte [] getBytes(String enc)
            throws java.io.UnsupportedEncodingException {
        return toString().getBytes(enc);
    }
    
    /**
     * Copies Unicode characters from this String into the destination
     * char array.  Note that if the String contains UTF-16 surrogate
     * pairs, each pair counts as a single character.
     *
     * @param str      the string
     * @param srcBegin where to start copying
     * @param srcEnd   index after last char to copy
     * @param dst      start of destination array
     * @param dstBegin where in the destination array to start copying
     */
    public static void getChars(String str, int srcBegin, int srcEnd,
            char [] dst, int dstBegin) {
        Ustr us = new Ustr(str);
        us.getChars(srcBegin, srcEnd, dst, dstBegin);
    }
    
    /**
     * Copies Unicode characters from this Ustr into the destination
     * char array.  We can't just dispatch to the String implementation
     * because we do Unicode characters, it does UTF-16 code points
     *
     * @param srcBegin where to start copying
     * @param srcEnd   index after last char to copy
     * @param dst      start of destination array
     * @param dstBegin where in the destination array to start copying
     */
    public void getChars(int srcBegin, int srcEnd, char [] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || dstBegin < 0)
            throw new IndexOutOfBoundsException("bogus getChars index bounds");
        if (dst == null)
            throw new NullPointerException("null 'dst' argument to getChars");
        
        prepareNext();
        while (srcBegin > 0) {
            srcBegin--;
            nextChar();
        }
        int c;
        int howMany = srcEnd - srcBegin;
        int i, j;
        for (i = j = 0; i < howMany; i++, j++) {
            c = nextChar();
            if (c == 0 && i < howMany - 1)
                throw new IndexOutOfBoundsException("getChars ran off buffer");
            if (c < 0x10000)
                dst[dstBegin + j] = (char) c;
            else {
                // two UTF-16 codepoints
                // 10346 => D800/DF46
                // 000uuuuuxxxxxxxxxxxxxxxx 110110wwwwxxxxxx 110111xxxxxxxxxx
                // where wwww = uuuuu - 1
                
                c -= 0x10000;
                int uHi = (c >> 10) & 0x3ff;
                dst[dstBegin + j] = (char) (0xd800 | uHi);
                j++;
                
                int uLo = c & 0x3ff;
                dst[dstBegin + j] = (char) (0xdc00 | uLo);
            }
        }
    }
    
    /**
     * Returns a hashcode for this Ustr.  The algorithm is that documented
     * for String, only that documentation says 'int'
     * arithmetic, which is clearly wrong, but this produces the same result
     * as String's hashCode() for the strings "1" and "2", and thus by
     * induction must be correct.
     *
     * @return an integer hashcode
     */
    public int hashCode() {
        long h = 0;
        long c;
        long n = length() - 1;
        prepareNext();
        while ((c = nextChar()) != 0) {
            h += c * pow(31, n);
            n--;
        }
        return (int) (h & 0xffffffff);
    }
    
    // er blush I'm on a plane and can't find long exponentiation in Java
    private static long pow(long a, long b) {
        long p = 1;
        while (b-- > 0)
            p *= a;
        return p;
    }
    
    /**
     * Returns the first index within this Ustr of the specified
     * Unicode character.
     *
     * @param ch    the character
     * @return      index (usable by charAt) in the string of the char, or -1
     */
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }
    
    /**
     * Returns the first index within this Ustr of the specified
     * character, starting at the specified index.
     *
     * @param ch    the character
     * @param start where to start looking
     * @return      index (usable by charAt) in the string of the char, or -1
     */
    public int indexOf(int ch, int start) {
        int i = 0;
        prepareNext();
        while (start-- > 0) {
            nextChar();
            i++;
        }
        int c;
        while ((c = nextChar()) != 0) {
            if (c == ch)
                return i;
            i++;
        }
        if (ch == 0)
            return i;
        return -1;
    }
    
    /**
     * Returns the index within this Ustr of the first occurrence of the
     * specified other Ustr, or -1.
     *
     * @param us the other Ustr
     * @return   the index of the match, or -1
     */
    public int indexOf(Ustr us) {
        return indexOf(us, 0);
    }
    
    /**
     * Returns the index within this Ustr of the first occurrence of the
     * specified other Ustr starting at the given offset, or -1.
     *
     * @param us    the other Ustr
     * @param start the index to start looking
     * @return      the index of the match, or -1
     */
    public int indexOf(Ustr us, int start) {
        int i = 0;
        prepareNext();
        while (start-- > 0) {
            nextChar();
            i++;
        }
        
        // we'll work at the UTF level, but this should be BoyerMoore-ized
        do {
            int j;
            for (j = 0; s[base + offset + j] != 0 && us.s[us.base + j] != 0; j++)
                if (s[base + offset + j] != us.s[us.base + j])
                    break;
            if (us.s[base + j] == 0)
                return i;
            i++;
        } while (nextChar() != 0);
        
        return -1;
    }
    
    /**
     * returns a canonical version of the Ustr, which should be treated as
     * read-only.  Differs from the intern function
     * of String in that it never returns the input string; if a new hashtable
     * entry is required, it makes a new Ustr and returns that.  If a programmer
     * updates the contents of a Ustr returned from intern(), grave disorder
     * will ensue.
     *
     * @return the canonical version of the Ustr.
     */
    public Ustr intern() {
        Ustr u = (Ustr)interns.get(this);
        if (u != null)
            return u;
        
        u = new Ustr(strlen() + 1);
        u.strcpy(this);
        interns.put(u, u);
        return u;
    }
    
    /**
     * Returns the index within this Ustr of the last occurrence of the
     * specified Unicode character.
     *
     * @param ch   the character
     * @return     the last index of the character, or -1
     */
    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length());
    }
    
    /**
     * Returns the index within this Ustr of the last occurrence of the
     * specified Unicode character before the specified stop index.
     *
     * @param ch   the character
     * @param stop last index to consider
     * @return     the last index of the character, or -1
     */
    public int lastIndexOf(int ch, int stop) {
        int i = 0;
        prepareNext();
        int foundAt = -1;
        do {
            if (ch == nextChar())
                foundAt = i;
            i++;
        } while (i <= stop);
        
        return foundAt;
    }
    
    /**
     * Finds the last substring match.
     *
     * @param us   the subtring to search for
     * @return     the match index, or =1
     */
    public int lastIndexOf(Ustr us) {
        return lastIndexOf(us, length());
    }
    
    /**
     * Finds the last substring match before the given index.
     *
     * @param us   the subtring to search for
     * @param stop where to stop searching
     * @return     the match index, or =1
     */
    public int lastIndexOf(Ustr us, int stop) {
        int i = 0;
        int foundAt = -1;
        
        // we'll work at the UTF level, but this should be BoyerMoore-ized
        prepareNext();
        do {
            int j;
            for (j = 0; s[base + offset + j] != 0 && us.s[us.base + j] != 0; j++)
                if (s[base + offset + j] != us.s[us.base + j])
                    break;
            if (us.s[base + j] == 0)
                foundAt = i;
            i++;
        } while (nextChar() != 0 && i <= stop);
        
        return foundAt;
    }
    
    private static int bytesInChar(int c) {
        if (c < 128)
            return 1;
        else if (c < 0x800)
            return 2;
        else if (c < 0x10000)
            return 3;
        else
            return 4;
    }
    
    /**
     * returns a new Ustr with all instances of one Unicode character replaced
     * by another.  Throws a UstrException if newChar
     * is not a Unicode codepoint (negative or >0x10ffff).
     *
     * @param oldChar the Unicode character to be replaced
     * @param newChar the Unicode character to replace it with
     * @return        the new Ustr
     * @throws        UstrException
     */
    public Ustr replace(int oldChar, int newChar) {
        if (newChar < 0)
            throw new UstrException("Negative replacement character");
        else if (newChar > 0x10ffff)
            throw new UstrException("Replacement character > 0x10ffff");
        
        // figure out how much space we need
        int space = strlen() + 1;
        int delta = bytesInChar(newChar) - bytesInChar(newChar);
        if (delta != 0) {
            int c;
            
            while ((c = nextChar()) != 0)
                if (c == oldChar)
                    space += delta;
        }
        
        Ustr us = new Ustr(space);
        prepareNext(); us.prepareAppend();
        int c;
        while ((c = nextChar()) != 0)
            us.appendChar((c == oldChar) ? newChar : c);
        return us;
    }
    
    /**
     * Tests if other Ustr is prefix of this.
     *
     * @param us    the other Ustr
     * @return      true/false
     */
    public boolean startsWith(Ustr us) {
        return startsWith(us, 0);
    }
    
    /**
     * Tests if other Ustr is prefix at given index.
     *
     * @param us    the other Ustr
     * @param start where to test
     * @return      true/false
     */
    public boolean startsWith(Ustr us, int start) {
        prepareNext();
        while (start-- > 0)
            nextChar();
        
        for (int i = 0; us.s[base + i] != 0; i++)
            if (s[base + offset + i] != us.s[us.base + i])
                return false;
        
        return true;
    }
    
    /**
     * makes a new substring of a Ustr given a start index.
     *
     * @param start index of start of substr
     * @return      new Ustr containing substr
     */
    public Ustr substring(int start) {
        return substring(start, length());
    }
    
    /**
     * makes a new substring of a Ustr identified by start and end
     *  indices.
     *
     * @param start index of start of substr
     * @param end   index of end of substr
     * @return      new Ustr containing substr
     */
    public Ustr substring(int start, int end) {
        if (start < 0 || end < start || end > length())
            throw new IndexOutOfBoundsException("bogus start/end");
        
        int howMany = end - start;
        offset = 0;
        
        // move up to the start
        while (start-- > 0) {
            int c = s[base + offset] & 0xff;
            if (c == 0)
                throw new IndexOutOfBoundsException("substring too long");
            offset += encLength[c];
        }
        
        int startAt = offset;
        for (int i = 0; i < howMany; i++) {
            int c = s[base + offset] & 0xff;
            if (c == 0)
                throw new IndexOutOfBoundsException("substring too long");
            offset += encLength[c];
        }
        int bytesToMove = offset - startAt;
        Ustr us = new Ustr(bytesToMove + 1);
        System.arraycopy(s, startAt, us.s, 0, bytesToMove);
        us.s[bytesToMove] = 0;
        
    /*
    int to = 0;
    while (startAt < offset)
      us.s[to++] = s[startAt++];
    us.s[to] = 0;
     */
        
    /*
    prepareNext();
    while (start-- > 0)
      nextChar();
     
    Ustr us = new Ustr(strlen(s, offset) + 1);
     
    us.prepareAppend();
    for (int i = 0; i < howMany; i++) {
      int c = nextChar();
      if (c == 0)
        throw new IndexOutOfBoundsException("substring too long");
      us.appendChar(c);
    }
     */
        return us;
    }
    
    /**
     * converts a Ustr to a char array.
     *
     * @return the new char array
     */
    public char [] toCharArray() {
        return toString().toCharArray();
    }
}
