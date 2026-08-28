
class Events
  def self.events
    @events ||= []
  end
end

TracePoint.new(:call,:return,:c_call,:c_return) do |trace_point|
  fail "trace_point.event is nil" unless trace_point.event
  Events.events << trace_point.event
end.enable do
  0.to_s
end