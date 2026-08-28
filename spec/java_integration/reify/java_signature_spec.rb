require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

describe "JRuby class reification with signatures" do
  subject { cls.become_java! }

  context "method signatures" do
    let(:cls) do
      _signature = signature
      Class.new do
        java_signature _signature
        def run(*args)
        end
      end
    end

    let(:signature) { "public void run()"}

    it "successfully reifies" do
      expect { subject }.to_not raise_exception
      expect { cls.new.run }.to_not raise_exception
    end

    context "with arguments" do
      let(:signature) { "public void run(java.lang.String)" }

      it "successfully reifies" do
        expect { subject }.to_not raise_exception
        expect { cls.new.run("bar") }.to_not raise_exception
      end
    end
  end
end
