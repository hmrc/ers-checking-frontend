/***********************/
/* Check ODS file page */
/***********************/

const MAX_ODS_FILESIZE = 209715200// 200 MB

document.getElementById("input-file-name").onchange = function (e) {
    const input = document.getElementById('input-file-name');
    document.getElementsByClassName("visibility")[0].classList.add("govuk-!-display-none")
    if (input.files.length !== 0 ) {
        let fileName
        if (ie < 10) {
            fileName = input.value.substr(input.value.lastIndexOf("\\") + 1, input.value.length);
        } else {
            fileName = input.files[0].name;
        }

        // Check file name
        if (validFileName(fileName)) {
            // check file name length
            if (fileName.length <= MAX_FILENAME_LENGTH) {
                if (fileSizeOK()) {
                    // file ok
                    removeErrorMsg();
                } else {
                    showErrorMsg(getLocalisedContent("ods.file.too.large", [MAX_ODS_FILESIZE / 1000000]));
                }
            } else {
                showErrorMsg(getLocalisedContent("ods.file.name.too.long", [MAX_FILENAME_LENGTH]));
            }
        } else {
            showErrorMsg(getLocalisedContent("ods.file.name.invalid.chars"));
        }
    } else {
        // this should display the 'you didn't select a file' error message
        removeErrorMsg()
    }
};



