# frozen_string_literal: false
#
# YAML::Store
#
require 'yaml'
require 'pstore'

# YAML::Store provides the same functionality as PStore, except it uses YAML
# to dump objects instead of Marshal.
#
# == Example
#
#   require 'yaml/store'
#
#   Person = Struct.new :first_name, :last_name
#
#   people = [Person.new("Bob", "Smith"), Person.new("Mary", "Johnson")]
#
#   store = YAML::Store.new "test.store"
#
#   store.transaction do
#     store["people"] = people
#     store["greeting"] = { "hello" => "world" }
#   end
#
# After running the above code, the contents of "test.store" will be:
#
#   ---
#   people:
#   - !ruby/struct:Person
#     first_name: Bob
#     last_name: Smith
#   - !ruby/struct:Person
#     first_name: Mary
#     last_name: Johnson
#   greeting:
#     hello: world

class YAML::Store < PStore

  # :call-seq:
  #   initialize( file_name, yaml_opts = {} )
  #
  # Creates a new YAML::Store object, which will store data in +file_name+.
  # If the file does not already exist, it will be created.
  #
  #
  # Options passed in through +yaml_opts+ will be used when converting the
  # store to YAML via Hash#to_yaml().
  def initialize file_name, yaml_opts = {}
    @opt = yaml_opts
    super
  end

  # :stopdoc:

  def dump(table)
    YAML.dump @table
  end

  def load(content)
    table = YAML.load(content)
    if table == false
      {}
    else
      table
    end
  end

  def marshal_dump_supports_canonical_option?
    false
  end

  EMPTY_MARSHAL_DATA = YAML.dump({})
  EMPTY_MARSHAL_CHECKSUM = Digest::MD5.digest(EMPTY_MARSHAL_DATA)
  def empty_marshal_data
    EMPTY_MARSHAL_DATA
  end
  def empty_marshal_checksum
    EMPTY_MARSHAL_CHECKSUM
  end

  # FIXME: These two constants and the method open_and_lock_file should not
  # be in this file (taken from pstore).  See #4779.
  RDWR_ACCESS = {mode: IO::RDWR | IO::CREAT | IO::BINARY, encoding: Encoding::UTF_8}.freeze
  RD_ACCESS = {mode: IO::RDONLY | IO::BINARY, encoding: Encoding::UTF_8}.freeze
  def open_and_lock_file(filename, read_only)
    if read_only
      begin
        file = File.new(filename, RD_ACCESS)
        begin
          file.flock(File::LOCK_SH)
          return file
        rescue
          file.close
          raise
        end
      rescue Errno::ENOENT
        return nil
      end
    else
      file = File.new(filename, RDWR_ACCESS)
      file.flock(File::LOCK_EX)
      return file
    end
  end
end
