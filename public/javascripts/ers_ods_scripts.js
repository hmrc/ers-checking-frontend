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
						showODSErrorMsg("The attached file is to large. We only accept files less than "+(MAX_FILESIZE/1000000)+"MB");
					}
				} else {
					showODSErrorMsg("Choose a different file &ndash; it must be an ODS file");
				}				
			} else {
				showODSErrorMsg("Choose a different file &ndash; the file's name must be "+ MAX_FILENAME_LENGTH +" characters or less");
			}
		} else {
			showODSErrorMsg("Choose a different file &ndash; the file's name can't contain invalid characters");
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
	
	
	
	    