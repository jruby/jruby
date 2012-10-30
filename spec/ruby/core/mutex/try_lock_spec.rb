require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/try_lock', __FILE__)

ruby_version_is "1.9" do
  describe "Mutex#try_lock" do
    it_behaves_like :mutex_try_lock, :try_lock
  end
end
