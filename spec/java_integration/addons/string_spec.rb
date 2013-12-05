require File.dirname(__FILE__) + "/../spec_helper"

describe "String#to_java_string" do
  it "produces a wrapped Java String" do
    wrapped = 'foo'.to_java_string
    expect(wrapped.class).to eq java.lang.String
  end
end
