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

# Duck refers to duck typing.
# If you include this module and override ==, implement it so that you FIRST check if
# the other object is of the same class --> if so, compare equality as normal. If not, call
# operands = self.coerce(other) and return operands[0] == operands[1]

module DuckNumber
  include Comparable

  # the symbol representing the method which returns the number the object corresponds to
  NUMBER_METHOD = :score
  # override this if you want to use some other method to return the numeric presentation
  def get_number_method
    NUMBER_METHOD
  end
  private :get_number_method

  def coerce(other)
    case other
      when Integer
        [other, self.score]
      when Float
        [other, Float(self.score)]
      when DuckNumber
        [other.score, self.score]
      else
        raise "can not convert #{self.class.to_s} to #{other.class.to_s}"
    end
  end

  def to_int
    self.send(get_number_method)
  end

  def -@
    -self.to_int
  end

  def +@
    self.to_int
  end

  [:+, :-, :/, :%, :*, :<=>].each { |operator|
    module_eval <<-END_DUCK
      def #{operator.to_s}(other)
        operands = self.coerce(other)
        operands[1] #{operator.to_s} operands[0]
      end
    END_DUCK
  }
end


class DuckNumberImpl
  include DuckNumber
  attr_accessor :score, :foo
end

SCORE = 3
@duck = DuckNumberImpl.new
@duck.score = SCORE
@duck.foo   = 12345

test_equal(SCORE,  +@duck)
test_equal(-SCORE, -@duck)

test_equal(SCORE + 1, 1 + @duck)
test_equal(SCORE + 1, @duck + 1)

test_equal(SCORE - 1, @duck - 1)
test_equal(1 - SCORE, -@duck + 1)
test_equal(1 - SCORE, 1 - @duck)

test_equal(SCORE + SCORE, @duck + @duck)

test_ok(@duck > 1)
test_ok(1 < @duck)
test_ok(@duck >= 1)
test_ok(1 <= @duck)    

# test Numeric#to_int
test_equal(1234, 1234.to_int)
