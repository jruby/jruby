require 'test/minirunit'

require 'readline'

def test_readline_history_size(expected_size)
  test_ok expected_size == Readline::HISTORY.to_a.size, "Wrong number of items in the Readline HISTORY"
end

test_no_exception { Readline::HISTORY.push('string') }
test_readline_history_size 1

test_no_exception { Readline::HISTORY.push(*['line1', 'line2']) }
test_readline_history_size 3

test_no_exception { Readline::HISTORY.push }
test_readline_history_size 3

