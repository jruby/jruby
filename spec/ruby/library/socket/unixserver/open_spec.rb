require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)

describe "UNIXServer.open" do
  it_behaves_like :unixserver_new, :open
end
