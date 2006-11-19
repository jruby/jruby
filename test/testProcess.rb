require 'test/minirunit'

test_check "Test Process"

tms = nil

test_no_exception {
    tms = Process.times
}

test_ok(tms != nil)
test_ok(tms.utime != nil)
test_ok(tms.stime != nil)
test_ok(tms.cutime != nil)
test_ok(tms.cstime != nil)

test_ok(tms.utime > 0)
