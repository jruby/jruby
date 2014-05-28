require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/lock', __FILE__)

describe "Mutex#lock" do
  it_behaves_like :mutex_lock, :lock
end
