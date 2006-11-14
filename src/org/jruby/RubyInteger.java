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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
public abstract class RubyInteger extends RubyNumeric { 
    public RubyInteger(IRuby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }
    
    public RubyInteger convertToInteger() {
    	return this;
    }

    // conversion
    protected RubyFloat toFloat() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    // Integer methods

    public RubyString chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            throw getRuntime().newRangeError(this.toString() + " out of char range");
        }
        return getRuntime().newString(new String(new char[] {(char) getLongValue()}));
    }

    // TODO: Make callCoerced work in block context...then fix downto, step, and upto.
    public IRubyObject downto(IRubyObject to) {
        RubyNumeric i = this;
        ThreadContext context = getRuntime().getCurrentContext();
        while (true) {
            if (i.callMethod(context, "<", to).isTrue()) {
                break;
            }
            context.yield(i);
            i = (RubyNumeric) i.callMethod(context, "-", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public RubyBoolean int_p() {
        return getRuntime().getTrue();
    }

    public IRubyObject step(IRubyObject to, IRubyObject step) {
    	RubyNumeric test = (RubyNumeric) to;
        RubyNumeric i = this;
        if (((RubyNumeric) step).getLongValue() == 0) {
            throw getRuntime().newArgumentError("step cannot be 0");
        }

        String cmp = "<";
        if (((RubyBoolean) step.callMethod(getRuntime().getCurrentContext(), "<", getRuntime().newFixnum(0))).isFalse()) {
            cmp = ">";
        }

        ThreadContext context = getRuntime().getCurrentContext();
        while (true) {
            if (i.callMethod(context, cmp, test).isTrue()) {
                break;
            }
            context.yield(i);
            i = (RubyNumeric) i.callMethod(context, "+", step);
        }
        return this;
    }

    public IRubyObject times() {
        RubyNumeric i = RubyFixnum.zero(getRuntime());
        ThreadContext context = getRuntime().getCurrentContext();
        while (true) {
            if (!i.callMethod(context, "<", this).isTrue()) {
                break;
            }
            context.yield(i);
            i = (RubyNumeric) i.callMethod(context, "+", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public IRubyObject next() {
        return callMethod(getRuntime().getCurrentContext(), "+", RubyFixnum.one(getRuntime()));
    }

    public IRubyObject upto(IRubyObject to) {
    	RubyNumeric test = (RubyNumeric) to;
        RubyNumeric i = this;
        ThreadContext context = getRuntime().getCurrentContext();
        while (true) {
            if (i.callMethod(context, ">", test).isTrue()) {
                break;
            }
            context.yield(i);
            i = (RubyNumeric) i.callMethod(context, "+", RubyFixnum.one(getRuntime()));
        }
        return this;
    }

    public RubyInteger to_i() {
        return this;
    }

    public RubyNumeric multiplyWith(RubyBignum value) {
        return value.multiplyWith(this);
    }
}
