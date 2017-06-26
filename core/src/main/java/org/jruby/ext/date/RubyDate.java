/****** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2008 Vladimir Sizikov <vsizikov@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.ext.date;

import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateParser;
import org.jruby.util.TypeConverter;

@JRubyModule(name = "Date")
public class RubyDate {
    private static final ByteList DEFAULT_FORMAT = new ByteList(new byte[] {'%', 'F'});

    public static IRubyObject _strptime(ThreadContext context, IRubyObject str) {
        return _strptime(context, str, context.runtime.newString(DEFAULT_FORMAT));
    }

    public static IRubyObject _strptime(ThreadContext context, IRubyObject string, IRubyObject format) {
        RubyString stringString = (RubyString) TypeConverter.checkStringType(context.runtime, string);
        RubyString formatString = (RubyString) TypeConverter.checkStringType(context.runtime, format);

        return new RubyDateParser().parse(context, formatString, stringString);
    }

    @JRubyMethod(meta = true, required = 1, optional = 1)
    public static IRubyObject _strptime(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        switch(args.length) {
            case 1:
                return _strptime(context, args[0]);
            case 2:
                return _strptime(context, args[0], args[1]);
            default:
                throw context.runtime.newArgumentError(args.length, 1);
        }
    }
}