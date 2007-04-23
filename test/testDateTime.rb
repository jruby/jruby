require 'test/minirunit'
require 'date'
test_check "Test datetime:"

##### constructor ######
t = DateTime.new
test_equal(DateTime, t.class)
test_ok(t.instance_of?(DateTime), "T should be instance of DateTime")

##### to_s ######
# JRUBY-16
t = DateTime.now
s = t.to_s
test_equal(String, s.class)
test_ok(s.instance_of?(String), "S should be instance of String")

##### now ######
early = DateTime.now
late = DateTime.now

##### <, <=, >, >=, <=> ######
test_ok(early < late, "Early should be less than late")
test_ok(early <= late, "Early should be less than or equal to late")
test_ok(late > early, "Late should be greater than early")
test_ok(late >= early, "Late should be greater than or equal to early")
test_ok(late != early, "Early and late should not be equal")
test_equal(-1, early <=> late)
test_equal(0, early <=> early)
test_equal(1, late <=> early)