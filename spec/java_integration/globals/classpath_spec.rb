require File.dirname(__FILE__) + "/../spec_helper"

describe "$CLASSPATH" do
  describe "<<" do
    it "accepts an object" do
      $CLASSPATH << "file"
      $CLASSPATH.to_a.include?("file:#{File.expand_path('file')}").should == true
    end
    
    it "accepts an array" do
      Dir.chdir(File.join(File.dirname(__FILE__), '..', '..', '..', 'lib')) do
        $CLASSPATH << Dir.glob("*.jar")
        ($CLASSPATH.to_a.include?("file:#{File.expand_path('jruby.jar')}")).should == true
      end
    end
  end
end
