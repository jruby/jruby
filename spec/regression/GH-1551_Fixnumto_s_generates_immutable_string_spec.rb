describe "Fixnum#to_s" do
  it "returns muttable string" do
    str = 100.to_s
    expect(str).to eq('100')
    str[0] = '2'
    expect(str).to eq('200')
  end
end