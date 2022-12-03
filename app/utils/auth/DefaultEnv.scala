package utils.auth

import io.github.honeycombcheesecake.play.silhouette.api.Env
import io.github.honeycombcheesecake.play.silhouette.impl.authenticators.CookieAuthenticator
import models.{ ContactTicket, SupportTicket, User }

/**
 * The default env.
 */
trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
  type C = ContactTicket
  type S = SupportTicket
}
