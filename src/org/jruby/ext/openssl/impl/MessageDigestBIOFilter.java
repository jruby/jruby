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
import java.security.MessageDigest;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class MessageDigestBIOFilter extends BIOFilter {
    private MessageDigest md;

    public MessageDigestBIOFilter(MessageDigest md) {
        this.md = md;
    }

    public int gets(byte[] in, int len) throws IOException {
        int read = next().gets(in, len);
        if(read > 0) {
            md.update(in, 0, read);
        }
        return read;
    }

    public int read(byte[] into, int offset, int len) throws IOException {
        int read = next().read(into, offset, len);
        if(read > 0) {
            md.update(into, offset, read);
        }
        return read;
    }

    public int write(byte[] out, int offset, int len) throws IOException {
        int written = next().write(out, offset, len);
        md.update(out, offset, written);
        return written;
    }

    public int getType() {
        return TYPE_MD;
    }

    /** c: BIO_get_md_ctx
     *
     */
    public MessageDigest getMessageDigest() {
        return md;
    }
}// MessageDigestBIOFilter
