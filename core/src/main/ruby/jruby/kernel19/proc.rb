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

    Proc.__make_curry_proc__(self, [], arity)
  end

  # Create a singleton class based on Proc that re-defines these methods but
  # otherwise looks just like Proc in every way. This allows us to override
  # the methods with different behavior without constructing a singleton every
  # time a proc is curried by using some JRuby-specific tricks below.
  curried_prototype = proc{}
  curried_prototype.instance_eval do
    def binding
      raise ArgumentError, "cannot create binding from f proc"
    end

    def parameters
      [[:rest]]
    end

    def source_location
      nil
    end
  end

  # Yank the singleton class out of the curried prototype object.
  Curried = JRuby.reference(curried_prototype).meta_class

  def self.__make_curry_proc__(proc, passed, arity)
    f = __send__((proc.lambda? ? :lambda : :proc)) do |*argv, &passed_proc|
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

    # Replace the curried proc's class with our prototype singleton class
    JRuby.reference(f).meta_class = Curried

    f
  end
end
