require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/abs', __FILE__)

ruby_version_is '1.9' do
  describe "Bignum#magnitude" do
    it_behaves_like(:bignum_abs, :magnitude)
  end
end


