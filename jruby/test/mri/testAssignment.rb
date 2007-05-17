require 'test/minirunit'

test_check "assignment"

a=[]; a[0] ||= "bar";
test_ok(a[0] == "bar")
h={}; h["foo"] ||= "bar";
test_ok(h["foo"] == "bar")

aa = 5
aa ||= 25
test_ok(aa == 5)
bb ||= 25
test_ok(bb == 25)
cc &&=33
test_ok(cc == nil)
cc = 5
cc &&=44
test_ok(cc == 44)


