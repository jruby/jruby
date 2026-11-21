require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe 'TracePoint#binding' do
  def test
    secret = 42
  end

  it 'return the generated binding object from event' do
    bindings = []
    TracePoint.new(:return) { |tp|
      next unless TracePointSpec.target_thread?
      bindings << tp.binding
    }.enable {
      test
    }
    bindings.size.should == 1
    bindings[0].should.is_a?(Binding)
    bindings[0].local_variables.should == [:secret]
  end

  def binding_spec_read(x)
    y = x * 2
    y
  end

  it 'reads the values of local variables of the method' do
    values = []
    TracePoint.new(:line) { |tp|
      next unless TracePointSpec.target_thread?
      next unless tp.method_id == :binding_spec_read
      values << [tp.binding.local_variable_get(:x), tp.binding.local_variable_get(:y)]
    }.enable {
      binding_spec_read(1)
    }
    values.should == [[1, nil], [1, 2]]
  end

  def binding_spec_write
    y = 1
    y
  end

  it 'changes local variables of the method' do
    trace = TracePoint.new(:line) { |tp|
      next unless TracePointSpec.target_thread?
      next unless tp.method_id == :binding_spec_write
      tp.binding.local_variable_set(:y, 42) if tp.binding.local_variable_get(:y) == 1
    }
    trace.enable { binding_spec_write }.should == 42
  end
end
