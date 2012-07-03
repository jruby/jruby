package org.jruby.runtime.encoding;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.Ruby;
import org.jruby.RubyConverter;
import org.jruby.RubyEncoding;
import org.jruby.exceptions.MainExitException;
import org.jruby.platform.Platform;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.Console;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import org.jruby.RubyFixnum;
import org.jruby.ext.nkf.RubyNKF;

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

        if (runtime.is1_9()) {
            RubyEncoding.createEncodingClass(runtime);
            RubyConverter.createConverterClass(runtime);
            defineEncodings();
            defineAliases();

            // External should always have a value, but Encoding.external_encoding{,=} will lazily setup
            String encoding = runtime.getInstanceConfig().getExternalEncoding();
            if (encoding != null && !encoding.equals("")) {
                Encoding loadedEncoding = loadEncoding(ByteList.create(encoding));
                if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
                runtime.setDefaultExternalEncoding(loadedEncoding);
            } else {
                Encoding consoleEncoding = getConsoleEncoding();
                Encoding availableEncoding = consoleEncoding == null ? getLocaleEncoding() : consoleEncoding;
                runtime.setDefaultExternalEncoding(availableEncoding);
            }

            encoding = runtime.getInstanceConfig().getInternalEncoding();
            if (encoding != null && !encoding.equals("")) {
                Encoding loadedEncoding = loadEncoding(ByteList.create(encoding));
                if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
                runtime.setDefaultInternalEncoding(loadedEncoding);
            }
        }
    }

    /**
     * Since Java 1.6, class {@link java.io.Console} is available.
     * But the encoding or codepage of the underlying connected
     * console is currently private. Had to use Reflection to get it.
     *
     * @return console codepage
     */
    private Encoding getConsoleEncoding() {
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

    public Encoding getAscii8bitEncoding() {
        return ascii8bit;
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

    public Entry findAliasEntry(ByteList bytes) {
        return aliases.get(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public Entry findEncodingOrAliasEntry(ByteList bytes) {
        Entry e = findEncodingEntry(bytes);
        return e != null ? e : findAliasEntry(bytes);
    }

    // rb_locale_charmap...mostly
    public Encoding getLocaleEncoding() {
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

    private void defineEncodings() {
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value;
            RubyEncoding encoding = RubyEncoding.newEncoding(runtime, e.bytes, e.p, e.end, ee.isDummy());
            encodingList[ee.getIndex()] = encoding;
            defineEncodingConstants(runtime, encoding, e.bytes, e.p, e.end);
        }
    }

    private void defineAliases() {
        HashEntryIterator hei = aliases.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value; 
            RubyEncoding encoding = (RubyEncoding)encodingList[ee.getIndex()];
            defineEncodingConstants(runtime, encoding, e.bytes, e.p, e.end);
        }
    }

    private void defineEncodingConstants(Ruby runtime, RubyEncoding encoding, byte[]name, int p,
            int end) {
        Encoding enc = ASCIIEncoding.INSTANCE;
        int s = p;

        int code = name[s] & 0xff;
        if (enc.isDigit(code)) return;

        boolean hasUpper = false;
        boolean hasLower = false;
        if (enc.isUpper(code)) {
            hasUpper = true;
            while (++s < end && (enc.isAlnum(name[s] & 0xff) || name[s] == (byte)'_')) {
                if (enc.isLower(name[s] & 0xff)) hasLower = true;
            }
        }

        boolean isValid = false;
        if (s >= end) {
            isValid = true;
            defineEncodingConstant(runtime, encoding, name, p, end);
        }

        if (!isValid || hasLower) {
            if (!hasLower || !hasUpper) {
                do {
                    code = name[s] & 0xff;
                    if (enc.isLower(code)) hasLower = true;
                    if (enc.isUpper(code)) hasUpper = true;
                } while (++s < end && (!hasLower || !hasUpper));
            }

            byte[]constName = new byte[end - p];
            System.arraycopy(name, p, constName, 0, end - p);
            s = 0;
            code = constName[s] & 0xff;

            if (!isValid) {
                if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                for (; s < constName.length; ++s) {
                    if (!enc.isAlnum(constName[s] & 0xff)) constName[s] = (byte)'_';
                }
                if (hasUpper) {
                    defineEncodingConstant(runtime, encoding, constName, 0, constName.length);
                }
            }
            if (hasLower) {
                for (s = 0; s < constName.length; ++s) {
                    code = constName[s] & 0xff;
                    if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                }
                defineEncodingConstant(runtime, encoding, constName, 0, constName.length);
            }
        }
    }

    private void defineEncodingConstant(Ruby runtime, RubyEncoding encoding, byte[]constName,
            int constP, int constEnd) {
        runtime.getEncoding().defineConstant(new String(constName, constP , constEnd), encoding);
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

    public Encoding getJavaDefault() {
        return javaDefault;
    }

    public Encoding getEncodingFromObject(IRubyObject arg) {
        if (arg == null) return null;

        Encoding encoding = null;
        if (arg instanceof RubyEncoding) {
            encoding = ((RubyEncoding) arg).getEncoding();
        } else if (arg instanceof RubyFixnum && RubyNKF.NKFCharsetMap.containsKey((int)arg.convertToInteger().getLongValue())) {
            return getEncodingFromNKFId(arg);
        } else if (!arg.isNil()) {
            encoding = arg.convertToString().toEncoding(runtime);
        }
        return encoding;
    }
    
    private Encoding getEncodingFromNKFId(IRubyObject id) {
        String name = RubyNKF.NKFCharsetMap.get((int)id.convertToInteger().getLongValue());
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            EncodingDB.Entry ee = e.value;
            String className = ee.getEncodingClass();
            if (className.equals(name)) {
                Encoding enc = ee.getEncoding();
                return enc;
            }
        }
        return null;   
    }

    public Encoding getEncodingFromString(String string) {
        if (string == null) return null;

        ByteList name = new ByteList(ByteList.plain(string));
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
        ByteList name = str.convertToString().getByteList();
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
    public Entry findEntry(IRubyObject str) {
        ByteList name = str.convertToString().getByteList();
        checkAsciiEncodingName(name);

        SpecialEncoding special = SpecialEncoding.valueOf(name);
        if (special != null) {
            return findEntryFromEncoding(special.toEncoding(runtime));
        }

        return findEntryWithError(name);
    }

    /**
     * Look up the pre-existing RubyEncoding object for an EncodingDB.Entry.
     *
     * @param str
     * @return
     */
    public IRubyObject rubyEncodingFromObject(IRubyObject str) {
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
        Charset charset = encoding.getCharset();

        if (encoding.toString().equals("ASCII-8BIT")) {
            return Charset.forName("ASCII");
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
                // This needs to do something different on Windows. See encoding.c,
                // in the enc_set_filesystem_encoding function.
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
}