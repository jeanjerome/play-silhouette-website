package controllers

import io.github.honeycombcheesecake.play.silhouette.api.exceptions.ProviderException
import io.github.honeycombcheesecake.play.silhouette.api.util.Credentials
import io.github.honeycombcheesecake.play.silhouette.impl.exceptions.IdentityNotFoundException
import io.github.honeycombcheesecake.play.silhouette.impl.providers._
import forms.{ SignInForm, TotpForm }

import javax.inject.Inject
import play.api.i18n.Messages
import play.api.mvc.{ Action, AnyContent, Request }
import utils.route.Calls

import scala.concurrent.{ ExecutionContext, Future }

/**
 * The Sign In controller.
 */
class SignInController @Inject() (
  scc: SilhouetteControllerComponents,
  signIn: views.html.signIn,
  activateAccount: views.html.activateAccount,
  totp: views.html.totp
)(implicit ex: ExecutionContext) extends AbstractAuthController(scc) {

  /**
   * Views the Sign In page.
   *
   * @return The result to display.
   */
  def view: Action[AnyContent] = UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(signIn(SignInForm.form, socialProviderRegistry)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit: Action[AnyContent] = UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignInForm.form.bindFromRequest().fold(
      form => Future.successful(BadRequest(signIn(form, socialProviderRegistry))),
      data => {
        logger.debug("it got the login data")
        val credentials = Credentials(data.email, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated =>
              val splittedEmail = data.email.split("@")
              Future.successful(Ok(activateAccount(data.email, splittedEmail.head.replaceAll("\\.", "____"), splittedEmail.last.replaceAll("\\.", "____"))))
            case Some(user) =>
              authInfoRepository.find[GoogleTotpInfo](user.loginInfo).flatMap {
                case Some(totpInfo) => Future.successful(Ok(totp(TotpForm.form.fill(TotpForm.Data(
                  user.userID, totpInfo.sharedKey, rememberMe = false)))))
                case _ => authenticateUser(user, rememberMe = false)
              }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case _: ProviderException =>
            Redirect(Calls.signin).flashing("error" -> Messages("invalid.credentials"))
        }
      }
    )
  }
}
