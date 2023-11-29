import config.ApplicationConfig
import models.{CsvFiles, SheetErrors}
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.HmrcLayout
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.ERSUtil
import views.html._

import scala.collection.mutable.ListBuffer

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  // Implicits for all views
  // curiously, super.arbRequest is also initialised as fixed(fakeRequest) but doesn't work here due to incompatible types, hence this:
  implicit val arbitraryRequest: Arbitrary[Request[AnyRef]] = fixed(fakeRequest)
  val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val arbitraryConfig: Arbitrary[ApplicationConfig] = fixed(this.appConfig)

  // for ers_main
  implicit val arbitraryHtml:Arbitrary[Html] = fixed(Html("<span />"))

  // for scheme_type
  implicit val arbitrarySchemeTypeForm: Arbitrary[Form[models.CS_schemeType]] = fixed(models.CSformMappings.schemeTypeForm)

  // for check_csv_file
  implicit val arbitraryErsUtil: Arbitrary[ERSUtil] = fixed(app.injector.instanceOf[ERSUtil])

  // for check_file_type
  implicit val arbitraryCheckFileTypeForm: Arbitrary[Form[models.CS_checkFileType]] = fixed(models.CSformMappings.checkFileTypeForm)

  // for html_error_report
  implicit val arbitrarySheetErrors: Arbitrary[Seq[SheetErrors]] = fixed(Seq(SheetErrors("errString", ListBuffer(ValidationError(Cell("", 1, ""), "", "", "")))))

  // for select_csv_file_types
  private val csvFileSequence = Seq(CsvFiles("file-id1"), CsvFiles("file-id2"))
  implicit val arbitraryCsvFileSequence: Arbitrary[Seq[CsvFiles]] = fixed(csvFileSequence)

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case check_csv_file: check_csv_file => render(check_csv_file)
    case check_file: check_file => render(check_file)
    case check_file_type: check_file_type => render(check_file_type)
    case checking_success: checking_success => render(checking_success)
    case file_upload_error: file_upload_error => {
      implicit val arbAsciiString = fixed("article")
      render(file_upload_error)
    }
    case file_upload_problem: file_upload_problem => render(file_upload_problem)
    case format_errors: format_errors => render(format_errors)
    case global_error: global_error => render(global_error)
    case html_error_report: html_error_report => render(html_error_report)
    case not_authorised: not_authorised => render(not_authorised)
    case scheme_type: scheme_type => render(scheme_type)
    case select_csv_file_types: select_csv_file_types => {
      implicit val arbAsciiString = fixed("csop")
      render(select_csv_file_types)
    }
    //    case ers_main: ers_main => render(ers_main) // commenting this back in causes the NoSuchMethodError to come back
    case start: start => render(start)
  }

  val viewPackageName = "views.html"

  val layoutClasses: Seq[Class[_]] = Seq(classOf[ers_main])

  runAccessibilityTests()
}

