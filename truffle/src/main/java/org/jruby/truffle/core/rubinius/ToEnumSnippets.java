/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.core.rubinius;

public abstract class ToEnumSnippets {
    public static final String ARRAY_DELETE_IF_TO_ENUM = "to_enum(:delete_if) { size }";
    public static final String ARRAY_EACH_TO_ENUM = "to_enum(:each) { size }";
    public static final String ARRAY_EACH_WITH_INDEX_TO_ENUM = "to_enum(:each_with_index) { size }";
    public static final String ARRAY_MAP_TO_ENUM = "to_enum(:map) { size }";
    public static final String ARRAY_MAP_BANG_TO_ENUM = "to_enum(:map!) { size }";
    public static final String ARRAY_REJECT_TO_ENUM = "to_enum(:reject) { size }";
    public static final String ARRAY_REJECT_BANG_TO_ENUM = "to_enum(:reject!) { size }";
    public static final String ARRAY_SELECT_TO_ENUM = "to_enum(:select) { size }";

    public static final String HASH_EACH_TO_ENUM = "to_enum(:each) { size }";

    public static final String RANGE_EACH_TO_ENUM = "to_enum { size }";

    public static final String STRING_EACH_BYTE_TO_ENUM = "to_enum(:each_byte) { bytesize }";
    public static final String STRING_EACH_CHAR_TO_ENUM = "to_enum(:each_char) { size }";

}
