require 'test/minirunit'
test_check "Test rand:"

test_equal(0, srand(1234))
test_equal(1234, srand(1001))

a = [rand(), rand(), rand()]
srand(1001)
test_equal(a, [rand(), rand(), rand()])

a = (0...50).collect {|i| rand() }
test_ok(a.detect {|v| v < 0 || v >= 1 }.nil?)

iv = rand(10)
test_equal(iv.floor, iv)

a = (0...50).collect {|i| rand(10) }
test_ok(a.detect {|v| v < 0 || v >= 10 }.nil?)
