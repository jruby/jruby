class Child1Override < java.lang.Object
  def toString
    "child1 #{super}"
  end
end

class Child1NoOverride < java.lang.Object
end

class Child1OverrideChild2Override < Child1Override
  def toString
    "child2 #{super}"
  end
end

class Child1OverrideChild2NoOverride < Child1Override
end

class Child1NoOverrideChild2Override < Child1NoOverride
  def toString
    "child2 #{super}"
  end
end

class Child1NoOverrideChild2NoOverride < Child1NoOverride
end

class Child1OverrideChild2OverrideChild3Override < Child1OverrideChild2Override
  def toString
    "child3 #{super}"
  end
end

class Child1OverrideChild2OverrideChild3NoOverride < Child1OverrideChild2Override
end

class Child1OverrideChild2NoOverrideChild3Override < Child1OverrideChild2NoOverride
  def toString
    "child3 #{super}"
  end
end

class Child1OverrideChild2NoOverrideChild3NoOverride < Child1OverrideChild2NoOverride
end

class Child1NoOverrideChild2NoOverride < Child1NoOverride
end

class Child1NoOverrideChild2OverrideChild3Override < Child1NoOverrideChild2Override
  def toString
    "child3 #{super}"
  end
end

class Child1NoOverrideChild2OverrideChild3NoOverride < Child1NoOverrideChild2Override
end

class Child1NoOverrideChild2NoOverrideChild3Override < Child1NoOverrideChild2NoOverride
  def toString
    "child3 #{super}"
  end
end

class Child1NoOverrideChild2NoOverrideChild3NoOverride < Child1NoOverrideChild2NoOverride
end