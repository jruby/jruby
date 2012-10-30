require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'
require File.expand_path('../shared/enque', __FILE__)

describe "Queue#<<" do
  it_behaves_like :queue_enq, :<<
end
