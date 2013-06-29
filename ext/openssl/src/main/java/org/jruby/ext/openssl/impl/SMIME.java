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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/** SMIME methods for PKCS7
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class SMIME {
    public final static int MAX_SMLEN = 1024;
    public final static byte[] NEWLINE = new byte[]{'\r','\n'};

    private Mime mime;

    public SMIME() {
        this(Mime.DEFAULT);
    }

    public SMIME(Mime mime) {
        this.mime = mime;
    }

    private static boolean equals(byte[] first, int firstIndex, byte[] second, int secondIndex, int length) {
        int len = length;
        for(int i=firstIndex, 
                j=secondIndex, 
                flen=first.length, 
                slen=second.length;
            i<flen && 
                j<slen && 
                len>0;
            i++, j++, len--) {

            if(first[i] != second[j]) {
                return false;
            }
        }
        return len == 0;
    }

    /* c: static strip_eol
     *
     */
    public static boolean stripEol(byte[] linebuf, int[] plen) {
        int len = plen[0];
        boolean isEol = false;
        
        for(int p = len - 1; len > 0; len--, p--) {
            byte c = linebuf[p];
            if(c == '\n') {
                isEol = true; 
            } else if(c != '\r') {
                break;
            }
            
        }
        plen[0] = len;
        return isEol;
    }


    /* c: SMIME_text
     *
     */
    public void text(BIO input, BIO output) {
// 	char iobuf[4096];
// 	int len;
// 	STACK_OF(MIME_HEADER) *headers;
// 	MIME_HEADER *hdr;

// 	if (!(headers = mime_parse_hdr(in))) {
// 		PKCS7err(PKCS7_F_SMIME_TEXT,PKCS7_R_MIME_PARSE_ERROR);
// 		return 0;
// 	}
// 	if(!(hdr = mime_hdr_find(headers, "content-type")) || !hdr->value) {
// 		PKCS7err(PKCS7_F_SMIME_TEXT,PKCS7_R_MIME_NO_CONTENT_TYPE);
// 		sk_MIME_HEADER_pop_free(headers, mime_hdr_free);
// 		return 0;
// 	}
// 	if (strcmp (hdr->value, "text/plain")) {
// 		PKCS7err(PKCS7_F_SMIME_TEXT,PKCS7_R_INVALID_MIME_TYPE);
// 		ERR_add_error_data(2, "type: ", hdr->value);
// 		sk_MIME_HEADER_pop_free(headers, mime_hdr_free);
// 		return 0;
// 	}
// 	sk_MIME_HEADER_pop_free(headers, mime_hdr_free);
// 	while ((len = BIO_read(in, iobuf, sizeof(iobuf))) > 0)
// 						BIO_write(out, iobuf, len);
// 	return 1;
    }

    /* c: static mime_bound_check
     *
     */
    private int boundCheck(byte[] line, int linelen, byte[] bound, int blen) {
        if(linelen == -1) {
            linelen = line.length;
        }

        if(blen == -1) {
            blen = bound.length;
        }

        // Quickly eliminate if line length too short
        if(blen + 2 > linelen) {
            return 0;
        }

        if(line[0] == '-' && 
           line[1] == '-' &&
           equals(line, 2, bound, 0, blen)) {
            if(line.length>=(blen+4) &&
               line[2 + blen] == '-' &&
               line[2 + blen + 1] == '-') {
                return 2;
            } else {
                return 1;
            }
        }
        return 0;
    }

    /* c: B64_read_PKCS7
     *
     */
    public PKCS7 readPKCS7Base64(BIO bio) throws IOException, PKCS7Exception {
        BIO bio64 = BIO.base64Filter(bio);
        return PKCS7.fromASN1(bio64);
    }

    /* c: static multi_split
     *
     */
    private List<BIO> multiSplit(BIO bio, byte[] bound) throws IOException {
        List<BIO> parts = new ArrayList<BIO>();
        byte[] linebuf = new byte[MAX_SMLEN];
        int blen = bound.length;
        boolean eol = false;
        int len = 0;
        int part = 0;
        int state = 0;
        boolean first = true;
        BIO bpart = null;

        while((len = bio.gets(linebuf, MAX_SMLEN)) > 0) {
            state = boundCheck(linebuf, len, bound, blen);
            if(state == 1) {
                first = true;
                part++;
            } else if(state == 2) {
                parts.add(bpart);
                return parts;
            } else if(part != 0) {
                // strip CR+LF from linebuf
                int[] tmp = new int[] {len};
                boolean nextEol = stripEol(linebuf, tmp);
                len = tmp[0];

                if(first) {
                    first = false;
                    if(bpart != null) {
                        parts.add(bpart);
                    }
                    bpart = BIO.mem();
                    bpart.setMemEofReturn(0);
                } else if(eol) {
                    bpart.write(NEWLINE, 0, 2);
                }
                eol = nextEol;
                if(len != 0) {
                    bpart.write(linebuf, 0, len);
                }
            }
        }

        return parts;
    }

    /* c: SMIME_read_PKCS7
     *
     */
    public PKCS7 readPKCS7(BIO bio, BIO[] bcont) throws IOException, PKCS7Exception {
        if(bcont != null && bcont.length > 0) {
            bcont[0] = null;
        }

        List<MimeHeader> headers = mime.parseHeaders(bio);
        if(headers == null) {
            throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_MIME_PARSE_ERROR);
        }

        MimeHeader hdr = mime.findHeader(headers, "content-type");
        if(hdr == null || hdr.getValue() == null) {
            throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_NO_CONTENT_TYPE);
        }

        if("multipart/signed".equals(hdr.getValue())) {
            MimeParam prm = mime.findParam(hdr, "boundary");
            if(prm == null || prm.getParamValue() == null) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_NO_MULTIPART_BOUNDARY);
            }

            byte[] boundary = null;
            try {
                boundary = prm.getParamValue().getBytes("ISO8859-1");
            } catch(Exception e) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_NO_MULTIPART_BOUNDARY, e);
            }

            List<BIO> parts = multiSplit(bio, boundary);
            if(parts == null || parts.size() != 2) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_NO_MULTIPART_BODY_FAILURE);
            }

            BIO p7in = parts.get(1);

            headers = mime.parseHeaders(p7in);

            if(headers == null) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_MIME_SIG_PARSE_ERROR);
            }

            hdr = mime.findHeader(headers, "content-type");
            if(hdr == null || hdr.getValue() == null) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_NO_SIG_CONTENT_TYPE);
            }

            if(!"application/x-pkcs7-signature".equals(hdr.getValue()) &&
               !"application/pkcs7-signature".equals(hdr.getValue()) &&
               !"application/x-pkcs7-mime".equals(hdr.getValue()) &&
               !"application/pkcs7-mime".equals(hdr.getValue())) {
                throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_SIG_INVALID_MIME_TYPE, "type: " + hdr.getValue());
            }

            PKCS7 p7 = readPKCS7Base64(p7in);

            if(bcont != null && bcont.length>0) {
                bcont[0] = parts.get(0);
            }

            return p7;
        }
        
        if(!"application/x-pkcs7-mime".equals(hdr.getValue()) &&
           !"application/pkcs7-mime".equals(hdr.getValue())) {
            throw new PKCS7Exception(PKCS7.F_SMIME_READ_PKCS7, PKCS7.R_INVALID_MIME_TYPE, "type: " + hdr.getValue());
        }

        return readPKCS7Base64(bio);
    }

    /* c: SMIME_write_PKCS7
    *
    */
    public String writePKCS7(PKCS7 p7, String data, int flags) throws PKCS7Exception, IOException {

        int ctype = p7.getType();
        Set<AlgorithmIdentifier> mdAlgs = null;

        if (ctype == ASN1Registry.NID_pkcs7_signed) {
            mdAlgs = p7.getSign().getMdAlgs();
        }

        if (data != null && p7.isDetached()) {
            flags |= PKCS7.SMIME_DETACHED; // to be compliant with cruby implementation.
        }

        String mimePrefix = "application/pkcs7-";
        String mimeEOL;
        String cName = "smime.p7s";

        if ((flags & PKCS7.SMIME_CRLFEOL) > 0) {
            mimeEOL = "\r\n";
        } else {
            mimeEOL = "\n";
        }

        StringBuilder output = new StringBuilder();
        output.append("MIME-Version: 1.0").append(mimeEOL);

        // Detached sign.
        if ((flags & PKCS7.SMIME_DETACHED) > 0 && data != null) {
            String mimeBoundary = generateMIMEBoundary(32);

            // write headers
            output.append("Content-Type: multipart/signed;");
            output.append(" protocol=\"").append(mimePrefix).append("signature\";");
            output.append(" micalg=\"").append(getMICalg(mdAlgs)).append("\";");
            output.append(" boundary=\"----").append(mimeBoundary).append("\"");
            output.append(mimeEOL).append(mimeEOL);

            // write S/MIME preamble message
            output.append("This is an S/MIME signed message.");
            output.append(mimeEOL).append(mimeEOL);

            // write data part
            output.append("------").append(mimeBoundary).append(mimeEOL);
            if ((flags & PKCS7.TEXT) > 0) {
                output.append("Content-Type: text/plain;").append(mimeEOL).append(mimeEOL);
            }
            output.append(data);
            output.append(mimeEOL);
            output.append("------").append(mimeBoundary).append(mimeEOL);

            // write signature part
            output.append("Content-Type: application/x-pkcs7-signature; name=").append(cName)
                    .append(mimeEOL);
            output.append("Content-Transfer-Encoding: base64").append(mimeEOL);
            output.append("Content-Description: S/MIME Signature").append(mimeEOL);
            output.append("Content-Disposition: attachment; filename=").append(cName);
            output.append(mimeEOL).append(mimeEOL);

            byte[] p7Bytes = p7.toASN1();
            String p7Base64 = Base64.encodeBytes(p7Bytes, Base64.DO_BREAK_LINES);
            output.append(p7Base64).append(mimeEOL);

            // write final boundary
            output.append("------").append(mimeBoundary).append("--").append(mimeEOL);

            return output.toString();
        }

        String msgType = null;

        // This is not a detached sign.
        if (ctype == ASN1Registry.NID_pkcs7_enveloped) {
            msgType = "enveloped-data";
        } else if (ctype == ASN1Registry.NID_pkcs7_signed) {
            if (mdAlgs != null && mdAlgs.size() > 0) {
                msgType = "signed-data";
            } else {
                msgType = "certs-only";
            }
        } else if (ctype == ASN1Registry.NID_id_smime_ct_compressedData) {
            msgType = "compressed-data";
            cName = "smime.p7z";
        }

        // MIME Headers
        output.append("Content-Disposition: attachment;");
        output.append(" filename=\"").append(cName).append("\"").append(mimeEOL);
        output.append("Content-Type: ").append(mimePrefix).append("mime;");
        if (msgType != null) {
            output.append(" smime-type=").append(msgType).append(";");
        }
        output.append(" name=").append(cName).append(mimeEOL);
        output.append("Content-Transfer-Encoding: base64").append(mimeEOL).append(mimeEOL);

        // Write content
        byte[] p7Bytes = p7.toASN1();
        String p7Base64 = Base64.encodeBytes(p7Bytes, Base64.DO_BREAK_LINES);
        output.append(p7Base64).append(mimeEOL);

        return output.toString();
    }

    /**
     * Generates MIME compatible MIC algorithm names for content-type header.
     *
     *  c : asn1_write_micalg
     */
    private String getMICalg(Set<AlgorithmIdentifier> mdAlgs) {

        StringBuilder output = new StringBuilder();
        Iterator<AlgorithmIdentifier> it = mdAlgs.iterator();

        boolean writeComma = false;
        while (it.hasNext()) {
            if (writeComma) {
                output.append(",");
            }

            String ln = ASN1Registry.nid2ln(ASN1Registry.obj2nid(it.next().getAlgorithm()));
            if (ln == null) {
                ln = "unknown";
            }

            output.append(ln);
            writeComma = true;
        }

        return output.toString();
    }

    /**
     * Generates a random boundary for MIME multipart messages.
     * The alphabet can include other characters, but digits and letters should suffice.
     *
     * @param length the length of the string. (MIME spec allows a max of 70 chars)
     * @return the generated boundary.
     */
    private String generateMIMEBoundary(int length) {

        final char alphabet[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9' };
        Random random = new Random();

        StringBuilder output = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            output.append(alphabet[random.nextInt(length)]);
        }

        return output.toString();
    }

 }
