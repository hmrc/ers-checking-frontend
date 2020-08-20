import io.netty.handler.codec.base64.Base64
import models.SheetErrors
import uk.gov.hmrc.services.validation.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer




val decoder = Base64.decode("5kJZ28QsKWc6paCmNKZDuRCVhp3VyVbrqagA8hRRGDgAMNoaaS2ouS3rj5f63wxrQOSS0C2kZPWf5tBzW7pNqHOjwVF87mz+Hc0dzRcFK5xSj3EhIVt7x5WqEzSFWnXK7eJIjHUASET2Sa5tCt18skdowSGOQC9ovYYmRZnq9g9pk7K+Uuc9dBluy3788C05HNKZ9b+2+FnGhxcVXGgLIOYxB9yR2MSRH8rFpEADSEiC95TXcasPoSYvk9hzPV3pBznqOIM5RIBef7c9hWevMEUfeKon+EKN2i66jBikvotqqmKlmj9ml3eXaf6SrvfzU+QOsv78ZYvR+Tc5KsUWDQ==")

  ListBuffer(SheetErrors("CSOP_OptionsExercised_V3",ListBuffer(ValidationError(Cell("A",1,"23-07-2015"),"error.1","001","ers.upload.error.date"))))