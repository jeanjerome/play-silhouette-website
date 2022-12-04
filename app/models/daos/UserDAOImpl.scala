package models.daos

import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import models.User
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection

import java.util.UUID
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

/**
 * Give access to the user object.
 */
@Singleton
class UserDAOImpl @Inject() (
  implicit
  executionContext: ExecutionContext,
  reactiveMongoApi: ReactiveMongoApi
) extends UserDAO {

  def collection: Future[BSONCollection] = reactiveMongoApi.database.map(_.collection("user"))

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    implicit val readerUser: BSONDocumentReader[User] = Macros.reader[User]
    implicit val writerUser: BSONDocumentWriter[User] = Macros.writer[User]
    implicit val readerLoginInfo: BSONDocumentReader[LoginInfo] = Macros.reader[LoginInfo]
    implicit val writerLoginInfo: BSONDocumentWriter[LoginInfo] = Macros.writer[LoginInfo]

    collection.flatMap(
      _.find(BSONDocument("loginInfo" -> loginInfo), Option.empty[User])
        .one[User]
    )
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID): Future[Option[User]] = {
    implicit val reader: BSONDocumentReader[User] = Macros.reader[User]
    implicit val writer: BSONDocumentWriter[User] = Macros.writer[User]

    collection.flatMap(
      _.find(BSONDocument("userID" -> userID), Option.empty[User])
        .one[User]
    )
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User] =
    Future {
      implicit val reader: BSONDocumentReader[User] = Macros.reader[User]
      implicit val writer: BSONDocumentWriter[User] = Macros.writer[User]

      val f = find(user.userID)
      f.onComplete {
        case Success(Some(existingUser)) =>
          collection.flatMap(
            _.update(ordered = false)
              .one(BSONDocument("_id" -> existingUser.userID), existingUser.copy())
          )
        //  result = user
        case Success(None) =>
          val userWithId = user.copy(userID = UUID.randomUUID())
          (collection.flatMap(_.insert(ordered = false)
            .one(userWithId)), userWithId)
        //   result = user
        case _ =>

      }
      user
    }

}
/**
 * The companion object.
 */
object UserDAOImpl {

}
