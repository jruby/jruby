package org.jruby.runtime.encoding;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.ISO8859_16Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.Console;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.ext.nkf.RubyNKF;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.encoding.ISO_8859_16;
import org.jruby.util.io.EncodingUtils;

public final class EncodingService {
    private final CaseInsensitiveBytesHash<Entry> encodings;
    private final CaseInsensitiveBytesHash<Entry> aliases;

    // for fast lookup: encoding entry => org.jruby.RubyEncoding
    private final IRubyObject[] encodingList;
    // for fast lookup: org.joni.encoding.Encoding => org.jruby.RubyEncoding
    private RubyEncoding[] encodingIndex = new RubyEncoding[4];
    // the runtime
    private final Ruby runtime;

    private final Encoding ascii8bit;
    private final Encoding javaDefault;

    private static final ByteList LOCALE_BL = ByteList.create("locale");
    private static final ByteList EXTERNAL_BL = ByteList.create("external");
    private static final ByteList INTERNAL_BL = ByteList.create("internal");
    private static final ByteList FILESYSTEM_BL = ByteList.create("filesystem");

    public EncodingService (Ruby runtime) {
        this.runtime = runtime;
        encodings = EncodingDB.getEncodings();
        aliases = EncodingDB.getAliases();
        ascii8bit = encodings.get("ASCII-8BIT".getBytes()).getEncoding();

        Charset javaDefaultCharset = Charset.defaultCharset();
        ByteList javaDefaultBL = new ByteList(javaDefaultCharset.name().getBytes());
        Entry javaDefaultEntry = findEncodingOrAliasEntry(javaDefaultBL);
        javaDefault = javaDefaultEntry == null ? ascii8bit : javaDefaultEntry.getEncoding();

        encodingList = new IRubyObject[encodings.size()];
    }

    /**
     * Since Java 1.6, class {@link java.io.Console} is available.
     * But the encoding or codepage of the underlying connected
     * console is currently private. Had to use Reflection to get it.
     *
     * @return console codepage
     */
    public Encoding getConsoleEncoding() {
        if (!Platform.IS_WINDOWS) return null;

        Encoding consoleEncoding = null;
        try {
            Console console = System.console();
            if (console != null) {
                final String CONSOLE_CHARSET = "cs";
                Field fcs = Console.class.getDeclaredField(CONSOLE_CHARSET);
                fcs.setAccessible(true);
                Charset cs = (Charset) fcs.get(console);
                consoleEncoding = loadEncoding(ByteList.create(cs.name()));
            }
        } catch (Throwable e) { // to cover both Exceptions and Errors
            // just fall back on local encoding above
        }
        return consoleEncoding;
    }

    // mri: rb_usascii_encoding
    public Encoding getUSAsciiEncoding() {
        return USASCIIEncoding.INSTANCE;
    }

    // mri: rb_ascii8bit_encoding
    public Encoding getAscii8bitEncoding() {
        return ascii8bit;
    }

    // mri: rb_filesystem_encoding
    public Encoding getFileSystemEncoding() {
        return SpecialEncoding.FILESYSTEM.toEncoding(runtime);
    }

    public CaseInsensitiveBytesHash<Entry> getEncodings() {
        return encodings;
    }

    public CaseInsensitiveBytesHash<Entry> getAliases() {
        return aliases;
    }

