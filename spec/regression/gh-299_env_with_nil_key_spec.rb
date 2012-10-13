require 'rspec'

describe "IO.popen" do
  context "when ENV with nil as key value" do
    it "ignores the environment variable" do
      env = {
        "ANYTHING_NIL" => nil
      }
      
      lambda {
        IO.popen([env, 'jruby -v'], "r") do |x|
          ENV.has_key?("ANYTHING_NIL").should be_false
        end
        }.should_not raise_error
    end
  end
end

