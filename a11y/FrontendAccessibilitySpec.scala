import config.ApplicationConfig
import models.{CsvFiles, SheetErrors}
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.ERSUtil
import views.html._

import scala.collection.mutable.ListBuffer

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  // Implicits for all views
  // curiously, super.arbRequest is also initialised as fixed(fakeRequest) but doesn't work here due to incompatible types, hence this:
  implicit val arbitraryRequest: Arbitrary[Request[AnyRef]] = fixed(fakeRequest)
  implicit val arbitraryConfig: Arbitrary[ApplicationConfig] = fixed(app.injector.instanceOf[ApplicationConfig])
  implicit val arbitraryErsUtil: Arbitrary[ERSUtil] = fixed(app.injector.instanceOf[ERSUtil])
  implicit val arbitraryHtml: Arbitrary[Html] = fixed(Html("<span />"))
  override implicit val arbAsciiString: Typeclass[String] = fixed("plain-text") // many strings are sensitive to HTML chars like <> in these tests

  val viewPackageName = "views.html"

  val layoutClasses: Seq[Class[_]] = Seq(classOf[ers_main])

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case check_csv_file: check_csv_file =>
      implicit val arbitraryErsUtil: Arbitrary[ERSUtil] = fixed(app.injector.instanceOf[ERSUtil])
      render(check_csv_file)
    case check_file: check_file => render(check_file)
    case check_file_type: check_file_type =>
      implicit val arbitraryCheckFileTypeForm: Arbitrary[Form[models.CS_checkFileType]] = fixed(models.CSformMappings.checkFileTypeForm)
      render(check_file_type)
    case checking_success: checking_success => render(checking_success)
    case file_upload_error: file_upload_error => render(file_upload_error)
    case file_upload_problem: file_upload_problem => render(file_upload_problem)
    case format_errors: format_errors =>
      render(format_errors)
    case global_error: global_error => render(global_error)
    case html_error_report: html_error_report =>
      implicit val arbitrarySheetErrors: Arbitrary[Seq[SheetErrors]] = fixed(Seq(SheetErrors("errString", ListBuffer(ValidationError(Cell("", 1, ""), "", "", "")))))
      render(html_error_report)
    case not_authorised: not_authorised => render(not_authorised)
    case scheme_type: scheme_type =>
      implicit val arbitrarySchemeTypeForm: Arbitrary[Form[models.CS_schemeType]] = fixed(models.CSformMappings.schemeTypeForm)
      render(scheme_type)
    case select_csv_file_types: select_csv_file_types =>
      implicit val arbAsciiString = fixed("csop")
      implicit val arbitraryCsvFileSequence: Arbitrary[Seq[CsvFiles]] = fixed(Seq(CsvFiles("file-id1"), CsvFiles("file-id2")))
      render(select_csv_file_types)
    case start: start => render(start)
  }

  runAccessibilityTests()
}

