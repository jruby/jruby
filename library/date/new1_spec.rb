require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/jd', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.new1" do
    it_behaves_like :date_jd, :new1
  end
end