    public Entry findEncodingEntry(ByteList bytes) {
        return encodings.get(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public Entry findEncodingEntry(byte[] bytes) {
        return encodings.get(bytes);
    }

    public Entry findAliasEntry(ByteList bytes) {
        return aliases.get(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public Entry findAliasEntry(byte[] bytes) {
        return aliases.get(bytes);
    }

    public Entry findEncodingOrAliasEntry(ByteList bytes) {
        Entry e = findEncodingEntry(bytes);
        return e != null ? e : findAliasEntry(bytes);
    }

    public Entry findEncodingOrAliasEntry(byte[] bytes) {
        Entry e = findEncodingEntry(bytes);
        return e != null ? e : findAliasEntry(bytes);
    }

    // rb_locale_charmap...mostly
    public Encoding getLocaleEncoding() {
        final Encoding consoleEncoding = getConsoleEncoding();

        if (consoleEncoding != null) {
            return consoleEncoding;
        }

        Entry entry = findEncodingOrAliasEntry(new ByteList(Charset.defaultCharset().name().getBytes()));
        return entry == null ? ASCIIEncoding.INSTANCE : entry.getEncoding();
    }

    public IRubyObject[] getEncodingList() {
        return encodingList;
    }

    public Encoding loadEncoding(ByteList name) {
        Entry entry = findEncodingOrAliasEntry(name);
        if (entry == null) return null;
        Encoding enc = entry.getEncoding(); // load the encoding
        int index = enc.getIndex();
        if (index >= encodingIndex.length) {
            RubyEncoding tmp[] = new RubyEncoding[index + 4];
            System.arraycopy(encodingIndex, 0, tmp, 0, encodingIndex.length);
            encodingIndex = tmp;
        }
        encodingIndex[index] = (RubyEncoding)encodingList[entry.getIndex()];
        return enc;
    }

    public RubyEncoding getEncoding(Encoding enc) {
        int index = enc.getIndex();
        RubyEncoding rubyEncoding;
        if (index < encodingIndex.length && (rubyEncoding = encodingIndex[index]) != null) {
            return rubyEncoding;
        }

        enc = loadEncoding(new ByteList(enc.getName(), false));
        return encodingIndex[enc.getIndex()];
    }

    public void defineEncodings() {
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                    ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value;

            RubyEncoding encoding = RubyEncoding.newEncoding(runtime, e.bytes, e.p, e.end, ee.isDummy());
            encodingList[ee.getIndex()] = encoding;

            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                defineEncodingConstant(runtime, (RubyEncoding) encodingList[ee.getIndex()], constName);
            }
        }
    }

    public void defineAliases() {
        HashEntryIterator hei = aliases.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                    ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value;

            // The constant names must be treated by the the <code>encodingNames</code> helper.
            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                defineEncodingConstant(runtime, (RubyEncoding) encodingList[ee.getIndex()], constName);
            }
        }
    }

    private void defineEncodingConstant(Ruby runtime, RubyEncoding encoding, String constName) {
        runtime.getEncoding().defineConstant(constName, encoding);
    }

    public IRubyObject getDefaultExternal() {
        IRubyObject defaultExternal = convertEncodingToRubyEncoding(runtime.getDefaultExternalEncoding());

        if (defaultExternal.isNil()) {
            // TODO: MRI seems to default blindly to US-ASCII and we were using Charset default from Java...which is right?
            ByteList encodingName = ByteList.create("US-ASCII");
            Encoding encoding = runtime.getEncodingService().loadEncoding(encodingName);

            runtime.setDefaultExternalEncoding(encoding);
            defaultExternal = convertEncodingToRubyEncoding(encoding);
        }

        return defaultExternal;
    }

    public IRubyObject getDefaultInternal() {
        return convertEncodingToRubyEncoding(runtime.getDefaultInternalEncoding());
    }

    public IRubyObject convertEncodingToRubyEncoding(Encoding defaultEncoding) {
        return defaultEncoding != null ? getEncoding(defaultEncoding) : runtime.getNil();
    }

    public IRubyObject findEncodingObject(byte[] bytes) {
        Entry entry = findEncodingEntry(bytes);
        Encoding enc;
        if (entry != null) {
            enc = entry.getEncoding();
        } else {
            enc = ASCIIEncoding.INSTANCE;
        }
        return convertEncodingToRubyEncoding(enc);
    }

    public Encoding getJavaDefault() {
        return javaDefault;
    }

    public Encoding getEncodingFromObject(IRubyObject arg) {
        return getEncodingFromObjectCommon(arg, true);
    }

