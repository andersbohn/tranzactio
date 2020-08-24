package samples

import io.github.gaelrenoux.tranzactio.ErrorStrategies
import zio.duration._
import zio.{Has, Layer, ZLayer}

object Conf {

  case class Root(
      db: DbConf,
      dbRecovery: ErrorStrategies
  )

  case class DbConf(
      url: String,
      username: String,
      password: String
  )

  def live(dbName: String): Layer[Nothing, Has[Root]] = ZLayer.succeed(
    Conf.Root(
      db = DbConf(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=10", "sa", "sa"),
      dbRecovery = ErrorStrategies.RetryForever.withTimeout(10.seconds).withRetryTimeout(1.minute)
    )
  )


  def liveLocalPg(dbName: String): Layer[Nothing, Has[Root]] = ZLayer.succeed {
    val url = s"jdbc:postgresql://localhost/$dbName"
    val x = Conf.Root(
      db = DbConf(url, "test", "test"),
      dbRecovery = ErrorStrategies.RetryForever.withTimeout(2.seconds).withRetryTimeout(5.seconds)
    )
    println(s"teh db '$url'")
    x
  }


}

