require File.dirname(__FILE__) + "/../spec_helper"

java_import java.lang.OutOfMemoryError
java_import "java_integration.fixtures.ThrowExceptionInInitializer"

describe "Non-wrapped Java throwables" do
  it "can be rescued" do
    exception = OutOfMemoryError.new
    begin
      raise exception
    rescue OutOfMemoryError => oome
    end
    
    oome.should_not == nil
    oome.should == exception
  end
end

describe "A Ruby-level exception" do
  it "carries its message along to the Java exception" do
    java_ex = JRuby.runtime.new_runtime_error("error message");
    java_ex.message.should == "(RuntimeError) error message"

    java_ex = JRuby.runtime.new_name_error("error message", "name");
    java_ex.message.should == "(NameError) error message"
  end
end

describe "A native exception wrapped by another" do  
  it "gets the first available message from the causes' chain" do
    begin
      ThrowExceptionInInitializer.new.test
    rescue NativeException => e
      e.message.should =~ /lets cause an init exception$/
    end
  end

  it "can be re-raised" do
    lambda {
      begin
        ThrowExceptionInInitializer.new.test
      rescue NativeException => e
        raise e.exception("re-raised")
      end
    }.should raise_error(NativeException)
  end
end

describe "A Ruby subclass of a Java exception" do
  before :all do
    @ex_class = Class.new(java.lang.RuntimeException)
  end

  it "is rescuable with all Java superclasses" do
    exception = @ex_class.new

    begin
      raise exception
      fail
    rescue java.lang.Throwable
      $!.should == exception
    end

    begin
      raise exception
      fail
    rescue java.lang.Exception
      $!.should == exception
    end

    begin
      raise exception
      fail
    rescue java.lang.RuntimeException
      $!.should == exception
    end
  end

  it "presents its Ruby nature when rescued" do
    exception = @ex_class.new

    begin
      raise exception
      fail
    rescue java.lang.Throwable => t
      t.class.should == @ex_class
      t.should equal(exception)
    end
  end
end
