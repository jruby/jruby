require File.expand_path('../../../../spec_helper', __FILE__)

describe "File::Stat#rdev_minor" do
  before :each do
    @name = tmp("file.txt")
    touch(@name)
  end
  after :each do
    rm_r @name
  end

  it "returns the minor part of File::Stat#rdev" do
    File.stat(@name).rdev_minor.should be_kind_of(Integer)
  end
end
