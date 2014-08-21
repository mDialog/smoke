package smoke.test

import smoke._
import scala.concurrent.duration._
import scala.concurrent.Await

trait TestSmoke {
  self: Smoke ⇒

  def send = application
  def sendAwait(r: Request)(implicit timeout: Duration = 5.seconds) = Await.result(application(r), timeout)
  override def start {}
}

case class TestSmokeApp(s: Smoke) {
  def send = s.application
  def sendAwait(r: Request)(implicit timeout: Duration = 5.seconds) = Await.result(s.application(r), timeout)
}