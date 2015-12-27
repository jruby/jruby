# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'coverage'

Coverage.start

require_relative 'subject.rb'

data = Coverage.result[File.join(File.dirname(__FILE__), 'subject.rb')]
expected = [1, 1, nil, 1, 10, nil, nil, 1, nil, 1, 1, nil, nil, 1, 2, nil, nil, 1, 1, nil, 1]

raise 'failed' unless data == expected
