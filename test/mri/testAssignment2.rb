require 'test/minirunit'

test_check "assignment"
a = nil
test_ok(defined?(a))
test_ok(a == nil)

# multiple asignment
a, b = 1, 2
test_ok(a == 1 && b == 2)

a, b = b, a
test_ok(a == 2 && b == 1)

a, = 1,2
test_ok(a == 1)

a, *b = 1, 2, 3
test_ok(a == 1 && b == [2, 3])

a, (b, c), d = 1, [2, 3], 4
test_ok(a == 1 && b == 2 && c == 3 && d == 4)

*a = 1, 2, 3
test_ok(a == [1, 2, 3])

*a = 4
test_ok(a == [4])

*a = nil
test_ok(a == [nil])


