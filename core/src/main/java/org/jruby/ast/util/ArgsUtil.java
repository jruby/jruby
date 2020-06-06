/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.ast.util;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import java.util.HashMap;

/**
 *
 * @author  jpetersen
 */
public final class ArgsUtil {
    /**
     * This name may be a bit misleading, since this also attempts to coerce
     * array behavior using to_ary.
     * 
     * @param runtime The JRuby runtime
     * @param value The value to convert
     * @param coerce Whether to coerce using to_ary or just wrap with an array
     */
    public static RubyArray convertToRubyArray(Ruby runtime, IRubyObject value, boolean coerce) {
        if (value == null) {
            return RubyArray.newEmptyArray(runtime);
        }
        
        if (coerce) return convertToRubyArrayWithCoerce(runtime, value);

        // don't attempt to coerce to array, just wrap and return
        return RubyArray.newArrayLight(runtime, value);
    }
    
    public static RubyArray convertToRubyArrayWithCoerce(Ruby runtime, IRubyObject value) {
        if (value instanceof RubyArray) return (RubyArray) value;
        
        IRubyObject newValue = TypeConverter.convertToType(value, runtime.getArray(), "to_ary", false);

        if (newValue.isNil()) {
            return RubyArray.newArrayLight(runtime, value);
        }
        
        // must be array by now, or error
        if (!(newValue instanceof RubyArray)) {
            throw runtime.newTypeError(newValue.getMetaClass() + "#to_ary should return Array");
        }
        
        return (RubyArray) newValue;
    }
    
    public static int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray) node).getLength() : 0;
    }
    
    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject... args) {
        if (args.length >= 1) {
            return TypeConverter.checkHashType(runtime, args[args.length - 1]);
        }
        return runtime.getNil();
    }

    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject arg) {
        return getOptionsArg(runtime, arg, true);
    }

    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject arg, boolean raise) {
        if (arg == null) return runtime.getNil();
        return TypeConverter.checkHashType(runtime, arg, raise);
    }

    private static final IRubyObject[] NULL_1 = new IRubyObject[] { null };
    private static final IRubyObject[] NULL_2 = new IRubyObject[] { null, null };

    /**
     * Check that the given kwargs hash doesn't contain any keys other than those which are given as valid.
     * @param context The context to execute in
     * @param options A RubyHash of options to extract kwargs from
     * @param validKeys A list of valid kwargs keys.
     * @return an array of objects corresponding to the given keys.
     */
    public static IRubyObject[] extractKeywordArgs(ThreadContext context, final RubyHash options, String... validKeys) {
        if (options.isEmpty()) {
            switch (validKeys.length) {
                case 1 : return NULL_1;
                case 2 : return NULL_2;
                default: return new IRubyObject[validKeys.length];
            }
        }

        IRubyObject[] ret = new IRubyObject[validKeys.length];

        HashMap<RubySymbol, ?> validKeySet = new HashMap<>(ret.length);

        // Build the return values
        for (int i=0; i<validKeys.length; i++) {
            final String key = validKeys[i];
            RubySymbol keySym = context.runtime.newSymbol(key);
            IRubyObject val = options.fastARef(keySym);
            ret[i] = val; // null if key missing
            validKeySet.put(keySym, null);
        }

        // Check for any unknown keys
        options.visitAll(context, new RubyHash.Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (!validKeySet.containsKey(key)) {
                    throw context.runtime.newArgumentError("unknown keyword: " + key);
                }
            }
        }, null);

        return ret;
    }

    // not used
    public static IRubyObject[] extractKeywordArgs(ThreadContext context, IRubyObject[] args, String... validKeys) {
        return extractKeywordArgs(context, ArgsUtil.getOptionsArg(context.runtime, args), validKeys);
    }

    public static IRubyObject[] extractKeywordArgs(ThreadContext context, IRubyObject maybeKwargs, String... validKeys) {
        IRubyObject options = ArgsUtil.getOptionsArg(context.runtime, maybeKwargs);

        if (options instanceof RubyHash) {
            return extractKeywordArgs(context, (RubyHash) options, validKeys);
        }

        return null;
    }

    /**
     * Same as {@link #extractKeywordArgs(ThreadContext, RubyHash, String...)}.
     * @param context
     * @param options
     * @param validKey the keyword to extract
     * @return null if key not within options, otherwise <code>options[:keyword]</code>
     */
    public static IRubyObject extractKeywordArg(ThreadContext context, final RubyHash options, String validKey) {
        if (options.isEmpty()) return null;

        IRubyObject ret = options.fastARef(context.runtime.newSymbol(validKey));

        if (ret == null || options.size() > 1) { // other (unknown) keys in options
            options.visitAll(context, new RubyHash.Visitor() {
                public void visit(IRubyObject key, IRubyObject value) {
                    throw context.runtime.newArgumentError("unknown keyword: " + key);
                }
            }, null);
        }

        return ret;
    }

    /**
     * Semi-deprecated, kept for compatibility.
     * Compared to {@link #extractKeywordArg(ThreadContext, RubyHash, String)} does not validate options!
     * @param context
     * @param keyword
     * @param opts
     * @return nil if key not within options (no way to distinguish a key: nil and missing key)
     */
    public static IRubyObject extractKeywordArg(ThreadContext context, String keyword, final RubyHash opts) {
        return opts.op_aref(context, context.runtime.newSymbol(keyword));
    }

    /**
     * Semi-deprecated, kept for compatibility.
     * Compared to {@link #extractKeywordArg(ThreadContext, RubyHash, String)} does not validate options!
     * @param context
     * @param keyword
     * @param arg
     * @return nil if key not within options (no way to distinguish a key: nil and missing key)
     */
    public static IRubyObject extractKeywordArg(ThreadContext context, String keyword, IRubyObject arg) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, arg);

        if (opts == context.nil) return context.nil;

        return extractKeywordArg(context, keyword, (RubyHash) opts);
    }

    /**
     * Semi-deprecated, kept for compatibility.
     * Compared to {@link #extractKeywordArg(ThreadContext, RubyHash, String)} does not validate options!
     * @param context
     * @param keyword
     * @param args
     * @return nil if key not within options (no way to distinguish a key: nil and missing key)
     */
    public static IRubyObject extractKeywordArg(ThreadContext context, String keyword, IRubyObject... args) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, args);

        if (opts == context.nil) return context.nil;

        return extractKeywordArg(context, keyword, (RubyHash) opts);
    }

}
