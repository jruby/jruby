require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Socket.gethostname" do
  # This currently works in Unix and Windows. Edit the helper
  # to add other platforms.
  it "returns the host name" do
    Socket.gethostname.should == hostname
  end
end
