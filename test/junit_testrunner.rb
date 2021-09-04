require 'test/unit/ui/testrunnermediator'
require 'test/unit/ui/testrunnerutilities'

module Test
  module Unit
    module UI
      module JUnit
        # Runs a Test::Unit::TestSuite on the console.
        class TestRunner 
          extend TestRunnerUtilities

          attr_accessor :faults

          # Creates a new TestRunner for running the passed
          # suite. If quiet_mode is true, the output while
          # running is limited to progress dots, errors and
          # failures, and the final result. io specifies
          # where runner output should go to; defaults to
          # STDOUT.
          def initialize(suite, output_level=NORMAL, io=STDOUT)
            if (suite.respond_to?(:suite))
              @suite = suite.suite
            else
              @suite = suite
            end
            @output_level = output_level
            @io = io
            @already_outputted = false
            @faults = []
          end

          # Begins the test run.
          def start
            setup_mediator
            attach_to_mediator
            return start_mediator
          end

          private
          def setup_mediator
            @mediator = create_mediator(@suite)
            suite_name = @suite.to_s
            if ( @suite.kind_of?(Module) )
              suite_name = @suite.name
            end
          end
          
          def create_mediator(suite)
            return TestRunnerMediator.new(suite)
          end
          
          def attach_to_mediator
            @mediator.add_listener(TestResult::FAULT, &method(:add_fault))
          end
          
          def start_mediator
            return @mediator.run_suite
          end
          
          def add_fault(fault)
            @faults << fault
          end
          
          def started(result)
          end
          
          def finished(elapsed_time)
          end
          
          def test_started(name)
          end
          
          def test_finished(name)
          end
        end
      end
    end
  end
end

if __FILE__ == $0
  Test::Unit::UI::JUnit::TestRunner.start_command_line_test
end
