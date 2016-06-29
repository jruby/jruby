require File.dirname(__FILE__) + "/../spec_helper"

describe "what happens with method new on interfaces" do
  
  it "should not be able to instantiate an interface" do
    expect{java.lang.Runnable.new}.to raise_error(NoMethodError)
  end
  
  it "should not affect impl()" do
    @runnable = java.lang.Runnable.impl {a = "Hello"}
    expect{@runnable.run}.to_not raise_error(NoMethodError)
    expect{@runnable.run}.to eq("Hello")
  end
  
end
