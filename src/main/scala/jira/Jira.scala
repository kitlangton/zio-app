package jira

import zio._

case class Issue(key: String, name: String)

trait Jira {
  def issues: UIO[List[Issue]]
}

object Jira {
  def test(issues: List[Issue]): ULayer[Has[Jira]] = ZLayer.succeed {
    val issues0 = issues
    new Jira {
      override def issues: UIO[List[Issue]] = UIO(issues0)
    }
  }
}
