require 'test/unit'

class TestUnicodePaths < Test::Unit::TestCase
  def test_expand_path
    expanded = File.expand_path('/ü')
    exp_hex = expanded.unpack('H*')

    # assert byte sequence is what we expect
    assert_equal "2fc3bc", exp_hex[0]
  end
end
