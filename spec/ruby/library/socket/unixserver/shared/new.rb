require File.expand_path('../../../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/classes', __FILE__)
require 'tempfile'

describe :unixserver_new, :shared => true do
  platform_is_not :windows do
    it "creates a new UNIXServer" do
      path = tmp("unixserver_spec")
      File.unlink(path) if File.exists?(path)
      unix = UNIXServer.send(@method, path)
      unix.path.should == path
      unix.addr.should == ["AF_UNIX", path]
      File.unlink(path)
    end
  end
end
