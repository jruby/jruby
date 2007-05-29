require 'test/unit'

class TestPack < Test::Unit::TestCase  
  def test_pack_w
    assert_equal( "\005", [5].pack('w'))
    assert_equal( "\203t", [500].pack('w'))
    assert_equal( "\203\206P", [50000].pack('w'))
    assert_equal( "\222\320\227\344\000", [5000000000].pack('w'))
    assert_equal( "\222\320\227\344\001", [5000000001].pack('w'))
    assert_equal( "\272\357\232\025", [123456789].pack('w'))
  end
  
  def test_pack_M
     assert_equal("alpha=\n", %w/alpha beta gamma/.pack("M")) # ok
     assert_equal("-1=\n", [-1, 0, 1, 128].pack("M"))
     assert_equal("1=\n", [1].pack("M"))
     assert_equal("-1.5=\n", [-1.5, 0.0, 1.5, 128.5].pack("M"))
     assert_equal("9223372036854775808=\n", [9223372036854775808].pack("M"))
  end
end
