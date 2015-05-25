require 'coverage'

Coverage.start

require_relative 'subject.rb'

data = Coverage.result.values.first
expected = [1, 1, nil, 1, 10, nil, nil, 1, nil, 1, 1, nil, nil, 1, 2, nil, nil, 1, 1, nil, 1]

p data

raise 'failed' unless data == expected
