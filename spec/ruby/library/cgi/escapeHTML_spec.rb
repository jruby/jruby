require File.expand_path('../../../spec_helper', __FILE__)
require 'cgi'

describe "CGI.escapeHTML" do
  ruby_version_is ""..."2.0" do
    it "escapes special HTML characters (&\"<>) in the passed argument" do
      CGI.escapeHTML('& < > "').should == '&amp; &lt; &gt; &quot;'
    end

    it "does not escape any other characters" do
      chars = " !\#$%'()*+,-./0123456789:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
      CGI.escapeHTML(chars).should == chars
    end
  end

  ruby_version_is "2.0" do
    it "escapes special HTML characters (&\"<>') in the passed argument" do
      CGI.escapeHTML(%[& < > " ']).should == '&amp; &lt; &gt; &quot; &#39;'
    end

    it "does not escape any other characters" do
      chars = " !\#$%()*+,-./0123456789:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
      CGI.escapeHTML(chars).should == chars
    end
  end
end
