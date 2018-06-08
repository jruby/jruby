require File.dirname(__FILE__) + "/../spec_helper"

describe "Ruby Dir" do

  it "is coercible to java.io.File" do
    dir = Dir.new('..')
    expect(java.io.File).to be === dir.to_java('java.io.File')
    file = dir.to_java(java.io.File)
    expect(file.getPath).to eql '..'
    dir.close
  end

  it "is coercible to (java) Path" do
    Dir.open('.') do |dir|
      java_file=  dir.to_java java.nio.file.Path
      expect(java_file).to eql java.io.File.new('.').toPath
    end
  end

end
