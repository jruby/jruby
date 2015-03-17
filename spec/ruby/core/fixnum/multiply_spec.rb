require File.expand_path('../../../spec_helper', __FILE__)

describe "Fixnum#*" do
  it "returns self multiplied by the given Integer" do
    (4923 * 2).should == 9846
    (1342177 * 800).should == 1073741600
    (65536 * 65536).should == 4294967296

    (256 * bignum_value).should == 2361183241434822606848
    (6712 * 0.25).should == 1678.0
  end

  it "raises a TypeError when given a non-Integer" do
    lambda {
      (obj = mock('10')).should_receive(:to_int).any_number_of_times.and_return(10)
      13 * obj
    }.should raise_error(TypeError)
    lambda { 13 * "10"    }.should raise_error(TypeError)
    lambda { 13 * :symbol }.should raise_error(TypeError)
  end

  it "overflows to Bignum when the result does not fit in Fixnum" do
    if 1.size == 4
      # 32-bit fixnums
      y = 0x4000
      result = y * y * y
      result.should == 0x40000000000
      result.should be_kind_of(Bignum)
    elsif 1.size == 8
      # 64-bit fixnums
      y = 0x40000000
      result = y * y * y
      result.should == 0x40000000000000000000000
      result.should be_kind_of(Bignum)
    end
  end
end
