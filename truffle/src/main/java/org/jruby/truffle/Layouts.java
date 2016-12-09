/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.object.HiddenKey;
import org.jruby.truffle.core.HandleLayout;
import org.jruby.truffle.core.HandleLayoutImpl;
import org.jruby.truffle.core.array.ArrayLayout;
import org.jruby.truffle.core.array.ArrayLayoutImpl;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.truffle.core.basicobject.BasicObjectLayoutImpl;
import org.jruby.truffle.core.binding.BindingLayout;
import org.jruby.truffle.core.binding.BindingLayoutImpl;
import org.jruby.truffle.core.dir.DirLayout;
import org.jruby.truffle.core.dir.DirLayoutImpl;
import org.jruby.truffle.core.encoding.EncodingConverterLayout;
import org.jruby.truffle.core.encoding.EncodingConverterLayoutImpl;
import org.jruby.truffle.core.encoding.EncodingLayout;
import org.jruby.truffle.core.encoding.EncodingLayoutImpl;
import org.jruby.truffle.core.exception.ExceptionLayout;
import org.jruby.truffle.core.exception.ExceptionLayoutImpl;
import org.jruby.truffle.core.exception.NameErrorLayout;
import org.jruby.truffle.core.exception.NameErrorLayoutImpl;
import org.jruby.truffle.core.exception.NoMethodErrorLayout;
import org.jruby.truffle.core.exception.NoMethodErrorLayoutImpl;
import org.jruby.truffle.core.exception.SystemCallErrorLayout;
import org.jruby.truffle.core.exception.SystemCallErrorLayoutImpl;
import org.jruby.truffle.core.fiber.FiberLayout;
import org.jruby.truffle.core.fiber.FiberLayoutImpl;
import org.jruby.truffle.core.hash.HashLayout;
import org.jruby.truffle.core.hash.HashLayoutImpl;
import org.jruby.truffle.core.klass.ClassLayout;
import org.jruby.truffle.core.klass.ClassLayoutImpl;
import org.jruby.truffle.core.method.MethodLayout;
import org.jruby.truffle.core.method.MethodLayoutImpl;
import org.jruby.truffle.core.method.UnboundMethodLayout;
import org.jruby.truffle.core.method.UnboundMethodLayoutImpl;
import org.jruby.truffle.core.module.ModuleLayout;
import org.jruby.truffle.core.module.ModuleLayoutImpl;
import org.jruby.truffle.core.mutex.MutexLayout;
import org.jruby.truffle.core.mutex.MutexLayoutImpl;
import org.jruby.truffle.core.numeric.BignumLayout;
import org.jruby.truffle.core.numeric.BignumLayoutImpl;
import org.jruby.truffle.core.proc.ProcLayout;
import org.jruby.truffle.core.proc.ProcLayoutImpl;
import org.jruby.truffle.core.queue.QueueLayout;
import org.jruby.truffle.core.queue.QueueLayoutImpl;
import org.jruby.truffle.core.queue.SizedQueueLayout;
import org.jruby.truffle.core.queue.SizedQueueLayoutImpl;
import org.jruby.truffle.core.range.IntRangeLayout;
import org.jruby.truffle.core.range.IntRangeLayoutImpl;
import org.jruby.truffle.core.range.LongRangeLayout;
import org.jruby.truffle.core.range.LongRangeLayoutImpl;
import org.jruby.truffle.core.range.ObjectRangeLayout;
import org.jruby.truffle.core.range.ObjectRangeLayoutImpl;
import org.jruby.truffle.core.regexp.MatchDataLayout;
import org.jruby.truffle.core.regexp.MatchDataLayoutImpl;
import org.jruby.truffle.core.regexp.RegexpLayout;
import org.jruby.truffle.core.regexp.RegexpLayoutImpl;
import org.jruby.truffle.core.rubinius.AtomicReferenceLayout;
import org.jruby.truffle.core.rubinius.AtomicReferenceLayoutImpl;
import org.jruby.truffle.core.rubinius.ByteArrayLayout;
import org.jruby.truffle.core.rubinius.ByteArrayLayoutImpl;
import org.jruby.truffle.core.rubinius.IOBufferLayout;
import org.jruby.truffle.core.rubinius.IOBufferLayoutImpl;
import org.jruby.truffle.core.rubinius.IOLayout;
import org.jruby.truffle.core.rubinius.IOLayoutImpl;
import org.jruby.truffle.core.rubinius.RandomizerLayout;
import org.jruby.truffle.core.rubinius.RandomizerLayoutImpl;
import org.jruby.truffle.core.rubinius.StatLayout;
import org.jruby.truffle.core.rubinius.StatLayoutImpl;
import org.jruby.truffle.core.rubinius.WeakRefLayout;
import org.jruby.truffle.core.rubinius.WeakRefLayoutImpl;
import org.jruby.truffle.core.string.StringLayout;
import org.jruby.truffle.core.string.StringLayoutImpl;
import org.jruby.truffle.core.symbol.SymbolLayout;
import org.jruby.truffle.core.symbol.SymbolLayoutImpl;
import org.jruby.truffle.core.thread.ThreadBacktraceLocationLayout;
import org.jruby.truffle.core.thread.ThreadBacktraceLocationLayoutImpl;
import org.jruby.truffle.core.thread.ThreadLayout;
import org.jruby.truffle.core.thread.ThreadLayoutImpl;
import org.jruby.truffle.core.time.TimeLayout;
import org.jruby.truffle.core.time.TimeLayoutImpl;
import org.jruby.truffle.core.tracepoint.TracePointLayout;
import org.jruby.truffle.core.tracepoint.TracePointLayoutImpl;
import org.jruby.truffle.extra.ffi.PointerLayout;
import org.jruby.truffle.extra.ffi.PointerLayoutImpl;
import org.jruby.truffle.stdlib.bigdecimal.BigDecimalLayout;
import org.jruby.truffle.stdlib.bigdecimal.BigDecimalLayoutImpl;
import org.jruby.truffle.stdlib.digest.DigestLayout;
import org.jruby.truffle.stdlib.digest.DigestLayoutImpl;
import org.jruby.truffle.stdlib.psych.EmitterLayout;
import org.jruby.truffle.stdlib.psych.EmitterLayoutImpl;

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
    public static final IntRangeLayout INT_RANGE = IntRangeLayoutImpl.INSTANCE;
    public static final IOBufferLayout IO_BUFFER = IOBufferLayoutImpl.INSTANCE;
    public static final IOLayout IO = IOLayoutImpl.INSTANCE;
    public static final LongRangeLayout LONG_RANGE = LongRangeLayoutImpl.INSTANCE;
    public static final MatchDataLayout MATCH_DATA = MatchDataLayoutImpl.INSTANCE;
    public static final MethodLayout METHOD = MethodLayoutImpl.INSTANCE;
    public static final ModuleLayout MODULE = ModuleLayoutImpl.INSTANCE;
    public static final MutexLayout MUTEX = MutexLayoutImpl.INSTANCE;
    public static final NameErrorLayout NAME_ERROR = NameErrorLayoutImpl.INSTANCE;
    public static final NoMethodErrorLayout NO_METHOD_ERROR = NoMethodErrorLayoutImpl.INSTANCE;
    public static final ObjectRangeLayout OBJECT_RANGE = ObjectRangeLayoutImpl.INSTANCE;
    public static final PointerLayout POINTER = PointerLayoutImpl.INSTANCE;
    public static final ProcLayout PROC = ProcLayoutImpl.INSTANCE;
    public static final QueueLayout QUEUE = QueueLayoutImpl.INSTANCE;
    public static final RegexpLayout REGEXP = RegexpLayoutImpl.INSTANCE;
    public static final SizedQueueLayout SIZED_QUEUE = SizedQueueLayoutImpl.INSTANCE;
    public static final StringLayout STRING = StringLayoutImpl.INSTANCE;
    public static final SymbolLayout SYMBOL = SymbolLayoutImpl.INSTANCE;
    public static final ThreadLayout THREAD = ThreadLayoutImpl.INSTANCE;
    public static final ThreadBacktraceLocationLayout THREAD_BACKTRACE_LOCATION = ThreadBacktraceLocationLayoutImpl.INSTANCE;
    public static final TimeLayout TIME = TimeLayoutImpl.INSTANCE;
    public static final UnboundMethodLayout UNBOUND_METHOD = UnboundMethodLayoutImpl.INSTANCE;
    public static final WeakRefLayout WEAK_REF_LAYOUT = WeakRefLayoutImpl.INSTANCE;
    public static final EmitterLayout PSYCH_EMITTER = EmitterLayoutImpl.INSTANCE;
    public static final RandomizerLayout RANDOMIZER = RandomizerLayoutImpl.INSTANCE;
    public static final AtomicReferenceLayout ATOMIC_REFERENCE = AtomicReferenceLayoutImpl.INSTANCE;
    public static final HandleLayout HANDLE = HandleLayoutImpl.INSTANCE;
    public static final TracePointLayout TRACE_POINT = TracePointLayoutImpl.INSTANCE;
    public static final DigestLayout DIGEST = DigestLayoutImpl.INSTANCE;
    public static final StatLayout STAT = StatLayoutImpl.INSTANCE;
    public static final SystemCallErrorLayout SYSTEM_CALL_ERROR = SystemCallErrorLayoutImpl.INSTANCE;

    // Other standard identifiers

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");
}
