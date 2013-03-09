class Enumerator

  class Lazy < Enumerator
    
    def initialize(*)
      _block_error(:new) unless block_given?
      super
    end
    
    def collect()
      _block_error(:collect) unless block_given?
      Lazy.new do |output|
        each do |element|
          output.yield yield(element)
        end
      end
    end
    alias map collect
    
    def cycle(n = nil, &block)
      if block
        if n
          n.times do
            each {|*elt| block.call(*elt)}
          end
        else
          loop do
            each {|*elt| block.call(*elt)}
          end
        end
        nil
      else
        if n
          Lazy.new do |output|
            n.times do
              each do |elt|
                output.yield elt
              end
            end
          end
        else
          Lazy.new do |output|
            loop do
              each do |elt|
                output.yield elt
              end
            end
          end
        end
      end
    end

    def drop(n)
      Lazy.new do |output|
        each_with_index do |element, index|
          next if index < n
          output.yield(element)
        end
      end
    end

    def drop_while(&block)
      _block_error(:collect) unless block
      Lazy.new do |output|
        each do |element|
          output.yield(element) unless yield(element)..true
        end
      end
    end

    alias force to_a

    def flat_map(&block)
      _block_error(:collect) unless block
      Lazy.new do |output|
        each do |element|
          result = yield element
          if result.is_a? Array
            ary = result
          elsif result.respond_to?(:force) && result.respond_to?(:each)
            lazy = result
          else
            ary = result.to_ary if result.respond_to?(:to_ary)
          end


          if ary
            # if it's an array or coerced to an array, iterate directly
            i, max = 0, ary.size
            while i < max
              output.yield ary.at(i)
              i+=1
            end
          elsif lazy
            # if it's lazy, each over it
            lazy.each {|value| output.yield value}
          else
            # otherwise just yield it
            output.yield(result)
          end
        end
      end
    end
  
    def grep(pattern, &block)
      if block_given?
        Lazy.new do |output|
          each do |element|
            output.yield(yield element) if pattern === element
          end
        end
      else
        Lazy.new do |output|
          each do |element|
            output.yield(element) if pattern === element
          end
        end
      end
    end
    def lazy
      self
    end

    def reject(&block)
      _block_error(:collect) unless block
      Lazy.new do |output|
        each do |element|
          output.yield(element) unless yield(element)
        end
      end
    end

    def select(&block)
      _block_error(:collect) unless block
      Lazy.new do |output|
        each do |element|
          output.yield(element) if yield(element)
        end
      end
    end
    
    def slice_before(pattern)
      Lazy.new do |output|
        slice = []
        catch(output) do
          each do |element|
            if pattern === element
              slice << element
            else
              throw output
            end
          end
        end
        output.yield slice
      end
    end

    def take(n)
      Lazy.new do |output|
        if n > 0
          catch(output) do
            each_with_index do |element, index|
              output.yield(element)
              # break failed to work for some reason
              throw output if index + 1 == n
            end
          end
        end
      end
    end
    
    def to_enum(name = nil, *args)
      if name
        Lazy.new do |yielder|
          send(name, *args) {|element| yielder.yield(element)}
        end
      else
        self
      end
    end
    alias enum_for to_enum

    def take_while(&block)
      _block_error(:collect) unless block
      Lazy.new do |output|
        catch(output) do
          each do |element|
            # break failed to work for some reason
            throw output unless yield(element)
            output.yield(element)
          end
        end
      end
    end
    
    def zip(*enumerables, &block)
      if block
        return super
      end
      
      all_arrays = true
      enumerators = enumerables.map do |enumerable|
        if enumerable.kind_of? Array
          next enumerable
        elsif enumerable.respond_to?(:to_ary)
          enumerator = enumerable.to_ary
          
          next enumerator unless enumerator.nil?
        end
        
        all_arrays = false
        next enumerable if enumerable.respond_to?(:each)
        
        raise TypeError, "wront argument type #{enumerable.class.name} (must respond to :each)"
      end
      
      if all_arrays
        Lazy.new do |output|
          each_with_index do |element, index|
            ary = [element]
            enumerators.each {|array| ary << array[index] if index < array.size}
            output.yield ary
          end
        end
      else
        Lazy.new do |output|
          enumerators = enumerators.map(&:to_enum)
          each do |element|
            ary = [element]
            enumerators.each {|enumerator| ary << (enumerator.next rescue nil)}
            output.yield ary
          end
        end
      end
    end
    
    def _block_error(name)
      raise ArgumentError.new("tried to call lazy #{name} without a block")
    end
    private :_block_error
    
  end
end

module Enumerable
  def lazy
    Enumerator::Lazy.new(self) do |output, *values|
      output.yield(*values)
    end
  end
  
  def chunk(init_val = (init_given = true; nil))
    cls = (self.kind_of? Enumerator) ? self.class : Enumerator
    cls.new do |yielder|
      
      chunk = nil
      each do |elt|
        new_val = init_given ? yield(elt, init_val) : yield(elt)
        
        if chunk.nil?
          chunk = [new_val, [elt]]
        elsif new_val == init_val
          chunk[1] << elt
        else
          yielder.yield chunk
          chunk = [new_val, [elt]]
        end
        
        init_val = new_val
      end
      
      yielder.yield chunk if chunk
    end
  end
end