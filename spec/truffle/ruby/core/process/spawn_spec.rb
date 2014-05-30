require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/process/spawn', __FILE__)

ruby_version_is "1.9" do
  describe "Process.spawn" do
    it_behaves_like :process_spawn, :spawn, Process
  end
end
