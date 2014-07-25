require 'test/unit'

class TestNumeric < Test::Unit::TestCase

  class NumericWithSomeMethods < Numeric
    def initialize(value = 0)
      @value = value
    end
    
    def expect(arg)
      @expected_arg = arg
    end

    def check(arg)
      raise "Expected #{@expected_arg.inspect}, but received #{arg.inspect}" unless @expected_arg == arg
    end

    def /(arg) check(arg); 543.21 end
    def %(arg) check(arg); 89 end
    def to_f() @value.to_f end
    def <=>(other) @value <=> other end
    def <(other) @value < other end
    def >(other) @value > other end
    def eql?(other) @value.eql?(other) end
    def to_i() @value.to_i end
  end

  def setup
    @a = Numeric.new
    @b = Numeric.new
  end

  def test_unary_plus
    assert_same @a, +@a
  end

  def test_unary_minus_should_raise_if_self_cannot_be_coerced_to_float
    assert_raises(TypeError) { -@a }
  end

  def test_unary_minus_should_coerce_to_float_and_negate_result 
    assert_equal 123.45, NumericWithSomeMethods.new(123.45).to_f
    assert_equal -123.45, -NumericWithSomeMethods.new(123.45)
  end

  def test_spaceship_should_return_zero_when_comparting_with_self_and_nil_otherwise
    assert_equal 0, @a <=> @a

    assert_nil @a <=> @b
    assert_nil @a <=> 1
    assert_nil @a <=> ""
    assert_nil @a <=> nil
  end

  def test_abs_should_return_self_if_bigger_than_zero_and_result_of_unitary_minus_on_self_otherwise
    assert_raises(ArgumentError) { @a.abs }

    positive = NumericWithSomeMethods.new(1).abs
    assert positive > 0 
    assert_same positive, positive.abs

    negative = NumericWithSomeMethods.new(-2)
    assert negative < 0 
    # unitary minus operator effectively returns -(self.to_f)
    assert_equal 2.0, negative.abs
    assert_equal Float, negative.abs.class
  end

  # ceil
  def test_ceil_should_delegate_to_self_to_f
    assert_equal 124, NumericWithSomeMethods.new(123.49).ceil
    assert_equal -123, NumericWithSomeMethods.new(-123.51).ceil
  end

  def test_ceil_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.ceil }
  end


  def test_coerce_should_copy_argument_and_self_if_both_have_the_same_type
    assert_equal [@b, @a], @a.coerce(@b)
    assert_equal [0.9, 0.1], 0.1.coerce(0.9)
    assert_equal [0, 1], 1.coerce(0)
  end

  def test_coerce_should_call_float_on_self_and_arg_if_they_are_of_different_type
    bignum = 100000000000000000000000
    assert bignum.is_a?(Bignum)
    assert_equal [Float(bignum), 0.0], 0.0.coerce(bignum)
    assert 0.0.coerce(bignum).first.is_a?(Float)
    assert 0.0.coerce(bignum).last.is_a?(Float)

    n = NumericWithSomeMethods.new(123.45)
    assert_equal [1.0, 123.45], n.coerce(1.0)   
  end

  def test_coerce_should_blow_up_if_self_or_arg_cannot_be_converted_to_float_by_kernel_Float
    assert_raises(TypeError) { @a.coerce(1.0) }
    assert_raises(ArgumentError) { 1.coerce("test") }
    assert_raises(TypeError) { 1.coerce(nil) }
    assert_raises(TypeError) { 1.coerce(false) }
  end

  def test_div_should_raise_if_self_doesnt_implement_divison_operator
    assert_raises(NoMethodError) { @a.div(@b) }
  end

  def test_div_should_call_division_method_and_floor_it
    n = NumericWithSomeMethods.new

    # n / anything returns 543.21
    n.expect(:foo)
    assert_equal 543.21, n / :foo
    n.expect(@a)
    assert_equal 543, n.div(@a)
  end
  
  def test_divmod_should_return_result_of_div_and_mod_as_array
    n = NumericWithSomeMethods.new
    n.expect(:foo)
    # n.div(anything) returns 543, n % anything returns 89
    assert_equal [543, 89], n.divmod(:foo)
  end
  
  def test_divmod_should_calculate_div_correctly
    dividends = [-0.58, 0.58, -0.59, 0.59, -0.63, 0.63, -0.66, 0.66, -0.67, 0.67]
    divisor = 1 / 12.0
    expected_divs = [-7, 6, -8, 7, -8, 7, -8, 7, -9, 8]
    dividends.each_with_index { |dividend, idx|
      assert_equal(expected_divs[idx], dividend.divmod(divisor)[0])
    }
  end

  def test_divmod_should_raise_when_self_doesnt_implement_div_or_mod
    assert_raises(NoMethodError) { @a.divmod(@b) }
  end

  def test_eql
    assert_equal true, @a.eql?(@a)
    assert_equal false, @a.eql?(@b)
  end
  
  def test_floor_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.51).floor
    assert_equal -124, NumericWithSomeMethods.new(-123.49).floor
  end

  def test_floor_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.floor }
  end

  def test_initialize_copy_should_raise
    assert_raises(TypeError) { @a.instance_eval("initialize_copy(:foo)") }
  end

  def test_initialize_copy_should_be_private
    assert @a.private_methods.include?("initialize_copy")
    assert_equal false, @a.methods.include?("initialize_copy")
  end

  def test_integer
    assert_equal false, @a.integer?
  end

  def test_modulo_should_raise_if_self_doesnt_have_percent_operator
    assert_raises(NoMethodError) { @a.modulo(@b) }
  end

  def test_modulo_should_call_percent_operator
    n = NumericWithSomeMethods.new
    n.expect(:foo)
    # n % anything returns 89
    assert_equal 89, n.modulo(:foo)
  end

  def test_nonzero_should_check_equality_and_returns_self_or_nil
    assert_same @a, @a.nonzero?
    assert_nil NumericWithSomeMethods.new(0).nonzero?
    one = NumericWithSomeMethods.new(1)
    minus_one = NumericWithSomeMethods.new(1)

    assert_same one, one.nonzero? 
    assert_same minus_one, minus_one.nonzero?
  end

  def test_quo
    assert_raises(NoMethodError) { @a.quo @b }
  end

  def test_fdiv
    assert_equal 0.5, 1.fdiv(2)
  end

  def test_remainder_should_raise_if_self_doesnt_implement_modulo
    assert_raises(NoMethodError) { @a.remainder(@b) }
  end
  
  def test_remainder_should_return_modulo_if_self_and_arg_have_the_same_sign_and_modulo_minus_arg_otherwise_except_when_modulo_is_zero
    # Float doesn't override Numeric#remainder, so that's what we are testng here
    assert_equal 2.0, 5.0.remainder(3)
    assert_equal 2.0, 5.0.remainder(-3)

    assert_equal -2.0, -5.0.remainder(-3)
    assert_equal -2.0, -5.0.remainder(3)

    # special case, was a bug
    assert_equal 0.0, 4.0.remainder(2)
    assert_equal 0.0, 4.0.remainder(-2)
    assert_equal 0.0, -4.0.remainder(2)
    assert_equal 0.0, -4.0.remainder(-2)
  end

  def test_round_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.49).round
    assert_equal 124, NumericWithSomeMethods.new(123.51).round
    assert_equal -123, NumericWithSomeMethods.new(-123.49).round
    assert_equal -124, NumericWithSomeMethods.new(-123.51).round
  end

  def test_round_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.round }
  end

  def test_step
    # Fixnum doesn't override Numeric#step()

    # ends exactly at :to
    a = []
    1.step(5, 2) { |x| a << x }
    assert_equal [1, 3, 5], a

    # ends before :to
    a = []
    1.step(4, 2) { |x| a << x }
    assert_equal [1, 3], a

    # step is too big
    a = []
    1.step(4, 10) { |x| a << x }
    assert_equal [1], a

    # step is zero
    assert_raises(ArgumentError) { 1.step(1, 0) }

    # same to and from
    a = []
    1.step(1, 1) { |x| a << x }
    assert_equal [1], a

    # from less than to, positive step value
    a = []
    1.step(0, 1) { |x| a << x }
    assert_equal [], a

    # from less than to, negative step value
    a = []
    1.step(0, 1) { |x| a << x }
    assert_equal [], a

    # default step value of 1
    a = []
    1.step(3) { |x| a << x }
    assert_equal [1, 2, 3], a

  end

  def test_step_should_raise_if_step_is_zero

  end


  def test_to_int_should_call_to_i_or_raise_if_to_i_is_not_implemented
    assert_equal 123, NumericWithSomeMethods.new(123).to_int
    assert_raises(NoMethodError) { @a.to_int }
  end

  def test_truncate_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.49).truncate
    assert_equal 123, NumericWithSomeMethods.new(123.51).truncate
    assert_equal -123, NumericWithSomeMethods.new(-123.49).truncate
    assert_equal -123, NumericWithSomeMethods.new(-123.51).truncate
  end

  def test_truncate_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.truncate }
  end

  def test_zero_should_check_equality
    assert_equal false, @a.zero?
    assert_equal true, NumericWithSomeMethods.new(0).zero?
    assert_equal false, NumericWithSomeMethods.new(1).zero?
    assert_equal false, NumericWithSomeMethods.new(-1).zero?
  end

