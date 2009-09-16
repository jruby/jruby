/*
 * **** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.common;

import org.jruby.lexer.yacc.ISourcePosition;

// FIXME: Document difference between warn and warning (or rename one better)
/**
 */
public interface IRubyWarnings {
    public enum ID {
        AMBIGUOUS_ARGUMENT("AMBIGUOUS_ARGUMENT"),
        ACCESSOR_NOT_INITIALIZED("ACCESSOR_NOT_INITIALIZED"),
        ARGUMENT_AS_PREFIX("ARGUMENT_AS_PREFIX"),
        ARGUMENT_EXTRA_SPACE("ARGUMENT_EXTRA_SPACE"),
        ASSIGNMENT_IN_CONDITIONAL("ASSIGNMENT_IN_CONDITIONAL"),
        BIGNUM_FROM_FLOAT_RANGE("BIGNUM_FROM_FLOAT_RANGE"),
        BLOCK_BEATS_DEFAULT_VALUE("BLOCK_BEATS_DEFAULT_VALUE"),
        BLOCK_NOT_ACCEPTED("BLOCK_NOT_ACCEPTED"),
        BLOCK_UNUSED("BLOCK_UNUSED"),
        CONSTANT_ALREADY_INITIALIZED("CONSTANT_ALREADY_INITIALIZED"),
        CONSTANT_BAD_REFERENCE("CONSTANT_BAD_REFERENCE"),
        CVAR_FROM_TOPLEVEL_SINGLETON_METHOD("CVAR_FROM_TOPLEVEL_SINGLETON_METHOD"),
        DECLARING_SCLASS_VARIABLE("DECLARING_SCLASS_VARIABLE"),
        DEPRECATED_METHOD("DEPRECATED_METHOD"),
        DUMMY_VALUE_USED("DUMMY_VALUE_USED"),
        END_IN_METHOD("END_IN_METHOD"),
        ELSE_WITHOUT_RESCUE("ELSE_WITHOUT_RESCUE"),
        EMPTY_IMPLEMENTATION("EMPTY_IMPLEMENTATION"),
        ENV_VARS_FROM_CLI_METHOD("ENV_VARS_FROM_CLI_METHOD"),
        FIXNUMS_NOT_SYMBOLS("FIXNUMS_NOT_SYMBOLS"),
        FLOAT_OUT_OF_RANGE("FLOAT_OUT_OF_RANGE"),
        GLOBAL_NOT_INITIALIZED("GLOBAL_NOT_INITIALIZED"),
        GROUPED_EXPRESSION("GROUPED_EXPRESSION"),
        INEFFECTIVE_GLOBAL("INNEFFECTIVE_GLOBAL"),
        INVALID_CHAR_SEQUENCE("INVALID_CHAR_SEQUENCE"),
        IVAR_NOT_INITIALIZED("IVAR_NOT_INITIALIZED"),
        MAY_BE_TOO_BIG("MAY_BE_TOO_BIG"),
        MISCELLANEOUS("MISCELLANEOUS"),
        MULTIPLE_VALUES_FOR_BLOCK("MULTIPLE_VALUES_FOR_BLOCK"),
        NEGATIVE_NUMBER_FOR_U("NEGATIVE_NUMBER_FOR_U"),
        NO_SUPER_CLASS("NO_SUPER_CLASS"),
        NOT_IMPLEMENTED("NOT_IMPLEMENTED"),
        OBSOLETE_ARGUMENT("OBSOLETE_ARGUMENT"),
        PARENTHISE_ARGUMENTS("PARENTHISE_ARGUMENTS"),
        PROXY_EXTENDED_LATE("PROXY_EXTENDED_LATE"),
        STATEMENT_NOT_REACHED("STATEMENT_NOT_REACHED"), 
        LITERAL_IN_CONDITIONAL_RANGE("LITERAL_IN_CONDITIONAL_RANGE"),
        REDEFINING_DANGEROUS("REDEFINING_DANGEROUS"),
        REGEXP_IGNORED_FLAGS("REGEXP_IGNORED_FLAGS"),
        REGEXP_LITERAL_IN_CONDITION("REGEXP_LITERAL_IN_CONDITION"),
        REGEXP_MATCH_AGAINST_STRING("REGEXP_MATCH_AGAINST_STRING"),
        SAFE_NOT_SUPPORTED("SAFE_NOT_SUPPORTED"),
        STRUCT_CONSTANT_REDEFINED("STRUCT_CONSTANT_REDEFINED"),
        SYMBOL_AS_INTEGER("SYMBOL_AS_INTEGER"),
        SYSSEEK_BUFFERED_IO("SYSSEEK_BUFFERED_IO"),
        SYSWRITE_BUFFERED_IO("SYSWRITE_BUFFERED_IO"),
        SWALLOWED_IO_EXCEPTION("SWALLOWED_IO_EXCEPTION"),
        TOO_MANY_ARGUMENTS("TOO_MANY_ARGUMENTS"),
        UNDEFINING_BAD("UNDEFINING_BAD"),
        USELESS_EXPRESSION("USELESS_EXPRESSION"),
        VOID_VALUE_EXPRESSION("VOID_VALUE_EXPRESSION");
        
        private final String id;
        
        ID(String id) {
            this.id = id;
        }
        
        public String getID() {
            return id;
        }
    }

    public abstract org.jruby.Ruby getRuntime();
    public abstract void warn(ID id, ISourcePosition position, String message, Object... data);
    public abstract void warn(ID id, String fileName, int lineNumber, String message, Object... data);
    public abstract boolean isVerbose();
    public abstract void warn(ID id, String message, Object... data);
    public abstract void warning(ID id, String message, Object... data);
    public abstract void warning(ID id, ISourcePosition position, String message, Object... data);
    public abstract void warning(ID id, String fileName, int lineNumber, String message, Object... data);
}
