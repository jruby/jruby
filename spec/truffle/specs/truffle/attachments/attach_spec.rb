# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Attachments.attach" do
  
  def fixture
    x = 14
    y = 15
    y = 16
    x
  end

  before :each do
    @attachments = []
  end

  after :each do
    @attachments.each(&:detach)
  end

  it "installs a block to be run on a line" do
    scratch = [false]
    
    @attachments << Truffle::Attachments.attach(__FILE__, 14) do
      scratch[0] = true
    end

    fixture.should == 14
    scratch[0].should be_true
  end

  it "only runs the block once each time the line is executed" do
    scratch = []
    
    @attachments << Truffle::Attachments.attach(__FILE__, 14) do
      scratch << :run
    end

    fixture.should == 14
    scratch.size.should == 1
  end
  
  it "allows multiple blocks to be installed on the same line and runs both of them" do
    scratch = []
    
    @attachments << Truffle::Attachments.attach(__FILE__, 14) do
      scratch << 1
    end
    
    @attachments << Truffle::Attachments.attach(__FILE__, 14) do
      scratch << 2
    end
    
    fixture.should == 14
    scratch.sort.should == [1, 2]
  end
  
  it "supplies a Binding to the block" do
    @attachments << Truffle::Attachments.attach(__FILE__, 14) do |binding|
      binding.should be_kind_of(Binding)
    end
    fixture.should == 14
  end
  
  it "allows read access to local variables in the block" do
    @attachments << Truffle::Attachments.attach(__FILE__, 15) do |binding|
      binding.local_variable_get(:x).should == 14
    end
    fixture.should == 14
  end
  
  it "allows write access to local variables in the block" do
    @attachments << Truffle::Attachments.attach(__FILE__, 15) do |binding|
      binding.local_variable_set(:x, 100)
    end
    fixture.should == 100
  end
  
  it "runs the block before running the line" do
    @attachments << Truffle::Attachments.attach(__FILE__, 16) do |binding|
      binding.local_variable_get(:x).should == 14
      binding.local_variable_get(:y).should == 15
    end
    fixture.should == 14
  end

end
