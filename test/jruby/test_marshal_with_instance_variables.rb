require 'test/unit'
require 'date'

# This test demonstrates and verifies the marshalling fix for JRUBY-3289

class JRubyHashSubclassTest < Test::Unit::TestCase

  private

  def ensure_equality(test_hash, unmarshalled_hash)
    test_hash.keys.each do |key|
      assert_equal(test_hash[key], unmarshalled_hash[key])
    end
  end

  public

  class SubHashOne < Hash
    def initialize
      @something = nil
    end
  end

  class SubHashTwo < Hash; end

  def setup
    @common_hash = {:date => Date.today, "key" => "value", 1 => 10, :other =>[]}
  end

  def test_normal_hash_marshals_successfully
    normal_hash = Hash.new
    test_hash = {:normal => normal_hash}.merge(@common_hash)

    dump_test = Marshal.dump(test_hash)

    loaded = Marshal.load(dump_test)
    ensure_equality(test_hash, loaded)
  end

  def test_subhashone_marshals_successfully
    sub_hash = SubHashOne.new
    test_hash = {:normal => sub_hash}.merge(@common_hash)

    dump_test = Marshal.dump(test_hash)
    #p dump_test

    loaded = Marshal.load(dump_test)
    ensure_equality(test_hash, loaded)
  end

  def test_subhashtwo_marshals_successfully
    sub_hash = SubHashTwo.new
    test_hash = {:normal => sub_hash}.merge(@common_hash)

    dump_test = Marshal.dump(test_hash)
    #p dump_test

    loaded = Marshal.load(dump_test)
    ensure_equality(test_hash, loaded)
  end
end
