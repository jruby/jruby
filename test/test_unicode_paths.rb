require 'test/unit'

class TestUnicodePaths < Test::Unit::TestCase
  def test_expand_path
    expanded = File.expand_path('/ü')
    exp_hex = expanded.unpack('H*')

    # assert byte sequence is what we expect
    assert exp_hex[0] =~ /2fc3bc$/
  end
end
