require 'test/minirunit'

test_check "float"

test_ok(2.6.floor == 2)
test_ok(-2.6.floor == -3)
test_ok(2.6.ceil == 3)
test_ok(-2.6.ceil == -2)
test_ok(2.6.truncate == 2)
test_ok(-2.6.truncate == -2)
test_ok(2.6.round == 3)
test_ok(-2.4.truncate == -2)
test_ok((13.4 % 1 - 0.4).abs < 0.0001)


