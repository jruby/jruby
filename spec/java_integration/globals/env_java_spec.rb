require File.dirname(__FILE__) + "/../spec_helper"

describe "ENV_JAVA" do
  before :each do
    ENV_JAVA['env.java.spec'] = nil
  end

  after :each do
    ENV_JAVA['env.java.spec'] = nil
  end

  it "writes to system properties" do
    java.lang.System.getProperty('env.java.spec').should == nil
    ENV_JAVA['env.java.spec'] = 'foo'
    ENV_JAVA['env.java.spec'].should == 'foo'
    java.lang.System.getProperty('env.java.spec').should == 'foo'
  end

  it "reflects changes to system properties" do
    ENV_JAVA['env.java.spec'].should == nil
    java.lang.System.setProperty('env.java.spec', 'foo')
    ENV_JAVA['env.java.spec'].should == 'foo'
    java.lang.System.getProperty('env.java.spec').should == 'foo'
  end
end