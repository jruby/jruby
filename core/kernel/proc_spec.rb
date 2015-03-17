require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/lambda', __FILE__)

# The functionality of Proc objects is specified in core/proc

describe "Kernel.proc" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:proc)
  end

  it_behaves_like(:kernel_lambda, :proc)

  it "returns from the creation site of the proc, not just the proc itself" do
    @reached_end_of_method = nil
    def test
      proc { return }.call
      @reached_end_of_method = true
    end
    test
    @reached_end_of_method.should be_nil
  end
end

describe "Kernel#proc" do
  it "uses the implicit block from an enclosing method" do
    def some_method
      proc
    end

    prc = some_method { "hello" }

    prc.call.should == "hello"
  end

  it "needs to be reviewed for spec completeness"
end
