function initLocalisedContent() {

  "use strict";

  var content = {
    "en" : {
      "ogl.pt.1" : "All content is available under the",
      "ogl.link.text" : "Open Government Licence v3.0",
      "ogl.pt.2" : ", except where otherwise stated",
      "crown.copyright" : "© Crown Copyright",
      "deskproAjax.received.pt1" : "We have received your contact details",
      "deskproAjax.received.pt2" : "Our technical team will send you an email to let you know when you can access the service again.",
      "deskproAjax.not.received.pt1" : "Sorry, we are unable to receive your contact details right now",
      "deskproAjax.not.received.pt2" : "Please try again later or email",
      "deskproAjax.not.received.pt3" : "if you need help with this service.",
      "upload.uploaded" : "Report uploaded – now check it for errors",
      "upload.cancel" : "Cancel",
      "upload.remove" : "Remove report",
      "upload.not.supported" : "This service doesn't currently support mobile devices"
    },
    "cy" : {
      "ogl.pt.1" : "Mae'r holl gynnwys ar gael dan y",
      "ogl.link.text" : "Drwydded Llywodraeth Agored, fersiwn 3.0",
      "ogl.pt.2" : ", oni nodir yn wahanol",
      "crown.copyright" : "© Hawlfraint y Goron",
      "deskproAjax.received.pt1" : "Mae’ch manylion cyswllt wedi dod i law",
      "deskproAjax.received.pt2" : "Bydd ein tîm technegol yn anfon e-bost atoch i roi gwybod i chi pryd gallwch gael mynediad at y gwasanaeth eto.",
      "deskproAjax.not.received.pt1" : "Mae’n ddrwg gennym. Ni allwn gymryd eich manylion cyswllt ar hyn o bryd",
      "deskproAjax.not.received.pt2" : "Rhowch gynnig arall arni yn nes ymlaen neu e-bostiwch",
      "deskproAjax.not.received.pt3" : "os oes angen help arnoch gyda’r gwasanaeth hon.",
      "upload.uploaded" : "Adroddiad wedi’i uwchlwytho - nawr ewch ati i’w wirio am wallau",
      "upload.cancel" : "Canslo",
      "upload.remove" : "Dileu’r adroddiad",
      "upload.not.supported" : "Nid yw’r gwasanaeth hwn yn cefnogi dyfeisiau symudol ar hyn o bryd"
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

  console.log("BANG", GOVUK.playLanguage);

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