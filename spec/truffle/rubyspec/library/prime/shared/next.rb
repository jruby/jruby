require 'stringio'

describe :prime_next, :shared => true do
  before(:all) do
    @orig_stderr = $stderr
    $stderr = StringIO.new('', 'w') # suppress warning
  end
  after(:all) do
    $stderr = @orig_stderr
  end

  it "returns the element at the current position and moves forward" do
    p = Prime.new
    p.next.should == 2
    p.next.should == 3
    p.next.next.should == 6
  end
end
