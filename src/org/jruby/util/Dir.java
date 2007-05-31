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

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

    public final static byte[] EMPTY = new byte[0];
    public final static byte[] SLASH = new byte[]{'/'};
    public final static byte[] STAR = new byte[]{'*'};
    public final static byte[] DOUBLE_STAR = new byte[]{'*','*'};

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
        //boolean nocase = (flags & FNM_CASEFOLD) != 0;

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
                if(s < slen && (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                if(pat > len || (pat == len && c == '*')) {
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
                test = (char)((escape && c == '\\' && pat < len ? _pat[pat] : c)&0xFF);
                test = Character.toLowerCase(test);
                pat--;
                while(s < slen) {
                    if((c == '?' || c == '[' || Character.toLowerCase((char) string[s]) == test) &&
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
                    if(Character.toLowerCase((char)c) != Character.toLowerCase((char)string[s])) {
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
        //boolean nocase = (flags & FNM_CASEFOLD) != 0;
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
            if (Character.toLowerCase((char) cstart) <= test && 
                    test <= Character.toLowerCase((char) cend)) {
                ok = true;
            }
        }

        return ok == not ? -1 : pat + 1;
    }


    public static List push_glob(String cwd, byte[] str, int p, int pend, int flags) {
        int buf;
        int nest, maxnest;
        int status = 0;
        boolean noescape = (flags & FNM_NOESCAPE) != 0;
        List ary = new ArrayList();
        
        while(p < pend) {
            nest = maxnest = 0;
            while(p<pend && str[p] == 0) {
                p++;
            }
            buf = p;
            while(p<pend && str[p] != 0) {
                if(str[p] == '{') {
                    nest++;
                    maxnest++;
                } else if(str[p] == '}') {
                    nest--;
                } else if(!noescape && str[p] == '\\') {
                    if(++p == pend) {
                        break;
                    }
                }
                p++;
            }
            if(maxnest == 0) {
                status = push_globs(cwd, ary, str, buf, pend, flags);
                if(status != 0) {
                    break;
                }
            } else if(nest == 0) {
                status = push_braces(cwd, ary, str, buf, pend, flags);
                if(status != 0) {
                    break;
                }
            }
            /* else unmatched braces */
        }
        return ary;
    }

    private static interface GlobFunc {
        int call(byte[] ptr, int p, int len, Object ary);
    }

    private static class GlobArgs {
        GlobFunc func;
        int c = -1;
        List v;
    }

    public final static GlobFunc push_pattern = new GlobFunc() {
            public int call(byte[] ptr, int p, int len, Object ary) {
                ((List)ary).add(new ByteList(ptr, p, len));
                return 0;
            }
        };
    public final static GlobFunc glob_caller = new GlobFunc() {
            public int call(byte[] ptr, int p, int len, Object ary) {
                GlobArgs args = (GlobArgs)ary;
                args.c = p;
                return args.func.call(ptr, args.c, len, args.v);
            }
        };

    private static int push_braces(String cwd, List ary, byte[] str, int _s, int slen, int flags) {
        ByteList buf = new ByteList(20);
        int s, p, t, lbrace, rbrace;
        int nest = 0;
        int status = 0;
        s = p = 0;

        s = p = _s;
        lbrace = rbrace = -1;

        while(p<slen) {
            if(str[p] == '{') {
                lbrace = p;
                break;
            }
            p++;
        }
        while(p<slen) {
            if(str[p] == '{') {
                nest++;
            } else if(str[p] == '}' && --nest == 0) {
                rbrace = p;
                break;
            }
            p++;
        }

        if(lbrace != -1 && rbrace != -1) {
            p = lbrace;
            while(str[p] != '}') {
                t = p + 1;
                for(p = t; p<slen && str[p] != '}' && str[p]!=','; p++) {
                    /* skip inner braces */
                    if(str[p] == '{') {
                        nest = 1;
                        while(str[++p] != '}' || --nest != 0) {
                            if(str[p] == '{') {
                                nest++;
                            }
                        }
                    }
                }
                buf.length(0);
                buf.append(str,s,lbrace-s);
                buf.append(str, t, p-t);
                buf.append(str, rbrace+1, slen-(rbrace+1));
                status = push_braces(cwd, ary, buf.bytes, buf.begin, buf.realSize, flags);
                if(status != 0) {
                    break;
                }
            }
        } else {
            status = push_globs(cwd, ary, str, s, slen, flags);
        }
        return status;
    }

    private static int push_globs(String cwd, List ary, byte[] str, int s, int slen, int flags) {
        return rb_glob2(cwd, str, s, slen, flags, push_pattern, ary);
    }

    private static int rb_glob2(String cwd, byte[] _path, int path, int plen, int flags, GlobFunc func, List arg) {
        GlobArgs args = new GlobArgs();
        args.func = func;
        args.v = arg;
        flags |= FNM_SYSCASE;
        return glob_helper(cwd, _path, path, plen, -1, flags, glob_caller, args);
    }

    private static boolean has_magic(byte[] str, int s, int send, int slen, int flags) {
        int p = s;
        byte c;
        int open = 0;
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;

        while(p < slen) {
            c = str[p++];
            switch(c) {
            case '?':
            case '*':
                return true;
            case '[':	/* Only accept an open brace if there is a close */
                open++;	/* brace to match it.  Bracket expressions must be */
                continue;	/* complete, according to Posix.2 */
            case ']':
                if(open > 0) {
                    return true;
                }
                continue;
            case '\\':
                if(escape && p == slen) {
                    return false;
                }
                break;
            default:
                if(FNM_SYSCASE==0 && Character.isLetter((char)(c&0xFF)) && nocase) {
                    return true;
                }
            }
            if(send != -1 && p >= send) {
                break;
            }
        }

        return false;
    }

    private static int remove_backslashes(byte[] pa, int p, int len) {
        int pend = len;
        int t = p;
        while(p < pend) {
            if(pa[p] == '\\') {
                if(++p == pend) {
                    break;
                }
            }
            pa[t++] = pa[p++];
        }
        return t;
    }

    private static int strchr(byte[] _s, int s, byte ch) {
        for(int i=s,e=_s.length;i<e; i++) {
            if(_s[i] == ch) {
                return i-s;
            }
        }
        return -1;
    }

    private static byte[] extract_path(byte[] _p, int p, int pend) {
        byte[] alloc;
        int len;
        len = pend-p;
        if(len > 1 && _p[pend-1] == '/' && (!DOSISH || (len < 2 || _p[pend-2] != ':'))) {
            len--;
        }
        alloc = new byte[len];
        System.arraycopy(_p,p,alloc,0,len);
        return alloc;
    }

    private static byte[] extract_elem(byte[] _p, int p, int pend) {
        int _pend = strchr(_p,p,(byte)'/');
        if(_pend == -1 || (_pend+p) > pend) {
            _pend = pend;
        }else {
            _pend+=p;
        }
        return extract_path(_p, p, _pend);
    }

    private static boolean BASE(byte[] base) {
        return DOSISH ? 
            (base.length > 0 && !((isdirsep(base[0]) && base.length < 2) || (base.length > 2 && base[1] == ':' && isdirsep(base[2]) && base.length < 4)))
            :
            (base.length > 0 && !(isdirsep(base[0]) && base.length < 2));
    }

    private static int glob_helper(String cwd, byte[] _path, int path, int plen, int sub, int flags, GlobFunc func, GlobArgs arg) {
        int p,m;
        int status = 0;
        ByteList buf = new ByteList(20);
        byte[] newpath = null;
        File st;
        List link = new ArrayList();
        p = sub != -1 ? sub : path;
        if(!has_magic(_path, p, -1, plen, flags)) {
            if(DOSISH || (flags & FNM_NOESCAPE) == 0) {
                newpath = new byte[plen];
                System.arraycopy(_path,0,newpath,0,plen);
                if(sub != -1) {
                    p = (sub - path);
                    plen = remove_backslashes(newpath, p, plen);
                    sub = p;
                } else {
                    plen = remove_backslashes(newpath, 0, plen);
                    _path = newpath;
                }
            }

            if(_path[path] == '/' || (DOSISH && path+2<plen && _path[path+1] == ':' && isdirsep(_path[path+2]))) {
                if(new File(new String(_path, path, plen - path)).exists()) {
                    status = func.call(_path, path, plen, arg);
                }
            } else {
                if(new File(cwd, new String(_path, path, plen - path)).exists()) {
                    status = func.call(_path, path, plen, arg);
                }
            }

            return status;
        }
        mainLoop: while(p != -1 && status == 0) {
            if(_path[p] == '/') {
                p++;
            }
            m = strchr(_path, p, (byte)'/');
            if(has_magic(_path, p, (m == -1 ? m : p+m), plen, flags)) {
                finalize: do {
                    byte[] base = extract_path(_path, path, p);
                    byte[] dir;
                    byte[] magic;
                    boolean recursive = false;
                    if(path == p) {
                        dir = new byte[]{'.'};
                    } else {
                        dir = base;
                    }
                    magic = extract_elem(_path,p,plen);
                    if(dir[0] == '/'  || (DOSISH && 2<dir.length && dir[1] == ':' && isdirsep(dir[2]))) {
                        st = new File(new String(dir));
                    } else {
                        st = new File(cwd, new String(dir));
                    }

                    if(st.isDirectory()) {
                        if(m != -1 && Arrays.equals(magic, DOUBLE_STAR)) {
                            int n = base.length;
                            recursive = true;
                            buf.length(0);
                            buf.append(base);
                            buf.append(_path, p + (base.length > 0 ? m : m + 1), plen - (p + (base.length > 0 ? m : m + 1)));
                            status = glob_helper(cwd, buf.bytes, buf.begin, buf.realSize, n, flags, func, arg);
                            if(status != 0) {
                                break finalize;
                            }
                        }
                    } else {
                        break mainLoop;
                    }

                    String[] dirp = st.list();
                    for(int i=0;i<dirp.length;i++) {
                        if(recursive) {
                            byte[] bs = dirp[i].getBytes();
                            if(fnmatch(STAR,0,1,bs,0,bs.length,flags) != 0) {
                                continue;
                            }
                            buf.length(0);
                            buf.append(base);
                            buf.append( BASE(base) ? SLASH : EMPTY );
                            buf.append(dirp[i].getBytes());
                            if(buf.bytes[0] == '/' || (DOSISH && 2<buf.realSize && buf.bytes[1] == ':' && isdirsep(buf.bytes[2]))) {
                                st = new File(new String(buf.bytes, buf.begin, buf.realSize));
                            } else {
                                st = new File(cwd, new String(buf.bytes, buf.begin, buf.realSize));
                            }
                            if(st.isDirectory()) {
                                int t = buf.realSize;
                                buf.append(SLASH);
                                buf.append(DOUBLE_STAR);
                                buf.append(_path, p+m, plen-(p+m));
                                status = glob_helper(cwd, buf.bytes, buf.begin, buf.realSize, t, flags, func, arg);
                                if(status != 0) {
                                    break;
                                }
                            }
                            continue;
                        }
                        byte[] bs = dirp[i].getBytes();
                        if(fnmatch(magic,0,magic.length,bs,0,bs.length,flags) == 0) {
                            buf.length(0);
                            buf.append(base);
                            buf.append( BASE(base) ? SLASH : EMPTY );
                            buf.append(dirp[i].getBytes());
                            if(m == -1) {
                                status = func.call(buf.bytes,0,buf.realSize,arg);
                                if(status != 0) {
                                    break;
                                }
                                continue;
                            }
                            link.add(buf);
                            buf = new ByteList(20);
                        }
                    }
                } while(false);
                if(link.size() > 0) {
                    for(Iterator iter = link.iterator();iter.hasNext();) {
                        if(status == 0) {
                            ByteList b = (ByteList)iter.next();
                            if(b.bytes[0] == '/'  || (DOSISH && 2<b.realSize && b.bytes[1] == ':' && isdirsep(b.bytes[2]))) {
                                st = new File(new String(b.bytes, 0, b.realSize));
                            } else {
                                st = new File(cwd, new String(b.bytes, 0, b.realSize));
                            }

                            if(st.isDirectory()) {
                                int len = b.realSize;
                                buf.length(0);
                                buf.append(b);
                                buf.append(_path, p+m, plen-(p+m));
                                status = glob_helper(cwd,buf.bytes,0,buf.realSize,len,flags,func,arg);
                            }
                        }
                    }
                    break mainLoop;
                }
            }
            p = (m == -1 ? m : p+m);
        }
        return status;
    }
}// Dir
