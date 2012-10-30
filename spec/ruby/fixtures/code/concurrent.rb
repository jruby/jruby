ScratchPad.recorded << :con_pre
Thread.current[:in_concurrent_rb] = true
sleep 0.5
if Thread.current[:con_raise]
  raise "con1"
end
sleep 0.5
ScratchPad.recorded << :con_post
