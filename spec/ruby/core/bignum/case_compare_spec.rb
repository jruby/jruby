require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/equal', __FILE__)

ruby_version_is "1.9" do
  describe "Bignum#===" do
    it_behaves_like :bignum_equal, :===
  end
end
