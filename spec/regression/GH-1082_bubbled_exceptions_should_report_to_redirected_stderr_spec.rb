# GH-1082: Redirecting STDERR not working as expected
# https://github.com/jruby/jruby/issues/1082

require 'rbconfig'
require 'tempfile'

describe "An exception that bubbles out when $stderr is redirected" do
  before :each do
    @ruby = '"' + File.join([RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name']]) << RbConfig::CONFIG['EXEEXT'] + '"'
    @tmpfile = Tempfile.new("GH-1082")
  end

  after :each do
    @tmpfile.close! rescue nil
  end
  
  it "logs to the redirected target" do
    system(
        "#{@ruby} -e '$stderr.reopen(File.open(\"#{@tmpfile.path}\", \"w\")); $stderr.puts \"first line\"; warn \"second line\"; raise'"
    )

    lines = File.readlines(@tmpfile.path)
    expect(lines[0]).to eq("first line\n")
    expect(lines[1]).to eq("second line\n")
    expect(lines[2]).to eq("RuntimeError: No current exception\n")
  end
end
