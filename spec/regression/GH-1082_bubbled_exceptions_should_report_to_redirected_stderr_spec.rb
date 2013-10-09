# GH-1082: Redirecting STDERR not working as expected
# https://github.com/jruby/jruby/issues/1082

require 'rbconfig'
require 'tempfile'

describe "An exception that bubbles out when $stderr is redirected" do
  before :each do
    @ruby = '"' + File.join([RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name']]) << RbConfig::CONFIG['EXEEXT'] + '"'
    @tmpfile = Tempfile.new("GH-1082")
  end
  
  it "logs to the redirected target" do
    system(
        "#{@ruby} -e '$stderr.reopen(File.open(\"#{@tmpfile.path}\", \"w\")); $stderr.puts \"first line\"; warn \"second line\"; raise'"
    )

    lines = File.readlines(@tmpfile.path)
    lines[0].should == "first line\n"
    lines[1].should == "second line\n"
    lines[2].should == "RuntimeError: No current exception\n"
  end
end
