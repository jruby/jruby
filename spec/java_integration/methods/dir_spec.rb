require File.dirname(__FILE__) + '/../spec_helper'

describe "Dir" do
  before(:each) do
    @path = "file:" + File.dirname(__FILE__) + "/../../../build_lib/junit.jar!/META-INF"
  end

  describe "new" do
    it "creates a new Dir object with an existing directory into a jar file" do
      dir = Dir.new(@path)

      dir.entries.should == %w|MANIFEST.MF|
    end
  end

  describe "entries" do
    it "returns an Array of filenames in an exisiting directory into a jar file" do
      a = Dir.entries(@path)

      a.should == %w|MANIFEST.MF|
    end
  end
end
