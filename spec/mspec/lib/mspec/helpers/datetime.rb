class Object
  # The new_datetime helper makes writing DateTime specs more simple by
  # providing default constructor values and accepting a Hash of only the
  # constructor values needed for the particular spec. For example:
  #
  #   new_datetime :hour => 1, :minute => 20
  #
  # Possible keys are:
  #   :year, :month, :day, :hour, :minute, :second, :offset and :sg.

  def new_datetime(opts={})
    require 'date'

    value = {
      :year   => -4712,
      :month  => 1,
      :day    => 1,
      :hour   => 0,
      :minute => 0,
      :second => 0,
      :offset => 0,
      :sg     => Date::ITALY
    }.merge opts

    DateTime.new value[:year], value[:month], value[:day], value[:hour],
      value[:minute], value[:second], value[:offset], value[:sg]
  end
end
