package de.sciss.impulse

case class InvalidParameter(name: String, value: Any, explanation: String) extends RuntimeException
