require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/synchronize', __FILE__)

describe "Mutex#synchronize" do
  it_behaves_like :mutex_synchronize, :synchronize
end
