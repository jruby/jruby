require 'test/unit'

class TestMethod < Test::Unit::TestCase
  # JRUBY-3491: NullPointerException when calling #arity on Klass.method(:new)
  def test_jruby_3491
    assert_nothing_raised do
      String.method(:new).arity
    end
  end

  def test_function_break
    obj = Object.new
    def obj.broken_method
      break # TODO this is a SyntaxError on MRI 2.2.2
    end
    assert_raise(LocalJumpError){obj.broken_method}
  end

  module Methods
    def self.req2(a1, a2); a1 || a2 end
    def self.opt1(a1, a2 = {}); a1 if a2 end
    def self.key(foo: 1, bar: 2) foo || bar end
    def self.keyrest(foo: 1, **bar) bar end
    def self.key_block(foo: 1, &bar) bar end
    def self.anonkeyrest(**) end
    def self.resta(*a) a end
    def self.mix(a1, *a2, a3, foo: 1, **bar) end
  end

  def test_parameters
    assert_equal [[:req, :a1], [:req, :a2]], Methods.method(:req2).parameters
    assert_equal [[:req, :a1], [:opt, :a2]], Methods.method(:opt1).parameters

    assert_equal [[:key, :foo], [:key, :bar]], Methods.method(:key).parameters
    assert_equal [[:key, :foo], [:keyrest, :bar]], Methods.method(:keyrest).parameters
    assert_equal [[:key, :foo], [:block, :bar]], Methods.method(:key_block).parameters
    assert_equal [[:keyrest]], Methods.method(:anonkeyrest).parameters

    assert_equal [[:rest, :a]], Methods.method(:resta).parameters
    assert_equal [[ :rest ]], String.method(:new).parameters

    assert_equal [[:req, :a1], [:rest, :a2], [:req, :a3], [:key, :foo], [:keyrest, :bar]], Methods.method(:mix).parameters

    assert_equal [[:req, :a1]], lambda { |a1| }.parameters
    assert_equal [[:req, :a1], [:opt, :a2]], lambda { |a1, a2 = {}| }.parameters

    assert_equal [[:key, :foo], [:key, :bar]], lambda { |foo: 1, bar: 2| }.parameters
    assert_equal [[:key, :foo], [:keyrest, :bar]], lambda { |foo: 1, **bar| }.parameters
    assert_equal [[:keyrest]], lambda { |**| }.parameters

    assert_equal [[:rest]], lambda { |*| }.parameters

    assert_equal [[:req, :a1], [:rest, :a2], [:req, :a3], [:key, :foo], [:keyrest, :bar]], lambda { |a1, *a2, a3, foo: 1, **bar| }.parameters
  end

end