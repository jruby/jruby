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
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class CipherBIOFilter extends BIOFilter {
    private Cipher cipher;

    private byte[] bufRead = new byte[4096];
    private int fillLen = 0;
    private int fillOffset = 0;

    private byte[] tmpBuf = new byte[1024];

    private boolean finalized = false;

    public CipherBIOFilter(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void flush() throws IOException, PKCS7Exception {
        try {
            byte[] result = cipher.doFinal();
            if(result == null) {
                return;
            }
            next().write(result, 0, result.length);
        } catch(IllegalBlockSizeException e) {
            throw new PKCS7Exception(-1, -1, e);
        } catch(BadPaddingException e) {
            throw new PKCS7Exception(-1, -1, e);
        }
    }

    public int read(byte[] into, int offset, int len) throws IOException {
        try {
            int read = 0;
            if(fillLen > 0) {
                read = Math.min(fillLen, len);
                System.arraycopy(bufRead, fillOffset, into, offset, read);
                fillOffset += read;
                fillLen -= read;
                if(fillLen == 0) {
                    fillOffset = 0;
                }
                if(read == len) {
                    return read;
                }
            }
            
            int req = len - read;
            int off = offset + read;
            
            if(finalized) {
                return 0;
            }

            while(req > 0) {
                int readFromNext = next().read(tmpBuf, 0, 1024);
                if(readFromNext > 0) {
                    int required = cipher.getOutputSize(readFromNext);
                    if(required > (bufRead.length - (fillOffset + fillLen))) {
                        byte[] newBuf = new byte[required + fillOffset + fillLen];
                        System.arraycopy(bufRead, fillOffset, newBuf, 0, fillLen);
                        fillOffset = 0;
                        bufRead = newBuf;
                    }
                    int outputted = cipher.update(tmpBuf, 0, readFromNext, bufRead, fillOffset + fillLen);
                    fillLen += outputted;

                    read = Math.min(fillLen, req);
                    System.arraycopy(bufRead, fillOffset, into, off, read);
                    fillOffset += read;
                    fillLen -= read;
                    if(fillLen == 0) {
                        fillOffset = 0;
                    }

                    req -= read;
                    off += read;
                } else {
                    int required = cipher.getOutputSize(0);
                    if(required > (bufRead.length - (fillOffset + fillLen))) {
                        byte[] newBuf = new byte[required + fillOffset + fillLen];
                        System.arraycopy(bufRead, fillOffset, newBuf, 0, fillLen);
                        fillOffset = 0;
                        bufRead = newBuf;
                    }
                    int outputted = cipher.doFinal(bufRead, fillOffset + fillLen);
                    finalized = true;
                    fillLen += outputted;

                    read = Math.min(fillLen, req);
                    System.arraycopy(bufRead, fillOffset, into, off, read);
                    fillOffset += read;
                    fillLen -= read;
                    if(fillLen == 0) {
                        fillOffset = 0;
                    }

                    req -= read;
                    return len-req;
                }
            }

            return len;
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int write(byte[] out, int offset, int len) throws IOException {
        byte[] result = cipher.update(out, offset, len);
        if(result == null) {
            return len;
        }
        next().write(result, 0, result.length);
        return len;
    }

    public int getType() {
        return TYPE_CIPHER;
    }
}// CipherBIOFilter
