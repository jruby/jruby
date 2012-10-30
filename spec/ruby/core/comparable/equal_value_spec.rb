require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Comparable#==" do
  it "returns true if other is the same as self" do
    a = ComparableSpecs::Weird.new(0)
    b = ComparableSpecs::Weird.new(20)

    (a == a).should == true
    (b == b).should == true
  end

  it "calls #<=> on self with other and returns true if #<=> returns 0" do
    a = ComparableSpecs::Weird.new(0)
    b = ComparableSpecs::Weird.new(10)

    a.should_receive(:<=>).any_number_of_times.and_return(0)
    (a == b).should == true

    a = ComparableSpecs::Weird.new(0)
    a.should_receive(:<=>).any_number_of_times.and_return(0.0)
    (a == b).should == true
  end

  it "returns false if calling #<=> on self returns a non-zero Integer" do
    a = ComparableSpecs::Weird.new(0)
    b = ComparableSpecs::Weird.new(10)

    a.should_receive(:<=>).any_number_of_times.and_return(1)
    (a == b).should == false

    a = ComparableSpecs::Weird.new(0)
    a.should_receive(:<=>).any_number_of_times.and_return(-1)
    (a == b).should == false
  end

  ruby_version_is ""..."1.9" do
    it "returns nil if calling #<=> on self returns nil or a non-Integer" do
      a = ComparableSpecs::Weird.new(0)
      b = ComparableSpecs::Weird.new(10)

      a.should_receive(:<=>).any_number_of_times.and_return(nil)
      (a == b).should == nil

      a = ComparableSpecs::Weird.new(0)
      a.should_receive(:<=>).any_number_of_times.and_return("abc")
      (a == b).should == nil
    end
  end

  ruby_version_is "1.9" do
    it "returns false if calling #<=> on self returns nil or a non-Integer" do
      a = ComparableSpecs::Weird.new(0)
      b = ComparableSpecs::Weird.new(10)

      a.should_receive(:<=>).any_number_of_times.and_return(nil)
      (a == b).should be_false

      a = ComparableSpecs::Weird.new(0)
      a.should_receive(:<=>).any_number_of_times.and_return("abc")
      (a == b).should be_false
    end
  end

  ruby_version_is ""..."1.9" do
    it "returns nil if calling #<=> on self raises a StandardError" do
      a = ComparableSpecs::Weird.new(0)
      b = ComparableSpecs::Weird.new(10)

      def a.<=>(b) raise StandardError, "test"; end
      (a == b).should == nil

      # TypeError < StandardError
      def a.<=>(b) raise TypeError, "test"; end
      (a == b).should == nil

      def a.<=>(b) raise Exception, "test"; end
      lambda { (a == b).should == nil }.should raise_error(Exception)
    end
  end

  ruby_version_is "1.9" do
    # Behaviour confirmed by MRI test suite
    it "returns false if calling #<=> on self raises an Exception" do
      a = ComparableSpecs::Weird.new(0)
      b = ComparableSpecs::Weird.new(10)

      def a.<=>(b) raise StandardError, "test"; end
      (a == b).should be_false

      def a.<=>(b) raise TypeError, "test"; end
      (a == b).should be_false

      def a.<=>(b) raise Exception, "test"; end
      lambda { (a == b).should be_false }.should raise_error(Exception)
    end
  end
end
