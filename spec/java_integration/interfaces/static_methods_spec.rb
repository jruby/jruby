require File.dirname(__FILE__) + "/../spec_helper"
require 'tmpdir'

if (ENV_JAVA['java.specification.version'] >= '1.8')
  describe "An interface with static methods" do
    before :all do
      src = <<-JAVA
      public interface Java8Interface {
        static String message() {
          return "hello";
        }
      }
      JAVA
      @tmpdir = Dir.mktmpdir
      $CLASSPATH << @tmpdir
      @src = File.open(@tmpdir + "/Java8Interface.java", 'w'){ |f| f.print(src) }

      system "javac #{@tmpdir + "/Java8Interface.java"}"
    end

    after :all do
      FileUtils.rm_rf @tmpdir
    end

    it "binds those methods on the proxy module" do
      expect(Java::Java8Interface.message).to eq("hello")
    end

    it "exposes those methods via java_send" do
      expect(Java::Java8Interface.java_send(:message)).to eq("hello")
    end

    it "exposes those methods via java_method" do
      expect(Java::Java8Interface.java_method(:message).call).to eq("hello")
    end
  end
end
