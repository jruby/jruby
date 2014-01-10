require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../../../shared/file/world_writable', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "1.9" do
  describe "File::Stat.world_writable?" do

    it_behaves_like(:file_world_writable, :world_writable?, FileStat)

  end
end

ruby_version_is "1.9" do
  describe "File::Stat#world_writable?" do
    it "needs to be reviewed for spec completeness"
  end
end
