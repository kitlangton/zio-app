package zio.app

import zio._
import zio.test._

object RenamerSpec extends ZIOSpecDefault {

  override def spec = suite("RenamerSpec")(
    test("Cool") {
      for {
        tempDir <- TemplateGenerator.cloneRepo
        _       <- ZIO.succeed(println(tempDir))
        _       <- Renamer.renameFolders(tempDir)
        _       <- Renamer.renameFiles(tempDir, "Funky")
        _       <- Renamer.printTree(tempDir)
      } yield assertCompletes
    }
  ).provide(Renamer.live)
}
