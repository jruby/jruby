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
require 'minirunit'

test_load('test/testRegexp.rb')
test_load('test/testStringEval.rb')
test_load('test/testHereDocument.rb')
test_load('test/testClass.rb')
test_load('test/testArray.rb')
test_load('test/testVariableAndMethod.rb')
test_load('test/testIf.rb')
test_load('test/testLoops.rb')
test_load('test/testMethods.rb')
test_load('test/testGlobalVars.rb')
test_load('test/testClasses.rb')
test_load('test/testNumber.rb')
test_load('test/testFloat.rb')
test_load('test/testBlock.rb')
test_load('test/testRange.rb')
test_load('test/testString.rb')
test_load('test/testException.rb')
test_load('test/testSpecialVar.rb')
test_load('test/testFile.rb')
test_load('test/testThread.rb')
test_load('test/testMarshal.rb')

# MRI Ruby tests (from sample/test.rb in Matz's Ruby Interpreter):

test_load('test/mri/testAssignment.rb')
test_load('test/mri/testCondition.rb')
test_load('test/mri/testCase.rb')
test_load('test/mri/testIfUnless.rb')
test_load('test/mri/testWhileUntil.rb')
test_load('test/mri/testException.rb')
test_load('test/mri/testArray.rb')
test_load('test/mri/testHash.rb')
test_load('test/mri/testIterator.rb')
test_load('test/mri/testFloat.rb')
test_load('test/mri/testBignum.rb')
test_load('test/mri/testString.rb')
test_load('test/mri/testAssignment2.rb')
test_load('test/mri/testCall.rb')

puts

test_print_report
