require 'tempfile'

# jruby/jruby#3249
describe "IO#syswrite" do
  it "does not freeze the string to be written" do
    begin
      w = Tempfile.new('gh3249')
      str = "string"
      w.syswrite(str)

      str.frozen?.should_not == true
    ensure
      w.close
    end
  end
end