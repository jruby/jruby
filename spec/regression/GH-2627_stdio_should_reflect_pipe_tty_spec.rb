describe "A stdio stream redirected from a pipe" do
  it "must report false for tty" do
    io = IO.popen("bin/jruby -e 'puts $stdin.tty?'", 'r+')
    expect(io.read.chomp).to eq("false")
  end
end