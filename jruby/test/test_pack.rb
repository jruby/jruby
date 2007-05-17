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
end
