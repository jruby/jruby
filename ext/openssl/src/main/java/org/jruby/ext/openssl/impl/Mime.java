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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ext.openssl.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public interface Mime {
    Mime DEFAULT = new Mime() {
            private final static int MIME_START = 1;
            private final static int MIME_TYPE = 2;
            private final static int MIME_NAME = 3;
            private final static int MIME_VALUE = 4;
            private final static int MIME_QUOTE = 5;
            private final static int MIME_COMMENT = 6;

            private final static int MAX_SMLEN = 1024;

            /* c: static strip_start
             */
            private int stripStart(byte[] buffer, int start, int end) {
                byte c;
                for(int p = start; p<end; p++) {
                    c = buffer[p];
                    if(c == '"') {
                        //Next char is start of string if non null
                        if(p+1 < end) {
                            return p+1;
                        }
                        return -1;
                    }
                    if(!Character.isWhitespace((char)c)) {
                        return p;
                    }
                }
                return -1;
            }

            /* c: static strip_end
             */
            private int stripEnd(byte[] buffer, int start, int end) {
                mimeDebug("stripEnd("+start+","+end+")");
                if(start == -1) {
                    return -1;
                }
                byte c;
                for(int p = end-1; p >= start; p--) {
                    mimeDebug("  p = "+p+", c = "+(char)buffer[p] + "(" + buffer[p] + ")");
                    c = buffer[p];
                    if(c == '"') {
                        if(p - 1 == start) {
                            return -1;
                        }
                        return p;
                    }
                    if(!Character.isWhitespace((char)c)) {
                        return p+1;
                    }
                }

                return -1;
            }

            /* c: static strip_ends
             */
            private String stripEnds(byte[] buffer, int start, int end) {
                start = stripStart(buffer, start, end);
                end = stripEnd(buffer, start, end);

                try {
                    return new String(buffer, start, end-start, "ISO8859-1");
                } catch(Exception e) {
                    return null;
                }
            }

            public void mimeDebug(String str) {
                //                System.err.println(str);
            }

            public List<MimeHeader> parseHeaders(BIO bio) throws IOException {
                mimeDebug("\n!!!!!!!!!!!!!!!!!\n" + bio + "\n^^^^^^^^^^^^^^^^^^^^^^^^\n"); 
                int state = 0;
                byte[] linebuf = new byte[MAX_SMLEN];
                int len = 0;
                String ntmp = null;
                int p, q;
                byte c;
                MimeHeader mhdr = null;
                int saveState = -1;

                List<MimeHeader> headers = new ArrayList<MimeHeader>();

                while((len = bio.gets(linebuf, MAX_SMLEN)) > 0) {
                    if(mhdr != null && Character.isWhitespace((char)linebuf[0])) {
                        state = MIME_NAME;
                    } else {
                        state = MIME_START;
                    }

                    for(p = 0, q = 0; p<len && linebuf[p] != '\r' && linebuf[p] != '\n'; p++) {
                        c = linebuf[p];
                        switch(state) {
                        case MIME_START:
                            if(c == ':') {
                                state = MIME_TYPE;
                                mimeDebug("creating new: " + q + ":" + p);
                                ntmp = stripEnds(linebuf, q, p);
                                q = p + 1;
                            }
                            break;
                        case MIME_TYPE:
                            if(c == ';') {
                                mimeDebug("Found End Value");
                                mimeDebug("creating new: " + q + ":" + p);
                                mhdr = new MimeHeader(ntmp, stripEnds(linebuf, q, p));
                                headers.add(mhdr);
                                ntmp = null;
                                q = p + 1;
                                state = MIME_NAME;
                            } else if(c == '(') {
                                saveState = state;
                                state = MIME_COMMENT;
                            }

                            break;
                        case MIME_COMMENT:
                            if(c == ')') {
                                state = saveState;
                            }
                            break;
                        case MIME_NAME:
                            if(c == '=') {
                                state = MIME_VALUE;
                                mimeDebug("creating new: " + q + ":" + p);
                                ntmp = stripEnds(linebuf, q, p);
                                q = p + 1;
                            }
                            break;
                        case MIME_VALUE:
                            if(c == ';') {
                                state = MIME_NAME;
                                mhdr.getParams().add(new MimeParam(ntmp, stripEnds(linebuf, q, p)));
                                ntmp = null;
                                q = p + 1;
                            } else if(c == '"') {
                                mimeDebug("Found Quote");
                                state = MIME_QUOTE;
                            } else if(c == '(') {
                                saveState = state;
                                state = MIME_COMMENT;
                            }
                            break;
                        case MIME_QUOTE:
                            if(c == '"') {
                                mimeDebug("Found Match Quote");
                                state = MIME_VALUE;
                            }
                            break;
                        }
                    }
                    if(state == MIME_TYPE) {
                        mimeDebug("creating new: " + q + ":" + p);
                        mhdr = new MimeHeader(ntmp, stripEnds(linebuf, q, p));
                        headers.add(mhdr);
                    } else if(state == MIME_VALUE) {
                        mimeDebug("creating new: " + q + ":" + p);
                        mhdr.getParams().add(new MimeParam(ntmp, stripEnds(linebuf, q, p)));
                    }
                    if(p == 0) {
                        break;
                    }
                }

                return headers;
            }

            public MimeHeader findHeader(List<MimeHeader> headers, String key) {
                for(MimeHeader hdr : headers) {
                    if(hdr.getName().equals(key)) {
                        return hdr;
                    }
                }

                return null;
            }

            public MimeParam findParam(MimeHeader header, String key) {
                for(MimeParam par : header.getParams()) {
                    if(par.getParamName().equals(key)) {
                        return par;
                    }
                }

                return null;
            }
        };


    /* c: mime_parse_hdr
     *
     */
    List<MimeHeader> parseHeaders(BIO bio) throws IOException; 

    /* c: mime_hdr_find
     *
     */
    MimeHeader findHeader(List<MimeHeader> headers, String key); 

    /* c: mime_param_find
     *
     */
    MimeParam findParam(MimeHeader header, String key); 
}// Mime
