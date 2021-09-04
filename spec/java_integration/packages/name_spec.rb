require File.dirname(__FILE__) + "/../spec_helper"

# for DefaultPackageClass
$CLASSPATH << File.dirname(__FILE__) + "/../../../target/test-classes"

describe "java package (and class)" do

  it 'have name' do
    expect( Java::JavaLang::Integer.name ).to eql 'Java::JavaLang::Integer'
    expect( Java::JavaLang.name ).to eql 'Java::JavaLang'
    expect( Java::Java.name ).to eql 'Java::Java'
    expect( Java::Javax.name ).to eql 'Java::Javax'
    expect( Java.name ).to eql 'Java'
    expect( Java::java.util.name ).to eql 'Java::JavaUtil'
    expect( org.xml.name ).to eql 'Java::OrgXml'
    expect( org.xml.sax.name ).to eql 'Java::OrgXmlSax'
    #expect( Java::Default.name ).to eql ''
    # TODO avoid Default package in favor of Java :
    #expect( Java::DefaultPackageClass.name ).to eql 'Java::DefaultPackageClass'
    expect( Java::DefaultPackageClass.name ).to eql 'Java::Default::DefaultPackageClass'
  end

  it 'handles Kernel methods' do
    expect( Java::JavaLang::Integer.to_s ).to eql 'Java::JavaLang::Integer'
    expect( Java::JavaLang.to_s ).to eql 'java.lang'
    expect( Java::Java.inspect ).to eql 'Java::Java'
    expect( Java::Javax.inspect ).to eql 'Java::Javax'
    expect( Java.inspect ).to eql 'Java'
    expect( Java::java.util.inspect ).to eql 'Java::JavaUtil'
    expect( org.xml.object_id ).to be_a Fixnum
    expect( org.xml.sax.singleton_class ).to be_a Class
    expect( org.xml == org.xml.sax ).to be false
    expect( org.xml.eql? Java::org::xml ).to be true
    expect( Java::OrgXmlSax.equal?org.xml.sax ).to be true
    expect( Java::OrgXmlSax === org.xml.sax ).to be true
    expect( Java::OrgXml === org.xml.sax ).to be false
    expect( Java::OrgXml === org ).to be false
  end

  it 'have package name' do
    expect( Java::JavaLang::Integer.respond_to? :package_name ).to be false
    expect( Java::JavaLang.respond_to? :package_name ).to be true
    expect( Java::JavaLang.package_name ).to eql 'java.lang'
    expect( Java::Java.package_name ).to eql 'java'
    expect( Java::Javax.package_name ).to eql 'javax'
    expect( Java::java.util.package_name ).to eql 'java.util'
    expect( org.xml.package_name ).to eql 'org.xml'
    expect( org.xml.sax.package_name ).to eql 'org.xml.sax'
    # TODO avoid Default package in favor of Java :
    #expect( Java.package_name ).to eql ''
    expect( Java::Default.package_name ).to eql ''
  end

  it 'respond to name (GH-2468)' do
    expect( java.lang.respond_to?(:name) ).to be true
    expect( Java::JavaLang.respond_to?(:name) ).to be true

    expect( Java::JavaLang::Integer.respond_to?(:name) ).to be true
    expect( Java::Java.respond_to?(:name) ).to be true
    expect( Java.respond_to?(:name) ).to be true
    expect( org.xml.respond_to?(:name) ).to be true
    expect( org.xml.sax.respond_to?(:name) ).to be true
    expect( Java::Default.respond_to?(:name) ).to be true

    quiet do
      expect( Java::JavaPackage.respond_to?(:name) ).to be true
    end
  end

end
