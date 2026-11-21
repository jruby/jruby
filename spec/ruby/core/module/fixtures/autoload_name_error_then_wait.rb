state = ScratchPad.recorded
state[:loads] += 1
raise NameError, "first load fails" if state[:loads] == 1

state[:started] << true
state[:go].pop

module ModuleSpecs::Autoload
  class NameErrorThenRetried
  end
end
