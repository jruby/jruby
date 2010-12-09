# prelude is called early in bootstrap for 1.9 so we need to load native
# thread bits to ensure our native classes are wired up instead.
require 'thread.jar'

# Mutex

class Mutex
  def synchronize
    self.lock
    begin
      yield
    ensure
      self.unlock rescue nil
    end
  end
end

# Thread

class Thread
  MUTEX_FOR_THREAD_EXCLUSIVE = Mutex.new
  def self.exclusive
    MUTEX_FOR_THREAD_EXCLUSIVE.synchronize{
      yield
    }
  end
end

module Kernel
  module_function
  def require_relative(relative_feature)
    c = caller.first
    e = c.rindex(/:\d+:in /)
    file = $`
    if /\A\((.*)\)/ =~ file # eval, etc.
      raise LoadError, "require_relative is called in #{$1}"
    end
    absolute_feature = File.join(File.dirname(File.realpath(file)), relative_feature)
    require absolute_feature
  end
end

class Proc
  def curry(arity = nil)
    if arity && lambda? && arity != self.arity
      raise ArgumentError, "wrong number of arguments (#{arity} for #{self.arity}"
    else
      arity = self.arity
    end

    make_curry_proc(self, [], arity)
  end

  private
  def make_curry_proc(proc, passed, arity)
    passed.freeze
    proc_or_lambda(proc.lambda?) do |*args|
      newpassed = passed + args
      if newpassed.length == arity
        call(*newpassed)
      else
        make_curry_proc(proc, newpassed, arity)
      end
    end
  end

  def proc_or_lambda(bool, &block)
    if bool
      lambda(&block)
    else
      proc(&block)
    end
  end
end
