require File.expand_path('../acceptance_test_helper', __FILE__)
require 'mocha'

class ExceptionRescueTest < Test::Unit::TestCase

  include AcceptanceTest

  def setup
    setup_acceptance_test
  end

  def teardown
    teardown_acceptance_test
  end

  def test_unexpected_invocation_exception_is_not_caught_by_standard_rescue
    test_result = run_as_test do
      mock = mock('mock')
      begin
        mock.some_method
      rescue => e
        flunk "should not rescue #{e.class}"
      end
    end
    assert_failed(test_result)
    failure_message_lines = test_result.failure_messages.first.split("\n")
    assert_equal "unexpected invocation: #<Mock:mock>.some_method()", failure_message_lines.first
  end

  def test_invocation_never_expected_exception_is_not_caught_by_standard_rescue
    test_result = run_as_test do
      mock = mock('mock')
      mock.expects(:some_method).never
      begin
        mock.some_method
      rescue => e
        flunk "should not rescue #{e.class}"
      end
    end
    assert_failed(test_result)
    failure_message_lines = test_result.failure_messages.first.split("\n")
    assert_equal "unexpected invocation: #<Mock:mock>.some_method()", failure_message_lines.first
  end

  def test_unsatisfied_expectation_exception_is_not_caught_by_standard_rescue
    test_result = run_as_test do
      mock = mock('mock')
      mock.expects(:some_method)
    end
    assert_failed(test_result)
    failure_message_lines = test_result.failure_messages.first.split("\n")
    assert_equal [
      "not all expectations were satisfied",
      "unsatisfied expectations:",
      "- expected exactly once, not yet invoked: #<Mock:mock>.some_method(any_parameters)"
    ], failure_message_lines
  end
end