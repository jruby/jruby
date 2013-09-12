# -*- coding: utf-8 -*-
require 'rspec'
require 'tempfile'


# This problem was originally reported against UTF-8 but the underlying
# problem was default_external encoding was being used to write IO instead
# the file encoding.  So we will use utf-16be since no native platform
# uses that for default_external.
if RUBY_VERSION >= "1.9"
  describe 'utf-16BE should write regardless of underlying locale encoding' do
    str = "My UTF-8 String: ✓ ®".force_encoding("UTF-16BE")
  
    it "should read the written string" do
      file = Tempfile.new 'temp'
      path = file.path
      file.write str
      file.close
  
      read = File.read(path, :encoding => 'utf-16be', :binmode => true)
      read.should == str
  
      read = File.open(path, 'rb:utf-16be') { |f| f.read }
      read.should == str
      
      File.unlink(path)
    end
  end
end
