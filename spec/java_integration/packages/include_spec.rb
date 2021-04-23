require File.dirname(__FILE__) + "/../spec_helper"

describe "jar with dependecies" do
  it "carries its error messages along to Ruby exception when one of its classes is imported" do
    begin
      mod = Module.new do
        import 'java_integration.fixtures.ThrowExceptionOnCreate'
      end
      mod::ThrowExceptionOnCreate
      fail 'expected to raise'
    rescue NameError
      expect($!.message).to eql 'cannot link Java class java_integration.fixtures.ThrowExceptionOnCreate (java.lang.NoClassDefFoundError: junit/framework/Test)'
    end
  end

  it "carries its error messages along to Ruby exception when it's included as package" do
    begin
      mod = Module.new do
        include_package 'java_integration.fixtures'
      end
      mod::ThrowExceptionOnCreate
      fail 'expected to raise'
    rescue NameError => e
      # NOTE: include_package no longer rescues (and re-wraps) all NameErrors (in JRuby 9.2 this was part of a message of another NameError)
      expect(e.message).to eql 'cannot link Java class java_integration.fixtures.ThrowExceptionOnCreate (java.lang.NoClassDefFoundError: junit/framework/Test)'
    end
  end
end
