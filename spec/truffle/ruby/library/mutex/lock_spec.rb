require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/lock', __FILE__)

ruby_version_is "".."1.9" do
  require 'thread'

  describe "Mutex#lock" do
    it_behaves_like :mutex_lock, :lock
  end
end
