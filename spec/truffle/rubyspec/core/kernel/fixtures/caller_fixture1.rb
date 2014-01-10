require File.expand_path('../caller_fixture2', __FILE__)
2 + 2
3 + 3
CallerFixture.capture do
  5 + 5
  6 + 6
  :seven
  8 + 8
end

module CallerFixture
  module_function
  def example_proc
    Proc.new do
      1 + 1
      2 + 2
    end
  end

  def entry_point
    second
  end

  def second
    third
  end

  def third
    b = fourth do
      1 + 1
      caller(0)
    end
    2 + 2
    3 + 3
    return b
  end

  def fourth(&block)
    return block
  end
end

