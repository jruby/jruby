/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007, 2008 Ola Bini <ola@ologix.com>
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

package org.jruby.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.platform.Platform;
import static org.jruby.util.ByteList.NULL_ARRAY;
import static org.jruby.util.ByteList.memcmp;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;
import static org.jruby.util.StringSupport.codePoint;

/**
 * This class exists as a counterpart to the dir.c file in
 * MRI source. It contains many methods useful for
 * File matching and Globbing.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Dir {
    public final static boolean DOSISH = Platform.IS_WINDOWS;
    public final static boolean CASEFOLD_FILESYSTEM = DOSISH;

    public final static int FNM_NOESCAPE = 0x01;
    public final static int FNM_PATHNAME = 0x02;
    public final static int FNM_DOTMATCH = 0x04;
    public final static int FNM_CASEFOLD = 0x08;

    public final static int FNM_SYSCASE = CASEFOLD_FILESYSTEM ? FNM_CASEFOLD : 0;

    public final static int FNM_NOMATCH = 1;
    public final static int FNM_ERROR   = 2;

    public final static byte[] EMPTY = NULL_ARRAY; // new byte[0];
    public final static byte[] SLASH = new byte[]{'/'};
    public final static byte[] STAR = new byte[]{'*'};
    public final static byte[] DOUBLE_STAR = new byte[]{'*','*'};

    private static boolean isdirsep(int c) {
        return c == '/' || DOSISH && c == '\\';
    }

    private static boolean isdirsep(byte c) {
        return isdirsep((int)(c & 0xFF));
    }

    private static int rb_path_next(byte[] _s, int s, int send) {
        while(s < send && !isdirsep(_s[s])) {
            s++;
        }
        return s;
    }

    static class FilenameMatch {
        int pcur;
        int scur;
        final int flags;
        final boolean escape;
        final boolean pathname;
        final boolean period;
        final boolean nocase;

        FilenameMatch(int pcur, int scur, int flags) {
            this.pcur = pcur;
            this.scur = scur;
            this.flags = flags;
            this.escape = (flags & FNM_NOESCAPE) == 0;
            this.pathname = (flags & FNM_PATHNAME) != 0;
            this.period = (flags & FNM_DOTMATCH) == 0;
            this.nocase = (flags & FNM_CASEFOLD) != 0;
        }

        // MRI: fnmatch_helper
        int helper(byte[] pbytes, int pend, byte[] sbytes, int send, Encoding enc) {
            int s = scur;
            int p = pcur;

            int ptmp = -1;
            int stmp = -1;

            if (s < send && p < pend) {
                if (period && sbytes[s] == '.' && pbytes[unescape(pbytes, p)] != '.') { /* leading period */
                    return FNM_NOMATCH;
                }
            }

            try { // RETURN macro in MRI
                while (true) {
                    // in place of expected C null char check that falls into default in switch below
                    if (p >= pend) {
                        return isEnd(sbytes, s, send) ? 0 : FNM_NOMATCH;
                    }

                    switch (pbytes[p]) {
                        case '*':
                            do p++; while (p < pend && pbytes[p] == '*');
                            if (isEnd(pbytes, unescape(pbytes, p), pend)) {
                                return 0;
                            }
                            if (isEnd(sbytes, s, send)) {
                                return FNM_NOMATCH;
                            }
                            ptmp = p;
                            stmp = s;
                            continue;

                        case '?':
                            if (isEnd(sbytes, s, send)) {
                                return FNM_NOMATCH;
                            }
                            p++;
                            s += StringSupport.length(enc, sbytes, s, send);
                            continue;

                        case '[':
                            if (isEnd(sbytes, s, send)) {
                                return FNM_NOMATCH;
                            }
                            int t = bracket(pbytes, p + 1, pend, sbytes, s, send, enc);
                            if (t != -1) {
                                p = t;
                                s += StringSupport.length(enc, sbytes, s, send);
                                continue;
                            }
                            break; // branch to failed

                        default:
                            p = unescape(pbytes, p);
                            if (isEnd(sbytes, s, send)) {
                                return isEnd(pbytes, p, pend) ? 0 : FNM_NOMATCH;
                            }
                            if (isEnd(pbytes, p, pend)) {
                                break; // branch to failed
                            }

                            // TODO: MBC
                            int r = StringSupport.preciseLength(enc, pbytes, p, pend);
                            if (!StringSupport.MBCLEN_CHARFOUND_P(r)) {
                                break; // branch to failed
                            }
                            if (r <= send - s && memcmp(pbytes, p, r, sbytes, s, r) == 0) {
                                p += r;
                                s += r;
                                continue;
                            }
                            if (DOSISH && (pathname && isdirsep(pbytes[p]) && isdirsep(sbytes[s]))) {
                            } else {
                                if (!nocase) {
                                    break; // branch to failed
                                }

                                // TODO: Use JOni case folding
                                if (Character.toLowerCase(codePoint(enc, pbytes, p, pend)) !=
                                        Character.toLowerCase(codePoint(enc, sbytes, s, send))) {
                                    break; // branch to failed
                                }

                            }
                            p += r;
                            s += StringSupport.length(enc, sbytes, s, send);
                            continue;
                    }

                    failed: // reached by breaking from above switch rather than continuing
                    if (ptmp != -1 && stmp != -1) {
                        p = ptmp;
                        stmp++; /* !ISEND(*stmp) */
                        s = stmp;
                        continue;
                    }
                    return FNM_NOMATCH;
                }
            } finally {
                // RETURN macro in MRI
                pcur = p;
                scur = s;
            }
        }

        public int bracket(byte[] pbytes, int p, int pend, byte[] sbytes, int s, int send, Encoding enc) {
            boolean not = false;
            boolean ok = false;
            int r;
            int c1, c2;

            if (p >= pend) return -1;

            if (pbytes[p] == '!' || pbytes[p] == '^') {
                not = true;
                p++;
            }

            while (true) {
                // in place of null terminator logic
                {
                    if (p >= pend) return -1;
                    if (pbytes[p] == ']') break;
                }

                int t1 = p;
                if(escape && pbytes[t1] == '\\') {
                    t1++;
                }
                if (t1 >= pend) return -1;
                p = t1 + (r = StringSupport.length(enc, pbytes, t1, pend));
                if (p >= pend) return -1;
                if (pbytes[p] == '-' && pbytes[p+1] != ']') {
                    int t2 = p + 1;
                    int r2;
                    if (escape && pbytes[t2] == '\\') {
                        t2++;
                    }
                    if (t2 >= pend) return -1;
                    p = t2 + (r2 = StringSupport.length(enc, pbytes, t2, pend));
                    if (ok) continue;
                    if ((r <= (send-s) && memcmp(pbytes, t1, r, sbytes, s, r) == 0) ||
                            (r2 <= (send-s) && memcmp(pbytes, t2, r2, sbytes, s, r2) == 0)) {
                        ok = true;
                        continue;
                    }
                    c1 = codePoint(enc, sbytes, s, send);
                    // TODO: Use JOni case folding
                    if (nocase) c1 = Character.toUpperCase(c1);
                    c2 = codePoint(enc, pbytes, t1, pend);
                    if (nocase) c2 = Character.toUpperCase(c2);
                    if (c1 < c2) continue;
                    c2 = codePoint(enc, pbytes, t2, pend);
                    if (nocase) c2 = Character.toUpperCase(c2);
                    if (c1 > c2) continue;
                } else {
                    if (ok) continue;
                    if (r <= (send-s) && memcmp(pbytes, t1, r, sbytes, s, r) == 0) {
                        ok = true;
                        continue;
                    }
                    if (!nocase) continue;
                    c1 = Character.toUpperCase(codePoint(enc, sbytes, s, send));
                    c2 = Character.toUpperCase(codePoint(enc, pbytes, p, pend));
                    if (c1 != c2) continue;
                }
                ok = true;
            }

            return ok == not ? -1 : p + 1;
        }

        private int unescape(byte[] bytes, int i) {
            if (escape && i < bytes.length && bytes[i] == '\\') {
                return i + 1;
            }
            return i;
        }

        private boolean isEnd(byte[] sbytes, int s, int send) {
            return s >= send || (pathname && isdirsep(sbytes[s]));
        }

    }

    public static int fnmatch(
            byte[] bytes, int pstart, int pend,
            byte[] string, int sstart, int send, int flags) {
        return fnmatch(bytes, pstart, pend, string, sstart, send, flags, ASCIIEncoding.INSTANCE);
    }

    public static int fnmatch(
            byte[] bytes, int pstart, int pend,
            byte[] string, int sstart, int send, int flags, Encoding enc) {

        // This method handles '**/' patterns and delegates to
        // fnmatch_helper for the main work.

        boolean period = (flags & FNM_DOTMATCH) == 0;
        boolean pathname = (flags & FNM_PATHNAME) != 0;

        int pat_pos = pstart;
        int str_pos = sstart;
        int ptmp = -1;
        int stmp = -1;

        if (pathname) {
            while (true) {
                if (isDoubleStarAndSlash(bytes, pat_pos)) {
                    do { pat_pos += 3; } while (isDoubleStarAndSlash(bytes, pat_pos));
                    ptmp = pat_pos;
                    stmp = str_pos;
                }

                FilenameMatch fnmatch = new FilenameMatch(pat_pos, str_pos, flags);
                if (fnmatch.helper(bytes, pend,
                        string, send, enc) == 0) {
                    pat_pos = fnmatch.pcur;
                    str_pos = fnmatch.scur;
                    while (str_pos < send && string[str_pos] != '/') {
                        str_pos++;
                    }
                    if (pat_pos < pend && str_pos < send) {
                        pat_pos++;
                        str_pos++;
                        continue;
                    }
                    if (pat_pos == pend && str_pos == send) {
                        return 0;
                    }
                }
                /* failed : try next recursion */
                if (ptmp != -1 && stmp != -1 && !(period && string[stmp] == '.')) {
                    stmp = nextSlashIndex(string, stmp, send);
                    if (stmp < send) {
                        pat_pos = ptmp;
                        stmp++;
                        str_pos = stmp;
                        continue;
                    }
                }
                return FNM_NOMATCH;
            }
        } else {
            FilenameMatch fnmatch = new FilenameMatch(pstart, sstart, flags);
            return fnmatch.helper(bytes, pend, string, send, enc);
        }

    }

    // are we at '**/'
    private static boolean isDoubleStarAndSlash(byte[] bytes, int pos) {
        if ((bytes.length - pos) <= 2) {
            return false; // not enough bytes
        }

        return bytes[pos] == '*'
            && bytes[pos + 1] == '*'
            && bytes[pos + 2] == '/';
    }

    // Look for slash, starting from 'start' position, until 'end'.
    private static int nextSlashIndex(byte[] bytes, int start, int end) {
        int idx = start;
        while (idx < end && idx < bytes.length && bytes[idx] != '/') {
            idx++;
        }
        return idx;
    }

    public static List<ByteList> push_glob(Ruby runtime, String cwd, ByteList globByteList, int flags) {
        if (globByteList.length() > 0) {
            final ArrayList<ByteList> result = new ArrayList<ByteList>();
            push_braces(runtime, cwd, result, new GlobPattern(globByteList, flags));
            return result;
        }

        return Collections.emptyList();
    }

    private static class GlobPattern {
        final byte[] bytes;
        final int begin;
        final int end;
        final Encoding enc;

        private int index;

        private final int flags;

        GlobPattern(ByteList bytes, int flags) {
            this(bytes.getUnsafeBytes(), bytes.getBegin(),  bytes.getBegin() + bytes.getRealSize(), bytes.getEncoding(), flags);
        }

        GlobPattern(byte[] bytes, int index, int end, Encoding enc, int flags) {
            this.bytes = bytes;
            this.index = index;
            this.begin = index;
            this.end = end;
            this.enc = enc;
            this.flags = flags;
        }

        public int findClosingIndexOf(int leftTokenIndex) {
            if (leftTokenIndex == -1 || leftTokenIndex > end) return -1;

            byte leftToken = bytes[leftTokenIndex];
            byte rightToken;

            switch (leftToken) {
            case '{': rightToken = '}'; break;
            case '[': rightToken = ']'; break;
            default: return -1;
            }

            int nest = 1; // leftToken made us start as nest 1
            index = leftTokenIndex + 1;
            while (hasNext()) {
                byte c = next();

                if (c == leftToken) {
                    nest++;
                } else if (c == rightToken && --nest == 0) {
                    return index();
                }
            }

            return -1;
        }

        public boolean hasNext() {
            return index < end;
        }

        public void reset() {
            index = begin;
        }

        public void setIndex(int value) {
            index = value;
        }

        // Get index of last read byte
        public int index() {
            return index - 1;
        }

        public int indexOf(byte c) {
            while (hasNext()) if (next() == c) return index();

            return -1;
        }

        public byte next() {
            return bytes[index++];
        }

    }

    private interface GlobFunc<T> {
        int call(byte[] ptr, int p, int len, Encoding enc, T ary);
    }

    private static class GlobArgs {
        final GlobFunc<List<ByteList>> func;
        final List<ByteList> arg;
        private int c = -1;

        GlobArgs(GlobFunc<List<ByteList>> func, List<ByteList> arg) {
            this.func = func;
            this.arg = arg;
        }
    }

    private final static GlobFunc<List<ByteList>> push_pattern = new GlobFunc<List<ByteList>>() {
        public int call(byte[] ptr, int p, int len, Encoding enc, List<ByteList> ary) {
            ary.add(new ByteList(ptr, p, len, enc, true));
            return 0;
        }
    };
    private final static GlobFunc<GlobArgs> glob_caller = new GlobFunc<GlobArgs>() {
        public int call(byte[] ptr, int p, int len, Encoding enc, GlobArgs args) {
            args.c = p;
            return args.func.call(ptr, args.c, len, enc, args.arg);
        }
    };

    /*
     * Process {}'s (example: Dir.glob("{jruby,jython}/README*")
     */
    private static int push_braces(Ruby runtime, String cwd, List<ByteList> result, GlobPattern pattern) {
        pattern.reset();
        int lbrace = pattern.indexOf((byte) '{'); // index of left-most brace
        int rbrace = pattern.findClosingIndexOf(lbrace);// index of right-most brace

        // No, mismatched or escaped braces..Move along..nothing to see here
        if (lbrace == -1 || rbrace == -1 ||
                lbrace > 0 && pattern.bytes[lbrace-1] == '\\' ||
                rbrace > 0 && pattern.bytes[rbrace-1] == '\\') {
            ByteList unescaped = new ByteList(pattern.bytes.length - 1);
            unescaped.setEncoding(pattern.enc);
            for (int i = pattern.begin; i < pattern.end; i++) {
                byte b = pattern.bytes[i];
                if (b == '\\' && i < pattern.bytes.length - 1) {
                    byte next_b = pattern.bytes[i + 1];
                    if (next_b != '{' && next_b != '}') {
                        unescaped.append(b);
                    }
                } else {
                    unescaped.append(b);
                }
            }
            return push_globs(runtime, cwd, result, unescaped, pattern.flags);
        }

        // Peel onion...make subpatterns out of outer layer of glob and recall with each subpattern
        // Example: foo{a{c},b}bar -> fooa{c}bar, foobbar
        final ByteList bytes = new ByteList(20);
        bytes.setEncoding(pattern.enc);
        int middleRegionIndex;
        int i = lbrace;
        while (pattern.bytes[i] != '}') {
            middleRegionIndex = i + 1;
            for (i = middleRegionIndex; i < pattern.end && pattern.bytes[i] != '}'; i++) {
                if (pattern.bytes[i] == ',') {
                    if (i > pattern.begin && pattern.bytes[i-1] == '\\') continue;
                    break;
                }
                if (pattern.bytes[i] == '{') i = pattern.findClosingIndexOf(i); // skip inner braces
            }

            bytes.length(0);
            bytes.append(pattern.bytes, pattern.begin, lbrace - pattern.begin);
            bytes.append(pattern.bytes, middleRegionIndex, i - middleRegionIndex);
            bytes.append(pattern.bytes, rbrace + 1, pattern.end - (rbrace + 1));
            int status = push_braces(runtime, cwd, result, new GlobPattern(bytes, pattern.flags));
            if (status != 0) return status;
        }

        return 0; // All braces pushed..
    }

    private static int push_globs(Ruby runtime, String cwd, List<ByteList> ary, ByteList pattern, int flags) {
        flags |= FNM_SYSCASE;
        return glob_helper(runtime, cwd, pattern, -1, flags, glob_caller, new GlobArgs(push_pattern, ary));
    }

    public static ArrayList<String> braces(String pattern, int flags, ArrayList<String> patterns) {
        boolean escape = (flags & FNM_NOESCAPE) == 0;

        int rbrace = -1;
        int lbrace = -1;

        // Do a quick search for a { to start the search better
        int i = pattern.indexOf('{');

        if(i >= 0) {
            int nest = 0;

            while(i < pattern.length()) {
                char c = pattern.charAt(i);

                if(c == '{') {
                    if(nest == 0) {
                        lbrace = i;
                    }
                    nest += 1;
                }

                if(c == '}') {
                    nest -= 1;
                }

                if(nest == 0) {
                    rbrace = i;
                    break;
                }

                if(c == '\\' && escape) {
                    i += 1;
                }

                i += 1;
            }
        }

        // There was a full {} expression detected, expand each part of it
        // recursively.
        if(lbrace >= 0 && rbrace >= 0) {
            int pos = lbrace;
            String front = pattern.substring(0, lbrace);
            String back = pattern.substring(rbrace + 1, pattern.length());

            while(pos < rbrace) {
                int nest = 0;
                pos += 1;
                int last = pos;

                while(pos < rbrace && !(pattern.charAt(pos) == ',' && nest == 0)) {
                    if(pattern.charAt(pos) == '{') {
                        nest += 1;
                    }
                    if(pattern.charAt(pos) == '}') {
                        nest -= 1;
                    }

                    if(pattern.charAt(pos) == '\\' && escape) {
                        pos += 1;
                        if(pos == rbrace) {
                            break;
                        }
                    }

                    pos += 1;
                }
                String brace_pattern = front + pattern.substring(last, pos) + back;
                patterns.add(brace_pattern);

                braces(brace_pattern, flags, patterns);
            }
        }

        return patterns;
    }

    private static boolean has_magic(byte[] bytes, int begin, int end, int flags) {
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;
        int open = 0;

        for (int i = begin; i < end; i++) {
            switch (bytes[i]) {
            case '?':
            case '*':
                return true;
            case '[':	/* Only accept an open brace if there is a close */
                open++;	/* brace to match it.  Bracket expressions must be */
                continue;	/* complete, according to Posix.2 */
            case ']':
                if (open > 0) return true;

                continue;
            case '\\':
                // We treat backslash as a magic character, since otherwise logic in glob_helper does not unescape.
                // See jruby/jruby#5333 for more details.

//                if (escape && i == end) return false;
//                break;

                if (escape) return true; // force magic logic whenever there's escaped chars

                continue;
            default:
                if (FNM_SYSCASE == 0 && nocase && Character.isLetter((char)(bytes[i] & 0xFF))) return true;
            }
        }

        return false;
    }

    private static int remove_backslashes(byte[] bytes, int index, int end) {
        int i = index;
        for ( ; index < end; index++, i++ ) {
            if (bytes[index] == '\\' && ++index == end) break;

            bytes[i] = bytes[index];
        }
        return i;
    }

    private static int indexOf(byte[] bytes, int begin, int end, final byte ch) {
        for ( int i = begin; i < end; i++ ) {
            if ( bytes[i] == ch ) return i;
        }

        return -1;
    }

    private static byte[] extract_path(byte[] bytes, int begin, int end) {
        int len = end - begin;

        if (len > 1 && bytes[end-1] == '/' && (!DOSISH || (len < 2 || bytes[end-2] != ':'))) len--;

        byte[] alloc = new byte[len];
        System.arraycopy(bytes,begin,alloc,0,len);
        return alloc;
    }

    private static byte[] extract_elem(byte[] bytes, int begin, int end) {
        int elementEnd = indexOf(bytes, begin, end, (byte)'/');
        if (elementEnd == -1) elementEnd = end;

        return extract_path(bytes, begin, elementEnd);
    }

    // Win drive letter X:/
    private static boolean beginsWithDriveLetter(byte[] path, int begin, int end) {
        return DOSISH && begin + 2 < end && path[begin + 1] == ':' && isdirsep(path[begin + 2]);
    }

    // Is this nothing or literally root directory for the OS.
    private static boolean isRoot(byte[] base) {
        int length = base.length;

        return length == 0 ||  // empty
               length == 1 && isdirsep(base[0]) || // Just '/'
               length == 3 && beginsWithDriveLetter(base, 0, length); // Just X:/
    }

    private static boolean isAbsolutePath(byte[] path, int begin, int length) {
        return isdirsep(path[begin]) || beginsWithDriveLetter(path, begin, length);
    }

    private static String[] files(final FileResource directory) {
        final String[] files = directory.list();
        return files == null ? EMPTY_STRING_ARRAY : files;
    }

    private static final class DirGlobber {
        public final ByteList link;

        DirGlobber(ByteList link) { this.link = link; }
    }

    private static boolean isSpecialFile(String name) {
        int length = name.length();

        if (length < 1 || length > 3 || name.charAt(0) != '.') return false;
        if (length == 1) return true;
        char c = name.charAt(1);
        if (length == 2 && (c == '.' || c == '/')) return true;
        return c == '.' && name.charAt(2) == '/';
    }

    private static int addToResultIfExists(Ruby runtime, String cwd, byte[] bytes, int begin, int end, Encoding enc, int flags, GlobFunc<GlobArgs> func, GlobArgs arg) {
        final String fileName = new String(bytes, begin, end - begin, enc.getCharset());

        // FIXME: Ultimately JRubyFile.createResource should do this but all 1.7.x is only selectively honoring raw
        // paths and using system drive make it absolute.  MRI does this on many methods we don't.
        if (Platform.IS_WINDOWS && cwd == null && !fileName.isEmpty() && fileName.charAt(0) == '/') {
            cwd = System.getenv("SYSTEMDRIVE");
            if (cwd == null) cwd = "C:";
            cwd = cwd + "/";
        }

        FileResource file = JRubyFile.createResource(runtime, cwd, fileName);

        if (file.exists()) {
            byte[] newBytes;
            
            // get the real filename (case-sensitive)
            if(file.isFile() && cwd != null) {
                String path = file.isSymLink() ? file.absolutePath() : file.canonicalPath();

                // compensate for missing slash
                if (fileName.endsWith("/")) {
                    path += "/";
                }

                if(fileName.contains("./")) {
                    newBytes = ("./" + path.substring((path.length() - end + 2))).getBytes();
                } else {
                    int tempBegin = path.length() - fileName.length();
                    newBytes = path.substring(tempBegin).getBytes();
                }

                end = newBytes.length;
            } else {
                newBytes = bytes;
            }

            return func.call(newBytes, begin, end - begin, enc, arg);
        }

        return 0;
    }

    private static int glob_helper(Ruby runtime, String cwd, ByteList path, int sub, int flags, GlobFunc<GlobArgs> func, GlobArgs arg) {
        final int begin = path.getBegin();
        final int end = begin + path.getRealSize();
        final Encoding enc = path.getEncoding();
        return glob_helper(runtime, cwd, path.getUnsafeBytes(), begin, end, enc, sub, flags, func, arg);
    }

    private static int glob_helper(Ruby runtime, String cwd,
        byte[] path, int begin, int end, Encoding enc, int sub,
        final int flags, GlobFunc<GlobArgs> func, GlobArgs arg) {
        int status = 0;

        int ptr = sub != -1 ? sub : begin;

        if ( ! has_magic(path, ptr, end, flags) ) {
            if ( DOSISH || (flags & FNM_NOESCAPE) == 0 ) {
                if ( sub != -1 ) { // can modify path (our internal buf[])
                    end = remove_backslashes(path, sub, end);
                }
                else {
                    final int len = end - begin;
                    final byte[] newPath = new byte[len];
                    System.arraycopy(path, begin, newPath, 0, len);
                    begin = 0; end = remove_backslashes(newPath, 0, len);
                    path = newPath;
                }
            }

            if (end > begin) {
                if ( isAbsolutePath(path, begin, end) ) {
                    status = addToResultIfExists(runtime, null, path, begin, end, enc, flags, func, arg);
                } else {
                    status = addToResultIfExists(runtime, cwd, path, begin, end, enc, flags, func, arg);
                }
            }

            return status;
        }

        final ArrayList<DirGlobber> links = new ArrayList<DirGlobber>();

        ByteList buf = new ByteList(20);
        buf.setEncoding(enc);
        FileResource resource;

        mainLoop: while(ptr != -1 && status == 0) {
            if ( path[ptr] == '/' ) ptr++;

            final int SLASH_INDEX = indexOf(path, ptr, end, (byte) '/');
            if (has_magic(path, ptr, SLASH_INDEX == -1 ? end : SLASH_INDEX, flags) ) {
                finalize: do {
                    byte[] base = extract_path(path, begin, ptr);
                    byte[] dir = begin == ptr ? new byte[] { '.' } : base;
                    byte[] magic = extract_elem(path, ptr, end);
                    boolean recursive = false;

                    resource = JRubyFile.createResource(runtime, cwd, new String(dir, 0, dir.length, enc.getCharset()));
                    if ( resource.isDirectory() ) {
                        if ( SLASH_INDEX != -1 && Arrays.equals(magic, DOUBLE_STAR) ) {
                            final int lengthOfBase = base.length;
                            recursive = true;
                            buf.length(0);
                            buf.append(base);
                            int nextStartIndex;
                            int indexOfSlash = SLASH_INDEX;
                            do {
                                nextStartIndex = indexOfSlash + 1;
                                indexOfSlash = indexOf(path, nextStartIndex, end, (byte) '/');
                                magic = extract_elem(path, nextStartIndex, end);
                            } while(Arrays.equals(magic, DOUBLE_STAR) && indexOfSlash != -1);

                            int remainingPathStartIndex;
                            if(Arrays.equals(magic, DOUBLE_STAR)) {
                                remainingPathStartIndex = nextStartIndex;
                            } else {
                                remainingPathStartIndex = nextStartIndex - 1;
                            }
                            remainingPathStartIndex = lengthOfBase > 0 ? remainingPathStartIndex : remainingPathStartIndex + 1;
                            buf.append(path, remainingPathStartIndex, end - remainingPathStartIndex);
                            status = glob_helper(runtime, cwd, buf, lengthOfBase, flags, func, arg);
                            if ( status != 0 ) break finalize;
                        }
                    } else {
                        break mainLoop;
                    }

                    final String[] files = files(resource);

                    for ( int i = 0; i < files.length; i++ ) {
                        final String file = files[i];
                        final byte[] fileBytes = getBytesInUTF8(file);
                        if (recursive) {
                            if ( fnmatch(STAR, 0, 1, fileBytes, 0, fileBytes.length, flags) != 0) {
                                continue;
                            }
                            buf.length(0);
                            buf.append(base);
                            buf.append( isRoot(base) ? EMPTY : SLASH );
                            buf.append( getBytesInUTF8(file) );
                            resource = JRubyFile.createResource(runtime, cwd, new String(buf.unsafeBytes(), buf.begin(), buf.length(), enc.getCharset()));
                            if ( !resource.isSymLink() && resource.isDirectory() && !".".equals(file) && !"..".equals(file) ) {
                                final int len = buf.getRealSize();
                                buf.append(SLASH);
                                buf.append(DOUBLE_STAR);
                                buf.append(path, SLASH_INDEX, end - SLASH_INDEX);
                                status = glob_helper(runtime, cwd, buf, buf.getBegin() + len, flags, func, arg);
                                if ( status != 0 ) break;
                            }
                            continue;
                        }
                        if ( fnmatch(magic, 0, magic.length, fileBytes, 0, fileBytes.length, flags) == 0 ) {
                            buf.length(0);
                            buf.append(base);
                            buf.append( isRoot(base) ? EMPTY : SLASH );
                            buf.append( getBytesInUTF8(file) );
                            if ( SLASH_INDEX == -1 ) {
                                status = func.call(buf.getUnsafeBytes(), 0, buf.getRealSize(), enc, arg);
                                if ( status != 0 ) break;
                                continue;
                            }
                            links.add(new DirGlobber(buf));
                            buf = new ByteList(20);
                            buf.setEncoding(enc);
                        }
                    }
                } while(false);

                if ( links.size() > 0 ) {
                    for ( DirGlobber globber : links ) {
                        final ByteList link = globber.link;
                        if ( status == 0 ) {
                            resource = JRubyFile.createResource(runtime, cwd, RubyString.byteListToString(link));
                            if ( resource.isDirectory() ) {
                                final int len = link.getRealSize();
                                buf.length(0);
                                buf.append(link);
                                buf.append(path, SLASH_INDEX, end - SLASH_INDEX);
                                status = glob_helper(runtime, cwd, buf, buf.getBegin() + len, flags, func, arg);
                            }
                        }
                    }
                    break mainLoop;
                }
            }
            ptr = SLASH_INDEX;
        }

        return status;
    }

    private static byte[] getBytesInUTF8(final String str) {
        return RubyEncoding.encodeUTF8(str);
    }

    /**
     * @deprecated No replacement; not intended to be made public
     */
    @Deprecated
    public static int range(byte[] _pat, int pat, int pend, char test, int flags) {
        return new FilenameMatch(pat, 0, flags).helper(_pat, pend, new byte[] {(byte) test}, 1, ASCIIEncoding.INSTANCE);
    }
}
