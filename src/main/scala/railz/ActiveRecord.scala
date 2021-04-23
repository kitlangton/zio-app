package railz

object ActiveRecord {
  case class Grade(grade: Int, student: Student)

  case class Student(teachers: List[Teacher], grades: List[Grade])

  case class ClassAssignment(student: Student, teacher: Teacher)

  case class Teacher(students: List[Student])

  val student: Student = ???
  val teacher: Student = ???

  case class Query[A]() {
    def includes[B](f: A => B): Query[B] = ???
  }

  val studentQuery: Query[Student] = ???

  /** Use Cases
    * 1. Get all students with all of their teachers // Student
    * 2. Get a specific student with all of their teachers // List[Student]
    * 3. Get a specific student's teachers // List[Teacher]
    * 4. Get a specific student's teachers
    */

}
