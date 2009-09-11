require File.dirname(__FILE__) + "/../spec_helper"

describe "A jar with dependecies" do
  it "carries its error messages along to Ruby exception when one of its classes is imported" do
    begin
      module Fixtures
        import 'java_integration.fixtures.ThrowExceptionOnCreate'
      end

      Fixtures::ThrowExceptionOnCreate.new
    rescue
      $!.message.should == 'cannot link Java class java_integration.fixtures.ThrowExceptionOnCreate, probable missing dependency: junit/framework/Test'
    end
  end

  it "carries its error messages along to Ruby exception when it's included as package" do
    begin
      module Fixtures
        include_package 'java_integration.fixtures'
      end

      Fixtures::ThrowExceptionOnCreate.new
    rescue
      $!.message.should == 'cannot link Java class java_integration.fixtures.ThrowExceptionOnCreate, probable missing dependency: junit/framework/Test'
    end
  end
end