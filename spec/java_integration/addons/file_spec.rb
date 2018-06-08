require File.dirname(__FILE__) + "/../spec_helper"

describe "Ruby File" do

  before(:all) { require 'tempfile' }

  it "is coercible to java.io.File" do
    file = Tempfile.new("io_spec").to_java 'java.io.File'
    expect(java.io.File).to be === file
    file = File.open(__FILE__).to_java java.io.File
    expect(java.io.File).to be === file
    expect(file.getPath).to eql __FILE__
  end

  it "is coercible to (java) Path" do
    File.open('.') do |file|
      java_file=  file.to_java java.nio.file.Path
      expect(java_file).to eql java.io.File.new('.').toPath
    end
  end

end
