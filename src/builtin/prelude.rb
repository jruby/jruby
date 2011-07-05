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
    if relative_feature.respond_to? :to_path
      relative_feature = relative_feature.to_path
    else
      relative_feature = relative_feature
    end
    unless relative_feature.respond_to? :to_str
      raise TypeError, "cannot convert #{relative_feature.class} into String"
    end
    relative_feature = relative_feature.to_str
    
    c = caller.first
    e = c.rindex(/:\d+:in /)
    file = $`
    if /\A\((.*)\)/ =~ file # eval, etc.
      raise LoadError, "cannot infer basepath"
    end
    absolute_feature = File.join(File.dirname(File.realpath(file)), relative_feature)
    require absolute_feature
  end

  def exec(*args)
    _exec_internal(*JRuby::ProcessUtil.exec_args(args))
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

module Process
  def self.spawn(*args)
    _spawn_internal(*JRuby::ProcessUtil.exec_args(args))
  end
end

module Kernel
  module_function
  def spawn(*args)
    Process.spawn(*args)
  end
end
  
module JRuby
  module ProcessUtil
    def self.exec_args(args)
      env, prog, opts = nil

      if args.size < 1
        raise ArgumentError, 'wrong number of arguments'
      end

      # peel off options
      if args.size >= 1
        maybe_hash = args[args.size - 1]
        if maybe_hash.respond_to?(:to_hash) && maybe_hash = maybe_hash.to_hash
          opts = maybe_hash
          args.pop
        end
      end

      # peel off env
      if args.size >= 1
        maybe_hash = args[0]
        if maybe_hash.respond_to?(:to_hash) && maybe_hash = maybe_hash.to_hash
          env = maybe_hash
          args.shift
        end
      end

      if args.size < 1
        raise ArgumentError, 'wrong number of arguments'
      end

      # if Array, pull out prog and insert command back into args list
      if Array === args[0]
        tmp_ary = args[0]

        if tmp_ary.size != 2
          raise ArgumentError, 'wrong number of arguments'
        end
        # scrub
        prog = String(tmp_ary[0].to_str)

        args[0] = tmp_ary[1]
      end

      # convert and scrub remaining args
      args.size.times do |i|
        # scrub
        args[i] = String(args[i].to_str)
      end

      return env, prog, opts, args
    end
  end
end
