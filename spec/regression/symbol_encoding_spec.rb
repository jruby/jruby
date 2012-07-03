# encoding: utf-8
require 'rspec'

if RUBY_VERSION >= "1.9.2"
  describe "symbol encoding" do
    context "for ASCII symbols" do
      it "should be US-ASCII" do
        :foo.encoding.name.should == "US-ASCII"
      end

      it "should be US-ASCII after converting to string" do
        :foo.to_s.encoding.name.should == "US-ASCII"
      end
    end

    context "for UTF-8 symbols" do
      it "should be UTF-8" do
        :åäö.encoding.name.should == "UTF-8"
      end

      it "should be UTF-8 after converting to string" do
        :åäö.to_s.encoding.name.should == "UTF-8"
      end
    end
  end
end

