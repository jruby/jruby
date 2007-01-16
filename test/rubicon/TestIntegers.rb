$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestIntegers < Rubicon::TestCase

end

# Run these tests if invoked directly

Rubicon::handleTests(TestIntegers) if $0 == __FILE__
