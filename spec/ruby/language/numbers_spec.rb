require_relative '../spec_helper'

describe "A number literal" do

  it "can be a sequence of decimal digits" do
    435.should == 435
  end

  it "can have '_' characters between digits" do
    4_3_5_7.should == 4357
  end

  it "cannot have a leading underscore" do
    -> { eval("_4_2") }.should.raise(NameError)
  end

  it "can have a decimal point" do
    4.35.should == 4.35
  end

  it "must have a digit before the decimal point" do
    0.75.should == 0.75
    -> { eval(".75")  }.should.raise(SyntaxError)
    -> { eval("-.75") }.should.raise(SyntaxError)
  end

  it "can have an exponent" do
    1.2e-3.should == 0.0012
  end

  it "can be a sequence of hexadecimal digits with a leading '0x'" do
    0xffff.should == 65535
  end

  it "can be a sequence of binary digits with a leading '0x'" do
    0b01011.should == 11
  end

  it "can be a sequence of octal digits with a leading '0'" do
    0377.should == 255
  end

  it "can be an integer literal with trailing 'r' to represent a Rational" do
    eval('3r').should == Rational(3, 1)
    eval('-3r').should == Rational(-3, 1)
  end

  it "can be an float literal with trailing 'r' to represent a Rational in a canonical form" do
    eval('1.0r').should == Rational(1, 1)
  end

  it "can be a float literal with trailing 'r' to represent a Rational" do
    eval('0.0174532925199432957r').should == Rational(174532925199432957, 10000000000000000000)
  end

  it "can be a bignum literal with trailing 'r' to represent a Rational" do
    eval('1111111111111111111111111111111111111111111111r').should == Rational(1111111111111111111111111111111111111111111111, 1)
    eval('-1111111111111111111111111111111111111111111111r').should == Rational(-1111111111111111111111111111111111111111111111, 1)
  end

  it "can be a decimal literal with trailing 'r' to represent a Rational" do
    eval('0.3r').should == Rational(3, 10)
    eval('-0.3r').should == Rational(-3, 10)
  end

  it "can be a hexadecimal literal with trailing 'r' to represent a Rational" do
    eval('0xffr').should == Rational(255, 1)
    eval('-0xffr').should == Rational(-255, 1)
  end

  it "can be an octal literal with trailing 'r' to represent a Rational"  do
    eval('042r').should == Rational(34, 1)
    eval('-042r').should == Rational(-34, 1)
  end

  it "can be a binary literal with trailing 'r' to represent a Rational" do
    eval('0b1111r').should == Rational(15, 1)
    eval('-0b1111r').should == Rational(-15, 1)
  end

  it "can be an integer literal with trailing 'i' to represent a Complex" do
    eval('5i').should == Complex(0, 5)
    eval('-5i').should == Complex(0, -5)
  end

  it "can be a decimal literal with trailing 'i' to represent a Complex" do
    eval('0.6i').should == Complex(0, 0.6)
    eval('-0.6i').should == Complex(0, -0.6)
  end

  # Warm the literal blocks before comparing results, since JIT compilation may
  # replace the interpreter's literal cache after an initial invocation.
  it "reuses the same Complex object when an integer imaginary literal is evaluated repeatedly" do
    literal = -> { 5i }
    2.times { literal.call }
    first = literal.call
    second = literal.call
    first.should.equal?(second)
    first.should.frozen?
  end

  it "reuses the same Complex object when a float imaginary literal is evaluated repeatedly" do
    literal = -> { 0.6i }
    2.times { literal.call }
    first = literal.call
    second = literal.call
    first.should.equal?(second)
    first.should.frozen?
  end

  it "reuses the same Complex object when a bignum imaginary literal is evaluated repeatedly" do
    literal = -> { 18446744073709551616i }
    2.times { literal.call }
    first = literal.call
    second = literal.call
    first.should.equal?(second)
    first.should.frozen?
  end

  it "reuses the same Complex object when a rational imaginary literal is evaluated repeatedly" do
    literals = [-> { 5ri }, -> { 0.6ri }, -> { 18446744073709551616ri }, -> { 0.00000000000000000001ri }]
    literals.each do |literal|
      2.times { literal.call }
      first = literal.call
      second = literal.call
      first.should.equal?(second)
      first.should.frozen?
    end
  end

  it "reuses the same Rational object when a rational literal is evaluated repeatedly" do
    literals = [-> { 5r }, -> { 0.6r }, -> { 18446744073709551616r }, -> { 0.00000000000000000001r }]
    literals.each do |literal|
      2.times { literal.call }
      first = literal.call
      second = literal.call
      first.should.equal?(second)
      first.should.frozen?
    end
  end

  it "normalizes and reuses Rational and Complex literals with large decimal components" do
    literals = [
      [-> { 0.5000000000000000000000000000r }, -> { 0.5000000000000000000000000000ri }, 1, 2],
      [-> { 0.00000000000000000001r }, -> { 0.00000000000000000001ri }, 1, 100000000000000000000],
      [-> { 100000000000000000000.5000000000000000000000r }, -> { 100000000000000000000.5000000000000000000000ri }, 200000000000000000001, 2],
      [-> { 1.23456789012345678901r }, -> { 1.23456789012345678901ri }, 123456789012345678901, 100000000000000000000]
    ]
    literals.each do |rational, complex, numerator, denominator|
      2.times do
        rational.call
        complex.call
      end
      first_rational = rational.call
      second_rational = rational.call
      first_complex = complex.call
      second_complex = complex.call
      first_rational.should.equal?(second_rational)
      first_complex.should.equal?(second_complex)
      [first_rational.numerator, first_rational.denominator].should == [numerator, denominator]
      [first_complex.imaginary.numerator, first_complex.imaginary.denominator].should == [numerator, denominator]
    end
  end

  it "can be a hexadecimal literal with trailing 'i' to represent a Complex" do
    eval('0xffi').should == Complex(0, 255)
    eval('-0xffi').should == Complex(0, -255)
  end

  it "can be a octal literal with trailing 'i' to represent a Complex" do
    eval("042i").should == Complex(0, 34)
    eval("-042i").should == Complex(0, -34)
  end

  it "can be a binary literal with trailing 'i' to represent a Complex" do
    eval('0b1110i').should == Complex(0, 14)
    eval('-0b1110i').should == Complex(0, -14)
  end
end
