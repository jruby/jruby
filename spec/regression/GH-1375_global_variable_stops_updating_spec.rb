# Spec for global variable behavior fixed for GH-1375.
#
# The bug caused global variables to stop updating after a particular number
# of updates. Specifically, with invokedynamic enabled, in order to reduce
# the cost of continuing to update global variables past a particular count,
# caching was turned off. However, the final update that switched off
# caching did not also properly invalidate the old value, allowing it to
# remain cached forever.
#
# This test puts the global variable updating logic in a method body, to
# help ensure it will run both interpreted and compiled in our various
# execution modes.

$gh1375

def gh1375_updater
  old = $gh1375
  $gh1375 += 1
  return old, $gh1375
end

describe "A global variable" do
  before :each do
    $gh1375 = 0
  end

  it "always updates for read when modified" do
    accum1 = []
    accum2 = []
    100_000.times do |i|
      accum1[i], accum2[i] = gh1375_updater
    end

    expect(accum1) == (0...100_000).to_a
    expect(accum2) == (1..100_000).to_a
  end
end