/*********************/
/* Global Check file */
/*********************/

var MAX_FILENAME_LENGTH = 240
var MAX_FILESIZE = 10000000  //10Mb
var errors = 0;

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

/* Sanatise Filename */
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
    // return isValid
    // return typeof fileName != "undefined" ? fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase() === shouldEqual : false;
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
    const fileInput = document.getElementById("file-input")
    const checkFileButton = document.getElementById("check-file-button")
    if (errorSummary) {
        errorSummary.parentNode.removeChild(errorSummary)
    }
    if (fileInput) {
        fileInput.classList.remove("fileAlert")
    }
    if (checkFileButton) {
        checkFileButton.disabled = false
        checkFileButton.focus()
    }
}

document.addEventListener("DOMContentLoaded", function () {
    /* Spinner */
    if (checkFileButton) {
        if (ie) {
            checkFileButton.addEventListener('click', function () {
                const temp = document.getElementById("progress-spinner-img").getAttribute("src");
                document.getElementById("progress-spinner").style.display = '';
                document.getElementById("progress-spinner-img").setAttribute("src", temp)
            });
        } else {
            checkFileButton.addEventListener('click', function () {
                document.getElementById("progress-spinner").style.display = '';
            });
        }
    }

    const summaryMessage = document.querySelector('.validation-summary-message a')
    if (summaryMessage) {
        document.querySelector('.validation-summary-message a').addEventListener('click', function () {
            e.preventDefault();
            var focusId = this.getAttribute('data-focuses'),
                thingToFocus = document.getElementById(focusId);
            $('html, body').animate({
                scrollTop: thingToFocus.parent().offset().top
            }, 500);
            const thingParent = thingToFocus.parentNode
            thingParent.querySelector("#error-summary").parentNode.querySelector("input").focus();
            thingParent.querySelector("#fileToUpload").focus();
            thingParent.querySelector(".block-label").focus();
            thingParent.querySelector(".form-control").focus();
        });
    }

    const errors = document.getElementById("errors")
    if (errors) {
        errors.focus()
    }
});
	
	    