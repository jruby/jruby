require File.dirname(__FILE__) + '/../spec_helper'

describe "Dir.entries" do
  it "returns an Array of filenames in an exisiting directory into a jar file" do
    a = Dir.entries("file:" + File.dirname(__FILE__) + "/../../../build_lib/junit.jar!/META-INF")

    a.should == %w|MANIFEST.MF|
  end
end
