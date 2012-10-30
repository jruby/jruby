require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)

describe "UNIXSocket.open" do
  it_behaves_like :unixsocket_new, :open
end
