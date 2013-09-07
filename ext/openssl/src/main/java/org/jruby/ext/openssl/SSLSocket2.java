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

package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.util.io.ReadBuffered;

/**
 * Represents an SSLSocket that can report whether data is buffered.
 * 
 * In order to introduce ReadBuffered interface in JRuby 1.7.5 without causing
 * SSLSocket to be incompatible with older JRuby versions, we must reflectively
 * check if the ReadBuffered interface is available. If it is, we return
 * instances of SSLSocket2 that implement that interface. Otherwise, we return
 * instances of the superclass SSLSocket that does not reference it in any way.
 * 
 * Because of the lazy nature of class linking in the JVM, this allows us to
 * add an interface impl to our SSL socket implementation without breaking
 * backward compatibility.
 */
public class SSLSocket2 extends SSLSocket implements ReadBuffered {
    SSLSocket2(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
}
