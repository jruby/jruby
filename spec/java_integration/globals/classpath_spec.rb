require File.dirname(__FILE__) + "/../spec_helper"

describe "$CLASSPATH" do
  describe "<<" do
    it "accepts an object" do
      $CLASSPATH << "file"
      $CLASSPATH.to_a.include?("file:#{File.expand_path('file')}").should == true
    end
    
    it "accepts an array" do
      Dir.chdir(File.join(File.dirname(__FILE__), '..', '..', '..', 'test')) do
        $CLASSPATH << Dir.glob("classpath*.jar")
        ($CLASSPATH.to_a.include?("file:#{File.expand_path('classpath_test.jar')}")).should == true
      end
      JRuby.runtime.getJRubyClassLoader.getResource('test_value.rb').should_not == nil
    end
  end
end
