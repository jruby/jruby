require 'rspec'

describe "JRUBY-5946 IO.select returns write array with nil elements" do
  it "returns writes elements correctly even when there aren't reads nor errors elements" do
    @rd, @wr = IO.pipe
    rd_arr, wr_arr, err_arr = IO.select([@rd,@wr],[@wr],[],0)
    rd_arr.size.should == 0
    wr_arr.size.should == 1
    wr_arr.should == [@wr]
    err_arr.size.should == 0     
  end
end
