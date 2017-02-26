require 'spec_helper'

describe "Running mspec" do
  it "runs the specs" do
    fixtures = "spec/fixtures"
    out = run_mspec("run", "#{fixtures}/a_spec.rb")
    out.should == <<EOS
RUBY_DESCRIPTION
.FE

1)
Foo#bar errors FAILED
Expected 1
 to equal 2

CWD/spec/fixtures/a_spec.rb:8:in `block (2 levels) in <top (required)>'
CWD/spec/fixtures/a_spec.rb:2:in `<top (required)>'
CWD/bin/mspec-run:7:in `<main>'

2)
Foo#bar fails ERROR
RuntimeError: failure
CWD/spec/fixtures/a_spec.rb:12:in `block (2 levels) in <top (required)>'
CWD/spec/fixtures/a_spec.rb:2:in `<top (required)>'
CWD/bin/mspec-run:7:in `<main>'

Finished in D.DDDDDD seconds

1 file, 3 examples, 2 expectations, 1 failure, 1 error, 0 tagged
EOS
  end
end
