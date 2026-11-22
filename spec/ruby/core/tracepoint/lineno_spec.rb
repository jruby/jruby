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

  it 'returns the line of end for a :return event at the end of a method' do
    obj = Object.new
    def obj.m
      :m
    end

    lineno = nil
    TracePoint.new(:return) { |tp|
      next unless TracePointSpec.target_thread? && tp.method_id == :m
      lineno = tp.lineno
    }.enable do
      obj.m
    end
    lineno.should == __LINE__ - 9
  end

  it 'returns the line where the block ends for a :b_return event, which is also the line of the block in the backtrace' do
    lines = nil
    TracePoint.new(:b_return) { |tp|
      next unless TracePointSpec.target_thread?
      lines ||= [tp.lineno, caller_locations(1, 1)[0].lineno]
    }.enable do
      [1].each { |x|
        x
      }
    end
    lines.should == [__LINE__ - 2, __LINE__ - 2]
  end
end
