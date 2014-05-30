describe :continuation_new, :shared => true do
  it "raises a NoMethodError" do
    lambda { Continuation.new }.should raise_error(NoMethodError)
  end
end
