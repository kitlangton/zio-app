package zio.app.database

import zio.app.database.MagicDataSpike.UserRequest.{GetUser, GetUserPosts}
import zio.app.database.QuillContext._
import zio.blocking.Blocking
import zio.query._
import zio.{query => _, _}

import java.sql.Connection

object MagicDataSpike extends App {

  // # USERS

  case class User(
      id: Long,
      name: String
  ) {
    def posts: ZQuery[Has[Connection] with Blocking, Nothing, List[Post]] =
      getUserPosts(id)

//    def comments: ZIO[Any, Nothing, List[Comment]] =
//      posts.flatMap { posts =>
//        ZIO.foreach(posts)(post => post.comments).map(_.flatten)
//      }
  }

  trait UserService {
    def userWithPosts(user: Long): UIO[(User, List[Post])]
  }

  sealed trait UserRequest[+A] extends Request[Nothing, A]

  object UserRequest {
    case class GetUser(id: Long)      extends UserRequest[User]
    case class GetUserPosts(id: Long) extends UserRequest[List[Post]]
  }

  def getUser(id: Long): ZQuery[Has[Connection] with Blocking, Nothing, User] =
    ZQuery.fromRequest(GetUser(id))(UserDataSource)

  def getUserPosts(id: Long): ZQuery[Has[Connection] with Blocking, Nothing, List[Post]] =
    ZQuery.fromRequest(GetUserPosts(id))(UserDataSource)

  lazy val UserDataSource: DataSource[Has[Connection] with Blocking, UserRequest[Any]] =
    DataSource.Batched.make("UserDataSource") { (requests: Chunk[UserRequest[Any]]) =>
      val getUserRequests: Chunk[Long] = requests.collect { case GetUser(id) => id }
      val getPostRequests: Chunk[Long] = requests.collect { case GetUserPosts(id) => id }

      val users = quote { querySchema[User]("uzer") }
      val getUsersQuery = quote {
        users.filter(lift(getUserRequests) contains _.id)
      }

      def runIfNonEmpty[R, E, A](list: Iterable[_])(zio: ZIO[R, E, List[A]]) =
        if (list.nonEmpty) zio else UIO(List.empty)

      val getPostsQuery = quote {
        query[Post].filter(lift(getPostRequests) contains _.userId)
      }

      for {
        users <- runIfNonEmpty(getUserRequests)(QuillContext.run(getUsersQuery).orDie.debug("USERS"))
        posts <- runIfNonEmpty(getPostRequests)(QuillContext.run(getPostsQuery).orDie.debug("POSTS"))
      } yield {
        val map0 = users.foldLeft(CompletedRequestMap.empty) { (map, user) =>
          map.insert(GetUser(user.id))(Right(user))
        }

        val emptyRequests = getPostRequests.foldLeft(CompletedRequestMap.empty) { (map, userId) =>
          map.insert(GetUserPosts(userId))(Right(List.empty))
        }

        posts.groupBy(_.userId).foldLeft(map0 ++ emptyRequests) { case (map, (userId, userPosts)) =>
          map.insert(GetUserPosts(userId))(Right(userPosts))
        }
      }
    }

  // # POSTS

  case class Post(id: Long, title: String, userId: Long)

  sealed trait PostRequest[+A] extends Request[Nothing, A]

  object PostRequest {
    case class GetPost(id: Long)         extends PostRequest[Post]
    case class GetPostComments(id: Long) extends PostRequest[List[Comment]]
  }

  case class Comment(id: Long, body: String)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    def userWithPosts(id: Long): ZQuery[Has[Connection] with Blocking, Nothing, (User, List[Post])] = for {
      user  <- getUser(id)
      posts <- getUserPosts(id)
    } yield (user, posts)

    val query: ZQuery[Has[Connection] with Blocking, Nothing, List[(User, List[Post])]] =
      ZQuery.collectAllBatched(List(userWithPosts(1), userWithPosts(2), userWithPosts(3)))

    query.run.debug.provideCustomLayer(Blocking.any >>> QuillContext.live).exitCode
  }
}
