# This file is used by test_require_once to ensure that concurrent requires at least do
# not cause a file to execute twice, which is about the only guarantee we can safely make.
# See JRUBY-3078.
$foo += 1
