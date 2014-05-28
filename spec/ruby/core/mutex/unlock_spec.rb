require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/unlock', __FILE__)

describe "Mutex#unlock" do
  it_behaves_like :mutex_unlock, :unlock
end
