require 'minijava'

import "javax.swing.JFrame"
import "javax.swing.JButton"
import "java.awt.event.ActionListener"
import "java.lang.Integer"

class JFrame
  alias_method :add, :"add(java.awt.Component)"
  alias_method :setSize, :"setSize(int,int)"
  
  def size=(size)
    x, y = *size
    setSize(x.to_java(Integer), y.to_java(Integer))
  end
  
  class << self
    alias_method :new_with_title, :"new(java.lang.String)"
    
    def new(title)
      new_with_title(title.to_java)
    end
  end
end

class JButton
  class << self
    alias_method :new_with_text, :"new(java.lang.String)"
    
    def new(text)
      new_with_text(text.to_java)
    end
  end
end

frame = JFrame.new "This is my frame"
frame.show
frame.size = [300, 300]

button = JButton.new "Press me"
button.addActionListener(proc { button.setText("Pressed!".to_java) }.to_java(ActionListener))

frame.add button
frame.show
