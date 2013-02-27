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

import java.util.ArrayList;
import java.util.List;

/** MIME_HEADER
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class MimeHeader {
    private String name;
    private String value;

    /**
     * Describe params here.
     */
    private List<MimeParam> params;

    public MimeHeader(String name, String value) {
        this(name, value, new ArrayList<MimeParam>());
    }

    public MimeHeader(String name, String value, List<MimeParam> params) {
        this.name = (name == null) ?
            null :
            name.toLowerCase();
        this.value = (value == null) ?
            null :
            value.toLowerCase();
        this.params = params;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Get the <code>Params</code> value.
     *
     * @return a <code>List<MimeParam></code> value
     */
    public final List<MimeParam> getParams() {
        return params;
    }

    /**
     * Set the <code>Params</code> value.
     *
     * @param newParams The new Params value.
     */
    public final void setParams(final List<MimeParam> newParams) {
        this.params = newParams;
    }

    @Override
    public boolean equals(Object other) {
        boolean ret = this == other;
        if(!ret && (other instanceof MimeHeader)) {
            MimeHeader mh = (MimeHeader)other;
            ret = 
                ((this.name == null) ? mh.name == null : this.name.equals(mh.name)) &&
                ((this.value == null) ? mh.value == null : this.value.equals(mh.value)) &&
                ((this.params == null) ? mh.params == null : this.params.equals(mh.params));
        }
        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "#<MimeHeader " + name + ": '"+value+"' params="+params+">";
    }
}// MimeHeader
