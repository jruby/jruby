require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Comparable#==" do
  before(:each) do
    @a = ComparableSpecs::Weird.new(0)
    @b = ComparableSpecs::Weird.new(10)
  end

  it "returns true if other is the same as self" do
    (@a == @a).should be_true
    (@b == @b).should be_true
  end

  it "calls #<=> on self with other and returns true if #<=> returns 0" do
    @a.should_receive(:<=>).any_number_of_times.and_return(0)
    (@a == @b).should be_true
  end

  it "calls #<=> on self with other and returns true if #<=> returns 0.0" do
    @a.should_receive(:<=>).any_number_of_times.and_return(0.0)
    (@a == @b).should be_true
  end

  it "returns false if calling #<=> on self returns a positive Integer" do
    @a.should_receive(:<=>).any_number_of_times.and_return(1)
    (@a == @b).should be_false
  end

  it "returns false if calling #<=> on self returns a negative Integer" do
    @a.should_receive(:<=>).any_number_of_times.and_return(-1)
    (@a == @b).should be_false
  end

  ruby_version_is ""..."1.9" do
    it "returns nil if calling #<=> on self returns nil" do
      @a.should_receive(:<=>).any_number_of_times.and_return(nil)
      (@a == @b).should be_nil
    end

    it "returns nil if calling #<=> on self returns a non-Integer" do
      @a.should_receive(:<=>).any_number_of_times.and_return("abc")
      (@a == @b).should be_nil
    end
  end

  ruby_version_is "1.9" do
    it "returns false if calling #<=> on self returns nil" do
      @a.should_receive(:<=>).any_number_of_times.and_return(nil)
      (@a == @b).should be_false
    end

    it "returns false if calling #<=> on self returns a non-Integer" do
      @a.should_receive(:<=>).any_number_of_times.and_return("abc")
      (@a == @b).should be_false
    end
  end

  describe "when calling #<=> on self raises an Exception" do
    before(:all) do
      @raise_standard_error = @a.dup
      def @raise_standard_error.<=>(b) raise StandardError, "test"; end

      @raise_sub_standard_error  = @a.dup
      def @raise_sub_standard_error.<=>(b) raise TypeError, "test"; end

      @not_standard_error = SyntaxError
      @raise_not_standard_error  = @a.dup
      def @raise_not_standard_error.<=>(b) raise SyntaxError, "test"; end
    end

    it "raises the error if #<=> raises an Exception that excluding StandardError" do
      lambda { @raise_not_standard_error == @b }.should raise_error(@not_standard_error)
    end

    ruby_version_is ""..."1.9" do
      it "returns nil if #<=> raises a StandardError" do
        (@raise_standard_error == @b).should be_nil
        (@raise_sub_standard_error == @b).should be_nil
      end
    end

    ruby_version_is "1.9" do
      # Behaviour confirmed by MRI test suite
      it "returns false if #<=> raises a StandardError" do
        (@raise_standard_error == @b).should be_false
        (@raise_sub_standard_error == @b).should be_false
      end
    end
  end
end