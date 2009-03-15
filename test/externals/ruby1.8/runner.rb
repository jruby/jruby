require 'test/unit'

rcsid = %w$Id: runner.rb 11708 2007-02-12 23:01:19Z shyouhei $
Version = rcsid[2].scan(/\d+/).collect!(&method(:Integer)).freeze
Release = rcsid[3].freeze

exit Test::Unit::AutoRunner.run(true, File.dirname($0))
