package examples

import zio.UIO

trait ExampleService {
  def magicNumber: UIO[Int]
}
