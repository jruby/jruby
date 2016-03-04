require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  it "generates Java source" do
    script = generate("class Foo; end")
    expect( script ).to_not be nil
    expect( script.classes[0].name ).to eql "Foo"
    java = script.classes[0].to_s

    # a few sanity checks for default behaviors
    expect( java ).to match /import org\.jruby\.Ruby;/n
    expect( java ).to match /public class Foo extends RubyObject\s+{/n
    expect( java ).to match /private static final Ruby __ruby__ = Ruby\.getGlobalRuntime\(\);/n
    expect( java ).to match /private static final RubyClass __metaclass__;/n
    expect( java ).to match /static {/n
    expect( java ).to match /metaclass = __ruby__\.getClass\("Foo"\);/n
    expect( java ).to match /__ruby__\.executeScript\(source, "#{__FILE__}"\)/n
    expect( java ).to match /private Foo\(Ruby \w+, RubyClass \w+\)/n
    expect( java ).to match /public static IRubyObject __allocate__\(Ruby \w+, RubyClass \w+\)/n
  end

  it "generates a javac command" do
    files = %w[Foo.java Bar.java]
    javac = JRuby::Compiler::JavaGenerator.generate_javac(
      files,
      :javac_options => [],
      :classpath => ENV_JAVA['java.class.path'].split(File::PATH_SEPARATOR),
      :target => '/tmp')

    expect( javac ).to match /javac/
    expect( javac ).to match /\".*?jruby\w*\.jar\"#{File::PATH_SEPARATOR}/
    expect( javac ).to match /Foo\.java/
    expect( javac ).to match /Bar\.java/
  end
end
