$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestClasses < Rubicon::TestCase

end

# Run these tests if invoked directly

Rubicon::handleTests(TestClasses) if $0 == __FILE__
