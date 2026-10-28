# https://github.com/jruby/jruby/issues/9590
describe "IO#tty?" do
  describe "on stdio" do
    it "should return false if JRuby was launched without native access or a terminal" do
      skip unless STDIN.tty? && STDOUT.tty? && STDERR.tty?

      output = `#{ENV_JAVA["jruby.home"]}/bin/jruby -Xnative.enabled=false -e "print STDIN.tty?" < /dev/null`
      output.should == "false"

      output = `#{ENV_JAVA["jruby.home"]}/bin/jruby -Xnative.enabled=false -e "print STDERR.tty?" 2> /dev/null`
      output.should == "false"
    end
  end
end
