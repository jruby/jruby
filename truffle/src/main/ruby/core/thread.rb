# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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

#--
# Be very careful about calling raise in here! Thread has its own
# raise which, if you're calling raise, you probably don't want. Use
# Kernel.raise to call the proper raise.
#++

class Thread
  MUTEX_FOR_THREAD_EXCLUSIVE = Mutex.new

  def self.exclusive
    MUTEX_FOR_THREAD_EXCLUSIVE.synchronize { yield }
  end

  def randomizer
    @randomizer ||= Rubinius::Randomizer.new
  end

  # TODO (pitr-ch 06-Apr-2016): thread local variables do not have to be synchronized,
  # they are only to protect against non-thread-safe Hash implementation

  def [](symbol)
    __thread_local_variables_lock { __thread_local_variables[symbol.to_sym] }
  end

  def []=(symbol, value)
    __thread_local_variables_lock { __thread_local_variables[symbol.to_sym] = value }
  end

  def thread_variable?(symbol)
    __thread_local_variables_lock { __thread_local_variables.has_key? symbol.to_sym }
  end

  alias_method :thread_variable_get, :[]
  alias_method :thread_variable_set, :[]=

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
    Rubinius.privately do
      current.handle_interrupt(exception, timing, &block)
    end
  end

  def freeze
    __thread_local_variables_lock { __thread_local_variables.freeze }
    super
  end
end

class ThreadGroup

  attr_reader :list

  def initialize
    @list = []
  end

  def add(thread)
    @list.push thread
  end

end
