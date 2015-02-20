module PushingPixelsFixtures

  module Foo
    extend self

    def foo(a, b, c)
      hash = {a: a, b: b, c: c}
      array = hash.map { |k, v| v }
      x = array[0]
      y = [a, b, c].sort[1]
      x + y
    end

  end

  class Bar

    def method_missing(method, *args)
      if Foo.respond_to?(method)
        Foo.send(method, *args)
      else
        0
      end
    end

  end

end

PETests.tests do

  broken_example "A set of constants used in a literal hash, mapped to an array, indexed, used in an array literal, sorted, indexed, and added, all via method_missing, respond_to? and send" do
    bar = PushingPixelsFixtures::Bar.new
    Truffle::Debug.assert_constant bar.foo(14, 8, 6)
  end

end
