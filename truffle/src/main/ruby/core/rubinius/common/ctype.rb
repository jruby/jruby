# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

##
# Mixin containing byte classification methods.
#--
# isspace, islower, ... are not in MRI core library.

module Rubinius::CType
  # The character literals (?x) are Fixnums in 1.8 and Strings in 1.9
  # so we use literal values instead so this is 1.8/1.9 compatible.

  # \n \r \t \f \v \a \b \e
  def self.isctrl(num)
    (num >= 7 and num <= 13) or num == 27
  end

  # ' ' \n \t \r \f \v
  def self.isspace(num)
    num == 32 or (num >= 9 and num <= 13)
  end

  def self.isupper(num)
    num >= 65 and num <= 90
  end

  def self.islower(num)
    num >= 97 and num <= 122
  end

  def self.isdigit(num)
    num >= 48 and num <= 57
  end

  def self.isalnum(num)
    islower(num) or isupper(num) or isdigit(num)
  end

  def self.toupper!(num)
    num - 32
  end

  def self.toupper(num)
    islower(num) ? toupper!(num) : num
  end

  def self.tolower!(num)
    num + 32
  end

  def self.tolower(num)
    isupper(num) ? tolower!(num) : num
  end
end