require 'timeout'

describe "Kernel#timeout" do
  it "dynamically dispatches to Timeout::timeout" do
    begin
      old_verbose = $VERBOSE
      $VERBOSE = nil
      old_timeout = Timeout
      new_timeout = Module.new do
        def self.timeout(*); "foo"; end
      end
      Object.const_set :Timeout, new_timeout

      expect(Kernel.send(:timeout, 0)).to eq "foo"
    ensure
      Object.const_set :Timeout, old_timeout
      $VERBOSE = old_verbose
    end
  end
end