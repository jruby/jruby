# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# TODO CS 19-Feb-16 Not compliant with MRI - here as a regression test

require 'coverage'

Coverage.start

require_relative 'subject.rb'

data = Coverage.result[File.join(File.dirname(__FILE__), 'subject.rb')]
expected = [0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 2, 20, 0, 0, 2, 0, 4, 2, 0, 0, 2, 4, 0, 0, 2, 2, 0, 2] # Doubled line counts

raise 'coverage data not as expected' unless data == expected
