# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

unless Truffle::Interop.mime_type_supported?('application/javascript')
  puts "JavaScript doesn't appear to be available - skipping execjs test"
  exit
end

require 'execjs'
require 'truffle/execjs'

exit 1 unless ExecJS.runtime.name == 'Truffle'
