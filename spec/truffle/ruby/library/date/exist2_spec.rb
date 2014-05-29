require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/valid_ordinal', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.exist2?" do
    it_behaves_like :date_valid_ordinal?, :exist2?
  end
end
