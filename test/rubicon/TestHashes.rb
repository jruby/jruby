$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

#
# The Hash tests are also in ../builtins/TestHash
#

class TestHashes < Rubicon::TestCase

end

# Run these tests if invoked directly

Rubicon::handleTests(TestHashes) if $0 == __FILE__