end
require 'test/unit'

class TestNumeric < Test::Unit::TestCase

  class NumericWithSomeMethods < Numeric
    def initialize(value = 0)
      @value = value
    end
    
    def expect(arg)
      @expected_arg = arg
    end

    def check(arg)
      raise "Expected #{@expected_arg.inspect}, but received #{arg.inspect}" unless @expected_arg == arg
    end

    def /(arg) check(arg); 543.21 end
    def %(arg) check(arg); 89 end
    def to_f() @value.to_f end
    def <=>(other) @value <=> other end
    def <(other) @value < other end
    def >(other) @value > other end
    def eql?(other) @value.eql?(other) end
    def to_i() @value.to_i end
  end

  def setup
    @a = Numeric.new
    @b = Numeric.new
  end

  def test_unary_plus
    assert_same @a, +@a
  end

  def test_unary_minus_should_raise_if_self_cannot_be_coerced_to_float
    assert_raises(TypeError) { -@a }
  end

  def test_unary_minus_should_coerce_to_float_and_negate_result 
    assert_equal 123.45, NumericWithSomeMethods.new(123.45).to_f
    assert_equal -123.45, -NumericWithSomeMethods.new(123.45)
  end

  def test_spaceship_should_return_zero_when_comparting_with_self_and_nil_otherwise
    assert_equal 0, @a <=> @a

    assert_nil @a <=> @b
    assert_nil @a <=> 1
    assert_nil @a <=> ""
    assert_nil @a <=> nil
  end

  def test_abs_should_return_self_if_bigger_than_zero_and_result_of_unitary_minus_on_self_otherwise
    assert_raises(ArgumentError) { @a.abs }

    positive = NumericWithSomeMethods.new(1).abs
    assert positive > 0 
    assert_same positive, positive.abs

    negative = NumericWithSomeMethods.new(-2)
    assert negative < 0 
    # unitary minus operator effectively returns -(self.to_f)
    assert_equal 2.0, negative.abs
    assert_equal Float, negative.abs.class
  end

  # ceil
  def test_ceil_should_delegate_to_self_to_f
    assert_equal 124, NumericWithSomeMethods.new(123.49).ceil
    assert_equal -123, NumericWithSomeMethods.new(-123.51).ceil
  end

  def test_ceil_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.ceil }
  end


  def test_coerce_should_copy_argument_and_self_if_both_have_the_same_type
    assert_equal [@b, @a], @a.coerce(@b)
    assert_equal [0.9, 0.1], 0.1.coerce(0.9)
    assert_equal [0, 1], 1.coerce(0)
  end

  def test_coerce_should_call_float_on_self_and_arg_if_they_are_of_different_type
    bignum = 100000000000000000000000
    assert bignum.is_a?(Bignum)
    assert_equal [Float(bignum), 0.0], 0.0.coerce(bignum)
    assert 0.0.coerce(bignum).first.is_a?(Float)
    assert 0.0.coerce(bignum).last.is_a?(Float)

    n = NumericWithSomeMethods.new(123.45)
    assert_equal [1.0, 123.45], n.coerce(1.0)   
  end

  def test_coerce_should_blow_up_if_self_or_arg_cannot_be_converted_to_float_by_kernel_Float
    assert_raises(TypeError) { @a.coerce(1.0) }
    assert_raises(ArgumentError) { 1.coerce("test") }
    assert_raises(TypeError) { 1.coerce(nil) }
    assert_raises(TypeError) { 1.coerce(false) }
  end

  def test_div_should_raise_if_self_doesnt_implement_divison_operator
    assert_raises(NoMethodError) { @a.div(@b) }
  end

  def test_div_should_call_division_method_and_floor_it
    n = NumericWithSomeMethods.new

    # n / anything returns 543.21
    n.expect(:foo)
    assert_equal 543.21, n / :foo
    n.expect(@a)
    assert_equal 543, n.div(@a)
  end
  
  def test_divmod_should_return_result_of_div_and_mod_as_array
    n = NumericWithSomeMethods.new
    n.expect(:foo)
    # n.div(anything) returns 543, n % anything returns 89
    assert_equal [543, 89], n.divmod(:foo)
  end

  def test_divmod_should_raise_when_self_doesnt_implement_div_or_mod
    assert_raises(NoMethodError) { @a.divmod(@b) }
  end

  def test_eql
    assert_equal true, @a.eql?(@a)
    assert_equal false, @a.eql?(@b)
  end
  
  def test_floor_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.51).floor
    assert_equal -124, NumericWithSomeMethods.new(-123.49).floor
  end

  def test_floor_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.floor }
  end

  def test_initialize_copy_should_raise
    assert_raises(TypeError) { @a.instance_eval("initialize_copy(:foo)") }
  end

  def test_initialize_copy_should_be_private
    assert @a.private_methods.include?("initialize_copy")
    assert_equal false, @a.methods.include?("initialize_copy")
  end

  def test_integer
    assert_equal false, @a.integer?
  end

  def test_modulo_should_raise_if_self_doesnt_have_percent_operator
    assert_raises(NoMethodError) { @a.modulo(@b) }
  end

  def test_modulo_should_call_percent_operator
    n = NumericWithSomeMethods.new
    n.expect(:foo)
    # n % anything returns 89
    assert_equal 89, n.modulo(:foo)
  end

  def test_nonzero_should_check_equality_and_returns_self_or_nil
    assert_same @a, @a.nonzero?
    assert_nil NumericWithSomeMethods.new(0).nonzero?
    one = NumericWithSomeMethods.new(1)
    minus_one = NumericWithSomeMethods.new(1)

    assert_same one, one.nonzero? 
    assert_same minus_one, minus_one.nonzero?
  end

  def test_quo
    assert_raises(NoMethodError) { @a.quo @b }
  end

  def test_remainder_should_raise_if_self_doesnt_implement_modulo
    assert_raises(NoMethodError) { @a.remainder(@b) }
  end
  
  def test_remainder_should_return_modulo_if_self_and_arg_have_the_same_sign_and_modulo_minus_arg_otherwise_except_when_modulo_is_zero
    # Float doesn't override Numeric#remainder, so that's what we are testng here
    assert_equal 2.0, 5.0.remainder(3)
    assert_equal 2.0, 5.0.remainder(-3)

    assert_equal -2.0, -5.0.remainder(-3)
    assert_equal -2.0, -5.0.remainder(3)

    # special case, was a bug
    assert_equal 0.0, 4.0.remainder(2)
    assert_equal 0.0, 4.0.remainder(-2)
    assert_equal 0.0, -4.0.remainder(2)
    assert_equal 0.0, -4.0.remainder(-2)
  end

  def test_round_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.49).round
    assert_equal 124, NumericWithSomeMethods.new(123.51).round
    assert_equal -123, NumericWithSomeMethods.new(-123.49).round
    assert_equal -124, NumericWithSomeMethods.new(-123.51).round
  end

  def test_round_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.round }
  end

  def test_step
    # Fixnum doesn't override Numeric#step()

    # ends exactly at :to
    a = []
    1.step(5, 2) { |x| a << x }
    assert_equal [1, 3, 5], a

    # ends before :to
    a = []
    1.step(4, 2) { |x| a << x }
    assert_equal [1, 3], a

    # step is too big
    a = []
    1.step(4, 10) { |x| a << x }
    assert_equal [1], a

    # step is zero
    assert_raises(ArgumentError) { 1.step(1, 0) }

    # same to and from
    a = []
    1.step(1, 1) { |x| a << x }
    assert_equal [1], a

    # from less than to, positive step value
    a = []
    1.step(0, 1) { |x| a << x }
    assert_equal [], a

    # from less than to, negative step value
    a = []
    1.step(0, 1) { |x| a << x }
    assert_equal [], a

    # default step value of 1
    a = []
    1.step(3) { |x| a << x }
    assert_equal [1, 2, 3], a

  end

  def test_step_should_raise_if_step_is_zero

  end


  def test_to_int_should_call_to_i_or_raise_if_to_i_is_not_implemented
    assert_equal 123, NumericWithSomeMethods.new(123).to_int
    assert_raises(NoMethodError) { @a.to_int }
  end

  def test_truncate_should_delegate_to_self_to_f
    assert_equal 123, NumericWithSomeMethods.new(123.49).truncate
    assert_equal 123, NumericWithSomeMethods.new(123.51).truncate
    assert_equal -123, NumericWithSomeMethods.new(-123.49).truncate
    assert_equal -123, NumericWithSomeMethods.new(-123.51).truncate
  end

  def test_truncate_should_raise_if_self_doesnt_implement_to_f
    assert_raises(TypeError) { @a.truncate }
  end

  def test_zero_should_check_equality
    assert_equal false, @a.zero?
    assert_equal true, NumericWithSomeMethods.new(0).zero?
    assert_equal false, NumericWithSomeMethods.new(1).zero?
    assert_equal false, NumericWithSomeMethods.new(-1).zero?
  end

end
