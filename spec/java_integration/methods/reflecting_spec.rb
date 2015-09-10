require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.MethodNames"

describe "The Ruby methods representing Java static methods" do
  it "should be accessible through Ruby's Module#method method" do
    method = MethodNames.method("lowercase1")
    expect(method).not_to eq(nil)
  end
  
  it "should be inspectable" do
    method = MethodNames.instance_method("lowercase2")
    
    expect(method.inspect).to eq("#<UnboundMethod: Java::Java_integrationFixtures::MethodNames#lowercase2>")
  end
end

describe "The Ruby methods representing Java instance methods" do
  it "should be accessible through Ruby's Module#instance_method method" do
    method = MethodNames.instance_method("lowercase2")
    expect(method).not_to eq(nil)
  end
  
  it "should be inspectable" do
    method = MethodNames.instance_method("lowercase2")
    
    expect(method.inspect).to eq("#<UnboundMethod: Java::Java_integrationFixtures::MethodNames#lowercase2>")
  end
end

describe "The 'new' method for an imported Java type" do
  it "should be accessible through Ruby's Module#method method as 'new'" do
    method = MethodNames.method("new")
    expect(method).not_to eq(nil)
  end
  
  it "should be inspectable" do
    method = MethodNames.method("new")
    
    expect(method.inspect).to eq("#<Method: Java::Java_integrationFixtures::MethodNames(ConcreteJavaProxy).new>")
  end
end
