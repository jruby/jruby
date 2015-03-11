require_relative "../../java_integration/spec_helper"
require 'rbconfig'
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end
    
  it "generates Java source" do
    script = generate("class Foo; end")
    script.should_not == nil
    script.classes[0].name.should == "Foo"
    java = script.classes[0].to_s

    # a few sanity checks for default behaviors
    java.should match /import org\.jruby\.Ruby;/
    java.should match /public class Foo {/
    java.should match /private static final Ruby __ruby__ = Ruby\.getGlobalRuntime\(\);/
    java.should match /private static final RubyClass __metaclass__;/
    java.should match /static {/
    java.should match /metaclass = __ruby__\.getClass\("Foo"\);/
    java.should match /__ruby__\.executeScript\(source, "#{__FILE__}"\)/
    java.should match /private Foo\(Ruby \w+, RubyClass \w+\)/
    java.should match /public static IRubyObject __allocate__\(Ruby \w+, RubyClass \w+\)/
  end

  it "generates a javac command" do
    files = %w[Foo.java Bar.java]
    javac = JRuby::Compiler::JavaGenerator.generate_javac(
      files,
      :javac_options => [],
      :classpath => ENV_JAVA['java.class.path'].split(File::PATH_SEPARATOR),
      :target => '/tmp')
    
    javac.should match /javac/
    javac.should match /jruby\w*\.jar#{File::PATH_SEPARATOR}/
    javac.should match /Foo\.java/
    javac.should match /Bar\.java/
  end
end
