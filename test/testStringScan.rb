require 'test/minirunit'
require 'strscan'

##### [] #####
s = StringScanner.new("Fri Dec 12 1975 14:39")
test_equal("Fri Dec 12 ", s.scan(/(\w+) (\w+) (\d+) /))
test_equal("Fri Dec 12 ", s[0])
test_equal("Fri", s[1])
test_equal("Dec", s[2])
test_equal("12", s[3])
test_equal(nil, s[4])
test_equal("1975 14:39", s.post_match)
test_equal("", s.pre_match)

##### check #####

s = StringScanner.new("Fri Dec 12 1975 14:39")
test_equal("Fri", s.check(/Fri/))
test_equal(0, s.pos)
test_equal("Fri", s.matched)
test_equal("", s.pre_match)
test_equal(" Dec 12 1975 14:39", s.post_match)
test_equal(nil, s.check(/12/))
test_equal(nil, s.matched)
  
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
test_exception(RangeError) { s.pos = 20 }

##### scan ######

s = StringScanner.new('test string')
test_equal("test", s.scan(/\w+/))
test_equal(nil, s.scan(/\w+/))
test_equal(" ", s.scan(/\s+/))
test_equal("string", s.scan(/\w+/))
test_equal(nil, s.scan(/./))
  
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

##### scan_full, search_full ######
s = StringScanner.new('test string')
test_equal(4, s.scan_full(/\w+/, true, false))
test_equal(4, s.pos)
test_equal("test", s.matched)
s.reset
test_equal("test", s.scan_full(/\w+/, true, true))
test_equal(4, s.pos)
s.reset
test_equal("test", s.scan_full(/\w+/, false, true))
test_equal(0, s.pos)
s.reset
test_equal("test str", s.search_full(/r/, true, true))
test_equal(8, s.pos)
s.reset
test_equal(8, s.search_full(/r/, true, false))
test_equal(8, s.pos)
test_equal("r", s.matched)
s.reset
test_equal("test str", s.search_full(/r/, false, true))
test_equal(0, s.pos)

##### JRUBY-214: arg 0 should have to_str called if not String ######
class Foo
  attr :to_str_called
  def to_str
    @to_str_called = true
    "bar"
  end
end
f = Foo.new
test_no_exception { StringScanner.new(f)  }
test_ok(f.to_str_called)

test_exception(TypeError) { StringScanner.new(Object.new) }