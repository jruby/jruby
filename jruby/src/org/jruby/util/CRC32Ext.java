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

import java.lang.reflect.Field;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * This class is a wrapper around Adler32 which provides the capability to 
 * update the running total. This functionality is provided by quite risky
 * reflection and should be fixed in a better way later on.
 */
public class CRC32Ext implements Checksum {
    private int crc;
    private final CRC32 intern;

    private static final Field intern_crc; 

    static {
        try {
            intern_crc = CRC32.class.getDeclaredField("crc");
            intern_crc.setAccessible(true);
        } catch(final NoSuchFieldException nsfe) {
            throw new RuntimeException("This class have stopped working, it should be updated and FIXED now.");
        }
    }

    /**
     * Creates the basic object with default initial adler.
     */
    public CRC32Ext() {
        this(1);
    }

    /**
     * Creates the basic object with the adler provided.
     *
     * @param adler the number to use as starting point for the CRC-32 algorithm
     */
    public CRC32Ext(final int crc) {
        super();
        this.crc=crc;
        this.intern = new CRC32();
        setCRCRef(this.crc);
    }

    /**
     * Sets the adler running total to the specified value.
     *
     * @param adler the number to use as current value for the CRC-32 algorithm
     */
    public void setAdler(final int crc) {
        this.crc = crc;
        setCRCRef(this.crc);
    }

    /**
     * @see java.util.zip.Checksum#update
     */
    public void update(final int b) {
        this.intern.update(b);
    }

    /**
     * @see java.util.zip.Checksum#update
     */
    public void update(final byte[] b, final int off, final int len) {
        this.intern.update(b,off,len);
    }

    /**
     * @see java.util.zip.Checksum#update
     */
    public void update(final byte[] b) {
        this.intern.update(b);
    }

    /**
     * @see java.util.zip.Checksum#reset
     */
    public void reset() {
        this.intern.reset();
	this.crc = 1;
    }

    /**
     * @see java.util.zip.Checksum#getValue
     */
    public long getValue() {
	return this.intern.getValue();
    }

    /**
     * Helper method to set the reference through reflection.
     *
     * @param val the value to set.
     */
    private void setCRCRef(final int val) {
        try {
            intern_crc.setInt(intern,val);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException(e.toString());
        }
    }
}
