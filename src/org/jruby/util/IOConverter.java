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
package org.jruby.util;

import java.io.InputStream;
import java.io.OutputStream;

import org.jruby.RubyIO;

/**
 * A wrapper object to allow access to the IO-internal input and outputstreams.
 * This class is a hack, and should be replaced by Ruby classes that extend
 * OutputStream and InputStream to act as wrappers to IO-objects.
 * This is not possible at this point, since those classes are abstract.
 *
 * The fatal flaw of this class is that there is no possibility for duck typing.
 */
public class IOConverter {
    private final RubyIO io;

    /**
     * Creates a new converter with the IO object provided.
     *
     * @param io the io object
     */
    public IOConverter(final RubyIO io) {
        this.io = io;
    }
    
    /**
     * Returns the internal OutputStream.
     *
     * @return the output stream
     */
    public OutputStream asOutputStream() {
        return io.getOutStream();
    }

    /**
     * Returns the internal InputStream.
     *
     * @return the input stream
     */
    public InputStream asInputStream() {
        return io.getInStream();
    }
}
