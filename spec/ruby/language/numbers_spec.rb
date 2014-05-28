require File.expand_path('../../spec_helper', __FILE__)

describe "Ruby numbers in various ways" do

  it "the standard way" do
    435.should == 435
  end

  it "with underscore separations" do
    4_35.should == 435
  end

  it "with some decimals" do
    4.35.should == 4.35
  end

  it "with decimals but no integer part should be a SyntaxError" do
    lambda { eval(".75")  }.should raise_error(SyntaxError)
    lambda { eval("-.75") }.should raise_error(SyntaxError)
  end

  # TODO : find a better description
  it "using the e expression" do
    1.2e-3.should == 0.0012
  end

  it "the hexdecimal notation" do
    0xffff.should == 65535
  end

  it "the binary notation" do
    0b01011.should == 11
  end

  it "octal representation" do
    0377.should == 255
  end
end
