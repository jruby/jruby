# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Runtime.dump_string" do

  it "returns a String" do
    Truffle::Runtime.dump_string('foo').should be_kind_of(String)
  end

  it "returns a sequence of escaped bytes in lower case" do
    Truffle::Runtime.dump_string('foo').should =~ /(\\x[0-9a-f][0-9a-f])+/
  end

  it "returns correct bytes for the given string" do
    Truffle::Runtime.dump_string('foo').should == "\\x66\\x6f\\x6f"
  end

end
