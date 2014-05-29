class SpecExpectationNotMetError < StandardError; end
class SpecExpectationNotFoundError < StandardError
  def message
    "No behavior expectation was found in the example"
  end
end

class SpecExpectation
  def self.fail_with(expected, actual)
    if expected.to_s.size + actual.to_s.size > 80
      message = expected.to_s.chomp + "\n" + actual.to_s
    else
      message = expected.to_s + " " + actual.to_s
    end
    Kernel.raise SpecExpectationNotMetError, message
  end
end
