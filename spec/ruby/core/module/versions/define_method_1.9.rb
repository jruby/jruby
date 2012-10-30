require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

ruby_version_is "1.9" do

  describe "passed { |a, b = 1|  } creates a method that" do
    before :each do
      @klass = Class.new do
        define_method(:m) { |a, b = 1| return a, b }
      end
    end

    it "raises an ArgumentError when passed zero arguments" do
      lambda { @klass.new.m }.should raise_error(ArgumentError)
    end

    it "has a default value for b when passed one argument" do
      @klass.new.m(1).should == [1, 1]
    end

    it "overrides the default argument when passed two arguments" do
      @klass.new.m(1, 2).should == [1, 2]
    end

    it "raises an ArgumentError when passed three arguments" do
      lambda { @klass.new.m(1, 2, 3) }.should raise_error(ArgumentError)
    end

  end

end
