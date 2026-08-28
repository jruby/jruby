describe "Kernel#Integer when given a string that contains invalid characters" do
  it "rejects the string" do
    expect(Integer("46_11_686_0184273_87904")).to eql(46_11_686_0184273_87904)
  end
end

