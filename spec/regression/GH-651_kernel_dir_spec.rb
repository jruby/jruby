describe "Kernel#__dir__" do
  it "returns the parent dir of the current file" do
    expect(File.dirname(__FILE__)).to eq(__dir__)
  end
end if RUBY_VERSION > "2.0"