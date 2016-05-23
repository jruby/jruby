# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

Readline = Truffle::Readline

module Readline

  HISTORY = Object.new
  VERSION = 'JLine wrapper'

  module_function

  %i[
    basic_quote_characters
    basic_quote_characters=
    completer_quote_characters
    completer_quote_characters=
    completer_word_break_characters
    completer_word_break_characters=
    completion_append_character
    completion_append_character=
    completion_case_fold
    completion_case_fold=
    completion_proc
    completion_proc=
    emacs_editing_mode
    emacs_editing_mode?
    filename_quote_characters
    filename_quote_characters=
    point=
    pre_input_hook
    pre_input_hook=
    redisplay
    set_screen_size
    special_prefixes
    special_prefixes=
    vi_editing_mode
    vi_editing_mode?
    set_screen_size
  ].each do |method_name|
    define_method(method_name) do
      raise NotImplementedError.new("#{method_name}() function is unimplemented on this machine")
    end
  end

  def input=(input)
    # TODO (nirvdrum 20-May-16): This should do something functional.
    nil
  end

  def output=(output)
    # TODO (nirvdrum 20-May-16): This should do something functional.
    nil
  end

end

class << Readline::HISTORY

  include Enumerable
  include Truffle::ReadlineHistory

  def empty?
    size == 0
  end

  def to_s
    'HISTORY'
  end

end
