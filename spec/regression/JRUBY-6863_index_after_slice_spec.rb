# -*- coding: utf-8 -*-
require 'rspec'

if RUBY_VERSION =~ /1\.9/
  describe 'JRUBY-6863' do
    let(:str) do 
      str = "あいうえおかきくけこ"
    end

    subject do
      str.slice!(3..-1) # => "えおかきくけこ"
    end

    it 'String#index without args' do 
      # See http://jira.codehaus.org/browse/JRUBY-xxxx
      subject.index(/[^ ]/).should == 0
    end

    it 'String#index with args' do
      subject.index(/[^ ]/, 2).should == 2
    end

    it 'String#rindex without args' do 
      subject.rindex(/[^ ]/).should == 6
    end

    it 'String#rindex with args' do 
      subject.rindex(/[^ ]/, 2).should == 2
    end
  end
end
