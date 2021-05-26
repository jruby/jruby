module JRuby
  # Base generator with unimplemented methods for children to fill in
  class AbstractGenerator
    def initialize(object, method, args, feed_value)
      @object = object
      @method = method
      @args = args
      @feed_value = feed_value
    end

    def next?
      raise NotImplementedError
    end

    def next
      raise NotImplementedError
    end

    def rewind
      raise NotImplementedError
    end

    def reset
      raise NotImplementedError
    end

    def result
      raise NotImplementedError
    end
  end

  # Array#each generator using a cursor
  class ArrayEachGenerator < AbstractGenerator
    def initialize(array, feed_value)
      super(array, nil, nil, feed_value)

      @index = 0
    end

    def next?
      @index < @object.size
    end

    def next
      feed_value = @feed_value.use_value

      return feed_value unless feed_value.nil?

      index = @index
      object = @object

      raise StopIteration.new("iteration reached an end") if index > object.size

      val = object.at(index)
      @index = index + 1

      val
    end

    def rewind
      @index = 0
    end
    alias reset rewind

    def result
      next? ? nil : self
    end
  end

  # Fiber-based internal iteration generator
  class FiberGenerator
    class State
      attr_reader :to_proc
      attr_accessor :done, :result

      def initialize(object, method, args, feed_value)
        @to_proc = proc do
          @result = object.__send__ method, *args do |*vals|
            ret = Fiber.yield(*vals)
            val = feed_value.use_value
            ret = val unless val.nil?
            ret
          end

          @done = true
        end
      end

    end
    private_constant :State

    def initialize(obj, method, args, feed_value)
      @state = State.new(obj, method, args, feed_value)
      @fiber = nil
      rewind
    end

    def result
      @state.result
    end

    def next?
      !@state.done
    end

    def next
      reset unless @fiber&.__alive__

      val = @fiber.resume

      raise StopIteration, 'iteration has ended' if @state.done

      val
    end

    def rewind
      fiber, @fiber = @fiber, nil
      fiber.send(:__finalize__) if fiber&.__alive__
      @state.done = false
    end

    def reset
      @state.done = false
      @state.result = nil
      @fiber = Fiber.new(&@state)
    end
  end
end

class Array
  # Use fiberless generator for simple :each cases
  def to_generator(method, args, feed_value)
    if method == :each && args.length == 0
      return JRuby::ArrayEachGenerator.new(self, feed_value)
    end
  end
end