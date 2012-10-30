require File.expand_path('../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Magic comment" do
    it "is optional" do
      lambda { eval("__ENCODING__") }.should_not raise_error(SyntaxError)
    end

    it "determines __ENCODING__" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
# coding: ASCII-8BIT
__ENCODING__
EOS
    end

    it "is case-insensitive" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
# CoDiNg:   aScIi-8bIt
__ENCODING__
EOS
    end

    it "must be at the first line" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::US_ASCII

# coding: ASCII-8BIT
__ENCODING__
EOS
    end

    it "must be the first token of the line" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::US_ASCII
1+1 # coding: ASCII-8BIT
__ENCODING__
EOS
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
    # coding: ASCII-8BIT
__ENCODING__
EOS
    end

    it "can be after the shebang" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
#!/usr/bin/ruby -Ku
# coding: ASCII-8BIT
__ENCODING__
EOS
    end

    it "can take Emacs style" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
# -*- coding: ascii-8bit -*-
__ENCODING__
EOS
    end

    it "can take vim style" do
      eval(<<EOS.force_encoding("US-ASCII")).should == Encoding::ASCII_8BIT
# vim: filetype=ruby, fileencoding=ascii-8bit, tabsize=3, shiftwidth=3
__ENCODING__
EOS
    end
  end
end
