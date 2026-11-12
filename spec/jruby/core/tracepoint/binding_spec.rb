require 'rbconfig'

# CI runs spec/ruby/core/tracepoint in the interpreter only. The main script is optimized, so run one here.
describe "TracePoint#binding with --debug" do
  it "reads and changes the locals of a method in the main script" do
    code = <<~RUBY
      def m(x)
        y = 1
        y + x
      end
      values = []
      result = TracePoint.new(:line) { |tp|
        next unless tp.method_id == :m
        values << tp.binding.local_variables
        tp.binding.local_variable_set(:y, 42) if tp.binding.local_variable_get(:y) == 1
      }.enable { m(1) }
      p values, result
    RUBY

    output = IO.popen([{"JRUBY_OPTS" => nil}, RbConfig.ruby, "--debug", "-e", code], &:read)

    output.should == "[[:x, :y], [:x, :y]]\n43\n"
  end
end
