# Import Java packages
JavaObject.import "java.awt.event"
JavaObject.import "javax.swing"

# Load the Swing classes which are used by this script
JavaObject.load_class "JFrame"
JavaObject.load_class "JButton"
JavaObject.load_class "JOptionPane"

# Create a frame with the title "HelloWorldSwing"
frame = JFrame.new("HelloWorldSwing")

# Create a button with the text "Klick Me!"
button = JButton.new("Klick Me!")

# Create a Proc object which shows a MessageDialog when it is called.
action = Proc.new() do |evt| JOptionPane.showMessageDialog NIL, "<html>Hello from JRuby.<br>The button was: <i>" + evt.getActionCommand + "</i>" end

# Add an action to the button
button.addActionListener(JavaInterface.listener "ActionListener", "actionPerformed", action)

# Add the button to the frame
frame.getContentPane().add(button)

# Show frame
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack();
frame.setVisible(true);
