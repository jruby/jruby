package org.jruby.runtime.encoding;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public final class EncodingService {
    private final Ruby runtime;

    private final CaseInsensitiveBytesHash<Entry> encodings;
    private final CaseInsensitiveBytesHash<Entry> aliases;

    // for fast lookup: encoding entry => org.jruby.RubyEncoding
    private final IRubyObject[] encodingList;
    // for fast lookup: org.joni.encoding.Encoding => org.jruby.RubyEncoding
    private RubyEncoding[] encodingIndex = new RubyEncoding[4];

    public EncodingService (Ruby runtime) {
        this.runtime = runtime;

        // TODO: make it cross runtime safe by COW or eager copy
        encodings = EncodingDB.getEncodings();
        aliases = EncodingDB.getAliases();

        encodingList = new IRubyObject[encodings.size()];
        defineEncodings();
        defineAliases();        
    }

    public CaseInsensitiveBytesHash<Entry> getEncodings() {
        return encodings;
    }

    public CaseInsensitiveBytesHash<Entry> getAliases() {
        return aliases;
    }

    public Entry findEncodingEntry(ByteList bytes) {
        return encodings.get(bytes.bytes, bytes.begin, bytes.begin + bytes.realSize);
    }

    public Entry findAliasEntry(ByteList bytes) {
        return aliases.get(bytes.bytes, bytes.begin, bytes.begin + bytes.realSize);
    }

    public Entry findEncodingOrAliasEntry(ByteList bytes) {
        Entry e = findEncodingEntry(bytes);
        return e != null ? e : findAliasEntry(bytes);
    }

    public IRubyObject[] getEncodingList() {
        return encodingList;
    }

    public Encoding loadEncoding(ByteList name) {
        Entry entry = findEncodingOrAliasEntry(name);
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
        loadEncoding(new ByteList(enc.getName(), false));
        return encodingIndex[index];
    }

    private void defineEncodings() {
        HashEntryIterator hei = encodings.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value; 
            RubyEncoding encoding = RubyEncoding.newEncoding(runtime, e.bytes, e.p, e.end, ee.isDummy());
            encodingList[ee.getIndex()] = encoding;
            defineEncodingConstants(encoding, e.bytes, e.p, e.end);
        }
    }

    private void defineAliases() {
        HashEntryIterator hei = aliases.entryIterator();
        while (hei.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)hei.next());
            Entry ee = e.value; 
            RubyEncoding encoding = (RubyEncoding)encodingList[ee.getIndex()];  
            defineEncodingConstants(encoding, e.bytes, e.p, e.end);
        }
    }

    private void defineEncodingConstants(RubyEncoding encoding, byte[]name, int p, int end) {
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
            defineEncodingConstant(encoding, name, p, end);
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
                    defineEncodingConstant(encoding, constName, 0, constName.length);
                }
            }
            if (hasLower) {
                for (s = 0; s < constName.length; ++s) {
                    code = constName[s] & 0xff;
                    if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                }
                defineEncodingConstant(encoding, constName, 0, constName.length);
            }
        }
    }

    private void defineEncodingConstant(RubyEncoding encoding, byte[]constName, int constP, int constEnd) {
        runtime.getEncoding().defineConstant(new String(constName, constP , constEnd), encoding);
    }
}