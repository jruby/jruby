require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe 'TracePoint#event' do
  it 'returns the type of event' do
    event_name = nil
    TracePoint.new(:end, :call) do |tp|
      next unless TracePointSpec.target_thread?
      event_name = tp.event
    end.enable do
      TracePointSpec.test
      event_name.should.equal?(:call)

      TracePointSpec::B.new.foo
      event_name.should.equal?(:call)

      class TracePointSpec::B; end
      event_name.should.equal?(:end)
    end

  end

  it 'returns :b_call and :b_return for a lambda' do
    events = []
    lambda = -> { 1 }
    _, line = lambda.source_location
    TracePoint.new(:b_call, :b_return) do |tp|
      next unless TracePointSpec.target_thread?
      events << tp.event if tp.lineno == line
    end.enable do
      lambda.call
    end

    events.should == [:b_call, :b_return]
  end
end
