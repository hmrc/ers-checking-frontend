/***********************/
/* Check ODS file page */
/***********************/

// var MAX_ODS_FILESIZE = 10000000// 100 MB
var MAX_ODS_FILESIZE = 100000// 1 MB

function showODSErrorMsg(msg) {
    if (document.getElementById("error-summary")) {
        document.getElementById("error-summary").remove()
    }
    // if ($("#error-summary").length) {
    //     $("#error-summary").remove();
    // }
    document.getElementById("check-file-button").disabled = true
    // $("#check-file-button").attr("disabled", true);
    document.getElementById("file-input").classList.add("fileAlert")
    // $("#file-input").addClass("fileAlert");
    document.querySelector(".visibility").style.display = ""
    // $(".visibility").show();
    document.getElementById("input-file-name")
        .insertAdjacentHTML('beforebegin',"<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>" + msg + ".</p>")
    // $("#input-file-name").before("<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>" + msg + ".</p>");
    document.querySelector(".govuk-error-summary__list a").innerHTML = msg
    // $(".validation-summary-message a").html(msg);
    document.getElementById("errors").focus()
    // $("#errors").focus();
}

document.getElementById("input-file-name").onchange = function (e) {
    // $("#input-file-name").change(function(e){
    const input = document.getElementById('input-file-name');
    // extract file name for validation
    document.getElementsByClassName("visibility")[0].style.display = 'none';
    let fileName
    if (ie < 10) {
        fileName = input.value.substr(input.value.lastIndexOf("\\") + 1, input.value.length);
    } else {
        fileName = document.getElementById('input-file-name').files[0].name;
    }

    // Check file name
    if (validFileName(fileName)) {
        // check file name length
        if (fileName.length <= MAX_FILENAME_LENGTH) {
            // Check file extn
            if (getFileNameExtension(fileName, "ods")) {
                if (fileSizeOK()) {
                    // file ok
                    removeErrorMsg();
                } else {
                    showODSErrorMsg(getLocalisedContent("ods.file.too.large", [MAX_ODS_FILESIZE / 1000000]));
                }
            } else {
                showODSErrorMsg(getLocalisedContent("ods.file.wrong.type"));
            }
        } else {
            showODSErrorMsg(getLocalisedContent("ods.file.name.too.long", [MAX_FILENAME_LENGTH]));
        }
    } else {
        showODSErrorMsg(getLocalisedContent("ods.file.name.invalid.chars"));
    }

    // extract filename for display
    // document.getElementById("file-name").textContent = fileName
    // $("#file-name").text(fileName);

    // show page elements
    // $("#file-header-bar").show();

    // if (ie < 11) {
    //     $("#remove-file-link").insertAfter("#input-file-name")
    //     $("#file-header-bar").css("padding-left", "3px")
    // }
    // $("#remove-file-link").show();
};



