//package database.ast
//
//import com.sun.nio.file.SensitivityWatchEventModifier
//import zio._
//import zio.nio.file.{FileSystem, Path, WatchKey, WatchService}
//
//import java.io.IOException
//import java.nio.file.{StandardWatchEventKinds, WatchEvent}
//
//final case class Watcher(private val watchService: WatchService) {
//  val allWatchEvents: List[WatchEvent.Kind[_]] =
//    List(
//      StandardWatchEventKinds.ENTRY_CREATE,
//      StandardWatchEventKinds.ENTRY_DELETE,
//      StandardWatchEventKinds.ENTRY_MODIFY
//    )
//
//  def watch(path: Path): IO[IOException, WatchKey] =
//    path.register(watchService, allWatchEvents, SensitivityWatchEventModifier.HIGH)
//
//  private val examplePath =
//    Path("/Users", "kit/code/zio-app/cli/src/main/resources/".split("/").toList: _*)
//
//  println(examplePath)
//
//  def start: ZIO[Any, IOException, Unit] =
//    for {
//      _ <- watch(examplePath)
//      _ <- watchService.stream.foreach { watchKey =>
//        for {
//          _      <- ZIO.debug(s"WatchKey: ${watchKey.toString}")
//          events <- ZIO.scoped(watchKey.pollEventsScoped)
//          _ = events.map { event => println(event.kind) }
//          _ <- parse
//        } yield ()
//      }
//    } yield ()
//
//  private def parse: ZIO[Any, IOException, Unit] =
//    for {
//      content <- ZIO.attemptBlockingIO(
//        scala.io.Source.fromFile("/Users/kit/code/zio-app/cli/src/main/resources/Schema.sql").mkString
//      )
//      parsed = SqlSyntax.createTable.parseString(content.trim)
//      _ <- ZIO.debug(content)
//      _ <- ZIO.debug(parsed)
//    } yield ()
//}
//
//object Watcher {
//
//  val live: ZLayer[Any, Nothing, Watcher] =
//    ZLayer.scoped(FileSystem.default.newWatchService.orDie) >>> ZLayer.fromFunction(Watcher.apply _)
//}
//
//object ParsingApp extends ZIOAppDefault {
//
//  val run = {
//    for {
//      watcher <- ZIO.service[Watcher]
//      _       <- watcher.start
//    } yield watcher
//  }.provide(Watcher.live)
//
//}
