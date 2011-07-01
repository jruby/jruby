#!/usr/bin/env ruby

require 'fileutils'

number_of_files = 1_000
directory = "c:/opt/test_data"

Dir.mkdir directory unless File.exists? directory
number_of_files.times do |i|
  file = directory + "/" + i.to_s
  FileUtils.touch file
end
