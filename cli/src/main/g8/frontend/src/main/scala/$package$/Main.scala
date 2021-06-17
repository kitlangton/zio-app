package $package$

import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`import`
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("stylesheets/main.scss", JSImport.Namespace)
object Css extends js.Any

object Main {
  val css: Css.type = Css

  def main(args: Array[String]): Unit =
    waitForLoad {
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      unmount()
      val rootNode = render(appContainer, Frontend.view)
      storeUnmount(rootNode)
    }

  def waitForLoad(f: => Any): Unit =
    if (dom.window.asInstanceOf[js.Dynamic].documentLoaded == null)
      documentEvents.onDomContentLoaded.foreach { _ =>
        dom.window.asInstanceOf[js.Dynamic].documentLoaded = true
        f
      }(unsafeWindowOwner)
    else
      f

  def unmount(): Unit =
    if (scala.scalajs.LinkingInfo.developmentMode) {
      Option(dom.window.asInstanceOf[js.Dynamic].__laminar_root_unmount)
        .collect {
          case x if !js.isUndefined(x) =>
            x.asInstanceOf[js.Function0[Unit]]
        }
        .foreach { _.apply() }
    }

  def storeUnmount(rootNode: RootNode): Unit = {
    val unmountFunction: js.Function0[Any] = () => {rootNode.unmount()}
    dom.window.asInstanceOf[js.Dynamic].__laminar_root_unmount = unmountFunction
  }

  if (!js.isUndefined(`import`.meta.hot) && !js.isUndefined(`import`.meta.hot.accept)) {
    `import`.meta.hot.accept { (_: Any) => }
  }
}
