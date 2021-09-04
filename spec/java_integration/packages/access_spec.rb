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
    m = Module.new { include_package java.lang }
    expect(m::System).to respond_to 'getProperty'
  end

  it "can be imported using 'include_package \"package.module\"'" do
    m = Module.new { include_package 'java.lang' }
    expect(m::System).to respond_to :getProperty
  end

  it "can be imported using 'import package.module" do
    m = Module.new { import java.lang }
    expect(m::System).to respond_to 'currentTimeMillis'
  end

  it "can be imported using 'import \"package.module\"'" do
    m = Module.new { import 'java.lang' }
    m::System.currentTimeMillis
  end

  it "supports const_get" do
    expect(java.util.respond_to?(:const_get)).to be true
    expect(java.util.const_get("Arrays")).to respond_to "asList"
  end

  it "supports const_get with inherit argument" do
    expect( java.util.const_get(:Arrays, false) ).to respond_to :asList
  end

  it "can be accessed using Java module and CamelCase" do
    expect(Java::JavaLang).to eq(java.lang)
    expect(Java::ComBlahV8Something).to eq(com.blah.v8.something)
    expect(Java::X_Y_).to eq(Java::x_.y_)
  end

  it 'sub-packages work with const_get' do
    java.const_get(:util)
    pkg = java::util.const_get(:zip)
    expect( pkg ).to be_a Module
    expect( pkg.is_a?(Class) ).to be false
    expect( pkg ).to equal Java::JavaUtilZip

    klass = java::util.const_get(:StringTokenizer)
    expect( klass ).to be_a Class
    expect( klass.name ).to eql 'Java::JavaUtil::StringTokenizer'

    pkg = Java::JavaxSecurityAuth.const_get(:callback, true)
    expect( pkg ).to eql Java::javax::security::auth::callback
  end

  it 'does not inherit constants' do
    #expect( Java::JavaLang::TOP_LEVEL_BINDING ).to raise_error(NameError)
    begin
      Java::JavaLang::TOP_LEVEL_BINDING
    rescue NameError
    else; fail 'error not raised' end
    #expect( Java::java.util::Object ).to raise_error(NameError)
    begin
      Java::java.util::Object
    rescue NameError
    else; fail 'error not raised' end
  end

  it 'fails with argument passed to package method' do
    begin
      javax.script(1)
    rescue ArgumentError => e
      expect( e.message ).to eql "Java package 'javax' does not have a method `script' with 1 argument"
    else; fail 'error not raised' end
  end

  # See GH issue ruboto/JRuby9K_POC#7
  it "allows calling const_missing" do
    expect(java.lang.const_missing(:System)).to eq(java.lang.System)
  end

  it "allows calling method_missing" do
    expect(java.lang.method_missing(:reflect)).to eq(java.lang.reflect)
  end

end

# for DefaultPackageClass
$CLASSPATH << File.dirname(__FILE__) + "/../../../target/test-classes"

describe "class in default package" do

  it "can be opened using Java::Foo syntax" do
    expect( Java::DefaultPackageClass.new.foo ).to eql "foo"
    class Java::DefaultPackageClass
      def bar; 'bar'; end
    end
    expect( Java::DefaultPackageClass.new.bar ).to eql "bar"
    expect( Java::DefaultPackageClass.new.foo ).to eql "foo"

    class Java::java::util::StringTokenizer
      def xxx; 'xxx' end
    end
    expect( java::util::StringTokenizer.new('').xxx ).to eql 'xxx'
  end

  it "does not failover to a package if there are classloading errors" do
    expect do
      Java::BadStaticInit.new
    end.to raise_error(NameError)
  end

  it "has accessible public fields" do
    expect( Java::DefaultPackageClass.new.x ).to eql 0
    expect( Java::DefaultPackageClass.anY ).to eql 1
  end

end
