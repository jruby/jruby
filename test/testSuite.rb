#
# testSuite.rb - 
# 
# launch all tests written in ruby
#
# Created on 10 jan 2002
# 
# Copyright (C) 2001 Jan Arne Petersen, Benoit Cerrina
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
require 'minirunit'

load('test/testRegexp.rb')
load('test/testStringEval.rb')
load('test/testHereDocument.rb')
load('test/testClass.rb')
load('test/testArray.rb')
load('test/testVariableAndMethod.rb')
load('test/testIf.rb')
load('test/testLoops.rb')
load('test/testMethods.rb')
load('test/testGlobalVars.rb')
load('test/testClasses.rb')
load('test/testNumber.rb')

#MRI Ruby tests:
load('test/mri/testAssignment.rb')
load('test/mri/testCondition.rb')
load('test/mri/testCase.rb')
load('test/mri/testIfUnless.rb')
load('test/mri/testWhileUntil')
load('test/mri/testException')
load('test/mri/testArray')
load('test/mri/testHash')
load('test/mri/testIterator')

#Output result
puts
if $failed > 0
   printf "test: %d failed %d\n", $ntest, $failed
else
   printf "end of test(test: %d)\n", $ntest
end
