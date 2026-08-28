describe "An unselectable (for read) channel" do
  it "is considered ready by IO.select" do
    bios = java.io.ByteArrayInputStream.new('hello'.to_java_bytes)
    ch = java.nio.channels.Channels.new_channel(bios)
    io = ch.to_io

    reads, * = IO.select([io], nil, nil, 1000)

    expect(reads.size).to eq(1)
    expect(reads[0]).to eq(io)
  end
end