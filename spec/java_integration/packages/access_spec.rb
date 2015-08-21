require File.dirname(__FILE__) + "/../spec_helper"

describe "java package" do
  it 'is accessible directly when starting with java, javax, com, or org' do
    # Using static methods here for simplicity; avoiding construction.
    expect(java.lang).to eq(JavaUtilities.get_package_module_dot_format("java.lang"))
    expect(java.lang.System).to respond_to 'getProperty'

    expect(javax.management).to eq(JavaUtilities.get_package_module_dot_format("javax.management"))
    expect(javax.management.MBeanServerFactory).to respond_to 'createMBeanServer'

    expect(org.xml.sax.helpers).to eq(JavaUtilities.get_package_module_dot_format("org.xml.sax.helpers"))
    expect(org.xml.sax.helpers.ParserFactory).to respond_to 'makeParser'

    expect(java.util).to eq(JavaUtilities.get_package_module_dot_format("java.util"))
    expect(java.util.Arrays).to respond_to "asList"
  end

  it "can be imported using 'include_package package.module" do
    skip "does not work; probably should for consistency?" do
      m = Module.new { include_package java.lang }
      expect(m::System).to respond_to 'getProperty'
    end
  end

  it "can be imported using 'include_package \"package.module\"'" do
    m = Module.new { include_package 'java.lang' }
    expect(m::System).to respond_to 'getProperty'
  end

  it "can be imported using 'import package.module" do
    m = Module.new { import java.lang }
    expect(m::System).to respond_to 'getProperty'
  end

  it "can be imported using 'import \"package.module\"'" do
    m = Module.new { import 'java.lang' }
    expect(m::System).to respond_to 'getProperty'
  end

  it "supports const_get" do
    expect(java.util.respond_to?(:const_get)).to be true
    expect(java.util.const_get("Arrays")).to respond_to "asList"
  end

  it "supports const_get with inherit argument" do
    expect(java.util.const_get("Arrays", false)).to respond_to :asList
  end
end

# for DefaultPackageClass
$CLASSPATH << File.dirname(__FILE__) + "/../../../target/test-classes"

describe "class in default package" do
  it "can be opened using Java::Foo syntax" do
    expect(Java::DefaultPackageClass.new.foo).to eq("foo")
    class Java::DefaultPackageClass
      def bar; 'bar'; end
    end
    expect(Java::DefaultPackageClass.new.bar).to eq("bar")
    expect(Java::DefaultPackageClass.new.foo).to eq("foo")
  end

  it "does not failover to a package if there are classloading errors" do
    expect do
      Java::BadStaticInit.new
    end.to raise_error(NameError)
  end
end
