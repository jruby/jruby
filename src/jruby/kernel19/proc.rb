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

    args = []

    my_self = self
    m = lambda? ? :lambda : :proc
    f = __send__(m) {|*x|
      call_args = args + x
      if call_args.length >= my_self.arity
        my_self[*call_args]
      else
        args = call_args
        f
      end
    }

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
end
