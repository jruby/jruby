#!/usr/bin/env jruby -Ku
# -*- coding: utf-8 -*-

if RUBY_VERSION =~ /1\.8/
  require 'tempfile'

  $KCODE = 'u'
  describe "Multibyte requirement: JRUBY-4940" do

    subject do
      file = Tempfile.open('loaded_file')
      file.puts "def \346\227\245\346\234\254\350\252\236;100;end" # UTF-8 'def 日本語;100;end'
      file.close
      file
    end

    after(:all) do
      subject.delete
    end

    it 'can be loaded' do
      lambda{load subject.path}.should_not raise_error
    end
  end
end