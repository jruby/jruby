require File.expand_path('../../../dir/fixtures/common', __FILE__)

describe :open_directory, :shared => true do
  it "opens directories" do
    File.send(@method, tmp("")).should be_kind_of(File)
  end
end
