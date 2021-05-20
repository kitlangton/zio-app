//package railz
//
//import zio.=!=
//
//object ActiveRecord {
//  case class Grade(grade: Int, student: Student)
//
//  case class Collection[A](list: List[A])
//
//  case class Student(teachers: Collection[Teacher], grades: List[Grade])
//
//  def withTeachers[A](a: A): A { def teachers: List[Teacher] } = ???
//  def withGrades[A](a: A): A { def grades: List[Grade] }       = ???
//
//  case class ClassAssignment(student: Student, teacher: Teacher)
//
//  case class Teacher(students: List[Student])
//
//  val student: Student = ???
//  val teacher: Student = ???
//
////  sealed trait Include[A, T <: Toggle] {}
//
////  Student
////    .all
////    .include(_.teachers)
//
//  case class Query[A]() {
//    def includes[B](f: A => B): Query[B] = ???
//  }
//
//  val studentQuery: Query[Student] = ???
//
//  /** Use Cases
//    * 1. Get all students with all of their teachers // Student
//    * 2. Get a specific student with all of their teachers // List[Student]
//    * 3. Get a specific student's teachers // List[Teacher]
//    * 4. Get a specific student's teachers
//    */
//
//}
//
///** 1. App ergonomics
//  * 2. Has[_] -> ()
//  */
//
//import zio._
//
//abstract class AppSimple[R, E, A](zio: ZIO[R, E, A])(implicit ev: ZEnv <:< R) extends App {
//  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
//    zio.provideSome(ev).exitCode
//}
//
//object MyApp extends AppSimple(program)
//
//trait AppSimple extends App {
//
//  def runSimple: ZIO[ZEnv, Any, Any] = ???
//
//  val program: Task[String] = ZIO("hello")
//
////  def runZIO[R, E](zio: ZIO[R, E, Any])(implicit ev: E <:< Nothing, ev2: R IsEnv ZEnv): ZIO[ZEnv, E, ExitCode] = {
////    program.exitCode
////  }
//
//  override def run[E](args: List[String]): ZIO[ZEnv, E, ExitCode] = {
//    runZIO(program)
////    ???
//  }
//
//}
//
//sealed trait Toggle
//
//object Toggle {
//  type On  = On.type
//  type Off = Off.type
//  case object On  extends Toggle
//  case object Off extends Toggle
//}
//
////trait HasMany[A, T <: Toggle]
//
//case class HasMany[A]()
//
//object domain {
//
//  case class Teacher()
//
//  case class Student(
//      name: String,
//      hasMany: HasMany[Teacher]
//  )
//}
//
//case class Prop[A]()
//
//object Prop {
//  implicit final class PropOps[Self <: Prop[_]](val self: Self) extends AnyVal {
//    def ++[That <: Prop[_]](that: That): Self with That =
//      Prop().asInstanceOf[Self with That]
//
//    def without[B, R](prop: Prop[B])( //
//        implicit
//        ev2: Self =!= R,
//        ev1: Self <:< Prop[B] with R
//    ): R = ???
//  }
//}
//
////object Test {
////
////  object Student {
////    val name: Prop[String] = Prop[String]()
////    val age: Prop[Int]     = Prop[Int]()
////
////    val * : Prop[String] with Prop[Int] = name ++ age
////
////    val cool: Prop[Int] = *.without(name)
////  }
////}
