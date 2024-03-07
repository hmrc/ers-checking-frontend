
# ers-checking-frontend
Employment Related Securities (ERS) allow employers to register their employee share (and other security) schemes with HMRC.

HMRC provides [public documentation](https://www.gov.uk/topic/business-tax/employment-related-securities), which contains standard template forms.

The service allows customers to upload their ERS annual return files and check for formatting errors.

This service does support WELSH.

This service utilises [Upscan](https://github.com/hmrc/upscan-initiate)


### Running the service

Service Manager: `sm2 --start ERS_CHECKING_ALL`

You can use the `./run_all_tests.sh` script to quickly run the unit tests, along with scalastyle and a dependency check,  for this repo

| Repositories      | Link                                                  |
|-------------------|-------------------------------------------------------|
| Acceptance tests  | https://github.com/hmrc/ers-checking-acceptance-tests |
| Performance tests | https://github.com/hmrc/ers-checking-perf-tests       |


### Useful Links

| Context              | Link                                                                        |
|----------------------|-----------------------------------------------------------------------------|
| Local                | http://localhost:9225/check-your-ers-files                                  |
| QA                   | https://www.qa.tax.service.gov.uk/check-your-ers-files                      |
| Staging              | https://www.staging.tax.service.gov.uk/check-your-ers-files                 |
| Test files to upload | https://github.com/hmrc/ers-checking-acceptance-tests/tree/master/resources |


### Running Accessibility Tests

The [Accessibility Linter](https://github.com/hmrc/sbt-accessibility-linter) has been added to SBT for this project.

To run the tests, execute `sbt clean A11y/test` from the command line.


### Licence

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
