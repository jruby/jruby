class Enumerator
  class Yielder
    # Current API for Lazy Enumerator does not provide an easy way
    # to handle internal state. We "cheat" and use yielder to hold it for us.
    # A new yielder is created when generating or after a `rewind`.
    # This way we avoid issues like http://bugs.ruby-lang.org/issues/7691
    # or http://bugs.ruby-lang.org/issues/7696
    attr_accessor :backports_memo
  end
  def next
    return @__lookahead__.shift unless @__lookahead__.empty?

    unless @__generator__
      # Allow #to_generator to return nil, indicating it has none for
      # this method.
      if @__object__.respond_to? :to_generator
        @__generator__ = @__object__.to_generator(@__method__, *@__args__)
      end

      if !@__generator__
        @__generator__ = FiberGenerator.new(@__object__, @__method__, @__args__)
      end
    end

    begin
      return @__generator__.next if @__generator__.next?
    rescue StopIteration
      nil # the enumerator could change between next? and next leading to StopIteration
    end

    exception = StopIteration.new 'iteration reached an end'
    JRuby.ref(exception).result = @__generator__.result

    raise exception
  end

  def next_values
    Array(self.next)
  end

  def peek
    return @__lookahead__.first unless @__lookahead__.empty?
    item = self.next
    @__lookahead__ << item
    item
  end

  def peek_values
    [*self.peek]
  end

  def rewind
    @object.rewind if @object.respond_to? :rewind
    @__generator__.rewind if @__generator__
    @__lookahead__ = []
    @__feedvalue__ = nil
    self
  end

  def feed(val)
    raise TypeError, 'Feed value already set' unless @__feedvalue__.nil?
    @__feedvalue__ = val
    nil
  end

  class FiberGenerator
    class State
      attr_reader :object, :method, :args, :to_proc
      attr_accessor :done, :result

      def initialize(object, method, args)
        @object = object
        @method = method
        @args = args
        @done = false
        @result = nil

        @to_proc = proc do
          obj = @object
          @result = obj.send(@method, *@args, &FIBER_YIELD_PROC)
          @done = true
        end
      end

      FIBER_YIELD_PROC = Fiber.method(:yield).to_proc
    end

    def initialize(obj, method, args)
      @state = State.new(obj, method, args)
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
      reset unless @fiber

      val = @fiber.resume

      raise StopIteration, 'iteration has ended' if @state.done

      val
    end

    def rewind
      fiber, @fiber = @fiber, nil
      if fiber
        fiber_ref = JRuby.ref(fiber)
        if fiber_ref.alive?
          fiber_ref.finalize rescue nil
        end
      end
      @state.done = false
    end

    def reset
      @state.done = false
      @state.result = nil
      @fiber = Fiber.new(&@state)
    end
  end

  class Lazy < Enumerator
    LAZY_WITH_NO_BLOCK = Struct.new(:object, :method, :args) # used internally to create lazy without block
    private_constant :LAZY_WITH_NO_BLOCK

    def initialize(obj)
      if obj.is_a?(LAZY_WITH_NO_BLOCK)
        @inspect_info = obj
        return super(@receiver = obj.object, @method = obj.method || :each, * @args = obj.args)
      end
      _block_error(:new) unless block_given?
      @receiver = obj
      super() do |yielder, *args|
        catch yielder do
          obj.each(*args) do |*x|
            yield yielder, *x
          end
        end
      end
    end

    alias_method :force, :to_a

    def lazy
      self
    end

    def to_enum(method = :each, *args)
      Lazy.new(LAZY_WITH_NO_BLOCK.new(self, method, args))
    end
    alias_method :enum_for, :to_enum

    def inspect
      suff = ''
      suff << ":#{@method}" unless @method.nil? || @method == :each
      suff << "(#{@args.inspect[1...-1]})" if @args && !@args.empty?
      "#<#{self.class}: #{@receiver.inspect}#{suff}>"
    end

    [
        :slice_before,
        :slice_after,
        :slice_when,
        :chunk_while,
        :chunk,
        :with_index,
        :cycle,
        :each_with_object,
        :each_slice,
        :each_entry,
        :each_cons,
    ].each do |method|
      module_eval <<-EOT, __FILE__, __LINE__ + 1
        def #{method}(*args)                                     # def cycle(*args)
          return to_enum(:#{method}, *args) unless block_given?  #   return to_enum(:cycle, *args) unless block_given?
          super                                                  #   super
        end                                                      # end
      EOT
    end

    def chunk(*)
      super.lazy
    end

    def map
      _block_error(:map) unless block_given?
      Lazy.new(self) do |yielder, *values|
        yielder << yield(*values)
      end.__set_inspect :map
    end
    alias_method :collect, :map

    def select
      _block_error(:select) unless block_given?
      Lazy.new(self) do |yielder, *values|
        values = values.first unless values.size > 1
        yielder.yield values if yield values
      end.__set_inspect :select
    end
    alias_method :find_all, :select

    def reject
      _block_error(:reject) unless block_given?
      Lazy.new(self) do |yielder, *values|
        values = values.first unless values.size > 1
        yielder.yield(values) unless yield values
      end.__set_inspect :reject
    end

    def grep(pattern)
      if block_given?
        # Split for performance
        Lazy.new(self) do |yielder, *values|
          values = values.first unless values.size > 1
          yielder.yield(yield(values)) if pattern === values
        end
      else
        Lazy.new(self) do |yielder, *values|
          values = values.first unless values.size > 1
          yielder.yield(values) if pattern === values
        end
      end.__set_inspect :grep, [pattern]
    end

    def grep_v(pattern)
      if block_given?
        # Split for performance
        Lazy.new(self) do |yielder, *values|
          values = values.first unless values.size > 1
          yielder.yield(yield(values)) unless pattern === values
        end
      else
        Lazy.new(self) do |yielder, *values|
          values = values.first unless values.size > 1
          yielder.yield(values) unless pattern === values
        end
      end.__set_inspect :grep_v, [pattern]
    end

    def drop(n)
      n = JRuby::Type.coerce_to_int(n)
      Lazy.new(self) do |yielder, *values|
        data = yielder.backports_memo ||= {:remain => n}
        if data[:remain] > 0
          data[:remain] -= 1
        else
          yielder.yield(*values)
        end
      end.__set_inspect :drop, [n]
    end

    def drop_while
      _block_error(:drop_while) unless block_given?
      Lazy.new(self) do |yielder, *values|
        data = yielder.backports_memo ||= {:dropping => true}
        yielder.yield(*values) unless data[:dropping] &&= yield(*values)
      end.__set_inspect :drop_while
    end

    def take(n)
      n = JRuby::Type.coerce_to_int(n)
      raise ArgumentError, 'attempt to take negative size' if n < 0
      Lazy.new(n == 0 ? [] : self) do |yielder, *values|
        data = yielder.backports_memo ||= {:remain => n}
        yielder.yield(*values)
        throw yielder if (data[:remain] -= 1) == 0
      end.__set_inspect :take, [n], self
    end

    def take_while
      _block_error(:take_while) unless block_given?
      Lazy.new(self) do |yielder, *values|
        throw yielder unless yield(*values)
        yielder.yield(*values)
      end.__set_inspect :take_while
    end

    def flat_map
      _block_error(:flat_map) unless block_given?
      Lazy.new(self) do |yielder, *values|
        result = yield(*values)
        ary = JRuby::Type.is_array?(result)
        if ary || (result.respond_to?(:each) && result.respond_to?(:force))
          (ary || result).each{|x| yielder << x }
        else
          yielder << result
        end
      end.__set_inspect :flat_map
    end
    alias_method :collect_concat, :flat_map

    def zip(*args)
      return super if block_given?
      arys = args.map{ |arg| JRuby::Type.is_array?(arg) }
      if arys.all?
        # Handle trivial case of multiple array arguments separately
        # by avoiding Enumerator#next for efficiency & compatibility
        Lazy.new(self) do |yielder, *values|
          data = yielder.backports_memo ||= {:iter => 0}
          values = values.first unless values.size > 1
          yielder << arys.map{|ary| ary[data[:iter]]}.unshift(values)
          data[:iter] += 1
        end
      else
        args.each do |a|
          raise TypeError, "wrong argument type #{a.class} (must respond to :each)" unless a.respond_to? :each
        end
        Lazy.new(self) do |yielder, *values|
          enums = yielder.backports_memo ||= args.map(&:to_enum)
          values = values.first unless values.size > 1
          others = enums.map do |arg|
            begin
              arg.next
            rescue StopIteration
              nil
            end
          end
          yielder << others.unshift(values)
        end
      end.__set_inspect :zip, args
    end

    def uniq
      if block_given?
        Lazy.new(self) do |yielder, obj|
          hash = yielder.backports_memo ||= {}
          ret = yield(*obj)
          next if hash.key? ret
          hash[ret] = obj
          yielder << obj
        end
      else
        Lazy.new(self) do |yielder, obj|
          hash = yielder.backports_memo ||= {}
          next if hash.key? obj
          hash[obj] = obj unless hash.key? obj
          yielder << obj
        end
      end
    end

    protected
    def __set_inspect(method, args = nil, receiver = nil)
      @method = method
      @args = args
      @receiver = receiver if receiver
      self
    end

    def _block_error(name)
      raise ArgumentError.new("tried to call lazy #{name} without a block")
    end
  end
end