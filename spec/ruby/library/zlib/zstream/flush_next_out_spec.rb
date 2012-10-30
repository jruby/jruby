require File.expand_path('../../../../spec_helper', __FILE__)
require 'zlib'

describe "Zlib::ZStream#flush_next_out" do

  it "flushes the stream and flushes the output buffer" do
    zs = Zlib::Inflate.new
    zs << "x\234K\313\317\a\000\002\202\001E"

    zs.flush_next_out.should == 'foo'
    zs.finished?.should == true
    zs.flush_next_out.should == ''
  end

end

