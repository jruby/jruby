require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.lang.NullPointerException"

describe "Kernel Ruby extensions" do
  it "allow raising a Java exception" do
    expect { raise NullPointerException.new }.to raise_error(NullPointerException)
  end

  it "allow failing a Java exception" do
    expect { fail NullPointerException.new }.to raise_error(NullPointerException)
  end
end
