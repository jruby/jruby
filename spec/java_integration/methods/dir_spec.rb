require File.dirname(__FILE__) + '/../spec_helper'

describe "Dir" do
  before(:each) do
    @path_in_jar = "file:" + File.dirname(__FILE__) + "/../../../test/target/junit.jar!/META-INF"
    @local_file_path = "file:" + File.dirname(__FILE__) + "/../../../test/target"
    @fs_path = File.absolute_path(File.join(File.dirname(__FILE__), '..'))
  end

  describe "new" do
    it "creates a new Dir object with an existing directory into a jar file" do
      dir = Dir.new(@path_in_jar)

      dir.entries.should == %w|MANIFEST.MF|
    end
  end

  describe "entries" do
    it "returns an Array of filenames in an exisiting directory into a jar file" do
      a = Dir.entries(@path_in_jar)

      a.should == %w|MANIFEST.MF|
    end
  end

  describe "glob file path" do
    it "lists contents of a file: path to a directory" do
      dir = Dir.new(@local_file_path)
      dir.entries.should include("junit.jar")
    end
  end

  describe '.chdir' do
    it 'sets working directory as seen by Java' do
      # See https://github.com/jruby/jruby/issues/971
      Dir.chdir @fs_path
      java_path_1 = java.io.File.new('').absolute_path
      java_path_2 = java.lang.System.get_property('user.dir')

      java_path_1.should == @fs_path
      java_path_2.should == @fs_path
    end
  end
end
