# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

if Truffle::Boot.rubygems_enabled?
  module Gem
    # Update the default_dir to match JRuby's.
    def self.default_dir
      File.expand_path(File.join(ConfigMap[:libdir], '..', '..', 'ruby', 'gems', 'shared'))
    end
  end
end
