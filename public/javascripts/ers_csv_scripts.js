/***********************/
/* Check CSV file page */
/***********************/

const MAX_CSV_FILESIZE = 209715200// 200 MB
document.getElementById("check-file-button").disabled = true;

function csvFileSizeOK(fileSize) {
    if (ie < 10) {
        return true
    } else {
        return fileSize <= MAX_CSV_FILESIZE;
    }
}

function validateFile(fileName, fileSize, e) {
    // Check file name
    if (validFileName(fileName)) {
        // check file name length
        if (fileName.length <= MAX_FILENAME_LENGTH) {
            if (csvFileSizeOK(fileSize)) {
                // file ok
                return true;
            } else {
                showErrorMsg(getLocalisedContent("csv.file.too.large", [MAX_CSV_FILESIZE / 1000000]));
                errors++;
                return false;
            }
        } else {
            showErrorMsg(getLocalisedContent("csv.file.name.too.long", [MAX_FILENAME_LENGTH]));
            errors++;
            return false;
        }
    } else {
        showErrorMsg(getLocalisedContent("csv.file.name.invalid.chars"));
        errors++;
        return false;
    }
}

document.getElementById("input-file-name").onchange = function (e) {
    document.querySelector("#errors").classList.add("govuk-!-display-none");
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
                e.target.parentNode.classList.add("govuk-form-group--error");
            }
        }
    }
    if (errors == 0) {
        removeErrorMsg();
    }
};


