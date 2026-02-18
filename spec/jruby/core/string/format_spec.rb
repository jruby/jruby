# encoding: utf-8
describe "String#format with %s passed US-ASCII input" do
  it "preserves the encoding of the ASCII-compatible format string" do
    format = "%s ø"
    formatted = format % 1

    expect(formatted).to eq("1 ø")
    expect(formatted.encoding).to eq(Encoding::UTF_8)
  end
end

