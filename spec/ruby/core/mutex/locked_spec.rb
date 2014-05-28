require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/mutex/locked', __FILE__)

describe "Mutex#locked?" do
  it_behaves_like :mutex_locked, :locked?
end
