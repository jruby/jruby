require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/lambda', __FILE__)

# The functionality of Proc objects is specified in core/proc

describe "Kernel.proc" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:proc)
  end

  it_behaves_like(:kernel_lambda, :proc)

  ruby_version_is ""..."1.9" do
    it_behaves_like(:kernel_lambda_return_like_method, :proc)
  end

  ruby_version_is "1.9" do
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
end

describe "Kernel#proc" do
  it "needs to be reviewed for spec completeness"
end
