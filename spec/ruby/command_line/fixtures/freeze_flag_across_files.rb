require_relative 'freeze_flag_required'

p "abc".freeze.object_id == $second_literal_id
