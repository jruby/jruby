require 'test/unit'

rcsid = %w$Id: runner.rb 26083 2009-12-13 18:50:31Z shyouhei $
Version = rcsid[2].scan(/\d+/).collect!(&method(:Integer)).freeze rescue nil
Release = rcsid[3].freeze rescue nil

exit Test::Unit::AutoRunner.run(true, File.dirname($0))
