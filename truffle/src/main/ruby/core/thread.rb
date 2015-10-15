# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Thread

  def [](symbol)
    __thread_local_variables[symbol.to_sym]
  end

  def []=(symbol, value)
    __thread_local_variables[symbol.to_sym] = value
  end

  def thread_variable?(symbol)
    __thread_local_variables.has_key? symbol.to_sym
  end

  alias_method :thread_variable_get, :[]
  alias_method :thread_variable_set, :[]=

  LOCK = Mutex.new

  def __thread_local_variables
    if defined?(@__thread_local_variables)
      @__thread_local_variables
    else
      LOCK.synchronize { @__thread_local_variables ||= {} }
    end
  end

  def thread_variables
    __thread_local_variables.keys
  end

  def self.start(&block)
    Thread.new(&block)
  end

  def self.abort_on_exception
    current.abort_on_exception
  end

  def self.abort_on_exception=(value)
    current.abort_on_exception = value
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
    __thread_local_variables.freeze
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
