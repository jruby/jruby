require 'minirunit'
test_check "Test Number"
test_ok(25.eql? 25)
test_ok(20.between? 15, 25)
test_ok(!(20.between? 10, 15))

