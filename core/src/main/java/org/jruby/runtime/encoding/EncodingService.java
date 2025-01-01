package org.jruby.runtime.encoding;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.ISO8859_16Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.spi.ISO_8859_16;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.javasupport.Java;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.Console;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyString;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.Options;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Warn.warn;

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
    private static final Pattern MS_CP_PATTERN = Pattern.compile("^MS([0-9]+)$");
    private Encoding consoleEncoding;

    public EncodingService(Ruby runtime) {
        this.runtime = runtime;
        encodings = EncodingDB.getEncodings();
        aliases = EncodingDB.getAliases();
        ascii8bit = encodings.get("ASCII-8BIT".getBytes()).getEncoding();

        String javaDefaultCharset = Charset.defaultCharset().name();
        Entry javaDefaultEntry = findEncodingOrAliasEntry(javaDefaultCharset.getBytes());
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

        Encoding consoleEncoding = this.consoleEncoding;

        if (consoleEncoding != null) return consoleEncoding;

        try {
            String stdoutEncoding = SafePropertyAccessor.getProperty("sun.stdout.encoding");
            Charset cs = Charset.forName(stdoutEncoding);
            this.consoleEncoding = consoleEncoding = loadEncoding(ByteList.create(cs.name()));
        } catch (Throwable t) {
            // try using System.console
            try {
                Console console = System.console();
                if (console != null) {
                    final String CONSOLE_CHARSET = "cs";
                    Field fcs = Console.class.getDeclaredField(CONSOLE_CHARSET);
                    Java.trySetAccessible(fcs);
                    Charset cs = (Charset) fcs.get(console);
                    this.consoleEncoding = consoleEncoding = loadEncoding(ByteList.create(cs.name()));
                }
            } catch (Throwable e) {
                // leave it null
            }
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

    private static ByteList defaultCharsetName;

    // rb_locale_charmap...mostly
    public Encoding getLocaleEncoding() {
        final Encoding consoleEncoding = getConsoleEncoding();

        if (consoleEncoding != null) {
            return consoleEncoding;
        }

        ByteList encName = defaultCharsetName;
        if (encName == null) {
            encName = new ByteList(Charset.defaultCharset().name().getBytes(), false);
            defaultCharsetName = encName;
        }

        final Entry entry = findEncodingOrAliasEntry(encName);
        return entry == null ? ASCIIEncoding.INSTANCE : entry.getEncoding();
    }

    public IRubyObject[] getEncodingList() {
        return encodingList;
    }

    public Encoding loadEncoding(ByteList name) {
        Entry entry = findEncodingOrAliasEntry(name);
        if (entry == null) return null;
        loadEncodingEntry(entry); // should not attempt RubyEncoding#getEncoding() here
        return entry.getEncoding();
    }

    private RubyEncoding loadEncodingEntry(final Entry entry) {
        Encoding enc = entry.getEncoding(); // load the encoding
        int index = enc.getIndex();
        RubyEncoding[] encodingIndex = this.encodingIndex;
        if (index >= encodingIndex.length) {
            encodingIndex = this.encodingIndex = Arrays.copyOf(encodingIndex, index + 4);
        }
        return encodingIndex[index] = (RubyEncoding) encodingList[entry.getIndex()];
    }

    public RubyEncoding getEncoding(Encoding enc) {
        int index = enc.getIndex();
        RubyEncoding rubyEncoding;
        RubyEncoding[] encodingIndex = this.encodingIndex;
        if (index < encodingIndex.length && (rubyEncoding = encodingIndex[index]) != null) {
            return rubyEncoding;
        }
        // loadEncoding :
        Entry entry = findEncodingOrAliasEntry(enc.getName());
        return loadEncodingEntry(entry);
    }

    public void defineEncodings(ThreadContext context) {
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                    ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value;

            RubyEncoding encoding = RubyEncoding.newEncoding(context.runtime, e.bytes, e.p, e.end, ee.isDummy());
            encodingList[ee.getIndex()] = encoding;

            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                defineEncodingConstant(context, (RubyEncoding) encodingList[ee.getIndex()], constName);
            }
        }
    }

    public void defineAliases(ThreadContext context) {
        HashEntryIterator i = aliases.entryIterator();
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                    ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            Entry entry = e.value;

            // The constant names must be treated by the the <code>encodingNames</code> helper.
            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                defineEncodingConstant(context, (RubyEncoding) encodingList[entry.getIndex()], constName);
            }
        }
    }

    private void defineEncodingConstant(ThreadContext context, RubyEncoding encoding, String constName) {
        runtime.getEncoding().defineConstant(context, constName, encoding);
    }

    public IRubyObject getDefaultExternal() {
        Encoding defaultEncoding = runtime.getDefaultExternalEncoding();
        if (defaultEncoding == null) {
            defaultEncoding = getLocaleEncoding();
        }
        return getEncoding(defaultEncoding);
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
        if ( ( arg = arg.checkStringType() ).isNil() ) {
            return null;
        }
        if ( ! ((RubyString) arg).getEncoding().isAsciiCompatible() ) {
            return null;
        }
        return findEncodingCommon(((RubyString) arg).getByteList(), error);
    }

    @Deprecated(since = "10.0")
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
     * @return the Encoding object found, nil (for internal)
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
            Encoding specialEncoding = special.toEncoding(runtime);
            if (specialEncoding == null) specialEncoding = ASCIIEncoding.INSTANCE;
            return specialEncoding;
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
        if (encoding == ASCIIEncoding.INSTANCE) {
            return RubyEncoding.ISO;
        }

        if (encoding == ISO8859_16Encoding.INSTANCE) {
            return ISO_8859_16.INSTANCE;
        }

        try {
            return EncodingUtils.charsetForEncoding(encoding);
        } catch (UnsupportedCharsetException uce) {
            throw runtime.newEncodingCompatibilityError("no java.nio.charset.Charset found for encoding '" + encoding.toString() + "'");
        }
    }

    private void checkAsciiEncodingName(ByteList name) {
        if (!name.getEncoding().isAsciiCompatible()) {
            throw argumentError(runtime.getCurrentContext(), "invalid encoding name (non ASCII)");
        }
    }

    @Deprecated(since = "10.0")
    public Encoding getWindowsFilesystemEncoding(Ruby runtime) {
        return getWindowsFilesystemEncoding(runtime.getCurrentContext());
    }

    public Encoding getWindowsFilesystemEncoding(ThreadContext context) {
        String encoding = Options.WINDOWS_FILESYSTEM_ENCODING.load();
        Encoding filesystemEncoding = loadEncoding(ByteList.create(encoding));

        // Use default external if file.encoding does not point at an encoding we recognize
        if (filesystemEncoding == null) {
            // if the encoding name matches /^MS[0-9]+/ we can assume it's a Windows code page and use CP### to look it up.
            Matcher match = MS_CP_PATTERN.matcher(encoding);
            if (match.find()) filesystemEncoding = loadEncoding(ByteList.create("CP" + match.group(1)));
        }

        if (filesystemEncoding == null) {
            warn(context, "unrecognized system encoding \"" + encoding + "\", using default external");
            filesystemEncoding = context.runtime.getDefaultExternalEncoding();
        }

        return filesystemEncoding;
    }

    // MRI: env_encoding
    public Encoding getEnvEncoding() {
        return Platform.IS_WINDOWS ? UTF8Encoding.INSTANCE : getLocaleEncoding();
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
            switch (this) {
            case LOCALE: return runtime.getEncodingService().getLocaleEncoding();
            case EXTERNAL: return runtime.getDefaultExternalEncoding();
            case INTERNAL: return runtime.getDefaultInternalEncoding();
            case FILESYSTEM: return runtime.getDefaultFilesystemEncoding();
            default:
                throw new AssertionError("invalid SpecialEncoding: " + this);
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

        if (e == null) throw argumentError(runtime.getCurrentContext(), "unknown encoding name - " + name);

        return e;
    }

    private Entry findEntryFromEncoding(Encoding e) {
        if (e == null) return null;
        return findEncodingEntry(e.getName());
    }

    @Deprecated
    public Encoding getFileSystemEncoding(Ruby runtime) {
        return getFileSystemEncoding();
    }
}