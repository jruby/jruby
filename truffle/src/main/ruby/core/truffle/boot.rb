# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

TOPLEVEL_BINDING = binding

module Truffle
  module Boot

    def self.main_s
      $0 = find_s_file
      load $0
      0
    end

    def self.check_syntax
      inner_check_syntax
      STDOUT.puts 'Syntax OK'
      true
    rescue SyntaxError => e
      STDERR.puts "SyntaxError in #{e.message}"
      false
    end

    private

    def self.find_s_file
      name = original_input_file

      return name if File.exists?(name)

      name_in_ruby_home_bin = "#{RbConfig::CONFIG['bindir']}/#{name}"
      return name_in_ruby_home_bin if File.exists?(name_in_ruby_home_bin)

      ENV['PATH'].split(File::PATH_SEPARATOR).each do |path|
        name_in_path = "#{path}/#{name}"
        return name_in_path if File.exists?(name_in_path)
      end

      raise LoadError.new("No such file or directory -- #{name}")
    end

  end
end

