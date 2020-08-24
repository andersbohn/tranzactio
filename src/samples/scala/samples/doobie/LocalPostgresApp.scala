package samples.doobie

import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.{DbException, ErrorStrategiesRef}
import samples.{Conf, ConnectionPool, Person}
import zio._

/** A sample app where all modules are linked through ZLayer. Should run as is (make sure you have com.h2database:h2 in
 * your dependencies). */
object LocalPostgresApp extends zio.App {

  private val zenv = ZEnv.any
  private val conf = Conf.liveLocalPg("tranzactio")
  private val datasource = (conf ++ zenv) >>> ConnectionPool.liveLocalPg
  private val database = (datasource ++ zenv) >>> Database.fromDatasource
  private val personQueries = PersonQueries.live

  type AppEnv = ZEnv with Database with PersonQueries with Conf
  private val appEnv = zenv ++ conf ++ database ++ personQueries

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val prog = for {
      _ <- console.putStrLn("Starting the app")
      trio <- myApp().provideLayer(appEnv)
      _ <- console.putStrLn(trio.mkString(", "))
    } yield ExitCode(0)

    prog.orDie
  }

  /** Main code for the application. Results in a big ZIO depending on the AppEnv. */
  def myApp(): ZIO[AppEnv, DbException, List[Person]] = {
    val queries: ZIO[Connection with AppEnv, DbException, List[Person]] = for {
      _ <- console.putStrLn("Creating the table")
      _ <- PersonQueries.setup
      _ <- PersonQueries.uqix
      _ <- console.putStrLn("Inserting the trio")
      _ <- PersonQueries.insert(Person("Buffy", "Summers"))
      _ <- PersonQueries.insert(Person("Willow", "Rosenberg"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris"))
      _ <- PersonQueries.insert(Person("Alexander", "Harris")).orElse(Task.succeed(())) // TODO duplicated name
      _ <- console.putStrLn("Reading the trio")
      trio <- PersonQueries.list
    } yield trio

    ZIO.accessM[AppEnv] { env =>
      implicit val errorRecovery: ErrorStrategiesRef = env.get[Conf.Root].dbRecovery
      Database.transactionOrWidenR[AppEnv](queries)
    }
  }

}
