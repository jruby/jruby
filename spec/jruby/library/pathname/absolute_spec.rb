require 'rspec'
require 'pathname'

describe "Pathname#absolute?" do
  # There is nothing special with these.
  it "should return false for non-root URI (GH-7745)" do
    expect(Pathname.new('http://10.1.1.1:32/bg.png').absolute?).to be false
    expect(Pathname.new('http://10.1.1.1:32/').absolute?).to be false
  end

  # This represents internal paths for files contain within jar files.
  it "should return true for 'classpath:uri:/'" do
    expect(Pathname.new('classpath:uri:/').absolute?).to be true
    expect(Pathname.new('classpath:uri:/home/me').absolute?).to be true
  end

  # Common URI for local file access.
  it "should return true for 'file:/'" do
    expect(Pathname.new('file:/').absolute?).to be true
    expect(Pathname.new('file:/home/me').absolute?).to be true
  end

  # Jar Resources
  it "should return true for 'some_jar!/'" do
    expect(Pathname.new('frogger.jar!/home').absolute?).to be true
    expect(Pathname.new('frogger.jar!/home/me').absolute?).to be true
    expect(Pathname.new('C:/opt/frogger.jar!/home/me').absolute?).to be true
  end
end
