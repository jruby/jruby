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

  def test_yield_arguments
    hash = { :a => 1, :b => 2, :c => 3 }

    passed = []; ret = hash.select { |pair| passed << pair; passed.size < 3 }
    assert_equal [:a, :b, :c], passed
    assert_equal( { :a => 1, :b => 2 }, ret )
    passed = []; ret = hash.select { |k,v| passed << k; passed << v }
    assert_equal [:a, 1, :b, 2, :c, 3], passed
    assert_equal hash, ret

    passed = []; ret = hash.reject { |pair| passed << pair; passed.size < 2 }
    assert_equal [:a, :b, :c], passed
    assert_equal( { :b => 2, :c => 3 }, ret )
    passed = []; ret = hash.reject { |k,v| passed << k; passed << v }
    assert_equal [:a, 1, :b, 2, :c, 3], passed
    assert_equal( {}, ret )

    passed = []; ret = hash.take_while { |pair| passed << pair; true }
    assert_equal [[:a, 1], [:b, 2], [:c, 3]], passed
    assert_equal [[:a, 1], [:b, 2], [:c, 3]], ret

    passed = []; ret = hash.take_while { |k,v| passed << k; passed << v; passed.size < 3 }
    assert_equal [:a, 1, :b, 2], passed
    assert_equal [[:a, 1]], ret

    passed = []; ret = hash.collect { |pair| passed << pair }
    assert_equal [[:a, 1], [:b, 2], [:c, 3]], passed

    passed = []; ret = hash.collect { |k,v| passed << k; passed << v; passed.size < 3 }
    assert_equal [:a, 1, :b, 2, :c, 3], passed
    assert_equal [true, false, false], ret
  end

  def test_compare_by_identity
    hash = {}.compare_by_identity
    arr = [0]
    hash[arr] = 42
    arr[0] = 1
    assert_equal 42, hash[arr]

    hash = {}.compare_by_identity
    arr1 = []; arr2 = []
    hash[arr2] = 2
    assert_equal nil, hash[arr1]
    assert_equal 2, hash[arr2]
    hash[arr1] = 1
    assert_equal 1, hash[arr1]
    assert_equal 2, hash[arr2]
  end

end
