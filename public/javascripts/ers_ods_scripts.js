	/***********************/
	/* Check ODS file page */
	/***********************/

	$("#choose-file-button").click(function (e) {
		//e.preventDefault();
		$("#input-file-name").click();
	});	

	function showODSErrorMsg(msg) {
    	if ($("#error-summary").length) {
    		$("#error-summary").remove();
    	}
    	$("#check-file-button").attr("disabled",true);
    	$("#file-input").addClass("fileAlert");
    	$(".visibility").show();
	    $("#input-file-name").before("<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>"+msg+".</p>");
	    $(".validation-summary-message a").html(msg);
	    $("#errors").focus();
	}
	
	$("#input-file-name").change(function(e){					
		var $el = $('#input-file-name');		
		// extract file name for validation
		$(".visibility").hide();
		if (ie<10) {		
			var fileName = $el.val().substr($el.val().lastIndexOf("\\")+1, $el.val().length);	
		} else {
			var fileName = $("#input-file-name")[0].files[0].name;
		}	
		
		// Check file name
		if (validFileName(fileName)) {			
			// check file name length
			if (fileName.length <= MAX_FILENAME_LENGTH) {
				// Check file extn
				if (getFileNameExtension(fileName) == "ods") {
					if (fileSizeOK()) {
						// file ok
				    	removeErrorMsg();						
					} else {
						showODSErrorMsg(GOVUK.getLocalisedContent("ods.file.too.large", [MAX_CSV_FILESIZE/1000000]));
					}
				} else {
					showODSErrorMsg(GOVUK.getLocalisedContent("ods.file.wrong.type"));
				}				
			} else {
				showODSErrorMsg(GOVUK.getLocalisedContent("ods.file.name.too.long", [MAX_FILENAME_LENGTH]));
			}
		} else {
			showODSErrorMsg(GOVUK.getLocalisedContent("ods.file.name.invalid.chars"));
		}		
				
		// extract filename for display 
		$("#file-name").text(fileName);	
		
		// show page elements
		$("#file-header-bar").show();
		
		if (ie<11) {
			$("#remove-file-link").insertAfter("#input-file-name")
			$("#file-header-bar").css("padding-left","3px")
		}		
		$("#remove-file-link").show();
	});
	
	
	
	    