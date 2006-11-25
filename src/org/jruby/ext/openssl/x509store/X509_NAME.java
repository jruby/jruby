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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl.x509store;

import java.security.MessageDigest;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_NAME {
    public X509Name name;

    public X509_NAME(X500Principal nm) {
        try {
            this.name = new X509Name((ASN1Sequence)new ASN1InputStream(nm.getEncoded()).readObject());
        } catch(Exception e) {
            this.name = null;
        }
    }

    public X509_NAME(X509Name nm) {
        this.name = nm;
    }

    public long hash() { 
        try {
            byte[] bytes = name.getEncoded();
            byte[] md = null;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md = md5.digest(bytes);
            return md[0] | ((long)md[1] << 8) | ((long)md[2] << 16) | ((long)md[3] << 24);
        } catch(Exception e) {
            return 0;
        }
    }

    public boolean isEqual(X500Principal oname) {
        try {
            return new X500Principal(name.getEncoded()).equals(oname);
        } catch(Exception e) {
            return false;
        }
    }
}// X509_NAME
