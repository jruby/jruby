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
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.util.Enumeration;

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

    public static int fnmatch(byte[] bytes, int pstart, int plen, byte[] string, int sstart, int slen, int flags) {
        char test;
        int s = sstart;
        int pat = pstart;
        int len = plen;
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean pathname = (flags & FNM_PATHNAME) != 0;
        boolean period = (flags & FNM_DOTMATCH) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;

        while(pat<len) {
            byte c = bytes[pat++];
            switch(c) {
            case '?':
                if(s >= slen || (pathname && isdirsep(string[s])) || 
                   (period && string[s] == '.' && (s == 0 || (pathname && isdirsep(string[s-1]))))) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '*':
                while(pat < len && (c = bytes[pat++]) == '*');
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
                test = (char)((escape && c == '\\' && pat < len ? bytes[pat] : c)&0xFF);
                test = Character.toLowerCase(test);
                pat--;
                while(s < slen) {
                    if((c == '?' || c == '[' || Character.toLowerCase((char) string[s]) == test) &&
                       fnmatch(bytes, pat, plen, string, s, slen, flags | FNM_DOTMATCH) == 0) {
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
                pat = range(bytes, pat, plen, (char)(string[s]&0xFF), flags);
                if(pat == -1) {
                    return FNM_NOMATCH;
                }
                s++;
                break;
            case '\\':
                if(escape &&
                   (!DOSISH ||
                    (pat < len && "*?[]\\".indexOf((char)bytes[pat]) != -1))) {
                    if(pat >= len) {
                        c = '\\';
                    } else {
                        c = bytes[pat++];
                    }
                }
            default:
                if(s >= slen) {
                    return FNM_NOMATCH;
                }
                if(DOSISH && (pathname && isdirsep(c) && isdirsep(string[s]))) {
                } else {
                    if (nocase) {
                        if(Character.toLowerCase((char)c) != Character.toLowerCase((char)string[s])) {
                            return FNM_NOMATCH;
                        }
                        
                    } else {
                        if(c != (char)string[s]) {
                            return FNM_NOMATCH;
                        }
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

    public static List<ByteList> push_glob(String cwd, ByteList globByteList, int flags) {
        List<ByteList> result = new ArrayList<ByteList>();

        push_braces(cwd, result, new GlobPattern(globByteList, flags));

        return result;
    }
    
    private static class GlobPattern {
        byte[] bytes;
        int index;
        int begin;
        int end;
        int flags;
        
        public GlobPattern(ByteList bytelist, int flags) {
            this(bytelist.bytes, bytelist.begin,  bytelist.begin + bytelist.realSize, flags);
        }
        
        public GlobPattern(byte[] bytes, int index, int end, int flags) {
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

    private static interface GlobFunc {
        int call(byte[] ptr, int p, int len, Object ary);
    }

    private static class GlobArgs {
        GlobFunc func;
        int c = -1;
        List<ByteList> v;
        
        public GlobArgs(GlobFunc func, List<ByteList> arg) {
            this.func = func;
            this.v = arg;
        }
    }

    public final static GlobFunc push_pattern = new GlobFunc() {
            @SuppressWarnings("unchecked")
            public int call(byte[] ptr, int p, int len, Object ary) {
                ((List) ary).add(new ByteList(ptr, p, len));
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

    /*
     * Process {}'s (example: Dir.glob("{jruby,jython}/README*") 
     */
    private static int push_braces(String cwd, List<ByteList> result, GlobPattern pattern) {
        pattern.reset();
        int lbrace = pattern.indexOf((byte) '{'); // index of left-most brace
        int rbrace = pattern.findClosingIndexOf(lbrace);// index of right-most brace

        // No or mismatched braces..Move along..nothing to see here
        if (lbrace == -1 || rbrace == -1) return push_globs(cwd, result, pattern); 

        // Peel onion...make subpatterns out of outer layer of glob and recall with each subpattern 
        // Example: foo{a{c},b}bar -> fooa{c}bar, foobbar
        ByteList buf = new ByteList(20);
        int middleRegionIndex;
        int i = lbrace;
        while (pattern.bytes[i] != '}') {
            middleRegionIndex = i + 1;
            for(i = middleRegionIndex; i < pattern.end && pattern.bytes[i] != '}' && pattern.bytes[i] != ','; i++) {
                if (pattern.bytes[i] == '{') pattern.findClosingIndexOf(i); // skip inner braces
            }

            buf.length(0);
            buf.append(pattern.bytes, pattern.begin, lbrace - pattern.begin);
            buf.append(pattern.bytes, middleRegionIndex, i - middleRegionIndex);
            buf.append(pattern.bytes, rbrace + 1, pattern.end - (rbrace + 1));
            int status = push_braces(cwd, result, new GlobPattern(buf.bytes, buf.begin, buf.realSize, pattern.flags));
            if(status != 0) return status;
        }
        
        return 0; // All braces pushed..
    }

    private static int push_globs(String cwd, List<ByteList> ary, GlobPattern pattern) {
        pattern.flags |= FNM_SYSCASE;
        return glob_helper(cwd, pattern.bytes, pattern.begin, pattern.end, -1, pattern.flags, glob_caller, new GlobArgs(push_pattern, ary));
    }

    private static boolean has_magic(byte[] bytes, int begin, int end, int flags) {
        boolean escape = (flags & FNM_NOESCAPE) == 0;
        boolean nocase = (flags & FNM_CASEFOLD) != 0;
        int open = 0;

        for (int i = begin; i < end; i++) {
            switch(bytes[i]) {
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
                if (FNM_SYSCASE == 0 && nocase && Character.isLetter((char)(bytes[i]&0xFF))) return true;
            }
        }

        return false;
    }

    private static int remove_backslashes(byte[] bytes, int index, int len) {
        int t = index;
        
        for (; index < len; index++, t++) {
            if (bytes[index] == '\\' && ++index == len) break;
            
            bytes[t] = bytes[index];
        }
        
        return t;
    }

    private static int strchr(byte[] bytes, int begin, int end, byte ch) {
        for (int i = begin; i < end; i++) {
            if (bytes[i] == ch) return i;
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
        int elementEnd = strchr(bytes, begin, end, (byte)'/');
        if (elementEnd == -1) elementEnd = end;
        
        return extract_path(bytes, begin, elementEnd);
    }

    private static boolean BASE(byte[] base) {
        return DOSISH ? 
            (base.length > 0 && !((isdirsep(base[0]) && base.length < 2) || (base.length > 2 && base[1] == ':' && isdirsep(base[2]) && base.length < 4)))
            :
            (base.length > 0 && !(isdirsep(base[0]) && base.length < 2));
    }
    
    private static boolean isJarFilePath(byte[] bytes, int begin, int end) {
        return end > 6 && bytes[begin] == 'f' && bytes[begin+1] == 'i' &&
            bytes[begin+2] == 'l' && bytes[begin+3] == 'e' && bytes[begin+4] == ':';
    }

    private static String[] files(File directory) {
        String[] files = directory.list();
        
        String[] filesPlusDotFiles = new String[files.length + 2];
        System.arraycopy(files, 0, filesPlusDotFiles, 2, files.length);
        filesPlusDotFiles[0] = ".";
        filesPlusDotFiles[1] = "..";
        
        return filesPlusDotFiles;
    }

    private static int glob_helper(String cwd, byte[] bytes, int begin, int end, int sub, int flags, GlobFunc func, GlobArgs arg) {
        int p,m;
        int status = 0;
        byte[] newpath = null;
        File st;
        p = sub != -1 ? sub : begin;
        if (!has_magic(bytes, p, end, flags)) {
            if (DOSISH || (flags & FNM_NOESCAPE) == 0) {
                newpath = new byte[end];
                System.arraycopy(bytes,0,newpath,0,end);
                if (sub != -1) {
                    p = (sub - begin);
                    end = remove_backslashes(newpath, p, end);
                    sub = p;
                } else {
                    end = remove_backslashes(newpath, 0, end);
                    bytes = newpath;
                }
            }

            if (bytes[begin] == '/' || (DOSISH && begin+2<end && bytes[begin+1] == ':' && isdirsep(bytes[begin+2]))) {
                if (new File(new String(bytes, begin, end - begin)).exists()) {
                    status = func.call(bytes, begin, end, arg);
                }
            } else if (isJarFilePath(bytes, begin, end)) {
                int ix = -1;
                for(int i = 0;i<end;i++) {
                    if(bytes[begin+i] == '!') {
                        ix = i;
                        break;
                    }
                }

                st = new File(new String(bytes, begin+5, ix-5));
                String jar = new String(bytes, begin+ix+1, end-(ix+1));
                try {
                    JarFile jf = new JarFile(st);
                    
                    if (jar.startsWith("/")) jar = jar.substring(1);
                    if (jf.getEntry(jar + "/") != null) jar = jar + "/";
                    if (jf.getEntry(jar) != null) {
                        status = func.call(bytes, begin, end, arg);
                    }
                } catch(Exception e) {}
            } else if ((end - begin) > 0) { // Length check is a hack.  We should not be reeiving "" as a filename ever. 
                if (new File(cwd, new String(bytes, begin, end - begin)).exists()) {
                    status = func.call(bytes, begin, end, arg);
                }
            }

            return status;
        }
        
        ByteList buf = new ByteList(20);
        List<ByteList> link = new ArrayList<ByteList>();
        mainLoop: while(p != -1 && status == 0) {
            if (bytes[p] == '/') p++;

            m = strchr(bytes, p, end, (byte)'/');
            if(has_magic(bytes, p, m == -1 ? end : m, flags)) {
                finalize: do {
                    byte[] base = extract_path(bytes, begin, p);
                    byte[] dir = begin == p ? new byte[]{'.'} : base; 
                    byte[] magic = extract_elem(bytes,p,end);
                    boolean recursive = false;
                    String jar = null;
                    JarFile jf = null;

                    if(dir[0] == '/'  || (DOSISH && 2<dir.length && dir[1] == ':' && isdirsep(dir[2]))) {
                        st = new File(new String(dir));
                    } else if(isJarFilePath(dir, 0, dir.length)) {
                        int ix = -1;
                        for(int i = 0;i<dir.length;i++) {
                            if(dir[i] == '!') {
                                ix = i;
                                break;
                            }
                        }

                        st = new File(new String(dir, 5, ix-5));
                        jar = new String(dir, ix+1, dir.length-(ix+1));
                        try {
                            jf = new JarFile(st);

                            if (jar.startsWith("/")) jar = jar.substring(1);
                            if (jf.getEntry(jar + "/") != null) jar = jar + "/";
                        } catch(Exception e) {
                            jar = null;
                            jf = null;
                        }
                    } else {
                        st = new File(cwd, new String(dir));
                    }

                    if((jf != null && ("".equals(jar) || (jf.getJarEntry(jar) != null && jf.getJarEntry(jar).isDirectory()))) || st.isDirectory()) {
                        if(m != -1 && Arrays.equals(magic, DOUBLE_STAR)) {
                            int n = base.length;
                            recursive = true;
                            buf.length(0);
                            buf.append(base);
                            buf.append(bytes, (base.length > 0 ? m : m + 1), end - (base.length > 0 ? m : m + 1));
                            status = glob_helper(cwd, buf.bytes, buf.begin, buf.realSize, n, flags, func, arg);
                            if(status != 0) {
                                break finalize;
                            }
                        }
                    } else {
                        break mainLoop;
                    }

                    if(jar == null) {
                        String[] dirp = files(st);

                        for(int i=0;i<dirp.length;i++) {
                            if(recursive) {
                                byte[] bs = dirp[i].getBytes();
                                if (fnmatch(STAR,0,1,bs,0,bs.length,flags) != 0) {
                                    continue;
                                }
                                buf.length(0);
                                buf.append(base);
                                buf.append( BASE(base) ? SLASH : EMPTY );
                                buf.append(dirp[i].getBytes());
                                if (buf.bytes[0] == '/' || (DOSISH && 2<buf.realSize && buf.bytes[1] == ':' && isdirsep(buf.bytes[2]))) {
                                    st = new File(new String(buf.bytes, buf.begin, buf.realSize));
                                } else {
                                    st = new File(cwd, new String(buf.bytes, buf.begin, buf.realSize));
                                }
                                if(st.isDirectory() && !".".equals(dirp[i]) && !"..".equals(dirp[i])) {
                                    int t = buf.realSize;
                                    buf.append(SLASH);
                                    buf.append(DOUBLE_STAR);
                                    buf.append(bytes, m, end - m);
                                    status = glob_helper(cwd, buf.bytes, buf.begin, buf.realSize, t, flags, func, arg);
                                    if(status != 0) {
                                        break;
                                    }
                                }
                                continue;
                            }
                            byte[] bs = dirp[i].getBytes();
                            if(fnmatch(magic,0,magic.length,bs,0, bs.length,flags) == 0) {
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
                    } else {
                        try {
                            List<JarEntry> dirp = new ArrayList<JarEntry>();
                            for(Enumeration<JarEntry> eje = jf.entries(); eje.hasMoreElements(); ) {
                                JarEntry je = eje.nextElement();
                                String name = je.getName();
                                int ix = name.indexOf('/', jar.length());
                                if((!name.startsWith("META-INF") && (ix == -1 || ix == name.length()-1))) {
                                    if("/".equals(jar) || (name.startsWith(jar) && name.length()>jar.length())) {
                                        dirp.add(je);
                                    }
                                }
                            }
                            for(JarEntry je : dirp) {
                                byte[] bs = je.getName().getBytes();
                                int len = bs.length;

                                if(je.isDirectory()) {
                                    len--;
                                }

                                if(recursive) {
                                    if(fnmatch(STAR,0,1,bs,0,len,flags) != 0) {
                                        continue;
                                    }
                                    buf.length(0);
                                    buf.append(base, 0, base.length - jar.length());
                                    buf.append( BASE(base) ? SLASH : EMPTY );
                                    buf.append(bs, 0, len);

                                    if(je.isDirectory()) {
                                        int t = buf.realSize;
                                        buf.append(SLASH);
                                        buf.append(DOUBLE_STAR);
                                        buf.append(bytes, m, end - m);
                                        status = glob_helper(cwd, buf.bytes, buf.begin, buf.realSize, t, flags, func, arg);
                                        if(status != 0) {
                                            break;
                                        }
                                    }
                                    continue;
                                }

                                if(fnmatch(magic,0,magic.length,bs,0,len,flags) == 0) {
                                    buf.length(0);
                                    buf.append(base, 0, base.length - jar.length());
                                    buf.append( BASE(base) ? SLASH : EMPTY );
                                    buf.append(bs, 0, len);
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
                        } catch(Exception e) {}
                    }
                } while(false);

                if (link.size() > 0) {
                    for (ByteList b : link) {
                        if (status == 0) {
                            if(b.bytes[0] == '/'  || (DOSISH && 2<b.realSize && b.bytes[1] == ':' && isdirsep(b.bytes[2]))) {
                                st = new File(new String(b.bytes, 0, b.realSize));
                            } else {
                                st = new File(cwd, new String(b.bytes, 0, b.realSize));
                            }

                            if(st.isDirectory()) {
                                int len = b.realSize;
                                buf.length(0);
                                buf.append(b);
                                buf.append(bytes, m, end - m);
                                status = glob_helper(cwd,buf.bytes,0,buf.realSize,len,flags,func,arg);
                            }
                        }
                    }
                    break mainLoop;
                }
            }
            p = m;
        }
        return status;
    }
}
