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
      ary = nil
      last_after = nil
      if size == 1
        each {|x| enum.yield [x]}
      else
        each_cons(2) do |before, after|
          last_after = after
          match = block.call before, after

          ary ||= []
          if match
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
  end
  
  def chunk_while(&block)
    raise ArgumentError.new("missing block") unless block

    Enumerator.new do |enum|
      ary = nil
      last_after = nil
      each_cons(2) do |before, after|
        last_after = after
        match = block.call before, after

        ary ||= []
        if match
          ary << before
        else
          ary << before
          enum.yield ary
          ary = []
        end
      end

      unless ary.nil?
        ary << last_after
        enum.yield ary
      end
    end
  end

  def lazy
    klass = Enumerator::Lazy::LAZY_WITH_NO_BLOCK # Note: class_variable_get is private in 1.8
    Enumerator::Lazy.new(klass.new(self, :each, []))
  end
end
