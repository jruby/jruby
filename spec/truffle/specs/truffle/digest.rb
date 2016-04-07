# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../ruby/spec_helper'

require 'digest'

describe "digest updating" do
  
  it "works on leaf ropes" do
    Digest::MD5.hexdigest('foo').should == 'acbd18db4cc2f85cedef654fccc4a4d8'
  end
  
  it "works on concat ropes" do
    Digest::MD5.hexdigest('foo' + 'bar').should == '3858f62230ac3c915f300c664312c63f'
  end
  
  it "works on substring ropes" do
    Digest::MD5.hexdigest('foo'[1...-1]).should == 'd95679752134a2d9eb61dbd7b91c4bcc'
  end
  
  it "works on substring ropes that cross a concat rope" do
    Digest::MD5.hexdigest(('foo' + 'bar')[2...-2]).should == '99faee4e1a331a7595932b7c18f9f5f6'
  end
  
  it "works on substring ropes that only use the left of a concat rope" do
    Digest::MD5.hexdigest(('foo' + 'bar')[1...2]).should == 'd95679752134a2d9eb61dbd7b91c4bcc'
  end
  
  it "works on substring ropes that only use the right of a concat rope" do
    Digest::MD5.hexdigest(('foo' + 'bar')[4...5]).should == '0cc175b9c0f1b6a831c399e269772661'
  end
  
end
