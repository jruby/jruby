require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ScalaOperators"

describe "Scala operators" do
  it "are callable using symbolic names" do
    obj = ScalaOperators.new

    expect(obj.send(:"+")).to eq("$plus")
    expect(obj.send(:"-")).to eq("$minus")
    expect(obj.send(:":")).to eq("$colon")
    expect(obj.send(:"/")).to eq("$div")
    expect(obj.send(:"=")).to eq("$eq")
    expect(obj.send(:"<")).to eq("$less")
    expect(obj.send(:">")).to eq("$greater")
    expect(obj.send(:"\\")).to eq("$bslash")
    expect(obj.send(:"#")).to eq("$hash")
    expect(obj.send(:"*")).to eq("$times")
    expect(obj.send(:"!")).to eq("$bang")
    expect(obj.send(:"@")).to eq("$at")
    expect(obj.send(:"%")).to eq("$percent")
    expect(obj.send(:"^")).to eq("$up")
    expect(obj.send(:"&")).to eq("$amp")
    expect(obj.send(:"~")).to eq("$tilde")
    expect(obj.send(:"?")).to eq("$qmark")
    expect(obj.send(:"|")).to eq("$bar")
    expect(obj.send(:"+=")).to eq("$plus$eq")
  end

  it "are callable using original names" do
    obj = ScalaOperators.new
    
    expect(obj.send(:"$plus")).to eq("$plus")
    expect(obj.send(:"$minus")).to eq("$minus")
    expect(obj.send(:"$colon")).to eq("$colon")
    expect(obj.send(:"$div")).to eq("$div")
    expect(obj.send(:"$eq")).to eq("$eq")
    expect(obj.send(:"$less")).to eq("$less")
    expect(obj.send(:"$greater")).to eq("$greater")
    expect(obj.send(:"$bslash")).to eq("$bslash")
    expect(obj.send(:"$hash")).to eq("$hash")
    expect(obj.send(:"$times")).to eq("$times")
    expect(obj.send(:"$bang")).to eq("$bang")
    expect(obj.send(:"$at")).to eq("$at")
    expect(obj.send(:"$percent")).to eq("$percent")
    expect(obj.send(:"$up")).to eq("$up")
    expect(obj.send(:"$amp")).to eq("$amp")
    expect(obj.send(:"$tilde")).to eq("$tilde")
    expect(obj.send(:"$qmark")).to eq("$qmark")
    expect(obj.send(:"$bar")).to eq("$bar")
    expect(obj.send(:"$plus$eq")).to eq("$plus$eq")
  end
end