/*
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.common;

import org.jruby.Ruby;
import org.jruby.lexer.yacc.ISourcePosition;

// FIXME: Document difference between warn and warning (or rename one better)
/**
 */
public interface IRubyWarnings {
    public enum ID {
        AMBIGUOUS_ARGUMENT,
        ACCESSOR_NOT_INITIALIZED,
        ACCESSOR_MODULE_FUNCTION,
        ARGUMENT_AS_PREFIX,
        ARGUMENT_EXTRA_SPACE,
        ASSIGNMENT_IN_CONDITIONAL,
        BIGNUM_FROM_FLOAT_RANGE,
        BLOCK_BEATS_DEFAULT_VALUE,
        BLOCK_NOT_ACCEPTED,
        BLOCK_UNUSED,
        CONSTANT_ALREADY_INITIALIZED,
        CONSTANT_BAD_REFERENCE,
        CVAR_FROM_TOPLEVEL_SINGLETON_METHOD,
        DECLARING_SCLASS_VARIABLE,
        DEPRECATED_METHOD,
        DUMMY_VALUE_USED,
        END_IN_METHOD,
        ELSE_WITHOUT_RESCUE,
        EMPTY_IMPLEMENTATION,
        ENV_VARS_FROM_CLI_METHOD,
        FIXNUMS_NOT_SYMBOLS,
        FLOAT_OUT_OF_RANGE,
        GLOBAL_NOT_INITIALIZED,
        GROUPED_EXPRESSION,
        INEFFECTIVE_GLOBAL,
        INVALID_CHAR_SEQUENCE,
        IVAR_NOT_INITIALIZED,
        MAY_BE_TOO_BIG,
        MISCELLANEOUS,
        MULTIPLE_VALUES_FOR_BLOCK,
        NEGATIVE_NUMBER_FOR_U,
        NO_SUPER_CLASS,
        NOT_IMPLEMENTED,
        OBSOLETE_ARGUMENT,
        PARENTHISE_ARGUMENTS,
        PRIVATE_ACCESSOR,
        PROXY_EXTENDED_LATE,
        STATEMENT_NOT_REACHED, 
        LITERAL_IN_CONDITIONAL_RANGE,
        REDEFINING_DANGEROUS,
        REGEXP_IGNORED_FLAGS,
        REGEXP_LITERAL_IN_CONDITION,
        REGEXP_MATCH_AGAINST_STRING,
        SAFE_NOT_SUPPORTED,
        STRUCT_CONSTANT_REDEFINED,
        SYMBOL_AS_INTEGER,
        SYSSEEK_BUFFERED_IO,
        SYSWRITE_BUFFERED_IO,
        SWALLOWED_IO_EXCEPTION,
        TOO_MANY_ARGUMENTS,
        UNDEFINING_BAD,
        USELESS_EXPRESSION,
        VOID_VALUE_EXPRESSION,
        NAMED_CAPTURE_CONFLICT,
        NON_PERSISTENT_JAVA_PROXY,
        LISTEN_SERVER_SOCKET,
        PROFILE_MAX_METHODS_EXCEEDED,
        UNSUPPORTED_SUBPROCESS_OPTION,
        GC_STRESS_UNIMPLEMENTED,
        GC_ENABLE_UNIMPLEMENTED,
        GC_DISABLE_UNIMPLEMENTED,
        TRUFFLE,
        RATIONAL_OUT_OF_RANGE,; // TODO(CS): divide up the Truffle warnings
        
        public String getID() {
            return name();
        }
    }

    public abstract Ruby getRuntime();
    public abstract boolean isVerbose();
    
    public abstract void warn(ID id, ISourcePosition position, String message);
    public abstract void warn(ID id, String fileName, int lineNumber, String message);
    public abstract void warn(ID id, String fileName, String message);
    public abstract void warn(ID id, String message);
    public abstract void warning(ID id, String message);
    public abstract void warning(ID id, ISourcePosition position, String message);
    public abstract void warning(ID id, String fileName, int lineNumber, String message);
}
