require 'java'

include_class "javax.swing.JFrame"
include_class "javax.swing.JLabel"

frame = JFrame.new("Hello Swing")
label = JLabel.new("Hello World")

frame.getContentPane().add(label)
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack()
frame.setVisible(true)
