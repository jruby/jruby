require 'coverage'

Coverage.start

require_relative 'subject.rb'

data = Coverage.result[File.join(File.dirname(__FILE__), 'subject.rb')]
expected = [1, 1, nil, 1, 10, nil, nil, 1, nil, 1, 1, nil, nil, 1, 2, nil, nil, 1, 1, nil, 1]

raise 'failed' unless data == expected
