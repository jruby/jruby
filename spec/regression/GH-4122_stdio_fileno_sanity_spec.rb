# The fix for stdio disappearing from Drip (#2690) modified stdio streams to report their *actual* fileno rather than
# the standard 0,1,2 typical of a "simple" process. However this broke the childprocess gem on Windows because it
# needs the stdio streams to have reasonable fileno and non-native IO on Windows started to use our fake internal
# fileno. This test is to confirm that any non-native JRuby instance will still have sane fileno for stdio, since
# even if stdio is not on 0,1,2 we can't see it without native integration anyway.
# See #4122.
describe "#4122 Stdio streams in a simple non-native JRuby instance" do
  it "should have sane fileno" do
    jruby_path = File.join(ENV_JAVA['jruby.home'], "bin", "jruby")
    output = `#{jruby_path} -Xnative.enabled=false -e 'p [$stdin.fileno,$stdout.fileno,$stderr.fileno]' 2>& 1`
    filenos = eval output
    unless Array === filenos && filenos.size = 3
      fail "non-native JRuby launch failed with output:\n" + output
    end
    expect(filenos[0]).to eq 0
    expect(filenos[1]).to eq 1
    expect(filenos[2]).to eq 2
  end
end