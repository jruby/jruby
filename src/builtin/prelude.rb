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
  def curry(arity = self.arity)
    check = check_arity(arity, self.arity, lambda?)
    if lambda? && arity != self.arity && !check
      argument_error(arity, self.arity)
    end

    make_curry_proc(self, [], arity)
  end

  private
  def make_curry_proc(proc, passed, arity)
    passed.freeze
    proc_lambda = proc.lambda?
    curried = proc_or_lambda(proc_lambda) do |*args, &blk|
      newpassed = passed + args
      if check_arity(newpassed.length, arity, proc_lambda)
        call(*newpassed, &blk)
      else
        make_curry_proc(proc, newpassed, arity)
      end
    end
    curried.singleton_class.send(:define_method, :binding) {
      raise ArgumentError, "cannot create binding from curried proc"
    }
    curried.singleton_class.send(:define_method, :parameters) {
      proc.parameters
    }
    curried.singleton_class.send(:define_method, :source_location) {
      proc.source_location
    }
    curried
  end

  def check_arity(args_arity, target_arity, lambda)
    if lambda
      if target_arity >= 0
        if args_arity == target_arity
          true
        elsif args_arity > target_arity
          argument_error(args_arity, target_arity)
        else
          false
        end
      else
        if args_arity >= -target_arity
          true
        elsif args_arity == (-target_arity) - 1
          true
        else
          false
        end
      end
    else
      if target_arity >= 0
        if args_arity >= target_arity
          true
        else
          false
        end
      else
        if args_arity >= (-target_arity) - 1
          true
        else
          false
        end
      end
    end
  end

  def argument_error(args_arity, target_arity)
    raise ArgumentError, "wrong number of arguments (#{args_arity} for #{target_arity})"
  end

  def proc_or_lambda(bool, &block)
    if bool
      lambda(&block)
    else
      proc(&block)
    end
  end
end
