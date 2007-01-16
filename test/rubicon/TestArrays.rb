$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestArrays < Rubicon::TestCase

end

# Run these tests if invoked directly

Rubicon::handleTests(TestArrays) if $0 == __FILE__
