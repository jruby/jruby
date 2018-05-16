require File.dirname(__FILE__) + "/../spec_helper"

describe "String#to_java_string" do
  it "produces a wrapped Java String" do
    wrapped = 'foo'.to_java_string
    expect(wrapped.class).to eq java.lang.String
  end
end

describe "java.lang.String" do
  describe "passed to a String-coercing method" do
    it "coerces successfully" do
      expect("foo".concat("bar".to_java)).to eq "foobar"
    end
  end
end
