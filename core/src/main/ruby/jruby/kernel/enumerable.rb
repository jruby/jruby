module Enumerable
  def slice_before(filter = (no_filter = true; nil), &block)
    if no_filter && !block
      raise ArgumentError.new("wrong number of arguments (0 for 1)")
    end

    if block
      if no_filter
        state = nil
      else
        initial_state = filter.dup
        state = initial_state
      end
    else
      state = nil
    end

    Enumerator.new do |yielder|
      ary = nil
      self.each do |*elt|
        if elt.size < 2
          elt = elt.size == 0 ? nil : elt[0]
        end

        if block
          if no_filter
            state = block.call elt
          else
            state = block.call elt, initial_state
          end
        else
          state = (filter === elt)
        end

        if ary
          if state
            yielder.yield ary
            ary = [elt]
          else
            ary << elt
          end
        else
          ary = [elt]
        end
      end
      yielder.yield ary unless ary.nil?
    end
  end

  def lazy
    klass = Enumerator::Lazy::LAZY_WITH_NO_BLOCK # Note: class_variable_get is private in 1.8
    Enumerator::Lazy.new(klass.new(self, :each, []))
  end
end
