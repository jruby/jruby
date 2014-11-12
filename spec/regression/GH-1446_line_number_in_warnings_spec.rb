# Regression test for https://github.com/jruby/jruby/issues/1446
# Line number in runtime warnings is one greater than the actual line number

describe "JRuby warnings" do
  specify "have the correct line number" do
    warning = "(eval):1 warning: already initialized constant GH1446\n"
    expect($stderr).to receive(:write).with(warning)
    eval "GH1446=0;GH1446=1"
  end
end
