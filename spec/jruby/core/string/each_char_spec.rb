describe "An ASCII string with high-range bytes" do
  it "can each_char through the high bytes successfully" do
    ary = []
    str = "M\xA1xico".force_encoding("ASCII")
    str.each_char {|c| ary << c}

    expect(ary.length).to eq 6
    expect(ary).to eq(['M', "\xA1", 'x', 'i', 'c', 'o'].map{|c| c.force_encoding('ASCII')})
  end
end
