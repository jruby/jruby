require File.dirname(__FILE__) + '/../spec_helper'

# %, &, *, **, **, +, -, -, /, <<, <=>, ==, >>, [], ^, abs, coerce,
# div, divmod, eql?, hash, modulo, power!, quo, quo, rdiv, remainder,
# rpower, size, to_f, to_s, |, ~

module B
  def self.sbm(plus=0)
    0x40000000 + plus
  end
end

describe "Bignum instance method" do
  # the smallest bignum in MRI is a different value
  # than the smallest bignum in Rubinius, however,
  # rubinius(smallest_bignum) <= MRI(smallest_bignum)
  # so these specs are written such that they should
  # pass on either target.

  it "% should return self modulo other" do
    a = B.sbm(2_222)
    (a % 5).should == 1
    (a % 2.22).inspect.should == '0.419999905491539'
    (a % B.sbm).should == 2222
  end

  it "% should raise ZeroDivisionError if other is zero and not a Float" do
    a = B.sbm
    should_raise(ZeroDivisionError) { a % 0 }
  end
  
  it "% should NOT raise ZeroDivisionError if other is zero and is a Float" do
    a = B.sbm(5_221)
    b = B.sbm(25)
    (a % 0.0).to_s.should == 'NaN'
    (b % -0.0).to_s.should == 'NaN'
  end
  
  it "& should return self bitwise AND other" do
    a = B.sbm(5)
    (a & 3.4).should == 1
    (a & 52).should == 4
    (a & B.sbm(9921)).should == 1073741825
  end
  
  it "* should return self multiplied by other" do
    a = B.sbm(772)
    (a * 98.6).to_s.should == '105871019965.6'
    (a * 10).to_s.should == '10737425960'
    (a * (a - 40)).to_s.should == '1152923119515115376'
  end
  
  it "** should return self raised to other power" do
    a = B.sbm(47)
    (a ** 5.2).to_s.should == '9.13438731244363e+46'
    (a ** 4).should == 1329228228517658539377366716859970881
  end
  
  it "+ should return self plus other" do
    a = B.sbm(76)
    (a + 4).should == 1073741904
    (a + 4.2).should == 1073741904.2
    (a + B.sbm(3)).should == 2147483727
  end
  
  it "- should return self minus other" do
    a = B.sbm(314)
    (a - 9).should == 1073742129
    (a - 12.57).should == 1073742125.43
    (a - B.sbm(42)).should == 272
  end
  
  it "- should negate self" do
    B.sbm.send(:-@).should == (-1073741824)
    B.sbm(921).send(:-@).should == (-1073742745)
  end
  
  it "/ should return self divided by other" do
    a = B.sbm(88)
    (a / 4).to_s.should == '268435478'
    (a / 16.2).to_s.should == '66280364.9382716'
    (a / B.sbm(2)).to_s.should == '1'
  end
  
  it "/ should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { B.sbm / 0 }
  end
  
  it "/ should NOT raise ZeroDivisionError if other is zero and is a Float" do
    a = B.sbm(5_221)
    b = B.sbm(25)
    (a / 0.0).to_s.should == 'Infinity'
    (b / -0.0).to_s.should == '-Infinity'
  end
  
  it "< should return true if self is less than other" do
    a = B.sbm(32)
    (a < 2).should == false
    (a < 456_912.410).should == false
    (a < B.sbm(42)).should == true
  end
  
  it "<= should return true if self is less than or equal to other" do
    a = B.sbm(39)
    (a <= a).should == true
    (a <= 4).should == false
    (a <= 45.8).should == false
  end
  
  it "<=> should return -1, 0, 1 when self is less than, equal, or greater than other" do
    a = B.sbm(96)
    (a <=> B.sbm(196)).should == -1
    (a <=> a).should == 0
    (a <=> 4.5).should == 1
  end
  
  it "== should true if self has the same value as other" do
    a = B.sbm(67)
    (a == a).should == true
    (a == 5.4).should == false
    (a == 121).should == false
  end

  specify "== calls 'other == self' if coercion fails" do
    class X; def ==(other); B.sbm(123) == other; end; end

    (B.sbm(120) == X.new).should == false
    (B.sbm(123) == X.new).should == true
  end
  
  it "> should return true if self is greater than other" do
    a = B.sbm(732)
    (a > (a + 500)).should == false
    (a > 14.6).should == true
    (a > (B.sbm - 1)).should == true
  end
  
  it ">= should return true if self is greater than or equal to other" do
    a = B.sbm(14)
    (a >= a).should == true
    (a >= (a + 2)).should == false
    (a >= 5664.2).should == true
    (a >= 4).should == true
  end

  it "abs should return the absolute value" do
    B.sbm(39).abs.should == 1073741863
    (-B.sbm(18)).abs.should == 1073741842
  end
  
  it "<< should return self shifted left other bits" do
    a = B.sbm(9)
    (a << 4).should == 17179869328
    (a << 1.5).should == 2147483666
    (a << 9).should == 549755818496
  end
  
  it ">> should return self shifted right other bits" do
    a = B.sbm(90812)
    (a >> 3.45).should == 134229079
    (a >> 2).should == 268458159
    (a >> 21).should == 512
  end
  
  it "[] should return the nth bit in the binary representation of self" do
    a = B.sbm(4996)
    a[2].should == 1
    a[9.2].should == 1
    a[21].should == 0
  end
  
  it "^ should return self bitwise EXCLUSIVE OR other" do
    a = B.sbm(18)
    (a ^ 2).should == 1073741840
    (a ^ a).should == 0
    (a ^ 14.5).should == 1073741852
  end
  
  it "coerce should return [other, self] both as Bignum if other is an Integer" do
    a = B.sbm
    a.coerce(2).should == [2, 1073741824]
    a.coerce(B.sbm(701)).should == [1073742525, 1073741824]
  end
  
  it "div should be a synonym for /" do
    a = B.sbm(41622)
    a.div(4).should == (a / 4)
    a.div(16.2).to_s.should == (a / 16.2).to_s
    a.div(B.sbm(2)).should == (a / (B.sbm(2)))
  end
  
  it "div should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { B.sbm.div(0) }
  end
  
  it "div should NOT raise ZeroDivisionError if other is zero and is a Float" do
    B.sbm(5_221).div(0.0)#.to_s.should == 'Infinity'
    B.sbm(25).div(-0.0)#.to_s.should == '-Infinity'
  end
  
  it "divmod should return an [quotient, modulus] from dividing self by other" do
    a = B.sbm(55)
    a.divmod(5).inspect.should == '[214748375, 4]'
    a.divmod(15.2).inspect.should == '[70640913, 1.40000005019339]'
    a.divmod(a + 9).inspect.should == '[0, 1073741879]'
  end
  
  it "divmod should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { B.sbm(2).divmod(0) }
  end
  
  it "divmod should raise FloatDomainError if other is zero and is a Float" do
    should_raise(FloatDomainError) { B.sbm(9).divmod(0.0) }
  end

  it "eql? should return true if other is a Bignum with the same value" do
    a = B.sbm(13)
    a.eql?(B.sbm(13)).should == true
    a.eql?(2).should == false
    a.eql?(3.14).should == false
  end
  
  it "hash should be provided" do
    B.sbm.respond_to?(:hash).should == true
  end
  
  it "modulo should be a synonym for %" do
    a = B.sbm(2_222)
    a.modulo(5).should == 1
    a.modulo(2.22).to_s.should == '0.419999905491539'
    a.modulo(B.sbm).should == 2222
  end
  
  it "% should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { B.sbm.modulo(0) }
  end
  
  it "modulo should NOT raise ZeroDivisionError if other is zero and is a Float" do
    a = B.sbm(5_221)
    b = B.sbm(25)
    a.modulo(0.0).to_s.should == 'NaN'
    b.modulo(-0.0).to_s.should == 'NaN'
  end
  
  it "quo should return the floating-point result of self divided by other" do
    a = B.sbm(3)
    a.quo(2.5).to_s.should == '429496730.8'
    a.quo(13).to_s.should == '82595525.1538462'
    a.quo(B.sbm).to_s.should == '1.00000000279397'
  end

  it "quo should NOT raise an exception when other is zero" do
    # a.quo(0) should also not raise (i.e works in irb and from a file),
    # but fails here.
    a = B.sbm(91)
    b = B.sbm(28)
    a.quo(0.0).to_s.should == "Infinity"
    b.quo(-0.0).to_s.should == "-Infinity"
  end
  
  it "remainder should return the remainder of dividing self by other" do
    a = B.sbm(79)
    a.remainder(2).should == 1
    a.remainder(97.345).to_s.should == '75.16000001254'
    a.remainder(B.sbm).should == 79
  end
  
  it "remainder should raise ZeroDivisionError if other is zero and not a Float" do
    should_raise(ZeroDivisionError) { B.sbm(66).remainder(0) }
  end
  
  it "remainder should NOT raise ZeroDivisionError if other is zero and is a Float" do
    a = B.sbm(7)
    b = B.sbm(32)
    a.remainder(0.0).to_s.should == 'NaN'
    b.remainder(-0.0).to_s.should == 'NaN'
  end    
  
  it "size should be provided" do
    B.sbm.respond_to?(:size).should == true
  end
  
  it "to_f should return self converted to Float" do
    B.sbm(2).to_f.should == 1073741826.0
    (-B.sbm(99)).to_f.should == -1073741923.0
    B.sbm(14).to_f.should == 1073741838.0
  end
  
  it "to_s should return a string representation of self" do
    B.sbm(9).to_s.should == "1073741833"
    B.sbm.to_s.should == "1073741824"
    (-B.sbm(675)).to_s.should == "-1073742499"
  end
  
  it "| should return self bitwise OR other" do
    a = B.sbm(11)
    (a | 2).should == 1073741835
    (a | 9.9).should == 1073741835
    (a | B.sbm).should == 1073741835
  end
  
  it "~ should return self bitwise inverted" do
    (~B.sbm(48)).should == -1073741873
    (~(-B.sbm(21))).should == 1073741844
    (~B.sbm(1)).should == -1073741826
  end  

  it "coerce should return [Bignum, Bignum] if other is a Bignum" do
    a = 0xffffffff.coerce(0xffffffff)
    a.should == [4294967295, 4294967295]
    a.collect { |i| i.class }.should == [Bignum, Bignum]
  end
end
