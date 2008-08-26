require File.dirname(__FILE__) + "/../spec_helper"

describe "Non-overloaded static Java methods" do
  it "should raise ArgumentError when called with incorrect arity" do
    lambda do
      java.util.Collections.empty_list('foo')
    end.should raise_error(ArgumentError)
  end
end
