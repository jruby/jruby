require 'rspec'

# in 1.8, IO.popen does not take the first array as additional ENV.
unless RUBY_VERSION =~ /\A1\.8/

  describe "IO.popen" do
    context "with ENV which has nil as value" do
      it "ignores the environment variable" do
        env = {
          "ANYTHING_NIL" => nil
        }
      
        lambda {
          IO.popen([env, 'true'], "r") do |x|
            ENV.has_key?("ANYTHING_NIL").should be_false
          end
          }.should_not raise_error
      end
    end
  end

end