
require 'minirunit'
test_check "Test pack/unpack:"
test_equal(["cat", "wom", "x", "yy"], %w( cat wombat x yy).pack("A3A3A3A3").unpack("A3A3A3A3"))
test_equal(["cat", "wom", "x", "yy"], %w( cat wombat x yy).pack("a3a3a3a3").unpack("Z3Z3Z3Z3"))
test_equal(["cat", "wom", "x  ", "yy "], %w( cat wombat x yy).pack("A3A3A3A3").unpack("Z3Z3Z3Z3"))
test_equal(["cat", "wom", "x\000\000", "yy\000"], %w( cat wombat x yy).pack("a3a3a3a3").unpack("a3a3a3a3"))
test_equal(["01100001"], ["01100001"].pack("b8").unpack("b8"))
test_equal(["01100001"], ["01100001"].pack("B8").unpack("B8"))
test_equal(["10000110"], ["01100001"].pack("B8").unpack("b8"))
test_equal(["1424", "a0", "2"],  ["1424", "a0", "21"].pack("h4h2h1").unpack("h4h2h1"))
test_equal(["4142", "0a", "1"],  ["4142", "0a", "12"].pack("H4H2H1").unpack("H4H2H1"))
test_equal(["1424", "a0", "0"],  ["4142", "0a", "12"].pack("H4H2H1").unpack("h4h2h1"))
test_equal([ 65, 66, 67 ], [ 65, 66, 67 ].pack("C3").unpack("C3"))
test_equal([ 255, 66, 67 ],[ -1, 66, 67 ].pack("C*").unpack("C*"))
test_equal([ -1, 66, 67 ],[ -1, 66, 67 ].pack("c*").unpack("c*"))
