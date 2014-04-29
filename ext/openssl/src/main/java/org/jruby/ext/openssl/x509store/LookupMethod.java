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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl.x509store;

import java.math.BigInteger;

/**
 * x509_lookup_method_st and X509_LOOKUP_METHOD in x509_vfy.h
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class LookupMethod {
    public String name;

    static interface NewItemFunction extends Function1<Lookup> {}
    static interface FreeFunction extends Function1<Lookup> {}
    static interface InitFunction extends Function1<Lookup> {}
    static interface ShutdownFunction extends Function1<Lookup> {}
    static interface ControlFunction extends Function5<Lookup, Integer, String, Number, String[]> {}
    static interface BySubjectFunction extends Function4<Lookup, Integer, Name, X509Object[]> {}
    static interface ByIssuerSerialNumberFunction extends Function5<Lookup, Integer, Name, BigInteger, X509Object[]> {}
    static interface ByFingerprintFunction extends Function4<Lookup, Integer, String, X509Object[]> {}
    static interface ByAliasFunction extends Function4<Lookup, Integer, String, X509Object[]> {}

    /**
     * c: new_item
     */
    NewItemFunction newItem;
    /**
     * c: free
     */
    FreeFunction free;
    /**
     * c: init
     */
    InitFunction init;
    /**
     * c: shutdown
     */
    ShutdownFunction shutdown;
    /**
     * c: ctrl
     */
    ControlFunction control;
    /**
     * c: get_by_subject
     */
    BySubjectFunction getBySubject;
    /**
     * c: get_by_issuer_serial
     */
    ByIssuerSerialNumberFunction getByIssuerSerialNumber;
    /**
     * c: get_by_fingerprint
     */
    ByFingerprintFunction getByFingerprint;
    /**
     * c: get_by_alias
     */
    ByAliasFunction getByAlias;

}// X509_LOOKUP_METHOD
