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

import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * This class is a wrapper around Adler32 which provides the capability to 
 * update the running total. This functionality is provided by quite risky
 * reflection and should be fixed in a better way later on.
 */
public class Adler32Ext implements Checksum {
    private int adler;
    private final Adler32 intern;

    private static final Field intern_adler; 

    static {
        try {
            intern_adler = Adler32.class.getDeclaredField("adler");
            intern_adler.setAccessible(true);
        } catch(final NoSuchFieldException nsfe) {
            throw new RuntimeException("This class have stopped working, it should be updated and FIXED now.");
        }
    }

    /**
     * Creates the basic object with default initial adler.
     */
    public Adler32Ext() {
        this(1);
    }

    /**
     * Creates the basic object with the adler provided.
     *
     * @param adler the number to use as starting point for the Adler-32 algorithm
     */
    public Adler32Ext(final int adler) {
        super();
        this.adler=adler;
        this.intern = new Adler32();
        setAdlerRef(this.adler);
    }

    /**
     * Sets the adler running total to the specified value.
     *
     * @param adler the number to use as current value for the Adler-32 algorithm
     */
    public void setAdler(final int adler) {
        this.adler = adler;
        setAdlerRef(this.adler);
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
	this.adler = 1;
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
    private void setAdlerRef(final int val) {
        try {
            intern_adler.setInt(intern,val);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException(e.toString());
        }
    }
}
