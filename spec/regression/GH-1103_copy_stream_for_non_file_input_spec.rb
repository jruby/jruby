# GH-1103: Support non-files for IO.copy_stream
# https://github.com/jruby/jruby/issues/1103

describe 'Process#copy_stream when given a popen-based "from"' do
  it 'copies the bytes from the process to the target stream' do
    from = IO.popen("echo test")
    to = IO.popen("cat", "r+")

    IO.copy_stream(from, to)

    expect(to.read(5)).to eq("test\n")
  end
end if RUBY_VERSION > "1.9"
