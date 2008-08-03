require File.dirname(__FILE__) + "/../spec_helper"

import "java.lang.NullPointerException"

describe "Kernel Ruby extensions" do
  it "allow raising a Java exception" do
    lambda { raise NullPointerException.new }.should raise_error(NullPointerException)
  end
end
