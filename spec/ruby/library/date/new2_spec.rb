require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/ordinal', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.new2" do
    it_behaves_like :date_ordinal, :new2
  end
end
