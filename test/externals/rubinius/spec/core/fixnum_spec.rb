require File.dirname(__FILE__) + '/../spec_helper'

# %, &, *, **, +, -, -@, /, <, <<, <=, <=>, ==, >, >=, >>, [], ^,
# abs, div, divmod, id2name, modulo, power!, quo, rdiv, rpower,
# size, to_f, to_s, to_sym, zero?, |, ~

context "Fixnum instance method" do
  
  specify "% should return self modulo other" do
    (451 % 2).should == 1
    (32 % 16).should == 0
  end

  specify "% should coerce fixnum and return self modulo other" do
    (93 % 3.2).to_s.should == '0.199999999999995'
    (120 % -4.5).to_s.should == '-1.5'
  end
  
  specify "% should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { 1 % 0 }
  end
  
  specify "% should NOT raise ZeroDivisionError if other is zero and is a Float" do
    1.modulo(0.0).to_s.should == 'NaN'
    1.modulo(-0.0).to_s.should == 'NaN'
  end
  
  specify "& should return self bitwise AND other" do
    (2010 & 5).should == 0
    (65535 & 1).should == 1
  end
  
  specify "& should coerce fixnum and return self bitwise AND other" do
    (256 & 16).should == 0
    (0xffff & 0xffffffff).should == 65535
  end

  specify "* should return self multiplied by other" do
    (4923 * 2).should == 9846
    (1342177 * 800).should == 1073741600
    (65536 * 65536).should == 4294967296
  end

  specify "* should coerce fixnum and return self multiplied by other" do 
    (256 * 0xffffffff).should == 1099511627520
    (6712 * 0.25).to_s.should == '1678.0'
  end

  specify "** should return self raise to the other power" do
    (2 ** 3).should == 8
  end

  specify "** should coerce fixnum and return self raise to the other power" do
    (5 ** -1).to_f.to_s.should == '0.2'
    (9 ** 0.5).to_s.should == '3.0'
  end

  specify "** should seamlessly progress into the bignum range" do
    (0..40).map {|x| 2**x}.should == [
      1, 2, 4, 8, 16, 32, 64, 128,
      256, 512, 1024, 2048, 4096, 8192, 16384, 32768,
      65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608,
      16777216, 33554432, 67108864, 134217728, 268435456, 536870912,
      1073741824, 2147483648, 4294967296, 8589934592, 17179869184,
      34359738368, 68719476736, 137438953472, 274877906944, 549755813888,
      1099511627776
    ]
  end

  specify "+ should return self plus other" do
    (491 + 2).should == 493
    (90210 + 10).should == 90220
  end
  
  specify "+ should coerce fixnum and return self plus other" do
    (9 + 0xffffffff).should == 4294967304
    (1001 + 5.219).to_s.should == '1006.219'
  end

  specify "- should return self minus other fixnum" do
    (5 - 10).should == -5
    (9237212 - 5_280).should == 9231932
  end
  
  specify "- should coerce fixnum and return self minus other fixnum" do
    (2_560_496 - 0xfffffffff).should == -68716916239
    (781 - 0.5).to_s.should == '780.5'
  end

  specify "-@ should negate self" do
    2.send(:-@).should == -2
    -2.should == -2
    -268435455.should == -268435455
    (--5).should == 5
    -8.send(:-@).should == 8
  end

  specify "/ should return self divided by other" do
    (2 / 2).should == 1
    (3 / 2).should == 1
  end
  
  specify "/ should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { 1 / 0 }
  end
  
  specify "/ should NOT raise ZeroDivisionError if other is zero and is a Float" do
    (1 / 0.0).to_s.should == 'Infinity'
    (-1 / 0.0).to_s.should == '-Infinity'
  end

  specify "/ should coerce fixnum and return self divided by other" do
    (-1 / 50.4).to_s.should == "-0.0198412698412698"
    (1 / 0xffffffff).should == 0
  end
  
  specify "< should return true if self is less than other" do
    (-1 < 0).should == true
  end
  
  specify "< should corece fixnum and return true if self is less than other" do
    (3 < -5.2).should == false
    (9 < 0xffffffff).should == true
  end

  specify "<= should return true if self is less than or equal to other" do
    (2 <= 2).should == true
  end

  specify "<= should coerce fixnum and return true if self is less than or equal to other" do
    (7 <= 5.4).should == false
    (11 <= 0xffffffff).should == true
  end
  
  specify "<=> should return -1, 0, 1 when self is less than, equal, or greater than other" do
    # 3 comparisions causes rubinius to crash, 2 comparisions work fine 
    # and each comparision tested singularly is ok
    (-3 <=> -1).should == -1
    (954 <=> 954).should == 0
    (496 <=> 5).should == 1
  end

  specify "<=> should coerce fixnum and return -1, 0, 1 when self is less than, equal, or greater than other" do
    (-1 <=> 0xffffffff).should == -1
    (954 <=> 954.0).should == 0
    (496 <=> 5).should == 1
  end
  
  specify "== should true if self has the same value as other" do
    (1 == 1).should == true
    (9 == 5).should == false
  end
  
  specify "== should coerce fixnum and return true if self has the same value as other" do
    (1 == 1.0).should == true
    (2 == 0xffffffff).should == false
  end

  specify "== calls 'other == self' if coercion fails" do
    class X; def ==(other); 2 == other; end; end

    (1 == X.new).should == false
    (2 == X.new).should == true
  end
  
  specify "> should return true if self is greater than other" do
    (-500 > -600).should == true
    (13 > 2).should == true
    (1 > 5).should == false
  end
  
  specify ">= should return true if self is greater than or equal to other" do
    (-50 >= -50).should == true
    (14 >= 2).should == true
    (900 >= 901).should == false
  end

  specify ">= should coerce fixnum and return true if self is greater than or equal to other" do
    (-50 >= -50).should == true
    (14 >= 2.5).should == true
    (900 >= 0xffffffff).should == false
  end

  specify "abs should return the absolute value" do
    0.abs.should == 0
    -2.abs.should == 2
    5.abs.should == 5
  end
  
  specify "<< should return self shifted left other bits" do
    (7 << 2).should == 28
    (9 << 4).should == 144
  end
  
  specify "<< should coerce result on overflow and return self shifted left other bits" do
    (9 << 4.2).should == 144
    (6 << 0xff).should == 347376267711948586270712955026063723559809953996921692118372752023739388919808
  end

  specify ">> should return self shifted right other bits" do
    (7 >> 1).should == 3
    (4095 >> 3).should == 511
    (9245278 >> 1).should == 4622639
  end
  
  specify ">> should coerce other to fixnum and return self shifted right other bits" do
    (7 >> 1.5).should == 3
    (0xfff >> 3).should == 511
    (9_245_278 >> 1).should == 4622639
  end

  specify "[] should return the nth bit in the binary representation of self" do
    2[3].should == 0
    15[1].should == 1
  end
  
  specify "[] should coerce the bit and return the nth bit in the binary representation of self" do
    2[3].should == 0
    15[1.3].should == 1
    3[0xffffffff].should == 0
  end

  specify "^ should return self bitwise EXCLUSIVE OR other" do
    (3 ^ 5).should == 6
    (-2 ^ -255).should == 255
  end
  
  specify "^ should coerce fixnum and return self bitwise EXCLUSIVE OR other" do
    (-7 ^ 15.2).should == -10
    (5 ^ 0xffffffff).should == 4294967290
  end

  specify "div should return self divided by other as an Integer" do
    2.div(2).should == 1
    1.div(2).should == 0
  end
  
  specify "div should coerce fixnum and return self divided by other as an Integer" do
    1.div(0.2).should == 5
    -1.div(50.4).should == -1
    1.div(0xffffffff).should == 0
  end
  
  specify "div should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { 1.div(0) }
  end
  
  specify "div should raise FloatDomainError if other is zero and is a Float" do
    should_raise(FloatDomainError) { 1.div(0.0) }
  end

  specify "divmod should return an [quotient, modulus] from dividing self by other" do
    1.divmod(2).should == [0, 1]
    -5.divmod(3).should == [-2, 1]
  end
  
  specify "divmod should coerce fixnum (if required) and return an [quotient, modulus] from dividing self by other" do
    1.divmod(2.0).should == [0, 1.0]
    200.divmod(0xffffffff).should == [0, 200]
  end
  
  specify "divmod should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { 1.divmod(0) }
  end
  
  specify "divmod should raise FloatDomainError if other is zero and is a Float" do
    should_raise(FloatDomainError) { 1.divmod(0.0) }
  end

  specify "id2name should return the string name of the object whose symbol ID is self" do
    a = :@sym
    b = :@ruby
    c = :@rubinius
    a.to_i.id2name.should == '@sym'
    b.to_i.id2name.should == '@ruby'
    c.to_i.id2name.should == '@rubinius'
  end
  
  specify "modulo should be a synonym for %" do
    451.modulo(2).should == 1
    93.modulo(3.2).to_s.should == '0.199999999999995'
    120.modulo(-4.5).to_s.should == '-1.5'
  end
  
  specify "modulo should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { 1.modulo(0) }
  end
  
  specify "modulo should NOT raise ZeroDivisionError if other is zero and is a Float" do
    1.modulo(0.0).to_s.should == 'NaN'
    1.modulo(-0.0).to_s.should == 'NaN'
  end
  
  specify "quo should return the floating-point result of self divided by other" do
    # the to_f is required because RSpec (I'm assuming) requires 'rational'
    2.quo(2.5).to_s.should == '0.8'
    5.quo(2).to_f.to_s.should == '2.5'
    45.quo(0xffffffff).to_f.to_s.should == '1.04773789668636e-08'
  end
  
  specify "quo should NOT raise an exception when other is zero" do
    # 1.quo(0) should also not raise (i.e works in irb and from a file),
    # but fails here.
    1.quo(0.0).to_s.should == 'Infinity'
    1.quo(-0.0).to_s.should == '-Infinity'
  end
  
  specify "size should be provided" do
    1.respond_to?(:size).should == true
  end
  
  specify "to_f should return self converted to Float" do
    0.to_f.to_s.should == '0.0'
    -500.to_f.to_s.should == '-500.0'
    9_641_278.to_f.to_s.should == '9641278.0'
  end
  
  specify "to_s should return a string representation of self" do
    255.to_s.should == '255'
    3.to_s.should == '3'
    0.to_s.should == '0'
    -9002.to_s.should == '-9002'
  end
  
  specify "to_sym should return the symbol whose integer value is self" do
    a = :fred.to_i
    b = :wilma.to_i
    c = :bambam.to_i
    a.to_sym.should == :fred
    b.to_sym.should == :wilma
    c.to_sym.should == :bambam
  end
  
  specify "zero? should return true if self is 0" do
    0.zero?.should == true
    -1.zero?.should == false
    1.zero?.should == false
  end
  
  specify "| should return self bitwise OR other" do
    (1 | 0).should == 1
    (5 | 4).should == 5
    (5 | 6).should == 7
    (248 | 4096).should == 4344
  end
  
  specify "~ should return self bitwise inverted" do
    (~0).should == -1
    (~1221).should == -1222
    (~-599).should == 598
    (~-2).should == 1
  end
  
  specify "coerce should return [Fixnum, Fixnum] if other is a Fixnum" do
    1.coerce(2).should == [2, 1]
    1.coerce(2).collect { |i| i.class }.should == [Fixnum, Fixnum]
  end
  
  specify "coerce should return [Float, Float] if other is not a Bignum or Fixnum" do
    a = 1.coerce("2")
    b = 1.coerce(1.5)
    a.should == [2.0, 1.0]
    a.collect { |i| i.class }.should == [Float, Float]
    b.should == [1.5, 1.0]
    b.collect { |i| i.class }.should == [Float, Float]
  end
end
