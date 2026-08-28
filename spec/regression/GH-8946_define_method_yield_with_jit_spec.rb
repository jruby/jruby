# https://github.com/jruby/jruby/issues/8946
#
# When define_method is called within another method and the block uses yield,
# the captured block must be used for yield even after JIT + invokedynamic
# optimization (which previously bypassed DefineMethodMethod.call and lost
# the captured block).

describe 'define_method with yield inside another method' do
  it 'yields to the captured block after JIT optimization' do
    cls = Class.new do
      class << self
        def go(&blk)
          define_method(:foo) { yield }
        end
      end
    end

    results = []
    cls.go { results << :called }

    obj = cls.new
    # call enough times to trigger JIT and indy binding
    10.times { obj.foo }

    expect(results).to eq(Array.new(10, :called))
  end

  it 'yields to the captured block with arguments' do
    cls = Class.new do
      class << self
        def go(&blk)
          define_method(:bar) { |x| yield(x) }
        end
      end
    end

    results = []
    cls.go { |v| results << v }

    obj = cls.new
    10.times { |i| obj.bar(i) }

    expect(results).to eq((0..9).to_a)
  end
end
