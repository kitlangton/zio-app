package zio.app.internal.macros

import zhttp.http._
import zio.Has

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[app] class Macros(val c: blackbox.Context) {
  import c.universe._

  def client_impl[Service: c.WeakTypeTag]: c.Tree = {
    val serviceType = c.weakTypeOf[Service]
    assertValidMethods(serviceType)

    val methodDefs = serviceType.decls.collect { case method: MethodSymbol =>
      val methodName = method.name

      val valDefs = method.paramLists.map {
        _.map { param =>
          ValDef(Modifiers(Flag.PARAM), TermName(param.name.toString), tq"${param.typeSignature}", EmptyTree)
        }
      }

      val params           = method.paramLists.flatten.map(param => TermName(param.name.toString))
      val tupleConstructor = TermName(s"Tuple${params.length}")
      val pickleType       = q"$tupleConstructor(..$params)"

      //                                            0  1  2 <-- Accesses the return type of the ZIO
      //                                        ZIO[R, E, A]
      val returnType = method.returnType.dealias.typeArgs(2)

      val request =
        if (params.isEmpty)
          q"_root_.zio.app.FrontendUtils.fetch[$returnType](${serviceType.typeConstructor.toString}, ${methodName.toString})"
        else
          q"_root_.zio.app.FrontendUtils.fetch[$returnType](${serviceType.typeConstructor.toString}, ${methodName.toString}, Pickle.intoBytes($pickleType))"

      q"def $methodName(...$valDefs): ${method.returnType} = $request"
    }

    val result = q"""
new ${serviceType.finalResultType} {
  import java.nio.ByteBuffer
  import boopickle.Default._

  ..$methodDefs
}
       """
    println(result)
    result
  }

  def routes_impl[Service: c.WeakTypeTag]: c.Expr[HttpApp[Has[Service], Throwable]] = {
    val serviceType = c.weakTypeOf[Service]
    assertValidMethods(serviceType)

    val blocks = serviceType.decls.collect { case method: MethodSymbol =>
      val methodName = method.name

      val argsType = method.paramLists.flatten.collect {
        case param: TermSymbol if !param.isImplicit => param.typeSignature
      } match {
        case Nil      => tq"Unit"
        case a :: Nil => tq"Tuple1[$a]"
        case tpes     => tq"(..$tpes)"
      }

      val callMethod = callServiceMethod(serviceType, method)

      val block =
        if (method.paramLists.flatten.isEmpty)
          q"""_root_.zio.app.internal.Utils.makeRouteNullary(${serviceType.finalResultType.toString}, ${methodName.toString}, { $callMethod })"""
        else
          q"""_root_.zio.app.internal.Utils.makeRoute(${serviceType.finalResultType.toString}, ${methodName.toString}, { (unpickled: $argsType) => $callMethod })"""

      block
    }

    val block = blocks.reduce((a, b) => q"$a +++ $b")

    val result = c.Expr[HttpApp[Has[Service], Throwable]](q"""
import zhttp.http._
import boopickle.Default._
$block
        """)

    println(result)

    result
  }

  private def callServiceMethod(service: Type, method: MethodSymbol): c.Tree = {
    var idx = 0

    val params = method.paramLists.map { paramList =>
      paramList.map { _ =>
        idx += 1
        q"unpickled.${TermName("_" + idx)}"
      }
    }

    q"ZIO.serviceWith[$service](_.${method.name}(...$params))"
  }

  private def hasTypeParameters(t: Type): Boolean = t match {
    case _: PolyType => true
    case _           => false
  }

  private def assertValidMethods(t: Type): Unit = {
    val methods = t.decls.filter(m => hasTypeParameters(m.typeSignature))
    if (methods.nonEmpty) {
      c.abort(c.enclosingPosition, s"Invalid methods:\n  - ${methods.map(_.name).mkString("\n  - ")}")
    }
  }
}
