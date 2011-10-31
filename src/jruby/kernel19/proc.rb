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

    m = lambda? ? :lambda : :proc
    f = send(m) {|*x|
      args += x
      if args.length >= arity
        self[*args]
      else
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
