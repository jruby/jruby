require 'rspec'
require 'pathname'

describe "Pathname#root?" do
  # There is nothing special with these.
  it "should return false for non-root URI (GH-7745)" do
    expect(Pathname.new('http://10.1.1.1:32/').root?).to be false
  end

    # This represents internal paths for files contain within jar files.
  it "should return true for 'classpath:/'" do
    expect(Pathname.new('classpath:/').root?).to be true
    expect(Pathname.new('classpath://').root?).to be true
  end

  # This represents internal paths for files contain within jar files.
  it "should return true for 'classpath:uri:/'" do
    expect(Pathname.new('classpath:uri:/').root?).to be true
    expect(Pathname.new('classpath:uri://').root?).to be true
  end

  it "should return true for 'uri::classloader:/'" do
    expect(Pathname.new('uri:classloader:/').root?).to be true
    expect(Pathname.new('uri:classloader://').root?).to be true
  end

  # Common URI for local file access.
  it "should return true for 'file:/'" do
    expect(Pathname.new('file:/').root?).to be true
    expect(Pathname.new('file://').root?).to be true
  end

  it "should return true for 'uri:file:/'" do
    expect(Pathname.new('uri:file:/').root?).to be true
    expect(Pathname.new('uri:file://').root?).to be true
  end  

  # Jar Resources
  it "should return true for 'some_jar!/'" do
    expect(Pathname.new('frogger.jar!/').root?).to be true
  end

  # For some reason all others will work with // but jar resources never
  # had that behavior.
  
  it "should return false for 'some_jar!//'" do
    expect(Pathname.new('frogger.jar!//').root?).to be false
  end

  it "should return true for 'jar:file:/'" do
    expect(Pathname.new('jar:file:/my.jar!/').root?).to be true
  end

  it "should return true for 'jar:file:/'" do
    expect(Pathname.new('jar:file:/my.jar!//').root?).to be false
  end

  it "should return true for 'jar:/'" do
    expect(Pathname.new('jar:/my.jar!/').root?).to be true
  end

  it "should return true for 'jar:/'" do
    expect(Pathname.new('jar:/my.jar!//').root?).to be false
  end

end
