/*
 * RubyNumeric.java - No description
 * Created on 10. September 2001, 17:49
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby;

/**
 *
 * @author  jpetersen
 * @version 
 */
public abstract class RubyNumeric extends RubyObject {

    public RubyNumeric(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }
    
    public RubyNumeric[] getCoerce(RubyNumeric other) {
        if (getRubyClass() == other.getRubyClass()) {
            return new RubyNumeric[] {this, other};
        } else {
            // HACK +++
            // todo: convert both to float.
            return new RubyNumeric[] {this, other};
            // HACK ---
        }
    }
    
    /* Numeric methods. (num_*)
     *
     */
    
    /** num_coerce
     *
     */
    public RubyArray m_coerce(RubyNumeric other) {
        if (getRubyClass() == other.getRubyClass()) {
            return RubyArray.m_newArray(getRuby(), this, other);
        } else {
            return null; // Convert both to float.
        }
    }
    
    /** num_clone
     *
     */
    public RubyObject m_clone() {
        return this; 
    }
    
    /** num_uplus
     *
     */
    public RubyNumeric op_uplus() {
        return this;
    }
    
    /** num_uminus
     *
     */
    public RubyNumeric op_uminus() {
        RubyNumeric[] coerce = getCoerce(RubyFixnum.m_newFixnum(getRuby(), 0));
        
        return (RubyNumeric)coerce[1].funcall(getRuby().intern("-"), coerce[0]);
    }
    
    /** num_divmod
     *
     */
    public RubyArray m_divmod(RubyNumeric other) {
        RubyNumeric div = (RubyNumeric)funcall(getRuby().intern("/"), other);
        /*if (div instanceof RubyFloat) {
            double d = Math.floor(((RubyFloat)div).getValue());
            if (((RubyFloat)div).getValue() > d) {
                div = RubyFloat.m_newFloat(getRuby(), d);
            }
        }*/
        RubyNumeric mod = (RubyNumeric)funcall(getRuby().intern("%"), other);
        
        return RubyArray.m_newArray(getRuby(), div, mod);
    }
    
    /** num_modulo
     *
     */
    public RubyNumeric m_modulo(RubyNumeric other) {
        return (RubyNumeric)funcall(getRuby().intern("%"), other);
    }
    
    /** num_remainder
     *
     */
    public RubyNumeric m_remainder(RubyNumeric other) {
        RubyNumeric mod = (RubyNumeric)funcall(getRuby().intern("%"), other);
        
        final RubyNumeric zero = RubyFixnum.m_newFixnum(getRuby(), 0);
        
        if (funcall(getRuby().intern("<"), zero).isTrue() &&
            other.funcall(getRuby().intern(">"), zero).isTrue() ||
            funcall(getRuby().intern(">"), zero).isTrue() &&
            other.funcall(getRuby().intern("<"), zero).isTrue()) {
                
            return (RubyNumeric)mod.funcall(getRuby().intern("-"), other);
        }
        
        return mod;
    }
    
    /** num_equal
     *
     */
    public RubyBoolean m_equal(RubyObject other) {
        return super.m_equal(other);
    }
    
    /** num_eql
     *
     */
    public RubyBoolean m_eql(RubyObject other) {
        if (getRubyClass() != other.getRubyClass()) {
            return getRuby().getFalse();
        } else {
            return super.m_equal(other);
        }
    }
    
    /** num_abs
     *
     */
    public RubyNumeric m_abs() {
        if (((RubyBoolean)funcall(getRuby().intern("<"), RubyFixnum.m_newFixnum(getRuby(), 0))).isTrue()) {
            return (RubyNumeric)funcall(getRuby().intern("-@"));
        } else {
            return this;
        }
    }
    
    /** num_int_p
     *
     */
    public RubyBoolean m_int_p() {
        return getRuby().getFalse();
    }
    
    /** num_zero_p
     *
     */
    public RubyBoolean m_zero_p() {
        return m_equal(RubyFixnum.m_newFixnum(getRuby(), 0));
    }
    
    /** num_nonzero_p
     *
     */
    public RubyObject m_nonzero_p() {
        if (((RubyBoolean)funcall(getRuby().intern("zero?"))).isTrue()) {
            return getRuby().getNil();
        }
        return this;
    }
    
    /** num_floor
     *
     */
    public RubyNumeric m_floor() {
        // HACK +++
        return RubyFixnum.m_newFixnum(getRuby(), 0);
        // HACK ---
    }
    
    /** num_ceil
     *
     */
    public RubyNumeric m_ceil() {
        // HACK +++
        return RubyFixnum.m_newFixnum(getRuby(), 0);
        // HACK ---
    }
    
    /** num_round
     *
     */
    public RubyNumeric m_round() {
        // HACK +++
        return RubyFixnum.m_newFixnum(getRuby(), 0);
        // HACK ---
    }
    
    /** num_truncate
     *
     */
    public RubyNumeric m_truncate() {
        // HACK +++
        return RubyFixnum.m_newFixnum(getRuby(), 0);
        // HACK ---
    }
}