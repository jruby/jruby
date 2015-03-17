
describe :string_each_codepoint_without_block, :shared => true do
  it "returns an Enumerator when no block is given" do
    "".send(@method).should be_an_instance_of(enumerator_class)
  end

  it "returns an Enumerator when no block is given even when self has an invalid encoding" do
    s = "\xDF".force_encoding(Encoding::UTF_8)
    s.valid_encoding?.should be_false
    s.send(@method).should be_an_instance_of(enumerator_class)
  end
end
