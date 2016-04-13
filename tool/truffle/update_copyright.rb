# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'rugged'

RB_COPYRIGHT = <<-EOS
# Copyright (c) #{Time.now.year} Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

EOS

JAVA_COPYRIGHT = <<-EOS
/*
 * Copyright (c) #{Time.now.year} Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
EOS

NEW_COPYRIGHT = {
  '.rb' => RB_COPYRIGHT,
  '.java' => JAVA_COPYRIGHT
}

EXTENSIONS = %w[.java .rb]

COPYRIGHT = /Copyright \(c\) (?<year1>\d{4})(?:, (?<year2>\d{4}))? Oracle\b/

OTHER_COPYRIGHTS = [
  /Copyright \(c\) \d{4} Software Architecture Group, Hasso Plattner Institute/,
  /Copyright \(c\) \d{4}(?:-\d{4})?,? Evan Phoenix/,
  /Copyright \(c\) \d{4} Engine Yard/
]

truffle_paths = %w[
  truffle/src
  test/truffle
  spec/truffle
] + [__FILE__]

excludes = %w[
  test/truffle/pack-real-usage.rb
]

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
  EXTENSIONS.include?(File.extname(path)) &&
    truffle_paths.any? { |prefix| path.start_with? prefix } &&
    excludes.none? { |prefix| path.start_with? prefix }
}

paths.each do |file|
  next unless File.exist? file

  header = File.read(file, 200)

  unless COPYRIGHT =~ header
    if OTHER_COPYRIGHTS.none? { |copyright| copyright =~ header }
      puts "Adding copyright in #{file}"
      File.write(file, NEW_COPYRIGHT[File.extname(file)]+File.read(file))
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
