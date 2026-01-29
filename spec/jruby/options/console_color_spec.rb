# https://github.com/jruby/jruby/issues/8934
describe "org.jruby.util.cli.Options::IR_PRINT_COLOR" do
  it "should be off for non-terminals" do
    output = `#{ENV_JAVA["jruby.home"]}/bin/jruby -e "print org.jruby.util.cli.Options::IR_PRINT_COLOR.load.to_s"`
    output.chomp.should == "false"
  end
end
