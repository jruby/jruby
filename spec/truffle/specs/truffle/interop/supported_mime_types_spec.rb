# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Interop.supported_mime_types" do

  it "returns an Array" do
    Truffle::Interop.supported_mime_types.should be_kind_of(Array)
  end
  
  it "includes application/x-ruby" do
    Truffle::Interop.supported_mime_types.should include('application/x-ruby')
  end

end
