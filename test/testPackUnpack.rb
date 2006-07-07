require 'test/minirunit'

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
test_equal([-12, 65, 543, -12],[-12, 65, 543, 65524].pack("s*").unpack("s*"))
test_equal([65524, 65, 543, 65524],[-12, 65, 543, 65524].pack("S*").unpack("S*"))
test_equal([-12, 65, 543, -1339724956], [-12, 65, 543, 2955242340].pack('i*').unpack('i*'))
test_equal([4294967284, 65, 543, 2955242340],  [-12, 65, 543, 2955242340].pack('I*').unpack('I*'))
test_equal([-12, 65, 543, -1339724956], [-12, 65, 543, 2955242340].pack('l*').unpack('l*'))
test_equal([4294967284, 65, 543, 2955242340],  [-12, 65, 543, 2955242340].pack('L*').unpack('L*'))
test_equal([4294967284, 65, 543, 2955242340], [-12, 65, 543, 2955242340].pack('N*').unpack('N*'))
test_equal("\377\377\377\364\000\000\000A\000\000\002\037\260%kd" ,[-12, 65, 543, 2955242340].pack('N*'))
test_equal("\377\364\000A\002\037\377\364", [-12, 65, 543, 65524].pack('n*'))
test_equal([65524, 65, 543, 65524],[-12, 65, 543, 65524].pack("n*").unpack("n*"))
test_equal("\364\377\377\377A\000\000\000\037\002\000\000dk%\260" ,[-12, 65, 543, 2955242340].pack('V*'))
test_equal([4294967284, 65, 543, 2955242340],  [-12, 65, 543, 2955242340].pack('V*').unpack('V*'))
test_equal("\364\377A\000\037\002\212W" ,[-12, 65, 543, 295524234].pack('v*'))
test_equal([65524, 65, 543, 22410], [-12, 65, 543, 295524234].pack('v*').unpack('v*'))
test_equal("\xc2\xa9B\xe2\x89\xa0", [0xa9, 0x42, 0x2260].pack("U*"))
test_equal([0xa9, 0x42, 0x2260], [0xa9, 0x42, 0x2260].pack("U*").unpack('U*'))
test_equal([127, 128], [127, 128].pack("xCxC").unpack("xCxC"))
test_equal(["h", "e", "l", "l", "o"], "whole".unpack('xax2aX2aX1aX2a'))
test_equal([987.654321098/100.0], [987.654321098/100.0].pack("d*").unpack("d*"))
test_equal([987.654321098/100.0], [987.654321098/100.0].pack("E*").unpack("E*"))
test_equal([987.654321098/100.0], [987.654321098/100.0].pack("G*").unpack("G*"))
test_equal(["123"], ["123"].pack("m").unpack("m"))
test_equal(["12"], ["12"].pack("m").unpack("m"))
# TODO: Missing more tests for double precision.  
# TODO: Missing all single precision tests.
test_exception(ArgumentError) { ["A"].pack("X2") }
test_exception(ArgumentError) { "A".unpack("X2") }
test_exception(ArgumentError) { "A".unpack("x2") }

