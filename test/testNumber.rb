require 'test/minirunit'
test_check "Test Number"
test_ok(25.eql?(25))
test_ok(10000.eql?(10000))
test_ok(20.between?(15, 25))
test_ok(!(20.between?(10, 15)))
test_ok(78.chr == 'N')

n1 = 0b0101
n2 = 0b1100
test_equal(0,      n1 & 0)
test_equal(0b0100, n1 & n2)
test_equal(n1,     n1 & -1)
test_equal(1,      3 & 10000000000000000000000000000000001)

test_equal(1, 10000000000000000000000000000000001 & 3)
test_equal(10000000000000000000000000000000001,
           10000000000000000000000000000000001 & 10000000000000000000000000000000001)
test_equal(-10000000000000000000000000000000002,
           ~10000000000000000000000000000000001)

test_equal(0, 0[0])
test_equal([0,1,1], [3[2], 3[1], 3[0]])
test_equal([1,1,0], [-2[2], -2[1], -2[0]])
test_equal(1, -2[1000])

test_equal(1, (2 ** 100)[100])
test_equal(0, (2 ** 100)[101])
test_equal(0, (2 ** 100)[0])

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

# You can't freeze Fixnums...
test_ok(! 303.freeze.frozen?)
test_ok(! 303.taint.tainted?)

# ... but you can apparently freeze Bignums and Floats.
test_ok(1000000000000000000000000000000.freeze.frozen?)
test_ok(1000000000000000000000000000000.taint.tainted?)
test_ok(1.337.freeze.frozen?)
test_ok(1.337.taint.tainted?)

test_ok(! nil.taint.tainted?)
test_ok(! nil.freeze.frozen?)

test_equal(0, 1 ^ 1)
test_equal(1005, 1000 ^ 5)
test_equal(0, (10 ** 70) ^ (10 ** 70))
test_equal(1 + (10 ** 70), (10 ** 70) ^ 1)
test_equal(10 ** 70, (10 ** 70) ^ 0)

test_equal(-1, ~0)
test_equal(-2, ~1)
test_equal(1, 1 | 1)
test_equal(1, 1 | 0)
test_equal(10001, 10000 | 1)
test_equal(1 + (10 ** 70), (10 ** 70) | 1)
test_equal(1 + (10 ** 70), 1 | (10 ** 70))
test_equal(10 ** 70, (10 ** 70) | (10 ** 70))

test_equal(20, (256**20 - 1).size)
test_equal(40, (256**40 - 1).size)

test_exception(TypeError) { 20['x'] }

test_equal(1, 0.object_id)
test_equal(3, 1.object_id)
test_equal(5, 2.object_id)
test_equal(9, 4.object_id)
test_equal(2, 5 / 2)
test_equal(2, 5.div(2))

test_equal(1.object_id, 1.__id__) # Testing lexer's handling of numbers here

test_exception(NameError) { Integer.new }
test_exception(NameError) { Fixnum.new }
test_exception(NameError) { Float.new }

x = 1234
test_exception(TypeError) {
  def x.+(other)
    "fools"
  end
}

test_equal("8", 8.to_s)
test_equal("10", 8.to_s(8))

class IntClass
  def to_int; 8; end
end

test_equal("10", 8.to_s(IntClass.new))