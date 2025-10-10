package org.jruby;

import jnr.constants.platform.Errno;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.OpenFile;

import java.nio.ByteBuffer;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Error.argumentError;

public class FiberScheduler {
    // MRI: rb_fiber_scheduler_kernel_sleep
    public static IRubyObject kernelSleep(ThreadContext context, IRubyObject scheduler, IRubyObject timeout) {
        return scheduler.callMethod(context, "kernel_sleep", timeout);
    }

    // MRI: rb_fiber_scheduler_kernel_sleepv
    public static IRubyObject kernelSleep(ThreadContext context, IRubyObject scheduler, IRubyObject[] args) {
        return scheduler.callMethod(context, "kernel_sleep", args);
    }

    // MRI: rb_fiber_scheduler_process_wait
    public static IRubyObject processWait(ThreadContext context, IRubyObject scheduler, long pid, int flags) {
        return Helpers.invokeChecked(context, scheduler, "process_wait", asFixnum(context, pid), asFixnum(context, flags));
    }

    // MRI: rb_fiber_scheduler_block
    public static IRubyObject block(ThreadContext context, IRubyObject scheduler, IRubyObject blocker, IRubyObject timeout) {
        return Helpers.invoke(context, scheduler, "block", blocker, timeout);
    }

    // MRI: rb_fiber_scheduler_unblock
    public static IRubyObject unblock(ThreadContext context, IRubyObject scheduler, IRubyObject blocker, IRubyObject fiber) {
        return Helpers.invoke(context, scheduler, "unblock", blocker, fiber);
    }

    // MRI: rb_fiber_scheduler_io_wait
    public static IRubyObject ioWait(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject events, IRubyObject timeout) {
        return Helpers.invoke(context, scheduler, "io_wait", io, events, timeout);
    }

    // MRI: rb_fiber_scheduler_io_wait_readable
    public static IRubyObject ioWaitReadable(ThreadContext context, IRubyObject scheduler, IRubyObject io) {
        return ioWait(context, scheduler, io, asFixnum(context, OpenFile.READABLE), context.nil);
    }

    // MRI: rb_fiber_scheduler_io_wait_writable
    public static IRubyObject ioWaitWritable(ThreadContext context, IRubyObject scheduler, IRubyObject io) {
        return ioWait(context, scheduler, io, asFixnum(context, OpenFile.WRITABLE), context.nil);
    }

    // MRI: rb_fiber_scheduler_io_select
    public static IRubyObject ioSelect(ThreadContext context, IRubyObject scheduler, IRubyObject readables, IRubyObject writables, IRubyObject exceptables, IRubyObject timeout) {
        return ioSelectv(context, scheduler, readables, writables, exceptables, timeout);
    }

    // MRI: rb_fiber_scheduler_io_selectv
    public static IRubyObject ioSelectv(ThreadContext context, IRubyObject scheduler, IRubyObject... args) {
        return Helpers.invokeChecked(context, scheduler, "io_select", args);
    }

    // MRI: rb_fiber_scheduler_io_read
    public static IRubyObject ioRead(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, int length, int offset) {
        Ruby runtime = context.runtime;
        return Helpers.invokeChecked(context, scheduler, "io_read", io, buffer, asFixnum(context, length), asFixnum(context, offset));
    }

    public static IRubyObject ioRead(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, RubyInteger length, RubyInteger offset) {
        return Helpers.invokeChecked(context, scheduler, "io_read", io, buffer, length, offset);
    }

    // MRI: rb_fiber_scheduler_io_pread
    public static IRubyObject ioPRead(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, int from, int length, int offset) {
        return Helpers.invokeChecked(context, scheduler, "io_pread", io, buffer, asFixnum(context, from), asFixnum(context, length), asFixnum(context, offset));
    }

    public static IRubyObject ioPRead(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, RubyInteger from, RubyInteger length, RubyInteger offset) {
        return Helpers.invokeChecked(context, scheduler, "io_pread", io, buffer, from, length, offset);
    }

    // MRI: rb_fiber_scheduler_io_write
    public static IRubyObject ioWrite(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, int length, int offset) {
        return Helpers.invokeChecked(context, scheduler, "io_write", io, buffer, asFixnum(context, length), asFixnum(context, offset));
    }

    public static IRubyObject ioWrite(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, RubyInteger length, RubyInteger offset) {
        return Helpers.invokeChecked(context, scheduler, "io_write", io, buffer, length, offset);
    }

    // MRI: rb_fiber_scheduler_io_pwrite
    public static IRubyObject ioPWrite(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, int from, int length, int offset) {
        return Helpers.invokeChecked(context, scheduler, "io_pwrite", io, buffer, asFixnum(context, from), asFixnum(context, length), asFixnum(context, offset));
    }

