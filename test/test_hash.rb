require 'test/unit'

class TestHash < Test::Unit::TestCase
  def test_clone_copies_default_initializer
    hash = Hash.new { |h, k| h[k] = 0 }
    clone = hash.clone
    assert_equal 0, clone[:test]
  end

  class Eql
    def initialize(hash); @hash=hash; end
    def eql?(other); true; end
    def hash; @hash; end
  end

  class Ee
    def initialize(hash); @hash=hash; end
    def ==(other); true; end
    def hash; @hash; end
  end

  class Equal
    def initialize(hash); @hash=hash; end
    def equal?(other); true; end
    def hash; @hash; end
  end

  def test_lookup_with_eql_and_same_hash_should_work
    eql1 = Eql.new(5)
    eql2 = Eql.new(5)

    hash = {eql1 => "bar"}
    assert_equal("bar", hash[eql1])
    assert_equal("bar", hash[eql2])
  end

  def test_lookup_with_eql_and_different_hash_should_not_work
    eql1 = Eql.new(5)
    eql2 = Eql.new(6)

    hash = {eql1 => "bar"}
    assert_equal("bar", hash[eql1])
    assert_nil(hash[eql2])
  end

  def test_lookup_with_ee_and_same_hash_should_not_work
    ee1 = Ee.new(5)
    ee2 = Ee.new(5)
    hash = {ee1 => 'bar'}
    assert_nil(hash[ee2])
  end

  def test_lookup_with_equal_and_same_hash_should_not_work
    equal1 = Equal.new(5)
    equal2 = Equal.new(5)
    hash = {equal1 => 'bar'}
    assert_nil(hash[equal2])
  end
end
