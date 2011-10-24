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