    // rb_to_encoding_index
    public Encoding getEncodingFromObjectNoError(IRubyObject arg) {
        return getEncodingFromObjectCommon(arg, false);
    }

    private Encoding getEncodingFromObjectCommon(IRubyObject arg, boolean error) {
        if (arg == null) return null;

        if (arg instanceof RubyEncoding) {
            return ((RubyEncoding) arg).getEncoding();
        }
        if (arg instanceof RubyFixnum) {
            final int id = (int) arg.convertToInteger().getLongValue();
            final String name = RubyNKF.NKFCharsetMap.get(id);
            if ( name != null ) return getEncodingFromNKFName(name);
        }
        if ( ( arg = arg.checkStringType19() ).isNil() ) {
            return null;
        }
        if ( ! ((RubyString) arg).getEncoding().isAsciiCompatible() ) {
            return null;
        }
        if (error) {
            return findEncoding((RubyString)arg);
        } else {
            return findEncodingNoError((RubyString)arg);
        }
    }

    private Encoding getEncodingFromNKFName(final String name) {
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            @SuppressWarnings("unchecked")
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>) hei.next());
            EncodingDB.Entry entry = e.value;
            String className = entry.getEncodingClass();
            if ( className.equals(name) ) {
                return entry.getEncoding();
            }
        }
        return null;
    }

    public Encoding getEncodingFromString(String string) {
        if (string == null) return null;

        ByteList name = new ByteList(ByteList.plain(string), false);
        checkAsciiEncodingName(name);

        SpecialEncoding special = SpecialEncoding.valueOf(name);
        if (special != null) {
            return special.toEncoding(runtime);
        }

        return findEncodingWithError(name);
    }

    /**
     * Find an encoding given a Ruby object, coercing it to a String in the process.
     *
     * @param str the object to coerce and use to look up encoding. The coerced String
     * must be ASCII-compatible.
     * @return the Encoding object found, nil (for internal), or raises ArgumentError
     */
    public Encoding findEncoding(IRubyObject str) {
        return findEncodingCommon(str, true);
    }

    /**
     * Find an encoding given a Ruby object, coercing it to a String in the process.
     * This version does not raise a Ruby error if it can't find the encoding,
     * and simply returns null.
     *
     * @param str the object to coerce and use to look up encoding. The coerced String
     * must be ASCII-compatible.
     * @return the Encoding object found, nil (for internal), or raises ArgumentError
     */
    public Encoding findEncodingNoError(IRubyObject str) {
        return findEncodingCommon(str, false);
    }

    public Encoding findEncodingNoError(ByteList str) {
        return findEncodingCommon(str, false);
    }

    private Encoding findEncodingCommon(IRubyObject str, boolean error) {
        ByteList name = str.convertToString().getByteList();
        return findEncodingCommon(name, error);
    }

    private Encoding findEncodingCommon(ByteList name, boolean error) {
        checkAsciiEncodingName(name);

        SpecialEncoding special = SpecialEncoding.valueOf(name);
        if (special != null) {
            return special.toEncoding(runtime);
        }

        if (error) return findEncodingWithError(name);

        Entry e = findEncodingOrAliasEntry(name);

        if (e == null) return null;

        return e.getEncoding();
    }

    /**
     * Find an encoding given a Ruby object, coercing it to a String in the process.
     *
     * @param str the object to coerce and use to look up encoding. The coerced String
     * must be ASCII-compatible.
     * @return the Encoding object found, nil (for internal), or raises ArgumentError
     */
    public Entry findEntry(IRubyObject str) {
        ByteList name = str.convertToString().getByteList();
        checkAsciiEncodingName(name);

        SpecialEncoding special = SpecialEncoding.valueOf(name);
        if (special != null) {
            return findEntryFromEncoding(special.toEncoding(runtime));
        }

        return findEntryWithError(name);
    }

    public IRubyObject rubyEncodingFromObject(IRubyObject str) {
        if (str instanceof RubyEncoding) {
            return str;
        }

        Entry entry = findEntry(str);
        if (entry == null) return runtime.getNil();
        return getEncodingList()[entry.getIndex()];
    }

    /**
     * Get a java.nio Charset for the given encoding, or null if impossible
     *
     * @param encoding the encoding
     * @return the charset
     */
    public Charset charsetForEncoding(Encoding encoding) {
        if (encoding.toString().equals("ASCII-8BIT")) {
            return Charset.forName("ISO-8859-1");
        }

        if (encoding == ISO8859_16Encoding.INSTANCE) {
            return ISO_8859_16.INSTANCE;
        }

        try {
            return Charset.forName(encoding.toString());
        } catch (UnsupportedCharsetException uce) {
            throw runtime.newEncodingCompatibilityError("no java.nio.charset.Charset found for encoding `" + encoding.toString() + "'");
        }
    }

    private void checkAsciiEncodingName(ByteList name) {
        if (!name.getEncoding().isAsciiCompatible()) {
            throw runtime.newArgumentError("invalid name encoding (non ASCII)");
        }
    }

    /**
     * Represents one of the four "special" internal encoding names: internal,
     * external, locale, or filesystem.
     */
    private enum SpecialEncoding {
        LOCALE, EXTERNAL, INTERNAL, FILESYSTEM;
        public static SpecialEncoding valueOf(ByteList name) {
            if (name.caseInsensitiveCmp(LOCALE_BL) == 0) {
                return LOCALE;
            } else if (name.caseInsensitiveCmp(EXTERNAL_BL) == 0) {
                return EXTERNAL;
            } else if (name.caseInsensitiveCmp(INTERNAL_BL) == 0) {
                return INTERNAL;
            } else if (name.caseInsensitiveCmp(FILESYSTEM_BL) == 0) {
                return FILESYSTEM;
            }
            return null;
        }

        public Encoding toEncoding(Ruby runtime) {
            EncodingService service = runtime.getEncodingService();
            switch (this) {
            case LOCALE: return service.getLocaleEncoding();
            case EXTERNAL: return runtime.getDefaultExternalEncoding();
            case INTERNAL: return runtime.getDefaultInternalEncoding();
            case FILESYSTEM:
                if (Platform.IS_WINDOWS) {
                    String fileEncoding = SafePropertyAccessor.getProperty("file.encoding", "UTF-8");
                    try {
                        return service.getEncodingFromString(fileEncoding);
                    } catch (RaiseException re) {
                        runtime.getWarnings().warning("could not load encoding for file.encoding of " + fileEncoding + ", using default external");
                        if (runtime.isDebug()) re.printStackTrace();
                    }
                }
                return runtime.getDefaultExternalEncoding();
            default:
                throw new RuntimeException("invalid SpecialEncoding: " + this);
            }
        }
    }

    /**
     * Find a non-special encoding, raising argument error if it does not exist.
     *
     * @param name the name of the encoding to look up
     * @return the Encoding object found, or raises ArgumentError
     */
    public Encoding findEncodingWithError(ByteList name) {
        return findEntryWithError(name).getEncoding();
    }

    /**
     * Find a non-special encoding Entry, raising argument error if it does not exist.
     *
     * @param name the name of the encoding to look up
     * @return the EncodingDB.Entry object found, or raises ArgumentError
     */
    private Entry findEntryWithError(ByteList name) {
        Entry e = findEncodingOrAliasEntry(name);

        if (e == null) throw runtime.newArgumentError("unknown encoding name - " + name);

        return e;
    }

    private Entry findEntryFromEncoding(Encoding e) {
        if (e == null) return null;
        return findEncodingEntry(new ByteList(e.getName()));
    }

    @Deprecated
    public Encoding getFileSystemEncoding(Ruby runtime) {
        return getFileSystemEncoding();
    }
}