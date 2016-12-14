require 'spec_helper'

describe "Running mspec" do
  it "runs the specs" do
    fixtures = "spec/fixtures"
    cwd = Dir.pwd

    cmd = "bin/mspec run"
    cmd << " -B #{fixtures}/config.mspec"
    cmd << " #{fixtures}/a_spec.rb"
    out = `#{cmd}`
    out = out.lines.reject { |line|
      line.chomp == RUBY_DESCRIPTION
    }.join
    out = out.gsub(/\d\.\d{6}/, "D.DDDDDD")
    out.should == <<EOS
.FE

1)
Foo#bar errors FAILED
Expected 1
 to equal 2

#{cwd}/spec/fixtures/a_spec.rb:8:in `block (2 levels) in <top (required)>'
#{cwd}/spec/fixtures/a_spec.rb:2:in `<top (required)>'
#{cwd}/bin/mspec-run:7:in `<main>'

2)
Foo#bar fails ERROR
RuntimeError: failure
#{cwd}/spec/fixtures/a_spec.rb:12:in `block (2 levels) in <top (required)>'
#{cwd}/spec/fixtures/a_spec.rb:2:in `<top (required)>'
#{cwd}/bin/mspec-run:7:in `<main>'

Finished in D.DDDDDD seconds

1 file, 3 examples, 2 expectations, 1 failure, 1 error, 0 tagged
EOS
  end
end
