require 'test/minirunit'
require 'strscan'

##### getch #####
s = StringScanner.new('abc')
test_equal("a", s.getch)
test_equal("a", s.matched)
test_equal("b", s.getch)
test_equal("a", s.pre_match)
test_equal("c", s.getch)
test_equal("ab", s.pre_match)
test_equal(nil, s.getch)

##### pos=, rest #####
s = StringScanner.new('test string')
test_equal(7, s.pos = 7)
test_equal("ring", s.rest)
  
##### scan_until, pre_match #####
s = StringScanner.new("Fri Dec 12 1975 14:39")
test_equal("Fri Dec 1", s.scan_until(/1/))
test_equal("Fri Dec ", s.pre_match)
test_equal("2 1975 14:39", s.post_match)
test_equal("1", s.matched)
test_equal(nil, s.scan_until(/XYZ/))
test_equal(nil, s.pre_match)
test_equal(nil, s.matched)
test_equal(nil, s.post_match)

##### skip #####
s = StringScanner.new('test string')
test_equal(4, s.skip(/\w+/))
test_equal("test", s.matched)
test_equal(nil, s.skip(/\w+/))
test_equal(1, s.skip(/\s+/))
test_equal(6, s.skip(/\w+/))
test_equal(nil, s.skip(/./))

