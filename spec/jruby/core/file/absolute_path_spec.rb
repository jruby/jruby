require 'rspec'
require 'pathname'

# This spec documents special behavior by marking methods so that it is
# special if it is skipped by MRI.

describe "File#absolute_path" do
  # Treats as absolute path (unlike the predicates on File/Pathname)
  it "should not do something special for non-root URI" do
    pending "Very inconsistent behavior (consistently through all versions of JRuby)"
    ['http://10.1.1.1:32/bg.png', 'http://10.1.1.1:32/'].each do |path|
      expect(File.absolute_path(path)).to eq path
    end
  end unless RUBY_ENGINE == 'ruby'

  # This represents internal paths for files contain within jar files.
  it "should return itself for 'classpath:uri:/'" do
    ['classpath:uri:/', 'classpath:uri:/home/me'].each do |path|
      expect(File.absolute_path(path)).to eq path
    end
  end unless RUBY_ENGINE == 'ruby'

  # Common URI for local file access.
  it "should return itself for 'file:/'" do
    ['file:/', 'file:/home/me'].each do |path|
      expect(File.absolute_path(path)).to eq path
    end
  end unless RUBY_ENGINE == 'ruby'

  # Jar Resources
  it "should return itself for '/some_jar!/'" do
    ['/frogger.jar!/home', '/frogger.jar!/home/me'].each do |path|
      expect(File.absolute_path(path)).to eq path
    end
  end

  it "should return qualified name for 'some_jar!/'" do
    ['frogger.jar!/home', 'frogger.jar!/home/me'].each do |path|
      expected_path = File.join(Dir.pwd, path)
      expect(File.absolute_path(path)).to eq expected_path
    end
  end
end

describe "File#absolute_path?" do
  # There is nothing special with these.
  it "should return false for non-root URI (GH-7744)" do
    expect(File.absolute_path?('http://10.1.1.1:32/bg.png')).to be false
    expect(File.absolute_path?('http://10.1.1.1:32/')).to be false
  end

  # This represents internal paths for files contain within jar files.
  it "should return true for 'classpath:uri:/'" do
    expect(File.absolute_path?('classpath:uri:/')).to be true
    expect(File.absolute_path?('classpath:uri:/home/me')).to be true
  end unless RUBY_ENGINE == 'ruby'

  # Common URI for local file access.
  it "should return true for 'file:/'" do
    expect(File.absolute_path?('file:/')).to be true
    expect(File.absolute_path?('file:/home/me')).to be true
  end unless RUBY_ENGINE == 'ruby'

  # Jar Resources
  it "should return true for 'some_jar!/'" do
    expect(File.absolute_path?('frogger.jar!/home')).to be true
    expect(File.absolute_path?('frogger.jar!/home/me')).to be true
  end unless RUBY_ENGINE == 'ruby'
end if File.respond_to? :absolute_path?
