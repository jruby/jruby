describe "inspect called against the \"top self\"" do
  it "returns \"main\"" do
    topself_inspect = eval 'self.inspect', TOPLEVEL_BINDING

    expect(topself_inspect).to eq("main")
  end
end
