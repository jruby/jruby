require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java package" do
  it 'is accessible directly when starting with java, javax, com, or org' do
    # Using static methods here for simplicity; avoiding construction.
    java.lang.should == JavaUtilities.get_package_module_dot_format("java.lang")
    java.lang.System.should respond_to 'getProperty'

    javax.management.should == JavaUtilities.get_package_module_dot_format("javax.management")
    javax.management.MBeanServerFactory.should respond_to 'createMBeanServer'

    org.xml.sax.helpers.should == JavaUtilities.get_package_module_dot_format("org.xml.sax.helpers")
    org.xml.sax.helpers.ParserFactory.should respond_to 'makeParser'

    com.sun.jna.should == JavaUtilities.get_package_module_dot_format("com.sun.jna")
    com.sun.jna.Function.should respond_to "getFunction"
  end

  it "can be imported using 'include_package package.module" do
    pending "does not work; probably should for consistency?" do
      m = Module.new { include_package java.lang }
      m::System.should respond_to 'getProperty'
    end
  end

  it "can be imported using 'include_package \"package.module\"'" do
    m = Module.new { include_package 'java.lang' }
    m::System.should respond_to 'getProperty'
  end

  it "can be imported using 'import package.module" do
    m = Module.new { import java.lang }
    m::System.should respond_to 'getProperty'
  end

  it "can be imported using 'import \"package.module\"'" do
    m = Module.new { import 'java.lang' }
    m::System.should respond_to 'getProperty'
  end
end
