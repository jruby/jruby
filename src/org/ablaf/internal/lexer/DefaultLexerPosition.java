/*
 * DefaultLexerPosition.java
 * Created on 08.02.2002, 20:46:57
 * 
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "AbLaF" and "Abstract Language Framework" must not be 
 *    used to endorse or promote products derived from this software 
 *    without prior written permission. For written permission, please
 *    contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called 
 *    "Abstract Language Framework", nor may 
 *    "Abstract Language Framework" appear in their name, without prior 
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * ====================================================================
 *
 */
package org.ablaf.internal.lexer;

import java.io.Serializable;
import java.lang.ref.SoftReference;

import org.ablaf.common.ISourcePosition;

/**
 * @todo we need to implement sharing of those position, probably using a WeakHashMap
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultLexerPosition implements ISourcePosition, Serializable {
    // private static PositionMap positions = new PositionMap();

    private String file;
    private int line;
    private int column;

    private DefaultLexerPosition(String file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public static ISourcePosition getInstance(String file, int line, int column) {
        // Before we use the cache we should benchmark it. Because it seems to
        // slowdown the startup time.

        // return positions.get(file, line, column);
        return new DefaultLexerPosition(file, line, column);
    }
    

    /**
     * @see ISourcePosition#getFile()
     */
    public String getFile() {
        return file;
    }

    /**
     * @see ISourcePosition#getLine()
     */
    public int getLine() {
        return line;
    }

    /**
     * @see ISourcePosition#getColumn()
     */
    public int getColumn() {
        return column;
    }

    public boolean equals(Object iOther) {
        if (iOther instanceof DefaultLexerPosition) {
            DefaultLexerPosition lOther = (DefaultLexerPosition) iOther;
            return file.equals(lOther.file) && line == lOther.line && column == lOther.column;
        }
        return false;
    }

    /**
     * hashcode based on the position value.
     **/
    public int hashCode() {
        return file.hashCode() ^ line ^ column;
    }

    public String toString() {
        return file + ":" + line + ":" + column;
    }

    private static class PositionMap {
        private final static int MAP_SIZE = 500;
        private final static int ARRAY_SIZE = 10;

        private Object[] map = new Object[MAP_SIZE];

        public PositionMap() {
        }

        public ISourcePosition get(String file, int line, int column) {
            int hash = Math.abs((file.hashCode() ^ line ^ column) % MAP_SIZE);
            Object obj = map[hash];
            if (obj == null) {
                ISourcePosition position = new DefaultLexerPosition(file, line, column);
                map[hash] = new SoftReference(position);
                return position;
            } else if (obj instanceof SoftReference) {
                if (((SoftReference) obj).get() == null) {
                    ISourcePosition position = new DefaultLexerPosition(file, line, column);
                    map[hash] = new SoftReference(position);
                    return position;
                } else if (
                    ((ISourcePosition) ((SoftReference) obj).get()).getFile().equals(file)
                        && ((ISourcePosition) ((SoftReference) obj).get()).getLine() == line
                        && ((ISourcePosition) ((SoftReference) obj).get()).getColumn() == column) {
                    return (ISourcePosition) ((SoftReference) obj).get();
                } else {
                    map[hash] = new Object[ARRAY_SIZE];
                    ((Object[]) map[hash])[0] = obj;
                    ISourcePosition position = new DefaultLexerPosition(file, line, column);
                    ((Object[]) map[hash])[1] = new SoftReference(position);
                    return position;
                }
            } else {
                boolean free = false;
                for (int i = 0, size = ((Object[]) obj).length; i < size; i++) {
                    Object obj2 = ((Object[]) obj)[i];
                    if (obj2 == null) {
                        free = true;
                    } else if (((SoftReference) obj2).get() == null) {
                        free = true;
                        ((Object[]) obj)[i] = null;
                    } else if (
                        ((ISourcePosition) ((SoftReference) obj2).get()).getFile().equals(file)
                            && ((ISourcePosition) ((SoftReference) obj2).get()).getLine() == line
                            && ((ISourcePosition) ((SoftReference) obj2).get()).getColumn() == column) {

                        return (ISourcePosition) ((SoftReference) obj2).get();
                    }
                }
                if (free) {
                    for (int i = 0, size = ((Object[]) obj).length; i < size; i++) {
                        if (((Object[]) obj)[i] == null) {
                            ISourcePosition position = new DefaultLexerPosition(file, line, column);
                            ((Object[]) obj)[i] = new SoftReference(position);
                            return position;
                        }
                    }
                }
                Object[] obj2 = new Object[((Object[]) obj).length + 10];
                System.arraycopy(obj, 0, obj2, 0, ((Object[]) obj).length);
                ISourcePosition position = new DefaultLexerPosition(file, line, column);
                obj2[((Object[]) obj).length] = new SoftReference(position);
                map[hash] = obj2;
                return position;
            }
        }

        public ISourcePosition get(ISourcePosition position) {
            int hash = Math.abs(position.hashCode() % MAP_SIZE);
            Object obj = map[hash];
            if (obj == null) {
                map[hash] = new SoftReference(position);
                return position;
            } else if (obj instanceof SoftReference) {
                if (((SoftReference) obj).get() == null) {
                    map[hash] = new SoftReference(position);
                    return position;
                } else if (((SoftReference) obj).get().equals(position)) {
                    return (ISourcePosition) ((SoftReference) obj).get();
                } else {
                    map[hash] = new Object[ARRAY_SIZE];
                    ((Object[]) map[hash])[0] = obj;
                    ((Object[]) map[hash])[1] = new SoftReference(position);
                    return position;
                }
            } else {
                boolean free = false;
                for (int i = 0, size = ((Object[]) obj).length; i < size; i++) {
                    Object obj2 = ((Object[]) obj)[i];
                    if (obj2 == null) {
                        free = true;
                    } else if (((SoftReference) obj2).get() == null) {
                        free = true;
                        ((Object[]) obj)[i] = null;
                    } else if (((SoftReference) obj2).get().equals(position)) {
                        return (ISourcePosition) ((SoftReference) obj2).get();
                    }
                }
                if (free) {
                    for (int i = 0, size = ((Object[]) obj).length; i < size; i++) {
                        if (((Object[]) obj)[i] == null) {
                            ((Object[]) obj)[i] = new SoftReference(position);
                            return position;
                        }
                    }
                }
                Object[] obj2 = new Object[((Object[]) obj).length + 10];
                System.arraycopy(obj, 0, obj2, 0, ((Object[]) obj).length);
                obj2[((Object[]) obj).length] = new SoftReference(position);
                map[hash] = obj2;
                return position;
            }
        }
    }
}
