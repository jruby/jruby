class Proc
  def curry(curried_arity = nil)
    if lambda? && curried_arity
      if arity > 0 && curried_arity != arity
        raise ArgumentError, "Wrong number of arguments (%i for %i)" % [
          curried_arity,
          arity
        ]
      end

      if arity < 0 && curried_arity < (-arity - 1)
        raise ArgumentError, "Wrong number of arguments (%i for %i)" % [
          curried_arity,
          -arity - 1
        ]
      end
    end

    f = Proc.__make_curry_proc__(self, [], arity)

    f.singleton_class.send(:define_method, :binding) {
      raise ArgumentError, "cannot create binding from f proc"
    }

    f.singleton_class.send(:define_method, :parameters) {
      [[:rest]]
    }

    f.singleton_class.send(:define_method, :source_location) {
      nil
    }

    f
  end

  def self.__make_curry_proc__(proc, passed, arity)
    is_lambda = proc.lambda?
    passed.freeze

    __send__((is_lambda ? :lambda : :proc)) do |*argv, &passed_proc|
      my_passed = passed + argv
      if my_passed.length < arity
        if !passed_proc.nil?
          warn "#{caller[0]}: given block not used"
        end
        __make_curry_proc__(proc, my_passed, arity)
      else
        proc.call(*my_passed)
      end
    end
  end
end
