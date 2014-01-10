require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../../../shared/file/zero', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "File::Stat#zero?" do
  it_behaves_like :file_zero, :zero?, FileStat

  platform_is :solaris do
    it "returns false for /dev/null" do
      FileStat.zero?('/dev/null').should == false
    end
  end
end
