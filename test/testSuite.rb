# testSuite.rb - Launch all unit tests written in ruby.
#
# Created on 10 jan 2002
# 
# Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
# Jan Arne Petersen <jpetersen@uni-bonn.de>
# Benoit Cerrina <benoit.cerrina@writeme.com>
# 
# JRuby - http://jruby.sourceforge.net
# 
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
# 
require 'test/minirunit'

TEST_INDEX = "test/test_index"

def valid?(line)
  if /^\#/ =~ line
    # Commented
    return false
  end
  if line.empty?
    return false
  end
  return true
end

open(TEST_INDEX) {|test_index|
  test_index.readlines.each {|line|
    line.strip!
    if valid?(line)
      test_load('test/' + line)
    end
  }
}

test_print_report
