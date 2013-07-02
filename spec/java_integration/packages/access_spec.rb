require File.dirname(__FILE__) + "/../spec_helper"

# for DefaultPackageClass
$CLASSPATH << File.dirname(__FILE__) + "/../../../target/test-classes"

describe "A Java package" do
  it 'is accessible directly when starting with java, javax, com, or org' do
    # Using static methods here for simplicity; avoiding construction.
    java.lang.should == JavaUtilities.get_package_module_dot_format("java.lang")
    java.lang.System.should respond_to 'getProperty'

    javax.management.should == JavaUtilities.get_package_module_dot_format("javax.management")
    javax.management.MBeanServerFactory.should respond_to 'createMBeanServer'

    org.xml.sax.helpers.should == JavaUtilities.get_package_module_dot_format("org.xml.sax.helpers")
    org.xml.sax.helpers.ParserFactory.should respond_to 'makeParser'

    java.util.should == JavaUtilities.get_package_module_dot_format("java.util")
    java.util.Arrays.should respond_to "asList"
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

  it "supports const_get" do
    java.util.const_get("Arrays").should respond_to "asList"
  end

  if RUBY_VERSION =~ /1\.9/
    it "supports const_get with inherit argument" do
      java.util.const_get("Arrays", false).should respond_to "asList"
    end
  end
end

describe "A class in the default package" do
  it "can be opened using Java::Foo syntax" do
    Java::DefaultPackageClass.new.foo.should == "foo"
    class Java::DefaultPackageClass
      def bar; 'bar'; end
    end
    Java::DefaultPackageClass.new.bar.should == "bar"
    Java::DefaultPackageClass.new.foo.should == "foo"
  end

  it "does not failover to a package if there are classloading errors" do
    lambda do
      Java::BadStaticInit.new
    end.should raise_error(NameError)
  end
end
