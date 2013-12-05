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

import org.bouncycastle.asn1.ASN1Encodable;
import java.util.List;
import java.util.ArrayList;

/** X509_ATTRIBUTE
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Attribute {
    private int type;
    private boolean single;
    private List<ASN1Encodable> set;

    private Attribute() {}

    public static Attribute create(int nid, int atrtype, ASN1Encodable value) {
        Attribute ret = new Attribute();

        ret.type = nid;
        ret.single = false;
        ret.set = new ArrayList<ASN1Encodable>();
        ret.set.add(value);

        return ret;
    }

    public int getType() {
        return type;
    }

    public List<ASN1Encodable> getSet() {
        return set;
    }

    public boolean isSingle() {
        return this.single;
    }

    @Override
    public boolean equals(Object obj) {
        boolean ret = this == obj;
        if(!ret && (obj instanceof Attribute)) {
            Attribute attr2 = (Attribute)obj;
            ret = 
                this.type == attr2.type &&
                this.set.equals(attr2.set);
        }
        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((set == null) ? 0 : set.hashCode());
        result = prime * result + type;
        return result;
    }
}// Attribute
