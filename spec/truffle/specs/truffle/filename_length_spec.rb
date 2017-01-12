# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../ruby/spec_helper'

describe "Truffle source files" do
  it "have a small enough filename for eCryptfs" do
    # For eCryptfs, see https://bugs.launchpad.net/ecryptfs/+bug/344878
    max_length = 143

    root = Dir.pwd
    File.directory?("#{root}/.git").should be_true # Make sure we are at the root

    too_long = []
    Dir.chdir(root) do
      Dir.glob("**/*") do |f|
        if File.basename(f).size > max_length
          too_long << f
        end
      end
    end

    too_long.should == []
  end
end