    public static IRubyObject ioPWrite(ThreadContext context, IRubyObject scheduler, IRubyObject io, IRubyObject buffer, RubyInteger from, RubyInteger length, RubyInteger offset) {
        return Helpers.invokeChecked(context, scheduler, "io_pwrite", io, buffer, from, length, offset);
    }

    // MRI: rb_fiber_scheduler_io_read_memory
    public static IRubyObject ioReadMemory(ThreadContext context, IRubyObject scheduler, IRubyObject io, ByteBuffer base, int size, int length) {
        RubyIOBuffer buffer = RubyIOBuffer.newBuffer(context, base, size, RubyIOBuffer.LOCKED);

        IRubyObject result = ioRead(context, scheduler, io, buffer, length, 0);

        buffer.unlock(context);
        buffer.free(context);

        return result;
    }

    // MRI: rb_fiber_scheduler_io_pread_memory
    public static IRubyObject ioPReadMemory(ThreadContext context, IRubyObject scheduler, IRubyObject io, ByteBuffer base, int from, int size, int length) {
        RubyIOBuffer buffer = RubyIOBuffer.newBuffer(context, base, size, RubyIOBuffer.LOCKED);

        IRubyObject result = ioPRead(context, scheduler, io, buffer, from, length, 0);

        buffer.unlock(context);
        buffer.free(context);

        return result;
    }

    // MRI: rb_fiber_scheduler_io_write_memory
    public static IRubyObject ioWriteMemory(ThreadContext context, IRubyObject scheduler, IRubyObject io, ByteBuffer base, int size, int length) {
        RubyIOBuffer buffer = RubyIOBuffer.newBuffer(context, base, size, RubyIOBuffer.LOCKED | RubyIOBuffer.READONLY);

        IRubyObject result = ioWrite(context, scheduler, io, buffer, length, 0);

        buffer.unlock(context);
        buffer.free(context);

        return result;
    }

    // MRI: p
    public static IRubyObject ioPWriteMemory(ThreadContext context, IRubyObject scheduler, IRubyObject io, ByteBuffer base, int from, int size, int length) {
        RubyIOBuffer buffer = RubyIOBuffer.newBuffer(context, base, size, RubyIOBuffer.LOCKED | RubyIOBuffer.READONLY);

        IRubyObject result = ioPWrite(context, scheduler, io, buffer, from, length, 0);

        buffer.unlock(context);
        buffer.free(context);

        return result;
    }

    // MRI: rb_fiber_scheduler_io_close
    public static IRubyObject ioClose(ThreadContext context, IRubyObject scheduler, IRubyObject io) {
        return Helpers.invokeChecked(context, scheduler, "io_close", io);
    }

    // MRI: rb_fiber_scheduler_address_resolve
    public static IRubyObject addressResolve(ThreadContext context, IRubyObject scheduler, IRubyObject hostname) {
        return Helpers.invokeChecked(context, scheduler, "address_resolve", hostname);
    }

    // MRI: verify_scheduler
    static void verifyInterface(ThreadContext context, IRubyObject scheduler) {
        if (!scheduler.respondsTo("block")) {
            throw argumentError(context, "Scheduler must implement #block");
        }

        if (!scheduler.respondsTo("unblock")) {
            throw argumentError(context, "Scheduler must implement #unblock");
        }

        if (!scheduler.respondsTo("kernel_sleep")) {
            throw argumentError(context, "Scheduler must implement #kernel_sleep");
        }

        if (!scheduler.respondsTo("io_wait")) {
            throw argumentError(context, "Scheduler must implement #io_wait");
        }
    }

    // MRI: rb_fiber_scheduler_close
    public static IRubyObject close(ThreadContext context, IRubyObject scheduler) {
//        VM_ASSERT(ruby_thread_has_gvl_p());

        IRubyObject result;

        result = Helpers.invokeChecked(context, scheduler, "scheduler_close");
        if (result != null) return result;

        result = Helpers.invokeChecked(context, scheduler, "close");
        if (result != null) return result;

        return context.nil;
    }

    // MRI: rb_fiber_scheduler_io_result_apply
    public static int resultApply(ThreadContext context, IRubyObject result) {
        int resultInt;
        if (result instanceof RubyFixnum fixnum && (resultInt = fixnum.asInt(context)) < 0) {
            context.runtime.getPosix().errno(-resultInt);
            return -1;
        } else {
            return toInt(context, result);
        }
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject result(Ruby runtime, int result, Errno error) {
        return result(runtime.getCurrentContext(), result, error);
    }

    public static IRubyObject result(ThreadContext context, int result, Errno error) {
        return asFixnum(context, result == -1 ? error.value(): result);
    }
}
