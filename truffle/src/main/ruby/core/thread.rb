# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Copyright (c) 2011, Evan Phoenix
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of the Evan Phoenix nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Thread

  # Implementation note: ideally, the recursive_objects
  # lookup table would be different per method call.
  # Currently it doesn't cause problems, but if ever
  # a method :foo calls a method :bar which could
  # recurse back to :foo, it could require making
  # the tables independant.

  def self.recursion_guard(obj)
    id = obj.object_id
    objects = current.recursive_objects
    objects[id] = true

    begin
      yield
    ensure
      objects.delete id
    end
  end

  def self.guarding?(obj)
    current.recursive_objects[obj.object_id]
  end

  # detect_recursion will return if there's a recursion
  # on obj (or the pair obj+paired_obj).
  # If there is one, it returns true.
  # Otherwise, it will yield once and return false.

  def self.detect_recursion(obj, paired_obj=nil)
    id = obj.object_id
    pair_id = paired_obj.object_id
    objects = current.recursive_objects

    case objects[id]

      # Default case, we haven't seen +obj+ yet, so we add it and run the block.
      when nil
        objects[id] = pair_id
        begin
          yield
        ensure
          objects.delete id
        end

      # We've seen +obj+ before and it's got multiple paired objects associated
      # with it, so check the pair and yield if there is no recursion.
      when Hash
        return true if objects[id][pair_id]
        objects[id][pair_id] = true

        begin
          yield
        ensure
          objects[id].delete pair_id
        end

      # We've seen +obj+ with one paired object, so check the stored one for
      # recursion.
      #
      # This promotes the value to a Hash since there is another new paired
      # object.
      else
        previous = objects[id]
        return true if previous == pair_id

        objects[id] = { previous => true, pair_id => true }

        begin
          yield
        ensure
          objects[id] = previous
        end
    end

    false
  end

  # Similar to detect_recursion, but will short circuit all inner recursion
  # levels (using a throw)

  class InnerRecursionDetected < Exception; end

  def self.detect_outermost_recursion(obj, paired_obj=nil, &block)
    rec = current.recursive_objects

    if rec[:__detect_outermost_recursion__]
      if detect_recursion(obj, paired_obj, &block)
        raise InnerRecursionDetected.new
      end
      false
    else
      begin
        rec[:__detect_outermost_recursion__] = true

        begin
          detect_recursion(obj, paired_obj, &block)
        rescue InnerRecursionDetected
          return true
        end

        return nil
      ensure
        rec.delete :__detect_outermost_recursion__
      end
    end
  end

  def recursive_objects
    @recursive_objects ||= {}
  end

  def self.stop
    sleep
    nil
  end

  def raise_prim(exc)
    Truffle.primitive :thread_raise
    Kernel.raise PrimitiveFailure, "Thread#raise primitive failed"
  end
  private :raise_prim

  def priority
    Truffle.primitive :thread_get_priority
    Kernel.raise ThreadError, "Thread#priority primitive failed"
  end

  def priority=(val)
    Truffle.primitive :thread_set_priority
    Kernel.raise TypeError, "priority must be a Fixnum" unless val.kind_of? Fixnum
    Kernel.raise ThreadError, "Thread#priority= primitive failed"
  end

  def name
    Truffle.primitive :thread_get_name
    Kernel.raise ThreadError, "Thread#name primitive failed"
  end

  def name=(val)
    unless val.nil?
      val = Rubinius::Type.check_null_safe(StringValue(val))
      raise ArgumentError, "ASCII incompatible encoding #{val.encoding.name}" unless val.encoding.ascii_compatible?
      # TODO BJF Aug 27, 2016 Need to rb_str_new_frozen the val here and SET_ANOTHER_THREAD_NAME
    end
    Truffle.invoke_primitive :thread_set_name, self, val
    val
  end

  def inspect
    stat = status()
    stat = "dead" unless stat

    "#<#{self.class}:0x#{object_id.to_s(16)} id=#{@thread_id} #{stat}>"
  end

  alias_method :to_s, :inspect

  def self.exit
    Thread.current.kill
  end

  def self.kill(thread)
    thread.kill
  end

  def raise(exc=undefined, msg=nil, trace=nil)
    return self unless alive?

    if undefined.equal? exc
      no_argument = true
      exc         = nil
    end

    if exc.respond_to? :exception
      exc = exc.exception msg
      Kernel.raise TypeError, 'exception class/object expected' unless Exception === exc
      exc.set_backtrace trace if trace
    elsif no_argument
      exc = RuntimeError.exception nil
    elsif exc.kind_of? String
      exc = RuntimeError.exception exc
    else
      Kernel.raise TypeError, 'exception class/object expected'
    end

    if $DEBUG
      STDERR.puts "Exception: #{exc.message} (#{exc.class})"
    end

    if self == Thread.current
      Kernel.raise exc
    else
      raise_prim exc
    end
  end

  MUTEX_FOR_THREAD_EXCLUSIVE = Mutex.new

  def self.exclusive
    MUTEX_FOR_THREAD_EXCLUSIVE.synchronize { yield }
  end

  def randomizer
    @randomizer ||= Rubinius::Randomizer.new
  end

  # Fiber-local variables

  def [](name)
    var = name.to_sym
    Rubinius.synchronize(self) do
      locals = Truffle.invoke_primitive :thread_get_fiber_locals, self
      Truffle.invoke_primitive :object_ivar_get, locals, var
    end
  end

  def []=(name, value)
    var = name.to_sym
    Rubinius.synchronize(self) do
      Truffle.check_frozen
      locals = Truffle.invoke_primitive :thread_get_fiber_locals, self
      Truffle.invoke_primitive :object_ivar_set, locals, var, value
    end
  end

  def keys
    Rubinius.synchronize(self) do
      locals = Truffle.invoke_primitive :thread_get_fiber_locals, self
      locals.instance_variables
    end
  end

  # Thread-local variables

  # TODO (pitr-ch 06-Apr-2016): thread local variables do not have to be synchronized,
  # they are only to protect against non-thread-safe Hash implementation

  def thread_variable_get(name)
    __thread_local_variables_lock { __thread_local_variables[name.to_sym] }
  end

  def thread_variable_set(name, value)
    __thread_local_variables_lock { __thread_local_variables[name.to_sym] = value }
  end

  def thread_variable?(symbol)
    __thread_local_variables_lock { __thread_local_variables.has_key? symbol.to_sym }
  end

  LOCK = Mutex.new

  def __thread_local_variables
    if defined?(@__thread_local_variables)
      @__thread_local_variables
    else
      LOCK.synchronize do
        @__thread_local_variables ||= {}
      end
    end
  end

  def __thread_local_variables_lock
    if defined?(@__thread_local_variables_lock)
      @__thread_local_variables_lock
    else
      LOCK.synchronize do
        @__thread_local_variables_lock ||= Mutex.new
      end
    end.synchronize { yield }
  end

  def thread_variables
    __thread_local_variables_lock { __thread_local_variables.keys }
  end

  def self.start(*args, &block)
    Thread.new(*args, &block)
  end

  @abort_on_exception = false

  def self.abort_on_exception
    @abort_on_exception
  end

  def self.abort_on_exception=(value)
    @abort_on_exception = value
  end

  def self.handle_interrupt(config, &block)
    unless config.is_a?(Hash) and config.size == 1
      raise ArgumentError, "unknown mask signature"
    end
    exception, timing = config.first
    Truffle.privately do
      current.handle_interrupt(exception, timing, &block)
    end
  end

  def freeze
    __thread_local_variables_lock { __thread_local_variables.freeze }
    super
  end

end

class ThreadGroup
  def initialize
    @enclosed = false
  end

  def enclose
    @enclosed = true
  end

  def enclosed?
    @enclosed
  end

  def add(thread)
    raise ThreadError, "can't move to the frozen thread group" if self.frozen?
    raise ThreadError, "can't move to the enclosed thread group" if self.enclosed?

    from_tg = thread.group
    return nil unless from_tg
    raise ThreadError, "can't move from the frozen thread group" if from_tg.frozen?
    raise ThreadError, "can't move from the enclosed thread group" if from_tg.enclosed?

    Truffle.invoke_primitive :thread_set_group, thread, self
    self
  end

  def list
    Thread.list.select { |th| th.group == self }
  end

  Default = ThreadGroup.new

end
Truffle.invoke_primitive :thread_set_group, Thread.current, ThreadGroup::Default


