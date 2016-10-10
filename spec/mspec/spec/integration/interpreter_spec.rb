require 'spec_helper'

describe "The interpreter passed with -t" do
  it "is used in subprocess" do
    fixtures = "spec/fixtures"
    interpreter = "#{fixtures}/my_ruby"
    cmd = "bin/mspec run"
    cmd << " -B #{fixtures}/config.mspec"
    cmd << " #{fixtures}/print_interpreter_spec.rb"
    cmd << " -t #{interpreter}"
    out = `#{cmd}`
    out = out.lines.map(&:chomp).reject { |line|
      line == RUBY_DESCRIPTION
    }.take(3)
    out.should == [
      interpreter,
      interpreter,
      File.expand_path(interpreter)
    ]
  end
end
