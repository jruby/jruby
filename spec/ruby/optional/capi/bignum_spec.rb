require File.expand_path('../spec_helper', __FILE__)

load_extension("bignum")

def ensure_bignum(n)
  0xffff_ffff_ffff_ffff_ffff.coerce(n)[0]
end

describe "CApiBignumSpecs" do
  before :each do
    @s = CApiBignumSpecs.new
    @max_long = ensure_bignum(2**(0.size * 8 - 1) - 1)
    @min_long = ensure_bignum(-@max_long - 1)
    @max_ulong = ensure_bignum(2**(0.size * 8) - 1)
  end

  describe "rb_big2long" do
    it "converts a Bignum" do
      @s.rb_big2long(@max_long).should == @max_long
      @s.rb_big2long(@min_long).should == @min_long
      @s.rb_big2long(ensure_bignum(0)).should == 0
    end

    it "raises RangeError if passed Bignum overflow long" do
      lambda { @s.rb_big2long(ensure_bignum(@max_long + 1)) }.should raise_error(RangeError)
      lambda { @s.rb_big2long(ensure_bignum(@min_long - 1)) }.should raise_error(RangeError)
    end
  end

  describe "rb_big2ll" do
    it "converts a Bignum" do
      @s.rb_big2ll(@max_long).should == @max_long
      @s.rb_big2ll(@min_long).should == @min_long
      @s.rb_big2ll(ensure_bignum(0)).should == 0
    end

    it "raises RangeError if passed Bignum overflow long" do
      lambda { @s.rb_big2ll(ensure_bignum(@max_long << 40)) }.should raise_error(RangeError)
      lambda { @s.rb_big2ll(ensure_bignum(@min_long << 40)) }.should raise_error(RangeError)
    end
  end

  describe "rb_big2ulong" do
    it "converts a Bignum" do
      @s.rb_big2ulong(@max_ulong).should == @max_ulong
      @s.rb_big2long(ensure_bignum(0)).should == 0
    end

    it "wraps around if passed a negative bignum" do
      @s.rb_big2ulong(ensure_bignum(-1)).should == @max_ulong
      @s.rb_big2ulong(ensure_bignum(@min_long + 1)).should == -(@min_long - 1)
    end

    it "raises RangeError if passed Bignum overflow long" do
      lambda { @s.rb_big2ulong(ensure_bignum(@max_ulong + 1)) }.should raise_error(RangeError)
      lambda { @s.rb_big2ulong(ensure_bignum(@min_long - 1)) }.should raise_error(RangeError)
    end

    ruby_bug "#", "1.9.3" do
      it "wraps around if passed a negative bignum" do
        @s.rb_big2ulong(ensure_bignum(@min_long)).should == -(@min_long)
      end
    end
  end

  describe "rb_big2dbl" do
    it "converts a Bignum to a double value" do
      @s.rb_big2dbl(ensure_bignum(1)).eql?(1.0).should == true
      @s.rb_big2dbl(ensure_bignum(Float::MAX.to_i)).eql?(Float::MAX).should == true
    end

    it "returns Infinity if the number is too big for a double" do
      huge_bignum = ensure_bignum(Float::MAX.to_i * 2)
      @s.rb_big2dbl(huge_bignum).should == infinity_value
    end

    ruby_bug "#3362", "1.8.7.357" do
      it "returns -Infinity if the number is negative and too big for a double" do
        huge_bignum = -ensure_bignum(Float::MAX.to_i * 2)
        @s.rb_big2dbl(huge_bignum).should == -infinity_value
      end
    end
  end

  describe "rb_big2str" do

    it "converts a Bignum to a string with base 10" do
      @s.rb_big2str(ensure_bignum(1), 10).eql?("1").should == true
      @s.rb_big2str(ensure_bignum(4611686018427387904), 10).eql?("4611686018427387904").should == true
    end

    it "converts a Bignum to a string with a different base" do
      @s.rb_big2str(ensure_bignum(1), 16).eql?("1").should == true
      @s.rb_big2str(ensure_bignum(4611686018427387904), 16).eql?("4000000000000000").should == true
    end
  end

  ruby_version_is "1.8.7" do
    describe "RBIGNUM_SIGN" do
      it "returns C true if the Bignum has a positive sign" do
        @s.RBIGNUM_SIGN(bignum_value()).should be_true
      end

      it "retuns C false if the Bignum has a negative sign" do
        @s.RBIGNUM_SIGN(-bignum_value()).should be_false
      end
    end

    describe "RBIGNUM_POSITIVE_P" do
      it "returns C true if the Bignum has a positive sign" do
        @s.RBIGNUM_POSITIVE_P(bignum_value()).should be_true
      end

      it "retuns C false if the Bignum has a negative sign" do
        @s.RBIGNUM_POSITIVE_P(-bignum_value()).should be_false
      end
    end

    describe "RBIGNUM_NEGATIVE_P" do
      it "returns C false if the Bignum has a positive sign" do
        @s.RBIGNUM_NEGATIVE_P(bignum_value()).should be_false
      end

      it "retuns C true if the Bignum has a negative sign" do
        @s.RBIGNUM_NEGATIVE_P(-bignum_value()).should be_true
      end
    end
  end
end
