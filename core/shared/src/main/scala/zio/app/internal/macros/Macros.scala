package zio.app.internal.macros

import zio.http._
import zio.app.ClientConfig
import zio.stream.ZStream

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[app] class Macros(val c: blackbox.Context) {
  import c.universe._

  // Frontend Macro for deriving Clients

  def client_impl[Service: c.WeakTypeTag]: c.Tree =
    client_config_impl[Service](c.Expr[ClientConfig](q"_root_.zio.app.ClientConfig.empty"))

  def client_config_impl[Service: c.WeakTypeTag](config: c.Expr[ClientConfig]): c.Tree = {
    val serviceType = c.weakTypeOf[Service]
    assertValidMethods(serviceType)

    val appliedTypes               = getAppliedTypes(serviceType)
    def applyType(tpe: Type): Type = tpe.map(tpe => appliedTypes.getOrElse(tpe.typeSymbol, tpe))

    val methodDefs = serviceType.decls.collect { case method: MethodSymbol =>
      val methodName = method.name

      val valDefs = method.paramLists.map {
        _.map { param =>
          val applied = applyType(param.typeSignature)
          ValDef(Modifiers(Flag.PARAM), TermName(param.name.toString), tq"$applied", EmptyTree)
        }
      }

      val params           = method.paramLists.flatten.map(param => TermName(param.name.toString))
      val tupleConstructor = TermName(s"Tuple${params.length}")
      val pickleType       = q"$tupleConstructor(..$params)"

      //                                            0  1  2 <-- Accesses the return type of the ZIO
      //                                        ZIO[R, E, A]
      val errorType  = method.returnType.dealias.typeArgs(1)
      val returnType = applyType(method.returnType.dealias.typeArgs(2))
      val isStream =
        method.returnType.dealias.typeConstructor <:< weakTypeOf[ZStream[Any, Nothing, Any]].typeConstructor

      val request =
        if (isStream) {
          if (params.isEmpty)
            q"_root_.zio.app.FrontendUtils.fetchStream[$errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, $config)"
          else
            q"_root_.zio.app.FrontendUtils.fetchStream[$errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, Pickle.intoBytes($pickleType), $config)"
        } else {
          if (params.isEmpty)
            q"_root_.zio.app.FrontendUtils.fetch[$errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, $config)"
          else
            q"_root_.zio.app.FrontendUtils.fetch[$errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, Pickle.intoBytes($pickleType), $config)"
        }

      q"def $methodName(...$valDefs): ${applyType(method.returnType)} = $request"
    }

    val result = q"""
new ${serviceType.finalResultType} {
  import _root_.java.nio.ByteBuffer
  import _root_.boopickle.Default._
  import _root_.zio.app.internal.CustomPicklers._
  import _root_.zio.app.FrontendUtils.exPickler

  ..$methodDefs
}
       """
    result
  }

  // Backend Macro for deriving Routes

  // Type     -> c.WeakTypeTag
  // TypeRepr -> c.Type
  def routes_impl[Service: c.WeakTypeTag]: c.Expr[HttpApp[Service, Throwable]] = {
    val serviceType = c.weakTypeOf[Service]
    assertValidMethods(serviceType)

    val appliedTypes               = getAppliedTypes(serviceType)
    def applyType(tpe: Type): Type = tpe.map(tpe => appliedTypes.getOrElse(tpe.typeSymbol, tpe))

    val blocks = serviceType.decls.collect { case method: MethodSymbol =>
      val methodName = method.name

      // (Int, String)
      val argsType = method.paramLists.flatten.collect {
        case param: TermSymbol if !param.isImplicit => applyType(param.typeSignature)
      } match {
        case Nil      => tq"Unit"
        case a :: Nil => tq"Tuple1[$a]"
        case as       => tq"(..$as)"
      }

      val callMethod = callServiceMethod(serviceType, method)
      val isStream =
        method.returnType.dealias.typeConstructor <:< weakTypeOf[ZStream[Any, Nothing, Any]].typeConstructor

      //                                            0  1  2 <-- Accesses the return type of the ZIO
      //                                        ZIO[R, E, A]
      val errorType  = applyType(method.returnType.dealias.typeArgs(1))
      val returnType = applyType(method.returnType.dealias.typeArgs(2))

      val block =
        if (isStream) {
          if (method.paramLists.flatten.isEmpty)
            q"""_root_.zio.app.internal.BackendUtils.makeRouteNullaryStream[$serviceType, $errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, { $callMethod })"""
          else
            q"""_root_.zio.app.internal.BackendUtils.makeRouteStream[$serviceType, $errorType, $argsType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, { (unpickled: $argsType) => $callMethod })"""
        } else {
          if (method.paramLists.flatten.isEmpty)
            q"""_root_.zio.app.internal.BackendUtils.makeRouteNullary[$serviceType, $errorType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, { $callMethod })"""
          else
            q"""_root_.zio.app.internal.BackendUtils.makeRoute[$serviceType, $errorType, $argsType, $returnType](${serviceType.finalResultType.toString}, ${methodName.toString}, { (unpickled: $argsType) => $callMethod })"""
        }

      block
    }

    val block = blocks.reduce((a, b) => q"$a ++ $b")

    val result = c.Expr[HttpApp[Service, Throwable]](q"""
import _root_.zio.http._
import _root_.boopickle.Default._
import _root_.zio.app.internal.CustomPicklers._
import _root_.zio.app.internal.BackendUtils.exPickler
  
$block
        """)

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

    if (method.returnType.dealias.typeConstructor <:< typeOf[ZStream[Any, Nothing, Any]].typeConstructor)
      q"_root_.zio.stream.ZStream.environmentWithStream[$service](_.get.${method.name}(...$params))"
    else
      q"_root_.zio.ZIO.serviceWithZIO[$service](_.${method.name}(...$params))"
  }

  private def hasTypeParameters(t: Type): Boolean = t match {
    case _: PolyType => true
    case _           => false
  }

  /**
   * Assures the given trait's declarations contain no type parameters.
   */
  private def assertValidMethods(t: Type): Unit = {
    val methods = t.decls.filter(m => hasTypeParameters(m.typeSignature))
    if (methods.nonEmpty) {
      c.abort(c.enclosingPosition, s"Invalid methods:\n  - ${methods.map(_.name).mkString("\n  - ")}")
    }
  }

  // Symbol -> Type
  // Service[A] -> Service[Int]
  // Map(A -> Int)
  private def getAppliedTypes[Service: c.WeakTypeTag](serviceType: c.Type): Map[c.universe.Symbol, c.universe.Type] =
    (serviceType.typeConstructor.typeParams zip serviceType.typeArgs).toMap
}
