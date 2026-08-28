class Proc
  def curry(curried_arity = nil)
    if lambda? && curried_arity
      if arity >= 0 && curried_arity != arity
        raise ArgumentError, "wrong number of arguments (given %i, expected %i)" % [
          curried_arity,
          arity
        ]
      end

      if arity < -1
        is_rest, opt = false, 0
        parameters.each do |arr|
          case arr[0]
          when :rest then
            is_rest = true
          when :opt then
            opt += 1
          end
        end
        req = -arity - 1
        if curried_arity < req || curried_arity > (req + opt) && !is_rest
          expected = is_rest ?  "#{req}+" : "#{req}..#{req+opt}"
          raise ArgumentError, "wrong number of arguments (given %i, expected %s)" % [
                  curried_arity,
                  expected
                ]
        end
      end
    end

    Proc.__make_curry_proc__(self, [], curried_arity || arity)
  end

  # From https://github.com/marcandre/backports, MIT license (with modifications)
  def <<(g)
    lambda = if g.kind_of? Proc
               g.lambda?
             else
               raise TypeError, "callable object is expected" unless g.respond_to?(:call)
               true
             end

    if lambda
      lambda { |*args, **kw, &blk| call(g.call(*args, **kw, &blk)) }
    else
      proc { |*args, **kw, &blk| call(g.call(*args, **kw, &blk)) }
    end
  end

  # From https://github.com/marcandre/backports, MIT license (with modifications)
  def >>(g)
    raise TypeError, "callable object is expected" unless g.respond_to?(:call)

    if lambda?
      lambda { |*args, **kw, &blk| g.call(call(*args, **kw, &blk)) }
    else
      proc { |*args, **kw, &blk| g.call(call(*args, **kw, &blk)) }
    end
  end

  # Create a singleton class based on Proc that re-defines these methods but
  # otherwise looks just like Proc in every way. This allows us to override
  # the methods with different behavior without constructing a singleton every
  # time a proc is curried by using some JRuby-specific tricks below.
  curried_prototype = proc {}
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
  Curried = curried_prototype
  private_constant :Curried

  def self.__make_curry_proc__(proc, passed, arity)
    f = __send__((proc.lambda? ? :lambda : :proc)) do |*argv, &passed_proc|
      my_passed = passed + argv
      abs_arity = (arity < 0 ? (-arity - 1) : arity)
      if my_passed.length < abs_arity
        if !passed_proc.nil?
          warn "#{caller[0]}: given block not used"
        end
        __make_curry_proc__(proc, my_passed, arity)
      else
        proc.call(*my_passed, &passed_proc)
      end
    end

    # Replace the curried proc's class with our prototype singleton class
    JRuby::Util.send(:set_meta_class, f, Curried)

    f
  end
end
