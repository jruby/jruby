require 'date'
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/civil', __FILE__)
require File.expand_path('../shared/new_bang', __FILE__)

describe "Date.new" do
  it_behaves_like(:date_civil, :new)
end

ruby_version_is "" ... "1.9.3" do
  describe "Date.new!" do
    it_behaves_like(:date_new_bang, :new!)
  end
end
