ScratchPad << :load
raise NameError, "first load fails" if ScratchPad.recorded.size == 1

module ModuleSpecs::Autoload
  class NameErrorThenDefine
  end
end
