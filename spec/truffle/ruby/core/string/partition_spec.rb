require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is '1.8.7' do
  describe "String#partition with String" do
    it "returns an array of substrings based on splitting on the given string" do
      "hello world".partition("o").should == ["hell", "o", " world"]
    end

    it "always returns 3 elements" do
      "hello".partition("x").should == ["hello", "", ""]
      "hello".partition("hello").should == ["", "hello", ""]
    end

    it "accepts regexp" do
      "hello!".partition(/l./).should == ["he", "ll", "o!"]
    end

    it "sets global vars if regexp used" do
      "hello!".partition(/(.l)(.o)/)
      $1.should == "el"
      $2.should == "lo"
    end

    ruby_bug "redmine #1510", '1.9.1' do
      it "converts its argument using :to_str" do
        find = mock('l')
        find.should_receive(:to_str).and_return("l")
        "hello".partition(find).should == ["he","l","lo"]
      end
    end

    it "raises error if not convertible to string" do
      lambda{ "hello".partition(5) }.should raise_error(TypeError)
      lambda{ "hello".partition(nil) }.should raise_error(TypeError)
    end

    it "takes precedence over a given block" do
      "hello world".partition("o") { true }.should == ["hell", "o", " world"]
    end
  end
end

ruby_version_is ''...'1.9' do
  describe "String#partition with a block" do
    it "is still available" do
      "hello\nworld".partition{|w| w < 'k' }.should == [["hello\n"], ["world"]]
    end
  end
end
