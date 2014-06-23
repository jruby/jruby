describe "String#rindex" do
  it "works with matchdata" do
    message = "I love this new status update..."
    matched_string = message.match(/status update/)[0]
    expect(message.rindex(matched_string)).to eql 16
  end
end
