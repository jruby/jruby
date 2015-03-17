require File.expand_path('../../../spec_helper', __FILE__)

describe "Fixnum#-" do
  it "returns self minus the given Integer" do
    (5 - 10).should == -5
    (9237212 - 5_280).should == 9231932

    (781 - 0.5).should == 780.5
    (2_560_496 - bignum_value).should == -9223372036852215312
  end

  it "raises a TypeError when given a non-Integer" do
    lambda {
      (obj = mock('10')).should_receive(:to_int).any_number_of_times.and_return(10)
      13 - obj
    }.should raise_error(TypeError)
    lambda { 13 - "10"    }.should raise_error(TypeError)
    lambda { 13 - :symbol }.should raise_error(TypeError)
  end
end
