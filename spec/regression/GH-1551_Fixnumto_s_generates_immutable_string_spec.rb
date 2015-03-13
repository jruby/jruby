describe "Fixnum#to_s" do
  it "returns muttable string" do
    str = 100.to_s
    str.should == '100'
    str[0] = '2'
    str.should == '200'
  end
end