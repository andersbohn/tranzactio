package samples

import javax.sql.DataSource
import org.h2.jdbcx.JdbcDataSource
import zio.blocking.Blocking
import zio.{Has, ZIO, ZLayer, blocking}

/**
 * Typically, you would use a Connection Pool like HikariCP. Here, we're just gonna use the JDBC H2 datasource directly.
 * Don't do that in production !
 */

object ConnectionPool {

  val live: ZLayer[Conf with Blocking, Throwable, Has[DataSource]] =
    ZIO.accessM[Conf with Blocking] { env =>
      val conf = env.get[Conf.Root]
      blocking.effectBlocking {
        val ds = new JdbcDataSource
        ds.setURL(conf.db.url)
        ds.setUser(conf.db.username)
        ds.setPassword(conf.db.password)
        ds
      }
    }.toLayer

  val liveLocalPg: ZLayer[Conf with Blocking, Throwable, Has[DataSource]] =
    ZIO.accessM[Conf with Blocking] { env =>
      val conf = env.get[Conf.Root]
      import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
      val config = new HikariConfig


      config.setJdbcUrl(conf.db.url)

      config.setUsername(conf.db.username)

      config.setPassword(conf.db.password)

      config.setMaximumPoolSize(10)

      config.setAutoCommit(false)
// FIXME remove ?
//      config.addDataSourceProperty("cachePrepStmts", "true")
//      config.addDataSourceProperty("prepStmtCacheSize", "250")
//      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
      ZIO.succeed(new HikariDataSource(config))
    }.toLayer


}
