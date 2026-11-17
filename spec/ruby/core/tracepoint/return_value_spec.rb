require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe 'TracePoint#return_value' do
  def test; 'test' end

  it 'returns value from :return event' do
    trace_value = nil
    TracePoint.new(:return) { |tp|
      next unless TracePointSpec.target_thread?
      trace_value = tp.return_value
    }.enable do
      test
      trace_value.should == 'test'
    end
  end

  it 'returns value from :b_return event' do
    trace_value = nil
    TracePoint.new(:b_return) { |tp|
      next unless TracePointSpec.target_thread?
      trace_value = tp.return_value
    }.enable do
      [1, 2].map { |x| x * 10 }
      trace_value.should == 20
    end
  end

  it 'returns value from an explicit return' do
    def early(v); return :early if v; :late end
    def ensure_return; x = 1; return x; ensure; x = 2 end

    values = []
    TracePoint.new(:return) { |tp|
      next unless TracePointSpec.target_thread?
      values << tp.return_value
    }.enable do
      early(true)
      early(false)
      ensure_return
    end
    values.should == [:early, :late, 1]
  end

  it 'raises RuntimeError for events without a return value' do
    called = false
    TracePoint.new(:call) { |tp|
      next unless TracePointSpec.target_thread?
      called = true
      -> { tp.return_value }.should raise_error(RuntimeError, 'not supported by this event')
    }.enable do
      test
    end
    called.should == true
  end
end
