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

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class MemBIO extends BIO {
    private byte[] buffer = new byte[1024];
    private int wpointer = 0;
    private int rpointer = 0;
    private int slen = 0;
    
    private void realloc() {
        byte[] newBuffer = new byte[buffer.length*2];
        System.arraycopy(buffer, 0, newBuffer, 0, wpointer);
        buffer = newBuffer;
    }

    @Override
    public int gets(byte[] in, int len) throws IOException {
        if(rpointer == slen) {
            return 0;
        }

        int i=0;
        for(;i<len && rpointer<slen; i++, rpointer++) {
            in[i] = buffer[rpointer];

            if(in[i] == '\n') {
                i++; rpointer++;
                break;
            }
        }

        return i;
    }

    @Override
    public int read(byte[] in, int index, int len) throws IOException {
        if(rpointer == slen) {
            return 0;
        }
        int toRead = Math.min(len, slen-rpointer);
        System.arraycopy(buffer, rpointer, in, index, toRead);
        rpointer+=toRead;
        return toRead;
    }

    @Override
    public int write(byte[] out, int offset, int len) throws IOException {
        while(wpointer + len > buffer.length) {
            realloc();
        }

        System.arraycopy(out, offset, buffer, wpointer, len);
        wpointer += len;
        slen += len;

        return len;
    }

    @Override
    public String toString() {
        try {
            return "<MemBIO w:" + wpointer + " r:" + rpointer + " buf:\"" + new String(buffer,rpointer,slen-rpointer) + "\" next=" + next() + ">";
        } catch(Exception e) {}

        return null;
    }

    @Override
    public void setMemEofReturn(int value) {
    }

    public int getType() {
        return TYPE_MEM;
    }

    public byte[] getMemCopy() {
        byte[] nbuf = new byte[slen];
        System.arraycopy(buffer, 0, nbuf, 0, slen);
        return nbuf;
    }

    public void reset() {
        this.rpointer = 0;
    }
}// MemBIO
