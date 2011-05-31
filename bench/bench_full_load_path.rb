require 'benchmark'
require 'fileutils'

size = (ARGV.shift || '500').to_i
target_size = (ARGV.shift || '10000').to_i # must be bigger than 50
require_size = 50

TEST_DIR = File.expand_path('full_load_path_test', File.dirname(__FILE__))

unless File.directory?(TEST_DIR)
  FileUtils.mkdir(TEST_DIR)
  target_size.times do |idx|
    FileUtils.touch(File.join(TEST_DIR, "bogus#{idx}.rb"))
  end
end

def fill_load_path(size)
  $LOAD_PATH.unshift(TEST_DIR)
  size.times do |idx|
    $LOAD_PATH.unshift("/tmp/folder#{idx}")
  end
end

def with_loadvars(size)
  load_path, loaded_features = $LOAD_PATH.dup, $LOADED_FEATURES.dup
  begin
    fill_load_path(size)
    yield
  ensure
    $LOAD_PATH.replace(load_path)
    $LOADED_FEATURES.replace(loaded_features)
  end
end

Benchmark.bmbm do |bm|
  bm.report("empty counterpart") do
    with_loadvars(size) do
      # do nothing
    end
  end

  bm.report("#{require_size} times require, #{size} in load path") do
    with_loadvars(size) do
      require_size.times do |idx|
        require "bogus#{idx}"
      end
    end
  end
end
