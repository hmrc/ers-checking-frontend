/*********************/
/* Global Check file */
/*********************/

const MAX_FILENAME_LENGTH = 240
const MAX_FILESIZE = 10000000  //10Mb

const checkFileButton = document.getElementById("check-file-button")
if (checkFileButton) checkFileButton.disabled = true

// ----------------------------------------------------------
// If you're not in IE (or IE version is less than 5) then:
// ie === undefined
// If you're in IE (>=5) then you can determine which version:
// ie === 7; // IE7
// Thus, to detect IE:
// if (ie) {}
// And to detect the version:
// ie === 6 // IE6
// ie > 7 // IE8, IE9, IE10 ...
// ie < 9 // Anything less than IE9
// ----------------------------------------------------------
var ie = (function () {
    var undef, rv = -1; // Return value assumes failure.
    var ua = window.navigator.userAgent;
    var msie = ua.indexOf('MSIE ');
    var trident = ua.indexOf('Trident/');
    var edge = ua.indexOf('Edge/');
    if (msie > 0) {
        // IE 10 or older => return version number
        rv = parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
    } else if (trident > 0) {
        // IE 11 (or newer) => return version number
        var rvNum = ua.indexOf('rv:');
        rv = parseInt(ua.substring(rvNum + 3, ua.indexOf('.', rvNum)), 10);
    } else if (edge > 0) {
        // Edge
        rv = 13
    }
    return ((rv > -1) ? rv : undef);
}());

/* Sanitise Filename */
function validFileName(fileName) {
    return fileName.match(INVALID_CHARACTERS) == null;
}

function getFileNameExtension(fileName, shouldEqual) {
    if (typeof fileName != "undefined") {
        const extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase()
        return extension === shouldEqual
    } else {
        return false
    }
}

function fileSizeOK() {
    if (ie < 10) {
        return true
    } else {
        return document.getElementById('input-file-name').files[0].size <= MAX_FILESIZE;
    }
}

function removeErrorMsg() {
    const errorSummary = document.getElementById("error-summary")
    const checkFileButton = document.getElementById("check-file-button")
    const allErrors = document.querySelectorAll(".govuk-form-group--error")
    if (allErrors.length !== 0) {
        allErrors.forEach(error => error.classList.remove("govuk-form-group--error"))
    }

    if (errorSummary) {
        errorSummary.parentNode.removeChild(errorSummary)
    }
    if (checkFileButton) {
        if (document.getElementById("input-file-name").files.length !== 0) {
            checkFileButton.disabled = false
            checkFileButton.focus()
        } else {
            checkFileButton.disabled = true
        }

    }
}

function showErrorMsg(msg) {
    const button = document.getElementById("check-file-button");

    if (document.getElementById("error-summary")) {
        document.getElementById("error-summary").remove()
    }

    document.querySelector(".govuk-\\!-display-none").classList.remove("govuk-!-display-none")
    button.disabled = true
    document.getElementById("input-file-name")
        .insertAdjacentHTML('beforebegin',"<p id='error-summary' class='govuk-error-message' tabindex'-1' role='alert' aria-labelledby='error-heading'>" + msg + ".</p>")
    document.querySelector(".govuk-form-group").classList.add("govuk-form-group--error")

    document.querySelector(".govuk-error-summary__list a").innerHTML = msg
    document.getElementById("errors").focus()
}

document.addEventListener("DOMContentLoaded", function () {
    const fileProcessingAlertDiv = document.getElementById("file-processing-alert")

    /* Spinner */
    if (checkFileButton) {
            checkFileButton.addEventListener('click', function () {
                fileProcessingAlertDiv.style.display = ''
                fileProcessingAlertDiv.classList.remove("govuk-!-display-none")
            })
    }

    const errors = document.getElementById("errors")
    if (errors) {
        errors.focus()
    }
});
	
	    