package zio.app

import zio._
import zio.magic._
import zio.test._

object RenamerSpec extends DefaultRunnableSpec {

  override def spec = suite("RenamerSpec")(
    testM("Cool") {
      for {
        tempDir <- TemplateGenerator.cloneRepo
        _       <- UIO(println(tempDir))
        _       <- Renamer.renameFolders(tempDir)
        _       <- Renamer.renameFiles(tempDir, "Funky")
        _       <- Renamer.printTree(tempDir)
      } yield assertCompletes
    }
  ).injectCustom(Renamer.live)
}
