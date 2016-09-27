describe "java.script.ScriptManager for JRuby" do
  describe "when assigning global variables" do
    it "allows different types of values to be assigned in sequence" do
      engine = javax.script.ScriptEngineManager.new.getEngineByName("jruby")
      expect(engine.eval("$x = 10")).to eq 10
      expect(engine.eval("$x = 'a'")).to eq 'a'
    end
  end
end
