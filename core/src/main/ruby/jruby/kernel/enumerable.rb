module Enumerable
  def slice_before(filter = (no_filter = true; nil), &block)
    raise ArgumentError.new("wrong number of arguments (given 0, expected 1)") if (no_filter && !block) || (!no_filter && block)

    state = nil

    Enumerator.new do |yielder|
      ary = nil
      each do |*elt|
        if elt.size < 2
          elt = elt.size == 0 ? nil : elt[0]
        end

        if block
          state = block.call elt
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

  def slice_after(filter = (no_filter = true; nil), &block)
    raise ArgumentError.new("wrong number of arguments (given 0, expected 1)") if no_filter && !block
    raise ArgumentError.new("cannot pass both filter argument and block") if !no_filter && block

    state = nil

    Enumerator.new do |enum|
      ary = []
      each do |*element|
        if element.size < 2
          element = element.size == 0 ? nil : element[0]
        end

        if block
          state = block.call element
        else
          state = (filter === element)
        end

        if state
          ary << element
          enum.yield ary
          ary = []
        else
          ary << element
        end
      end
      enum.yield ary unless ary.nil? || ary.empty?
    end
  end

  def slice_when(&block)
    raise ArgumentError.new("missing block") unless block

    Enumerator.new do |enum|
      __slicey_chunky(false, enum, block)
    end
  end
  
  def chunk_while(&block)
    raise ArgumentError.new("missing block") unless block

    Enumerator.new do |enum|
      __slicey_chunky(true, enum, block)
    end
  end

  def __slicey_chunky(invert, enum, block)
    ary = nil
    last_after = nil
    element_present = false
    each_cons(2) do |before, after|
      element_present = true
      last_after = after
      match = block.call before, after

      ary ||= []
      if invert ? !match : match
        ary << before
        enum.yield ary
        ary = []
      else
        ary << before
      end
    end

    unless ary.nil?
      ary << last_after
      enum.yield ary
    end
    each_entry { |x| enum.yield [x] } unless element_present
  end
  private :__slicey_chunky

  def lazy
    Enumerator::Lazy.send :__from, self, :each, []
  end

  def enumerator_size
    respond_to?(:size) ? size : nil
  end
  private :enumerator_size

  # Passing arguments to this method is deprecated.
  def to_set(*args, &block)
    klass = if args.empty?
      Set
    else
      warn "passing arguments to Enumerable#to_set is deprecated", uplevel: 1
      args.shift
    end
    klass.new(self, *args, &block)
  end
end
