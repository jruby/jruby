require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/unlock', __FILE__)

ruby_version_is "".."1.9" do
  require 'thread'

  describe "Mutex#unlock" do
    it_behaves_like :mutex_unlock, :unlock
  end
end
