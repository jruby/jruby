require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.groups" do
  platform_is_not :windows do
    it "gets an Array of the gids of groups in the supplemental group access list" do
      groups = `id -G`.scan(/\d+/).map {|i| i.to_i}

      Process.groups.each do |g|
        groups.should include(g)
      end
    end

    # NOTE: This is kind of sketchy.
    it "sets the list of gids of groups in the supplemental group access list" do
      groups = Process.groups
      if Process.uid == 0
        Process.groups = []
        Process.groups.should == []
        Process.groups = groups
        Process.groups.sort.should == groups.sort
      else
        lambda { Process.groups = [] }.should raise_error(Errno::EPERM)
      end
    end
  end
end

describe "Process.groups=" do
  it "needs to be reviewed for spec completeness"
end
