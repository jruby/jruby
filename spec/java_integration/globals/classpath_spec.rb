require File.dirname(__FILE__) + "/../spec_helper"

describe "$CLASSPATH" do
  describe "<<" do
    it "accepts an object" do
      $CLASSPATH << "file"
      expect($CLASSPATH.to_a.include?("file:#{File.expand_path('file')}")).to eq(true)
    end
    
    it "accepts an array" do
      Dir.chdir(File.join(File.dirname(__FILE__), '..', '..', '..', 'test')) do
        $CLASSPATH << Dir.glob("classpath*.jar")
        expect($CLASSPATH.to_a.include?("file:#{File.expand_path('classpath_test.jar')}")).to eq(true)
      end
      expect(JRuby.runtime.getJRubyClassLoader.getResource('test_value.rb')).not_to eq(nil)
    end
  end
end
