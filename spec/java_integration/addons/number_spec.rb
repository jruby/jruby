require File.dirname(__FILE__) + "/../spec_helper"

describe "java.lang.Number subtypes" do
  describe "passed to numeric-coercing methods" do
    it "coerces successfully" do
      expect([42][0.to_java]).to eq 42

      m = Mutex.new
      m.lock
      m.sleep(0.01.to_java)
    end
  end
end