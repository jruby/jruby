/***** BEGIN LICENSE BLOCK *****
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
package org.jruby.truffle.util;

import org.jruby.truffle.util.ByteList;

import java.util.ArrayList;
import java.util.List;

import static org.jruby.truffle.util.ByteList.NULL_ARRAY;

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

    private static boolean isdirsep(char c) {
        return c == '/' || DOSISH && c == '\\';
    }

    private static boolean isdirsep(byte c) {
        return isdirsep((char)(c & 0xFF));
    }

    private static int rb_path_next(byte[] _s, int s, int send) {
        while(s < send && !isdirsep(_s[s])) {
            s++;
        }
        return s;
    }

    private static int fnmatch_helper(byte[] bytes, int pstart, int pend, byte[] string, int sstart, int send, int flags) {
        char test;
        int s = sstart;
        int pat = pstart;
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean pathname = (flags & FNM_PATHNAME) != 0;
        boolean period = (flags & FNM_DOTMATCH) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;

        while(pat<pend) {
            char c = (char)(bytes[pat++] & 0xFF);
            switch(c) {
            case '?':
                if(s >= send || (pathname && isdirsep(string[s])) ||
                   (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '*':
                while(pat < pend && (c = (char)(bytes[pat++] & 0xFF)) == '*') {}
                if(s < send && (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                if(pat > pend || (pat == pend && c == '*')) {
                    if(pathname && rb_path_next(string, s, send) < send) {
                        return FNM_NOMATCH;
                    } else {
                        return 0;
                    }
                } else if((pathname && isdirsep(c))) {
                    s = rb_path_next(string, s, send);
                    if(s < send) {
                        s++;
                        break;
                    }
                    return FNM_NOMATCH;
                }
                test = (char)(escape && c == '\\' && pat < pend ? (bytes[pat] & 0xFF) : c);
                test = Character.toLowerCase(test);
                pat--;
                while(s < send) {
                    if((c == '?' || c == '[' || Character.toLowerCase((char) string[s]) == test) &&
                       fnmatch(bytes, pat, pend, string, s, send, flags | FNM_DOTMATCH) == 0) {
                        return 0;
                    } else if((pathname && isdirsep(string[s]))) {
                        break;
                    }
                    s++;
                }
                return FNM_NOMATCH;
            case '[':
                if(s >= send || (pathname && isdirsep(string[s]) ||
                                 (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1])))))) {
                    return FNM_NOMATCH;
                }
                pat = range(bytes, pat, pend, (char)(string[s]&0xFF), flags);
                if(pat == -1) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '\\':
                if (escape) {
                    if (pat >= pend) {
                        c = '\\';
                    } else {
                        c = (char)(bytes[pat++] & 0xFF);
                    }
                }
            default:
                if(s >= send) {
                    return FNM_NOMATCH;
                }
                if(DOSISH && (pathname && isdirsep(c) && isdirsep(string[s]))) {
                } else {
                    if (nocase) {
                        if(Character.toLowerCase((char)c) != Character.toLowerCase((char)string[s])) {
                            return FNM_NOMATCH;
                        }

                    } else {
                        if(c != (char)(string[s] & 0xFF)) {
                            return FNM_NOMATCH;
                        }
                    }

                }
                s++;
                break;
            }
        }
        return s >= send ? 0 : FNM_NOMATCH;
    }

    public static int fnmatch(
            byte[] bytes, int pstart, int pend,
            byte[] string, int sstart, int send, int flags) {

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

                int patSlashIdx = nextSlashIndex(bytes, pat_pos, pend);
                int strSlashIdx = nextSlashIndex(string, str_pos, send);

                if (fnmatch_helper(bytes, pat_pos, patSlashIdx,
                        string, str_pos, strSlashIdx, flags) == 0) {
                    if (patSlashIdx < pend && strSlashIdx < send) {
                        pat_pos = ++patSlashIdx;
                        str_pos = ++strSlashIdx;
                        continue;
                    }
                    if (patSlashIdx == pend && strSlashIdx == send) {
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
            return fnmatch_helper(bytes, pstart, pend, string, sstart, send, flags);
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

    public static int range(byte[] _pat, int pat, int pend, char test, int flags) {
        boolean not;
        boolean ok = false;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;
        boolean escape = (flags & FNM_NOESCAPE) == 0;

        not = _pat[pat] == '!' || _pat[pat] == '^';
        if(not) {
            pat++;
        }

        if (nocase) {
            test = Character.toLowerCase(test);
        }

        while(_pat[pat] != ']') {
            char cstart, cend;
            if(escape && _pat[pat] == '\\') {
                pat++;
            }
            if(pat >= pend) {
                return -1;
            }
            cstart = cend = (char)(_pat[pat++]&0xFF);
            if(_pat[pat] == '-' && _pat[pat+1] != ']') {
                pat++;
                if(escape && _pat[pat] == '\\') {
                    pat++;
                }
                if(pat >= pend) {
                    return -1;
                }

                cend = (char)(_pat[pat++] & 0xFF);
            }

            if (nocase) {
                if (Character.toLowerCase(cstart) <= test
                        && test <= Character.toLowerCase(cend)) {
                    ok = true;
                }
            } else {
                if (cstart <= test && test <= cend) {
                    ok = true;
                }
            }
        }

        return ok == not ? -1 : pat + 1;
    }

    private static class GlobPattern {
        final byte[] bytes;
        final int begin;
        final int end;

        private int index;

        private final int flags;

        GlobPattern(ByteList bytes, int flags) {
            this(bytes.getUnsafeBytes(), bytes.getBegin(),  bytes.getBegin() + bytes.getRealSize(), flags);
        }

        GlobPattern(byte[] bytes, int index, int end, int flags) {
            this.bytes = bytes;
            this.index = index;
            this.begin = index;
            this.end = end;
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

    public static interface GlobFunc<T> {
        int call(byte[] ptr, int p, int len, T ary);
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

    final static GlobFunc<List<ByteList>> push_pattern = new GlobFunc<List<ByteList>>() {
        public int call(byte[] ptr, int p, int len, List<ByteList> ary) {
            ary.add(new ByteList(ptr, p, len));
            return 0;
        }
    };
    private final static GlobFunc<GlobArgs> glob_caller = new GlobFunc<GlobArgs>() {
        public int call(byte[] ptr, int p, int len, GlobArgs args) {
            args.c = p;
            return args.func.call(ptr, args.c, len, args.arg);
        }
    };

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
                if (escape && i == end) return false;

                break;
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

}
