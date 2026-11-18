require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe 'TracePoint#lineno' do
  it 'returns the line number of the event' do
    lineno = nil
    TracePoint.new(:line) { |tp|
      next unless TracePointSpec.target_thread?
      lineno = tp.lineno
    }.enable do
      line_event = true
    end
    lineno.should == __LINE__ - 2
  end

  it 'is reported each time a line runs again' do
    lines = []
    tp = TracePoint.new(:line) { |t|
      next unless TracePointSpec.target_thread?
      lines << t.lineno if t.path == __FILE__
    }
    tp.enable do
      3.times do
        line_event = true
      end
    end
    lines.should == [__LINE__ - 4, __LINE__ - 3, __LINE__ - 3, __LINE__ - 3]
  end

  it 'raises RuntimeError if accessed from outside' do
    tp = TracePoint.new(:line) {}
    -> { tp.lineno }.should.raise(RuntimeError, 'access from outside')
  end
end
