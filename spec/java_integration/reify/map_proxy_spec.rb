require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

describe "Reified Java Map subclasses" do
  class ReifiedConcurrentMapSubclass < java.util.concurrent.ConcurrentHashMap
  end

  class ReifiedHashMapSubclass < java.util.HashMap
  end

  class ReifiedHashMapSubclassWithInitialize < java.util.HashMap
    def initialize
      super()
    end
  end

  class ExplicitlyReifiedHashMapSubclass < java.util.HashMap
    def initialize(capacity)
      super(capacity)
    end
  end

  it "preserves MapJavaProxy methods for auto-reified subclasses" do
    expect(ReifiedConcurrentMapSubclass.new["missing"]).to be_nil

    map = ReifiedHashMapSubclass.new
    map["test"] = "value"

    expect(map["test"]).to eq("value")
    expect(map.to_hash).to eq({ "test" => "value" })
  end

  it "preserves MapJavaProxy methods for split-initialize subclasses" do
    map = ReifiedHashMapSubclassWithInitialize.new
    map["test"] = "value"

    expect(map["test"]).to eq("value")
    expect(map.to_hash).to eq({ "test" => "value" })
  end

  it "preserves MapJavaProxy methods for explicitly reified subclasses" do
    ExplicitlyReifiedHashMapSubclass.become_java!

    map = ExplicitlyReifiedHashMapSubclass.new(4)
    map["test"] = "value"

    expect(map["test"]).to eq("value")
    expect(map.to_hash).to eq({ "test" => "value" })
  end
end
