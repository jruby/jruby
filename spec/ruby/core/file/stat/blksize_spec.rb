require File.expand_path('../../../../spec_helper', __FILE__)

describe "File::Stat#blksize" do
  before :each do
    @file = tmp('i_exist')
    touch(@file) { |f| f.write "rubinius" }
  end

  after :each do
    rm_r @file
  end

  it "returns the blksize of a File::Stat object" do
    st = File.stat(@file)
    st.blksize.is_a?(Integer).should == true
    st.blksize.should > 0
  end
end
