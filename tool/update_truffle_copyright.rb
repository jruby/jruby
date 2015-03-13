# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'rugged'

COPYRIGHT = /Copyright \(c\) (?<year1>\d{4})(?:, (?<year2>\d{4}))? Oracle\b/

OTHER_COPYRIGHTS = [
  /Copyright \(c\) \d{4}(?:-\d{4})?, Evan Phoenix/
]

EXTENSIONS = %w[.java .rb]

truffle_paths = %w[
  truffle/src/main/java/org/jruby/truffle
  truffle/src/main/ruby
  test/truffle/pe/pe.rb
] + [__FILE__]

truffle_paths.each do |path|
  puts "WARNING: incorrect path #{path}" unless File.exist? path
end

abort "USAGE: ruby #{$0} DAYS" unless ARGV.first
days = Integer(ARGV.first)
since = Time.now - days * 24 * 3600

puts "Fixing copyright years for commits in the last #{days} days"

now_year = Time.now.year # Hack this with previous year if needed
abort "Too far back in time: #{since} but we are in #{now_year}" unless since.year == now_year

repo = Rugged::Repository.new('.')

head_commit = repo.head.target
first_commit = nil

repo.walk(head_commit, Rugged::SORT_DATE) { |commit|
  break if commit.time < since
  first_commit = commit
}

abort "No commit in that range" unless first_commit

diff = first_commit.diff(head_commit)

paths = diff.each_delta.to_a.map { |delta|
  delta.new_file[:path]
}.select { |path|
  EXTENSIONS.include?(File.extname(path)) and truffle_paths.any? { |prefix| path.start_with? prefix }
}

paths.each do |file|
  next unless File.exist? file

  header = File.read(file, 200)

  unless COPYRIGHT =~ header
    if OTHER_COPYRIGHTS.none? { |copyright| copyright =~ header }
      puts "WARNING: No copyright in #{file}"
      puts header
    end
    next
  end

  year1, year2 = $~[:year1], $~[:year2]
  year1 = Integer(year1)
  year2 = Integer(year2 || year1)

  if now_year > year2
    contents = File.read(file)
    years = "#{year1}, #{now_year}"
    contents.sub!(COPYRIGHT, "Copyright (c) #{years} Oracle")
    File.write(file, contents)

    puts "Updated year in #{file}"
  end
end
