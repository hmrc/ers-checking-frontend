function initLocalisedContent() {

  "use strict";

  var content = {
    "en" : {
      "ogl.pt.1" : "All content is available under the",
      "ogl.link.text" : "Open Government Licence v3.0",
      "ogl.pt.2" : ", except where otherwise stated",
      "crown.copyright" : "© Crown Copyright",
      "csv.already.chosen" : "Choose a different file &ndash; you've already chosen one with this name",
      "csv.file.too.large" : "Check this file (you can only upload files that are {0}MB or less)",
      "csv.file.wrong.type" : "Choose a different file &ndash; it must be a CSV file",
      "csv.file.name.too.long" : "The filename must contain {0} characters or less",
      "csv.file.name.invalid.chars" : "Choose a different file &ndash; the file's name can't contain invalid characters",
      "ods.file.too.large" : "The attached file is to large. We only accept files less than {0}MB",
      "ods.file.wrong.type" : "Choose a different file &ndash; it must be an ODS file",
      "ods.file.name.too.long" : "Choose a different file &ndash; the file's name must be {0} characters or less",
      "ods.file.name.invalid.chars" : "Choose a different file &ndash; the file's name can't contain invalid characters"
    },
    "cy" : {
      "ogl.pt.1" : "Mae'r holl gynnwys ar gael dan y",
      "ogl.link.text" : "Drwydded Llywodraeth Agored, fersiwn 3.0",
      "ogl.pt.2" : ", oni nodir yn wahanol",
      "crown.copyright" : "© Hawlfraint y Goron",
      "csv.already.chosen" : "Dewiswch ffeil wahanol &ndash; rydych eisoes wedi dewis un gyda’r enw hwn",
      "csv.file.too.large" : "Gwiriwch y ffeil hon (gallwch ond uwchlwytho ffeiliau sy’n {0}MB neu lai)",
      "csv.file.wrong.type" : "Dewiswch ffeil wahanol &ndash; rhaid iddi fod yn ffeil CSV",
      "csv.file.name.too.long" : "Mae’n rhaid i enw’r ffeil gynnwys {0} o gymeriadau neu lai",
      "csv.file.name.invalid.chars" : "Dewiswch ffeil wahanol &ndash; na all enw’r ffeil gynnwys cymeriadau annilys",
      "ods.file.too.large" : "Mae’r ffeil a atodwyd yn rhy fawr. Rydym dim ond yn derbyn ffeiliau sy’n llai na {0}MB",
      "ods.file.wrong.type" : "Dewiswch ffeil wahanol – rhaid iddi fod yn ffeil ODS",
      "ods.file.name.too.long" : "Dewiswch ffeil wahanol – rhaid i enw’r ffeil fod yn {0} o gymeriadau neu lai",
      "ods.file.name.invalid.chars" : "Dewiswch ffeil wahanol &ndash; na all enw’r ffeil gynnwys cymeriadau annilys"
    }
  }

  String.prototype.supplant = function (o) {
    return this.replace(/{([^{}]*)}/g,
      function (a, b) {
        var r = o[b];
        return typeof r === 'string' || typeof r === 'number' ? r : a;
      }
    );
  };

  GOVUK.playLanguage = (function() {
    var playCookieName = encodeURIComponent("PLAY_LANG") + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) === ' ')
        c = c.substring(1, c.length);
        if (c.indexOf(playCookieName) === 0)
          return decodeURIComponent(c.substring(playCookieName.length, c.length));
    }
    return "en";
  }());

  GOVUK.getLocalisedContent = function(key, args) {
    return content[GOVUK.playLanguage][key].supplant(args);
  }
}

$(document).ready(function () {

  initLocalisedContent();

  // Switch out OGL footer and Crown Copyright if welsh language
  if (GOVUK.playLanguage === "cy") {
    $(".footer-meta-inner .open-government-licence > p").remove();
    $(".footer-meta-inner .open-government-licence").append('<p>' +
      GOVUK.getLocalisedContent("ogl.pt.1") +
      ' <a href="http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3" target="_blank">' +
      GOVUK.getLocalisedContent("ogl.link.text") +
      '</a>' +
      GOVUK.getLocalisedContent("ogl.pt.2") +
      '</p>');

    $(".footer-meta .copyright > a").text(GOVUK.getLocalisedContent("crown.copyright"));
  }
});