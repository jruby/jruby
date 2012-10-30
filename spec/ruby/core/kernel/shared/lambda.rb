describe :kernel_lambda, :shared => true do
  it "returns a Proc object" do
    send(@method) { true }.kind_of?(Proc).should == true
  end

  it "raises an ArgumentError when no block is given" do
    lambda { send(@method) }.should raise_error(ArgumentError)
  end

end

describe :kernel_lambda_return_like_method, :shared => true do
  it "returns from the #{@method} itself, not the creation site of the #{@method}" do
    @reached_end_of_method = nil
    def test
      send(@method) { return }.call
      @reached_end_of_method = true
    end
    test
    @reached_end_of_method.should be_true
  end

  it "allows long returns to flow through it" do
    KernelSpecs::Lambda.new.outer(@method).should == :good
  end
end
