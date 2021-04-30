/***********************/
/* Check CSV file page */
/***********************/

//var MAX_CSV_FILESIZE = 100000000// 100 MB
var MAX_CSV_FILESIZE = 1000000    // 1 MB
document.getElementById("check-file-button").disabled = true;

function csvFileSizeOK(fileSize) {
    if (ie < 10) {
        return true
    } else {
        return fileSize <= MAX_CSV_FILESIZE;
    }
}

function removeFileAlert() {
	document.getElementById("input-file-name").parentNode.classList.remove("fileAlert")
    // $(".input-file-name").each(function (index) {
    //     $(this).parent("Div").removeClass("fileAlert")
    // });
}

function showCSVErrorMsg(e, msg) {
    const button = document.getElementById("check-file-button");

    if (document.getElementById("error-summary")) {
        document.getElementById("error-summary").remove()
    }

    document.querySelector(".visibility").style.display = "block";
    button.disabled = true
    e.insertAdjacentHTML('beforebegin',"<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>" + msg + ".</p>")
    // $(e).before("<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>" + msg + ".</p>")
    document.querySelector(".validation-summary-message a").innerHTML = msg
    // $(".validation-summary-message a").html(msg)
    // $("#errors").focus();
    document.getElementById("errors").focus();
}

function validateFile(fileName, fileSize, e) {
    // Check file name
    if (validFileName(fileName)) {
        // check file name length
        if (fileName.length <= MAX_FILENAME_LENGTH) {
            // Check file extn
            if (getFileNameExtension(fileName, "csv")) {
                if (csvFileSizeOK(fileSize)) {
                    // file ok
                    return true;
                } else {
                    showCSVErrorMsg(e, getLocalisedContent("csv.file.too.large", [MAX_CSV_FILESIZE / 1000000]));
                    errors++;
                    return false;
                }
            } else {
                showCSVErrorMsg(e, getLocalisedContent("csv.file.wrong.type"));
                errors++;
                return false;
            }
        } else {
            showCSVErrorMsg(e, getLocalisedContent("csv.file.name.too.long", [MAX_FILENAME_LENGTH]));
            errors++;
            return false;
        }
    } else {
        showCSVErrorMsg(e, getLocalisedContent("csv.file.name.invalid.chars"));
        errors++;
        return false;
    }
}

document.getElementById("input-file-name").onchange = function (e) {
    document.getElementsByClassName("visibility")[0].style.display = 'none';
    errors = 0;
    if (e.target.value.length > 0) {
        // extract file name for validation
        if (ie < 10) {
            var fileName = e.target.value.substr(
                e.target.value.lastIndexOf("\\") + 1,
                e.target.value.length);
        } else {
            const file = e.target.files.item(0);
            var fileName = file.name;
            var fileSize = file.size;
        }
        if (fileName != undefined) {
            if (!validateFile(fileName, fileSize, document.getElementById("input-file-name"))) {
                removeFileAlert();
                e.target.parentNode.classList.add("fileAlert");
            }
        }
    }
    if (errors == 0) {
        removeFileAlert();
        removeErrorMsg();
    }
};


