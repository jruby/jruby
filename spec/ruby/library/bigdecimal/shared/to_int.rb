require 'bigdecimal'

describe :bigdecimal_to_int , :shared => true do
  ruby_version_is "" ... "1.9" do
    ruby_bug "fixed_in_ruby_1_8_7@25799", "1.8.7.202" do
      it "returns nil if BigDecimal is infinity or NaN" do
        BigDecimal("Infinity").send(@method).should == nil
        BigDecimal("NaN").send(@method).should == nil
      end
    end
  end

  ruby_version_is "1.9" do
    it "raises FloatDomainError if BigDecimal is infinity or NaN" do
      lambda { BigDecimal("Infinity").send(@method) }.should raise_error(FloatDomainError)
      lambda { BigDecimal("NaN").send(@method) }.should raise_error(FloatDomainError)
    end
  end

  it "returns Integer or Bignum otherwise" do
    BigDecimal("3E-20001").send(@method).should == 0
    BigDecimal("2E4000").send(@method).should == 2 * 10 ** 4000
    BigDecimal("2").send(@method).should == 2
    BigDecimal("2E10").send(@method).should == 20000000000
    BigDecimal("3.14159").send(@method).should == 3
  end
end
