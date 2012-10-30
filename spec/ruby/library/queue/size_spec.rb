require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'
require File.expand_path('../shared/length', __FILE__)

describe "Queue#size" do
  it_behaves_like :queue_length, :size
end
