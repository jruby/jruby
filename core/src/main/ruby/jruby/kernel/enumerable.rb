module Enumerable
  def slice_before(filter = (no_filter = true; nil), &block)
    raise ArgumentError.new("wrong number of arguments (0 for 1)") if (no_filter && !block) || (!no_filter && block)

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
    raise ArgumentError.new("wrong number of arguments (0 for 1)") if no_filter && !block
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
    if respond_to?(:size) && size == 1
      each {|x| enum.yield [x]}
    else
      ary = nil
      last_after = nil
      each_cons(2) do |before, after|
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
    end
  end
  private :__slicey_chunky

  def lazy
    klass = Enumerator::Lazy::LAZY_WITH_NO_BLOCK # Note: class_variable_get is private in 1.8
    Enumerator::Lazy.new(klass.new(self, :each, []))
  end

  def uniq
    values = []
    hash = {}
    if block_given?
      each do |obj|
        ret = yield(*obj)
        next if hash.key? ret
        hash[ret] = obj
        values << obj
      end
    else
      each do |obj|
        next if hash.key? obj
        hash[obj] = obj unless hash.key? obj
        values << obj
      end
    end
    values
  end
end
