#include "ruby.h"

static VALUE Example;

//Methoden, die verwendet werden
VALUE method_hello_world(VALUE self);

//Ruby-Einstiegsmethode. Sie muss immer mit Init_ beginnen und auf den Namen der Extension enden. 
void Init_example(void)
{
  //Erstelle die Klasse Example als Subklasse von Object
  Example = rb_define_class("Example", rb_cObject);
  
  //Füge Example die Instanzmethode hello_world hinzu
  rb_define_method(Example, "hello_world", method_hello_world, 0);
}

//Dokumentation der Klassen

/*
*Document-class: Example
*
*Diese Klasse ist eine Beispielklasse. 
*/

//Definition der Methoden

/*
*call-seq: 
*  hello_world ==> "Hello world!"
*
*Diese Methode gibt den String <tt>"Hello world!"</tt> 
*zurück. 
*/
VALUE method_hello_world(VALUE self) //self sollte explizit angegeben werden, auch wenn es nicht benutzt wird
{
  return rb_str_new2("Hello World from C!");
}

