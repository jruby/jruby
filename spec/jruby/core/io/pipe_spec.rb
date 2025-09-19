require 'rspec'

describe "JRUBY-5946 IO.select returns write array with nil elements" do
  it "returns writes elements correctly even when there aren't reads nor errors elements" do
    @rd, @wr = IO.pipe
    rd_arr, wr_arr, err_arr = IO.select([@rd,@wr],[@wr],[],0)
    expect(rd_arr.size).to eq(0)
    expect(wr_arr.size).to eq(1)
    expect(wr_arr).to eq([@wr])
    expect(err_arr.size).to eq(0)     
  end
end
