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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ast.executable;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class RubiniusInstructions {
    public final static int NOOP=0;
    public final static int PUSH_NIL=1;
    public final static int PUSH_TRUE=2;
    public final static int PUSH_FALSE=3;
    public final static int ALLOCATE=4;
    public final static int SET_CLASS=5;
    public final static int STORE_FIELD=6;
    public final static int PUSH_INT=7;
    public final static int FETCH_FIELD=8;
    public final static int SEND_PRIMITIVE=9;
    public final static int PUSH_CONTEXT=10;
    public final static int PUSH_LITERAL=11;
    public final static int PUSH_SELF=12;
    public final static int GOTO=13;
    public final static int GOTO_IF_FALSE=14;
    public final static int GOTO_IF_TRUE=15;
    public final static int SWAP_STACK=16;
    public final static int SET_LOCAL=17;
    public final static int PUSH_LOCAL=18;
    public final static int PUSH_EXCEPTION=19;
    public final static int MAKE_ARRAY=20;
    public final static int SET_IVAR=21;
    public final static int PUSH_IVAR=22;
    public final static int GOTO_IF_DEFINED=23;
    public final static int PUSH_CONST=24;
    public final static int SET_CONST=25;
    public final static int SET_CONST_AT=26;
    public final static int FIND_CONST=27;
    public final static int ATTACH_METHOD=28;
    public final static int ADD_METHOD=29;
    public final static int OPEN_CLASS=30;
    public final static int OPEN_CLASS_UNDER=31;
    public final static int OPEN_MODULE=32;
    public final static int OPEN_MODULE_UNDER=33;
    public final static int UNSHIFT_TUPLE=34;
    public final static int CAST_TUPLE=35;
    public final static int MAKE_REST=36;
    public final static int DUP_TOP=37;
    public final static int POP=38;
    public final static int RET=39;
    public final static int SEND_METHOD=40;
    public final static int SEND_STACK=41;
    public final static int SEND_STACK_WITH_BLOCK=42;
    public final static int PUSH_BLOCK=43;
    public final static int CLEAR_EXCEPTION=44;
    public final static int SOFT_RETURN=45;
    public final static int CALLER_RETURN=46;
    public final static int PUSH_ARRAY=47;
    public final static int CAST_ARRAY=48;
    public final static int MAKE_HASH=49;
    public final static int RAISE_EXC=50;
    public final static int SET_ENCLOSER=51;
    public final static int PUSH_ENCLOSER=52;
    public final static int ACTIVATE_METHOD=53;
    public final static int PUSH_CPATH_TOP=54;
    public final static int CHECK_ARGCOUNT=55;
    public final static int PASSED_ARG=56;
    public final static int STRING_APPEND=57;
    public final static int STRING_DUP=58;
    public final static int SET_ARGS=59;
    public final static int GET_ARGS=60;
    public final static int SEND_WITH_ARG_REGISTER=61;
    public final static int CAST_ARRAY_FOR_ARGS=62;
    public final static int SEND_SUPER_STACK_WITH_BLOCK=63;
    public final static int PUSH_MY_FIELD=64;
    public final static int STORE_MY_FIELD=65;
    public final static int OPEN_METACLASS=66;
    public final static int SET_CACHE_INDEX=67;
    public final static int BLOCK_BREAK=68;
    public final static int SEND_SUPER_WITH_ARG_REGISTER=69;
    public final static int META_PUSH_NEG_1=70;
    public final static int META_PUSH_0=71;
    public final static int META_PUSH_1=72;
    public final static int META_PUSH_2=73;
    public final static int META_SEND_STACK_1=74;
    public final static int META_SEND_STACK_2=75;
    public final static int META_SEND_STACK_3=76;
    public final static int META_SEND_STACK_4=77;
    public final static int META_SEND_OP_PLUS=78;
    public final static int META_SEND_OP_MINUS=79;
    public final static int META_SEND_OP_EQUAL=80;
    public final static int META_SEND_OP_LT=81;
    public final static int META_SEND_OP_GT=82;
    public final static int META_SEND_OP_TEQUAL=83;
    public final static int META_SEND_OP_NEQUAL=84;
    public final static int PUSH_LOCAL_DEPTH=85;
    public final static int SET_LOCAL_DEPTH=86;
    public final static int CREATE_BLOCK=87;
    public final static int SEND_OFF_STACK=88;
    public final static int LOCATE_METHOD=89;
    public final static int KIND_OF=90;
    public final static int INSTANCE_OF=91;
    public final static int SET_CALL_FLAGS=92;
    public final static int YIELD_DEBUGGER=93;

    public final static boolean[] ONE_INT = new boolean[94];
    public final static boolean[] TWO_INT = new boolean[94];

    static {
        ONE_INT[PUSH_INT] = true;
        ONE_INT[PUSH_LITERAL] = true;
        ONE_INT[GOTO] = true;
        ONE_INT[GOTO_IF_FALSE] = true;
        ONE_INT[GOTO_IF_TRUE] = true;
        ONE_INT[GOTO_IF_DEFINED] = true;
        ONE_INT[SET_LOCAL] = true;
        ONE_INT[PUSH_LOCAL] = true;
        ONE_INT[MAKE_ARRAY] = true;
        ONE_INT[SET_IVAR] = true;
        ONE_INT[PUSH_IVAR] = true;
        ONE_INT[PUSH_CONST] = true;
        ONE_INT[SET_CONST] = true;
        ONE_INT[SET_CONST_AT] = true;
        ONE_INT[FIND_CONST] = true;
        ONE_INT[ATTACH_METHOD] = true;
        ONE_INT[ADD_METHOD] = true;
        ONE_INT[OPEN_CLASS] = true;
        ONE_INT[OPEN_CLASS_UNDER] = true;
        ONE_INT[OPEN_MODULE] = true;
        ONE_INT[OPEN_MODULE_UNDER] = true;
        ONE_INT[SEND_METHOD] = true;
        ONE_INT[MAKE_HASH] = true;
        ONE_INT[MAKE_REST] = true;
        ONE_INT[ACTIVATE_METHOD] = true;
        ONE_INT[PASSED_ARG] = true;
        ONE_INT[SEND_WITH_ARG_REGISTER] = true;
        ONE_INT[CAST_ARRAY_FOR_ARGS] = true;
        ONE_INT[PUSH_MY_FIELD] = true;
        ONE_INT[STORE_MY_FIELD] = true;
        ONE_INT[SET_CACHE_INDEX] = true;
        ONE_INT[SEND_SUPER_WITH_ARG_REGISTER] = true;
        ONE_INT[META_SEND_STACK_1] = true;
        ONE_INT[META_SEND_STACK_2] = true;
        ONE_INT[META_SEND_STACK_3] = true;
        ONE_INT[META_SEND_STACK_4] = true;
        ONE_INT[CREATE_BLOCK] = true;
        ONE_INT[SET_CALL_FLAGS] = true;

        TWO_INT[SEND_STACK] = true;
        TWO_INT[SEND_STACK_WITH_BLOCK] = true;
        TWO_INT[CHECK_ARGCOUNT] = true;
        TWO_INT[SEND_SUPER_STACK_WITH_BLOCK] = true;
        TWO_INT[SEND_PRIMITIVE] = true;
        TWO_INT[PUSH_LOCAL_DEPTH] = true;
        TWO_INT[SET_LOCAL_DEPTH] = true;
    }

    public static final String[] NAMES = new String[] {
        "noop",
        "push_nil",
        "push_true",
        "push_false",
        "allocate",
        "set_class",
        "store_field",
        "push_int",
        "fetch_field",
        "send_primitive",
        "push_context",
        "push_literal",
        "push_self",
        "goto",
        "goto_if_false",
        "goto_if_true",
        "swap_stack",
        "set_local",
        "push_local",
        "push_exception",
        "make_array",
        "set_ivar",
        "push_ivar",
        "goto_if_defined",
        "push_const",
        "set_const",
        "set_const_at",
        "find_const",
        "attach_method",
        "add_method",
        "open_class",
        "open_class_under",
        "open_module",
        "open_module_under",
        "unshift_tuple",
        "cast_tuple",
        "make_rest",
        "dup_top",
        "pop",
        "ret",
        "send_method",
        "send_stack",
        "send_stack_with_block",
        "push_block",
        "clear_exception",
        "soft_return",
        "caller_return",
        "push_array",
        "cast_array",
        "make_hash",
        "raise_exc",
        "set_encloser",
        "push_encloser",
        "activate_method",
        "push_cpath_top",
        "check_argcount",
        "passed_arg",
        "string_append",
        "string_dup",
        "set_args",
        "get_args",
        "send_with_arg_register",
        "cast_array_for_args",
        "send_super_stack_with_block",
        "push_my_field",
        "store_my_field",
        "open_metaclass",
        "set_cache_index",
        "block_break",
        "send_super_with_arg_register",
        "meta_push_neg_1",
        "meta_push_0",
        "meta_push_1",
        "meta_push_2",
        "meta_send_stack_1",
        "meta_send_stack_2",
        "meta_send_stack_3",
        "meta_send_stack_4",
        "meta_send_op_plus",
        "meta_send_op_minus",
        "meta_send_op_equal",
        "meta_send_op_lt",
        "meta_send_op_gt",
        "meta_send_op_tequal",
        "meta_send_op_nequal",
        "push_local_depth",
        "set_local_depth",
        "create_block",
        "send_off_stack",
        "locate_method",
        "kind_of",
        "instance_of",
        "set_call_flags",
        "yield_debugger"
    };
}// RubiniusInstructions
