class PersonController < ApplicationController

  protect_from_forgery :only => :nothing

  def shutdown
    java.lang.Runtime.runtime.exit 0
  end

  def app
    p @person = Myapp::Application.config.data
    render('person/show')
  end

  def get
    p @person = Myapp::Application.config.data
    render json: { :surname =>  Myapp::Application.config.data.surname, :firstname => Myapp::Application.config.data.firstname }
  end

  def patch
    payload = JSON.parse request.body.read
    Myapp::Application.config.data.send :"#{payload.keys.first}=", payload.values.first
    Myapp::Application.config.histogram.update( Myapp::Application.config.data.surname.length + Myapp::Application.config.data.firstname.length )

    # did not find a way tp set status 205 and tell rails NOT to set a
    # content-type so rack/lint does not bail out 
    render status: 200, nothing: true
  end
end
