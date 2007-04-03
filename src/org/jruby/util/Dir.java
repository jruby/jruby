/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby.util;

/**
 * This class exists as a counterpart to the dir.c file in 
 * MRI source. It contains many methods useful for 
 * File matching and Globbing.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Dir {
    public final static boolean DOSISH = System.getProperty("os.name").indexOf("Windows") != -1;
    public final static boolean CASEFOLD_FILESYSTEM = DOSISH;

    public final static int FNM_NOESCAPE = 0x01;
    public final static int FNM_PATHNAME = 0x02;
    public final static int FNM_DOTMATCH = 0x04;
    public final static int FNM_CASEFOLD = 0x08;

    public final static int FNM_SYSCASE = CASEFOLD_FILESYSTEM ? FNM_CASEFOLD : 0;

    public final static int FNM_NOMATCH = 1;
    public final static int FNM_ERROR   = 2;

    private static boolean isdirsep(byte c) {
        return DOSISH ? (c == '\\' || c == '/') : c == '/';
    }

    private static int rb_path_next(byte[] _s, int s, int len) {
        while(s < len && !isdirsep(_s[s])) {
            s++;
        }
        return s;
    }

    public static int fnmatch(byte[] _pat, int pstart, int plen, byte[] string, int sstart, int slen, int flags) {
        byte c;
        char test;
        int s = sstart;
        int pat = pstart;
        int len = plen;
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean pathname = (flags & FNM_PATHNAME) != 0;
        boolean period = (flags & FNM_DOTMATCH) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;

        while(pat<len) {
            c = _pat[pat++];
            switch(c) {
            case '?':
                if(s >= slen || (pathname && isdirsep(string[s])) || 
                   (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '*':
                while(pat < len && (c = _pat[pat++]) == '*');
                if((period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                if(pat >= len) {
                    if(pathname && rb_path_next(string, s, slen) < slen) {
                        return FNM_NOMATCH;
                    } else {
                        return 0;
                    }
                } else if((pathname && isdirsep(c))) {
                    s = rb_path_next(string, s, slen);
                    if(s < slen) {
                        s++;
                        break;
                    }
                    return FNM_NOMATCH;
                }

                test = (char)((escape && c == '\\' ? _pat[pat] : c)&0xFF);
                test = Character.toLowerCase(test);
                pat--;
                while(s < slen) {
                    if((c == '?' || c == '[' || Character.toLowerCase(string[s]) == test) &&
                       fnmatch(_pat, pat, plen, string, s, slen, flags | FNM_DOTMATCH) == 0) {
                        return 0;
                    } else if((pathname && isdirsep(string[s]))) {
                        break;
                    }
                    s++;
                }
                return FNM_NOMATCH;
            case '[':
                if(s >= slen || (pathname && isdirsep(string[s]) || 
                                 (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1])))))) {
                    return FNM_NOMATCH;
                }
                pat = range(_pat, pat, plen, (char)(string[s]&0xFF), flags);
                if(pat == -1) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '\\':
                if(escape &&
                   (!DOSISH ||
                    (pat < len && "*?[]\\".indexOf((char)_pat[pat]) != -1))) {
                    if(pat >= len) {
                        c = '\\';
                    } else {
                        c = _pat[pat++];
                    }
                }
            default:
                if(s >= slen) {
                    return FNM_NOMATCH;
                }
                if(DOSISH && (pathname && isdirsep(c) && isdirsep(string[s]))) {
                } else {
                    if(Character.toLowerCase((char)c) != Character.toLowerCase(string[s])) {
                        return FNM_NOMATCH;
                    }
                }
                s++;
                break;
            }
        }
        return s >= slen ? 0 : FNM_NOMATCH;
    }

    public static int range(byte[] _pat, int pat, int len, char test, int flags) {
        boolean not;
        boolean ok = false;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;
        boolean escape = (flags & FNM_NOESCAPE) == 0;

        not = _pat[pat] == '!' || _pat[pat] == '^';
        if(not) {
            pat++;
        }

        test = Character.toLowerCase(test);

        while(_pat[pat] != ']') {
            int cstart, cend;
            if(escape && _pat[pat] == '\\') {
                pat++;
            }
            if(pat >= len) {
                return -1;
            }
            cstart = cend = (char)(_pat[pat++]&0xFF);
            if(_pat[pat] == '-' && _pat[pat+1] != ']') {
                pat++;
                if(escape && _pat[pat] == '\\') {
                    pat++;
                }
                if(pat >= len) {
                    return -1;
                }

                cend = (char)(_pat[pat++] & 0xFF);
            }
            if(Character.toLowerCase(cstart) <= test && test <= Character.toLowerCase(cend)) {
                ok = true;
            }
        }

        return ok == not ? -1 : pat + 1;
    }
}// Dir
