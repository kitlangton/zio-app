package $package$

import zio._

trait ExampleService {
  def magicNumber: UIO[Int]
}