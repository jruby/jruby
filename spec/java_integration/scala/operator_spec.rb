require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ScalaOperators"

describe "Scala operators" do
  it "are callable using symbolic names" do
    obj = ScalaOperators.new

    obj.send(:"+").should == "$plus"
    obj.send(:"-").should == "$minus"
    obj.send(:":").should == "$colon"
    obj.send(:"/").should == "$div"
    obj.send(:"=").should == "$eq"
    obj.send(:"<").should == "$less"
    obj.send(:">").should == "$greater"
    obj.send(:"\\").should == "$bslash"
    obj.send(:"#").should == "$hash"
    obj.send(:"*").should == "$times"
    obj.send(:"!").should == "$bang"
    obj.send(:"@").should == "$at"
    obj.send(:"%").should == "$percent"
    obj.send(:"^").should == "$up"
    obj.send(:"&").should == "$amp"
    obj.send(:"~").should == "$tilde"
    obj.send(:"?").should == "$qmark"
    obj.send(:"|").should == "$bar"
    obj.send(:"+=").should == "$plus$eq"
  end

  it "are callable using original names" do
    obj = ScalaOperators.new
    
    obj.send(:"$plus").should == "$plus"
    obj.send(:"$minus").should == "$minus"
    obj.send(:"$colon").should == "$colon"
    obj.send(:"$div").should == "$div"
    obj.send(:"$eq").should == "$eq"
    obj.send(:"$less").should == "$less"
    obj.send(:"$greater").should == "$greater"
    obj.send(:"$bslash").should == "$bslash"
    obj.send(:"$hash").should == "$hash"
    obj.send(:"$times").should == "$times"
    obj.send(:"$bang").should == "$bang"
    obj.send(:"$at").should == "$at"
    obj.send(:"$percent").should == "$percent"
    obj.send(:"$up").should == "$up"
    obj.send(:"$amp").should == "$amp"
    obj.send(:"$tilde").should == "$tilde"
    obj.send(:"$qmark").should == "$qmark"
    obj.send(:"$bar").should == "$bar"
    obj.send(:"$plus$eq").should == "$plus$eq"
  end
end