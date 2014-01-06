require File.expand_path('../../../shared/complex/float/arg', __FILE__)

ruby_version_is "1.9" do
  describe "Float#phase" do
    it_behaves_like :float_arg, :phase
  end
end
