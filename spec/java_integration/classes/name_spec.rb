require File.dirname(__FILE__) + "/../spec_helper"

describe "Java class" do

  java_import "java_integration.fixtures.InnerClasses"

  it "has (qualified) Ruby name" do
    expect( InnerClasses.name ).to eql 'Java::Java_integrationFixtures::InnerClasses'
  end

  it "has name for inner classes" do
    base_name = InnerClasses.name
    expect( InnerClasses::CapsInnerClass.name ).to eql "#{base_name}::CapsInnerClass"
    expect( InnerClasses::lowerInnerClass.name ).to eql "#{base_name}::lowerInnerClass"
  end

  it "has name for local classes" do
    base_name = InnerClasses.name
    local = InnerClasses.localMethodClass
    # java_integration.fixtures.InnerClasses$1CapsImpl
    expect( local.class.name ).to eql "#{base_name}::1CapsImpl"
  end

  it "has name for anonymous classes" do
    base_name = InnerClasses.name
    anon = InnerClasses.anonymousMethodClass
    expect( anon.class.name ).to eql "#{base_name}::1"
  end

end
