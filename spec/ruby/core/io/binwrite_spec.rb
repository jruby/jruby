require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/binwrite', __FILE__)

ruby_version_is "1.9.3" do
  describe "IO.binwrite" do
    it_behaves_like :io_binwrite, :binwrite
    
    it "needs to be reviewed for spec completeness"
  end
end
