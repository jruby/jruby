require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)
require File.expand_path('../shared/equal_value.rb', __FILE__)

describe "String#==" do
  it_behaves_like(:string_equal_value, :==)
end

describe "String#==" do
  it "returns false if obj does not respond to to_str" do
    ('hello' == 5).should == false
    ('hello' == :hello).should == false
    ('hello' == mock('x')).should == false
  end

  it "returns obj == self if obj responds to to_str" do
    obj = Object.new

    # String#== merely checks if #to_str is defined. It does
    # not call it.
    obj.stub!(:to_str)

    obj.should_receive(:==).and_return(true)

    ('hello' == obj).should ==  true
  end

  it "is not fooled by NUL characters" do
    "abc\0def".should_not == "abc\0xyz"
  end
end
