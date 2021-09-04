require File.dirname(__FILE__) + "/../spec_helper"

describe "ENV_JAVA" do
  before :each do
    ENV_JAVA['env.java.spec'] = nil
  end

  after :each do
    ENV_JAVA['env.java.spec'] = nil
  end

  it "writes to system properties" do
    expect(java.lang.System.getProperty('env.java.spec')).to eq(nil)
    ENV_JAVA['env.java.spec'] = 'foo'
    expect(ENV_JAVA['env.java.spec']).to eq('foo')
    expect(java.lang.System.getProperty('env.java.spec')).to eq('foo')
  end

  it "reflects changes to system properties" do
    expect(ENV_JAVA['env.java.spec']).to eq(nil)
    java.lang.System.setProperty('env.java.spec', 'foo')
    expect(ENV_JAVA['env.java.spec']).to eq('foo')
    expect(java.lang.System.getProperty('env.java.spec')).to eq('foo')
  end
end