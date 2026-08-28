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
import org.jruby.api.Convert;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import java.util.HashSet;
import java.util.Set;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Error.argumentError;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;
import static org.jruby.util.TypeConverter.booleanExpected;

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
        if (value instanceof RubyArray ary) return ary;
        
        IRubyObject newValue = TypeConverter.convertToType(value, runtime.getArray(), "to_ary", false);

        return newValue.isNil() ?
                RubyArray.newArrayLight(runtime, value):
                Convert.castAsArray(runtime.getCurrentContext(), newValue);
    }
    
    public static int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray) node).getLength() : 0;
    }

    // MRI: rb_opts_exception_p
    public static boolean hasExceptionOption(ThreadContext context, IRubyObject options, boolean defaultValue) {
        IRubyObject opts = getOptionsArg(context.runtime, options, false);

        if (!opts.isNil()) {
            IRubyObject value = extractKeywordArg(context, "exception", (RubyHash) opts);

            if (value != null) return booleanExpected(context, value, "exception");
        }

        return defaultValue;
    }
    
    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject... args) {
        if (args.length >= 1) {
            return TypeConverter.checkHashType(runtime, args[args.length - 1]);
        }
        return runtime.getNil();
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject arg) {
        return getOptionsArg(runtime, arg, true);
    }

    public static IRubyObject getOptionsArg(ThreadContext context, IRubyObject arg) {
        return getOptionsArg(context.runtime, arg, true);
    }

    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject arg, boolean raise) {
        if (arg == null) return runtime.getNil();
        return TypeConverter.checkHashType(runtime, arg, raise);
    }

    private static final IRubyObject[] NULL_1 = new IRubyObject[] { null };
    private static final IRubyObject[] NULL_2 = new IRubyObject[] { null, null };
    private static final IRubyObject[] NULL_3 = new IRubyObject[] { null, null, null };

    public static final RubyHash.VisitorWithState<RubySymbol> SINGLE_KEY_CHECK_VISITOR = new RubyHash.VisitorWithState<RubySymbol>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubySymbol testKey) {
            if (!key.equals(testKey)) throw argumentError(context,"unknown keyword: " + key.inspect(context));
        }
    };
    public static final RubyHash.VisitorWithState<Set<RubySymbol>> MULTI_KEY_CHECK_VISITOR = new RubyHash.VisitorWithState<Set<RubySymbol>>() {
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Set<RubySymbol> validKeySet) {
            if (!validKeySet.contains(key)) throw argumentError(context, "unknown keyword: " + key.inspect(context));
        }
    };

    /**
     * Check that the given kwargs hash doesn't contain any keys other than those which are given as valid.
     * @param context The context to execute in
     * @param options A RubyHash of options to extract kwargs from
     * @param validKeys A list of valid kwargs keys.
     * @return an array of objects corresponding to the given keys.
     */
    public static IRubyObject[] extractKeywordArgs(ThreadContext context, final RubyHash options, String... validKeys) {
        if (options.isEmpty()) {
            return switch (validKeys.length) {
                case 1 -> NULL_1;
                case 2 -> NULL_2;
                case 3 -> NULL_3;
                default -> new IRubyObject[validKeys.length];
            };
        }

        IRubyObject[] ret = new IRubyObject[validKeys.length];

        Set<RubySymbol> validKeySet = new HashSet<>(ret.length);

        // Build the return values
        for (int i=0; i<validKeys.length; i++) {
            final String key = validKeys[i];
            RubySymbol keySym = asSymbol(context, key);
            IRubyObject val = options.fastARef(keySym);
            ret[i] = val; // null if key missing
            validKeySet.add(keySym);
        }

        // Check for any unknown keys
        options.visitAll(context, MULTI_KEY_CHECK_VISITOR, validKeySet);

        return ret;
    }

    // not used
    @Deprecated(since = "10.0.0.0")
    public static IRubyObject[] extractKeywordArgs(ThreadContext context, IRubyObject[] args, String... validKeys) {
        return extractKeywordArgs(context, ArgsUtil.getOptionsArg(context.runtime, args), validKeys);
    }

    public static IRubyObject[] extractKeywordArgs(ThreadContext context, IRubyObject maybeKwargs, String... validKeys) {
        IRubyObject options = ArgsUtil.getOptionsArg(context, maybeKwargs);

        return options instanceof RubyHash hash ? extractKeywordArgs(context, hash, validKeys) : null;
    }

    public static IRubyObject extractKeywordArg(ThreadContext context, IRubyObject maybeKwargs, String validKey) {
        IRubyObject options = ArgsUtil.getOptionsArg(context, maybeKwargs);

        return options instanceof RubyHash hash ? extractKeywordArg(context, hash, validKey) : null;
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

        RubySymbol testKey = asSymbol(context, validKey);
        IRubyObject ret = options.fastARef(testKey);

        if (ret == null || options.size() > 1) { // other (unknown) keys in options
            options.visitAll(context, SINGLE_KEY_CHECK_VISITOR, testKey);
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
        return opts.op_aref(context, asSymbol(context, keyword));
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
        IRubyObject opts = ArgsUtil.getOptionsArg(context, arg);

        return opts == context.nil ? context.nil : extractKeywordArg(context, keyword, (RubyHash) opts);
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

        return opts == context.nil ? context.nil : extractKeywordArg(context, keyword, (RubyHash) opts);
    }

    // FIXME: Remove this once invokers know about keyword arguments.
    public static RubyHash extractKeywords(IRubyObject possiblyKeywordArg) {
        return possiblyKeywordArg instanceof RubyHash hash ? hash : null;
    }

    public static IRubyObject getFreezeOpt(ThreadContext context, IRubyObject maybeOpts) {
        IRubyObject kwfreeze = null;
        IRubyObject opts = getOptionsArg(context, maybeOpts);

        if (!opts.isNil()) {
            IRubyObject freeze = extractKeywordArg(context, (RubyHash) opts, "freeze");
            if (freeze != null) {
                if (!freeze.isNil() && freeze != context.tru && freeze != context.fals) {
                    throw argumentError(context, str(context.runtime, "unexpected value for freeze: ", types(context.runtime, freeze.getType())));
                }
                kwfreeze = freeze;
            }
        }
        return kwfreeze;
    }
}
