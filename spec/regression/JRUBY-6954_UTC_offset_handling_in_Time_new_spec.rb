describe "JRUBY-6954: Time.new does not respect UTC offset in the 7-argument form" do
  it "should respect the UTC offset when called with 7 arguments" do
    t1 = Time.new(2012, 10, 19, nil, nil, nil, "-05:00")
    t2 = Time.new(2012, 10, 19, nil, nil, nil, "+03:00")

    expect(t1.utc_offset).to eq(-18000)
    expect(t2.utc_offset).to eq(10800)
  end
end
