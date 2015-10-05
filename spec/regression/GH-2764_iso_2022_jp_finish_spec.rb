describe "UTF-8 text transcoded to ISO-2022-JP" do
  it "does not include null bytes in finish sequence" do
    str = "あいうえお"
    dst = ""
    ec = Encoding::Converter.new str.encoding, Encoding::ISO_2022_JP, 0
    ec.primitive_convert str.dup, dst, nil, nil, 0
    expect(dst.bytesize).to eq(16)
  end
end