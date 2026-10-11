require 'rspec'

class KeywordsMethods
  class << self
    def foo(a: 1)
      b = a
    end

    def foo1(a:)
      b = a
    end

    def foo2(a: 1, c:)
      b = a
    end

    def foo3(a: 60, b: 60, c: 10, d: nil)
      true
    end

  end
end

describe 'Keywords' do
  # https://github.com/jruby/jruby/issues/8344
  it 'will reject parameters passed which happen to be lvars in method' do
    expect { KeywordsMethods.foo(b: "a") }.to raise_error(ArgumentError)
    expect { KeywordsMethods.foo1(b: "a") }.to raise_error(ArgumentError)
    expect { KeywordsMethods.foo2(b: "a", c: 2) }.to raise_error(ArgumentError)
    expect(KeywordsMethods.foo3(a: 1, b: 2, c: 3, d: 4)).to eq(true)
  end
end
