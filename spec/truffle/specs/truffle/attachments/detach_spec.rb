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
    x
  end

  it "returns nil" do
    Truffle::Attachments.detach(__FILE__, 14).should be_nil 
  end
  
  it "removes a previous attachment" do
    scratch = []

    scratch[0] = false
    Truffle::Attachments.attach __FILE__, 14 do
      scratch[0] = true
    end
    fixture.should == 14
    scratch[0].should be_true

    scratch[0] = false
    Truffle::Attachments.detach __FILE__, 14
    fixture.should == 14
    scratch[0].should be_false
  end
  
  it "removes multiple previous attachments" do
    scratch = []
    Truffle::Attachments.attach __FILE__, 14 do
      scratch << 1
    end
    Truffle::Attachments.attach __FILE__, 14 do
      scratch << 2
    end
    fixture.should == 14
    scratch.sort.should == [1, 2]

    Truffle::Attachments.detach __FILE__, 14
    fixture.should == 14
    scratch.sort.should == [1, 2]
  end

end
