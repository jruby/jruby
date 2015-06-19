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

class Mutex
  def initialize
    @owner = nil
  end

  # Check and only allow it to be marshal'd if there are no waiters.
  def marshal_dump
    raise "Unable to dump locked mutex" unless @waiters.empty?
    1
  end

  # Implemented because we must since we use marshal_load PLUS we need
  # to create AND prime @lock. If we didn't do this, then Marshal
  # wouldn't prime the lock anyway.
  def marshal_load(bunk)
    initialize
  end

  def locked?
    Rubinius.locked?(self)
  end

  def try_lock
    # Locking implies a memory barrier, so we don't need to use
    # one explicitly.
    if Rubinius.try_lock(self)
      return false if @owner == Thread.current
      @owner = Thread.current
      true
    else
      false
    end
  end

  def lock
    Rubinius.memory_barrier
    if @owner == Thread.current
      raise ThreadError, "Recursively locking not allowed"
    end

    Rubinius.lock self
    @owner = Thread.current
    Rubinius.memory_barrier
    return self
  end

  def unlock
    Rubinius.memory_barrier

    if @owner != Thread.current
      raise ThreadError, "Not owner, #{@owner.inspect} is"
    end

    @owner = nil
    Rubinius.unlock self
    self
  end

  def synchronize
    lock
    begin
      yield
    ensure
      unlock
    end
  end

  def sleep(duration=undefined)
    if duration.kind_of?(Numeric) && duration < 0
      raise ArgumentError, "time interval must be positive"
    end

    unlock
    begin
      Kernel.sleep(duration)
    ensure
      lock
    end
  end
end
