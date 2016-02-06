/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.object.HiddenKey;
import org.jruby.truffle.core.array.ArrayLayout;
import org.jruby.truffle.core.array.ArrayLayoutImpl;
import org.jruby.truffle.core.encoding.EncodingConverterLayout;
import org.jruby.truffle.core.encoding.EncodingConverterLayoutImpl;
import org.jruby.truffle.core.encoding.EncodingLayout;
import org.jruby.truffle.core.encoding.EncodingLayoutImpl;
import org.jruby.truffle.core.fiber.FiberLayout;
import org.jruby.truffle.core.fiber.FiberLayoutImpl;
import org.jruby.truffle.core.hash.HashLayout;
import org.jruby.truffle.core.hash.HashLayoutImpl;
import org.jruby.truffle.core.module.ModuleLayout;
import org.jruby.truffle.core.module.ModuleLayoutImpl;
import org.jruby.truffle.core.range.*;
import org.jruby.truffle.core.regexp.MatchDataLayout;
import org.jruby.truffle.core.regexp.MatchDataLayoutImpl;
import org.jruby.truffle.core.regexp.RegexpLayout;
import org.jruby.truffle.core.regexp.RegexpLayoutImpl;
import org.jruby.truffle.core.string.StringLayout;
import org.jruby.truffle.core.string.StringLayoutImpl;
import org.jruby.truffle.core.symbol.SymbolLayout;
import org.jruby.truffle.core.symbol.SymbolLayoutImpl;
import org.jruby.truffle.core.thread.ThreadLayout;
import org.jruby.truffle.core.thread.ThreadLayoutImpl;
import org.jruby.truffle.stdlib.BigDecimalLayout;
import org.jruby.truffle.stdlib.BigDecimalLayoutImpl;
import org.jruby.truffle.stdlib.psych.EmitterLayout;
import org.jruby.truffle.stdlib.psych.EmitterLayoutImpl;
import org.jruby.truffle.stdlib.psych.ParserLayout;
import org.jruby.truffle.stdlib.psych.ParserLayoutImpl;
import org.jruby.truffle.core.rubinius.*;

public abstract class Layouts {

    // Generated layouts

    public static final ArrayLayout ARRAY = ArrayLayoutImpl.INSTANCE;
    public static final BasicObjectLayout BASIC_OBJECT = BasicObjectLayoutImpl.INSTANCE;
    public static final BigDecimalLayout BIG_DECIMAL = BigDecimalLayoutImpl.INSTANCE;
    public static final BignumLayout BIGNUM = BignumLayoutImpl.INSTANCE;
    public static final BindingLayout BINDING = BindingLayoutImpl.INSTANCE;
    public static final ByteArrayLayout BYTE_ARRAY = ByteArrayLayoutImpl.INSTANCE;
    public static final ClassLayout CLASS = ClassLayoutImpl.INSTANCE;
    public static final DirLayout DIR = DirLayoutImpl.INSTANCE;
    public static final EncodingConverterLayout ENCODING_CONVERTER = EncodingConverterLayoutImpl.INSTANCE;
    public static final EncodingLayout ENCODING = EncodingLayoutImpl.INSTANCE;
    public static final ExceptionLayout EXCEPTION = ExceptionLayoutImpl.INSTANCE;
    public static final FiberLayout FIBER = FiberLayoutImpl.INSTANCE;
    public static final HashLayout HASH = HashLayoutImpl.INSTANCE;
    public static final IntegerFixnumRangeLayout INTEGER_FIXNUM_RANGE = IntegerFixnumRangeLayoutImpl.INSTANCE;
    public static final IOBufferLayout IO_BUFFER = IOBufferLayoutImpl.INSTANCE;
    public static final IOLayout IO = IOLayoutImpl.INSTANCE;
    public static final LongFixnumRangeLayout LONG_FIXNUM_RANGE = LongFixnumRangeLayoutImpl.INSTANCE;
    public static final MatchDataLayout MATCH_DATA = MatchDataLayoutImpl.INSTANCE;
    public static final MethodLayout METHOD = MethodLayoutImpl.INSTANCE;
    public static final ModuleLayout MODULE = ModuleLayoutImpl.INSTANCE;
    public static final MutexLayout MUTEX = MutexLayoutImpl.INSTANCE;
    public static final ObjectRangeLayout OBJECT_RANGE = ObjectRangeLayoutImpl.INSTANCE;
    public static final PointerLayout POINTER = PointerLayoutImpl.INSTANCE;
    public static final ProcLayout PROC = ProcLayoutImpl.INSTANCE;
    public static final QueueLayout QUEUE = QueueLayoutImpl.INSTANCE;
    public static final RegexpLayout REGEXP = RegexpLayoutImpl.INSTANCE;
    public static final SizedQueueLayout SIZED_QUEUE = SizedQueueLayoutImpl.INSTANCE;
    public static final StringLayout STRING = StringLayoutImpl.INSTANCE;
    public static final SymbolLayout SYMBOL = SymbolLayoutImpl.INSTANCE;
    public static final ThreadLayout THREAD = ThreadLayoutImpl.INSTANCE;
    public static final TimeLayout TIME = TimeLayoutImpl.INSTANCE;
    public static final UnboundMethodLayout UNBOUND_METHOD = UnboundMethodLayoutImpl.INSTANCE;
    public static final WeakRefLayout WEAK_REF_LAYOUT = WeakRefLayoutImpl.INSTANCE;
    public static final ParserLayout PSYCH_PARSER = ParserLayoutImpl.INSTANCE;
    public static final EmitterLayout PSYCH_EMITTER = EmitterLayoutImpl.INSTANCE;
    public static final RandomizerLayout RANDOMIZER = RandomizerLayoutImpl.INSTANCE;
    public static final AtomicReferenceLayout ATOMIC_REFERENCE = AtomicReferenceLayoutImpl.INSTANCE;
    public static final HandleLayout HANDLE = HandleLayoutImpl.INSTANCE;

    // Other standard identifiers

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");
}
