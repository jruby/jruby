describe "MapJavaProxy" do
  class A < java.util.concurrent.ConcurrentHashMap
  end

  class B < java.util.HashMap
  end

  class C < java.util.HashMap
    def initialize
      super()
    end
  end
  
  it "does not raise ClassCastException" do
    expect(A.new["test"]).to be_nil
    expect(B.new["test"]).to be_nil
  end

  it "preserves MapJavaProxy behavior for reified subclasses" do
    map = C.new

    map["test"] = "value"
    expect(map["test"]).to eq("value")
    expect(map.to_hash).to eq({ "test" => "value" })
  end
end
