# -*- ruby -*-

require 'rake'
require 'rake/testtask'
require 'rake/rdoctask'

desc "Run all the tests on a fresh test database"
task :default => [ :test ]

desc "Run unit tests"
Rake::TestTask.new("test") { |t|
  t.libs << "."
  t.pattern = '**/test_*.rb'
  t.verbose = true
}

task :zentest do
  tests = Dir["test*.rb"]
  ruby "-w -I. /usr/local/bin/ZenTest #{tests.join ' '}"
end

task :sort do
  sh %(for f in test_*.rb; do grep "def.test_" $f > x; sort x > y; echo $f; echo; diff x y; done)
end

task :audit => [ :zentest, :sort ]

task :clean do |t|
  sh "find . -name \*~ -exec rm {} \\;"
end
