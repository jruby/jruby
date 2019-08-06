class Enumerator

  def __each__
    @__object__.__send__ @__method__, *@__args__ do |*args|
      ret = yield(*args)
      unless @__feedvalue__.nil?
        ret, @__feedvalue__ = @__feedvalue__, nil
      end
      ret
    end
  end

  def next
    return @__lookahead__.shift unless @__lookahead__.empty?

    unless @__generator__
      # Allow #to_generator to return nil, indicating it has none for
      # this method.
      if @__object__.respond_to? :to_generator
        @__generator__ = @__object__.to_generator(@__method__, *@__args__)
      end

      @__generator__ = FiberGenerator.new(self) unless @__generator__
    end

    begin
      return @__generator__.next if @__generator__.next?
    rescue StopIteration
      nil # the enumerator could change between next? and next leading to StopIteration
    end

    exception = StopIteration.new 'iteration reached an end'
    exception.send(:__set_result__, @__generator__.result)

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
    Array(self.peek).dup
  end

  def rewind
    @__object__.rewind if @__object__.respond_to? :rewind
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
    FIBER_YIELD_PROC = Fiber.method(:yield).to_proc
    private_constant :FIBER_YIELD_PROC

    def initialize(enum)
      @object = enum
      @fiber = nil
      rewind
    end

    attr_reader :result

    def next?
      !@done
    end

    def next
      reset unless @fiber

      val = @fiber.resume

      raise StopIteration, 'iteration has ended' if @done

      val
    end

    def rewind
      fiber, @fiber = @fiber, nil
      fiber.send(:__finalize__) if fiber&.__alive__
      @done = false
    end

    def reset
      @done = false
      @fiber = Fiber.new do
        obj = @object
        @result = obj.__each__(&FIBER_YIELD_PROC)
        @done = true
      end
    end
  end

  class Lazy < Enumerator

    def initialize(obj, size = nil)
      _block_error(:new) unless block_given?
      @receiver = obj
      super(size) do |yielder, *args|
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

    def to_enum(method = :each, *args, &size)
      Lazy.send :__from, self, method, args, size # sets @receiver, @method, @args
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
      Lazy.new(self, enumerator_size) do |yielder, *values|
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
      raise ArgumentError, 'attempt to drop negative size' if n < 0

      size = enumerator_size
      size = n < size ? size - n : 0 if size.kind_of?(Integer)

      Lazy.new(self, size) do |yielder, *values|
        dropped = __memo(yielder) || 0
        if dropped < n
          __memo_set(yielder, dropped + 1)
        else
          yielder.yield(*values)
        end
      end.__set_inspect :drop, [n]
    end

    def drop_while
      _block_error(:drop_while) unless block_given?

      Lazy.new(self) do |yielder, *values|
        if (dropping = __memo(yielder)).nil?
          __memo_set(yielder, dropping = true)
        end
        yielder.yield(*values) unless dropping && (begin
          dropping = yield(*values)
          __memo_set(yielder, false) unless dropping
          dropping
        end)
        dropping
      end.__set_inspect :drop_while
    end

    def take(n)
      n = JRuby::Type.coerce_to_int(n)
      raise ArgumentError, 'attempt to take negative size' if n < 0

      return to_enum(:cycle, 0).lazy if n.zero?

      size = enumerator_size
      size = n < size ? n : size if size.kind_of?(Integer)

      Lazy.new(self, size) do |yielder, *values|
        taken = __memo(yielder) || 0

        if taken < n
          yielder.yield(*values)
          __memo_set(yielder, taken += 1)
        end
        throw yielder unless taken < n
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

      arys = args.map do |arg|
        if ary = JRuby::Type.is_array?(arg)
          ary
        else
          unless JRuby::Type.object_respond_to?(arg, :each)
            raise TypeError, "wrong argument type #{arg.class} (must respond to :each)"
          end
          arg
        end
      end

      if arys.all? { |arg| arg.is_a?(Array) }
        # Handle trivial case of multiple array arguments separately
        # by avoiding Enumerator#next for efficiency & compatibility
        Lazy.new(self, enumerator_size) do |yielder, *values|
          values = values.first unless values.size > 1
          index = __memo(yielder) || 0
          __memo_set(yielder, index + 1)
          yielder << arys.map { |ary| ary[index] }.unshift(values)
        end
      else
        Lazy.new(self, enumerator_size) do |yielder, *values|
          values = values.first unless values.size > 1
          enums = __memo(yielder) || __memo_set(yielder, args.map(&:to_enum))
          rests = enums.map do |arg|
            begin
              arg.next
            rescue StopIteration
              nil
            end
          end
          yielder << rests.unshift(values)
        end
      end.__set_inspect :zip, args
    end

    def uniq
      if block_given?
        Lazy.new(self) do |yielder, *obj|
          hash = __memo(yielder) || __memo_set(yielder, {})
          ret = yield(*obj)
          unless hash.key?(ret)
            hash[ret] = true
            yielder.yield(*obj)
          end
        end
      else
        Lazy.new(self) do |yielder, *obj|
          hash = __memo(yielder) || __memo_set(yielder, {})
          unless hash.key?(obj)
            hash[obj] = true
            yielder.yield(*obj)
          end
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

    private
    def __memo(yielder)
      yielder.instance_variable_get :@__memo
    end

    def __memo_set(yielder, value)
      yielder.instance_variable_set :@__memo, value
    end
  end
end