require 'minirunit'
test_check "Test Number"
test_ok(25.eql? 25)
test_ok(20.between? 15, 25)
test_ok(!(20.between? 10, 15))
test_ok(78.chr == 'N')

test_equal(1, Integer.induced_from(1))
test_equal(1, Integer.induced_from(1.0))
test_exception(TypeError) { Integer.induced_from(:hello) }
test_exception(TypeError) { Integer.induced_from("hello") }
test_exception(TypeError) { Integer.induced_from(true) }

test_equal(1, Fixnum.induced_from(1))
test_equal(1, Fixnum.induced_from(1.0))
test_equal(:hello.to_i, Fixnum.induced_from(:hello))
test_exception(TypeError) { Fixnum.induced_from("hello") }
test_exception(TypeError) { Fixnum.induced_from(true) }
test_exception(RangeError) { Fixnum.induced_from(1000000000000000000000000000000) }

test_equal(1.0, Float.induced_from(1))
test_equal(1.0, Float.induced_from(1.0))
test_exception(TypeError) { Float.induced_from(:hello) }
test_exception(TypeError) { Float.induced_from("hello") }
test_exception(TypeError) { Float.induced_from(true) }
