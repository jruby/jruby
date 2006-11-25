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

import java.util.List;
import java.util.Iterator;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class X509_OBJECT implements Comparable {
    public static int idx_by_subject(List h, int type, X509_NAME name) {
        int ix = 0;
        for(Iterator iter = h.iterator();iter.hasNext();ix++) {
            X509_OBJECT oo = (X509_OBJECT)iter.next();
            if(type == oo.type() && oo.isName(name)) {
                return ix;
            }
        }
        return -1;
    }

    public static X509_OBJECT retrieve_by_subject(List h,int type,X509_NAME name) {
        for(Iterator iter = h.iterator();iter.hasNext();) {
            X509_OBJECT o = (X509_OBJECT)iter.next();
            if(type == o.type() && o.isName(name)) {
                return o;
            }
        }
        return null;
    }

    public static X509_OBJECT retrieve_match(List h, X509_OBJECT x) {
        for(Iterator iter = h.iterator();iter.hasNext();) {
            X509_OBJECT o = (X509_OBJECT)iter.next();
            if(o.matches(x)) {
                return o;
            }
        }
        return null;
    }

    public boolean isName(X509_NAME nm) {
        return false;
    }

    public boolean matches(X509_OBJECT o) {
        return false;
    }

    public abstract int type();

    public int compareTo(Object other) {
        return type() - ((X509_OBJECT)other).type();
    }
}// X509_OBJECT
