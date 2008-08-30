class NoSessionController < ApplicationController
  session :off

  def do_something
  end

end
