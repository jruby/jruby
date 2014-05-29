require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/process/times', __FILE__)

describe "Process.times" do
  it_behaves_like :process_times, :times, Process
end
