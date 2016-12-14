/*
 ***** BEGIN LICENSE BLOCK *****
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
package org.jruby.truffle.parser;

/**
 * Created by headius on 5/8/15.
 */
public enum ArgumentType {

    key("key", 'k', false),
    keyreq("keyreq", 'K', false),
    keyrest("keyrest", 'e', false),
    block("block", 'b', false),
    opt("opt", 'o', false),
    rest("rest", 'r', false),
    req("req", 'q', false),
    anonreq("req", 'n', true),
    anonopt("opt", 'O', true),
    anonrest("rest", 'R', true),
    anonkeyrest("keyrest", 'N', true);

    public static final String ANONOPT = Character.toString( anonopt.prefix );
    public static final String ANONREST = Character.toString( anonrest.prefix );
    public static final String REQ = Character.toString( req.prefix );

    private ArgumentType(String symbolicName, char prefix, boolean anonymous) {
        this.symbolicName = symbolicName;
        this.prefix = prefix;
        this.anonymous = anonymous;
    }

    public static ArgumentType valueOf(char prefix) {
        switch (prefix) {
            case 'k': return key;
            case 'K': return keyreq;
            case 'e': return keyrest;
            case 'b': return block;
            case 'o': return opt;
            case 'r': return rest;
            case 'q': return req;
            case 'n': return anonreq;
            case 'O': return anonopt;
            case 'R': return anonrest;
            case 'N': return anonkeyrest;
            default: return null;
        }
    }

    public String renderPrefixForm(String name) {
        return anonymous ? String.valueOf(prefix) : prefix + name;
    }

    public ArgumentType anonymousForm() {
        switch (this) {
            case opt: return anonopt;
            case req: return anonreq;
            case rest: return anonrest;
            case keyrest: return anonkeyrest;
        }
        return this;
    }

    public final String symbolicName;
    private final char prefix;
    public final boolean anonymous;
}
