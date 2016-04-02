require_relative 'freeze_flag_required_diff_enc'

p "abc".freeze.object_id != $second_literal_id
