describe "defined?(::BasicObject)" do
  it "returns \"constant\"" do
    expect(defined?(::BasicObject)).to eq("constant")
  end
end
