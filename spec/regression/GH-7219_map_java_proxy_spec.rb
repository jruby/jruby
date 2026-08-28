describe "MapJavaProxy" do
  class A < java.util.concurrent.ConcurrentHashMap
  end

  class B < java.util.HashMap
  end
  
  it "does not raise ClassCastException" do
    A.new["test"].should be_nil
    B.new["test"].should be_nil
  end
end