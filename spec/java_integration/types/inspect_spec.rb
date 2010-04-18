require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java object's builtin inspect method" do
  it "produces the \"hashy\" inspect output" do
    o = java.lang.Object.new
    o.inspect.should match(/\#<Java::JavaLang::Object:0x[0-9a-f]+>/)
  end
end