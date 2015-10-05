# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::CExt.load_files" do
  
  if Truffle::CExt.supported?

    it "needs to be reviewed for spec completeness"

  else

    it "raises a RuntimeError" do
      files = [
        File.expand_path('fixtures/foo/ext/foo/foo.c', __FILE__),
        File.expand_path('fixtures/foo/ext/foo/add.c', __FILE__)
      ]
      lambda {
        Truffle::CExt.load_files ['Init_foo'], [], files
      }.should raise_error(RuntimeError)
    end

  end

end
