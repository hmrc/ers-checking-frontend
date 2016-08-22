	/***********************/
	/* Check CSV file page */
	/***********************/

	var MAX_CSV_FILESIZE = 100000000// 100 MB
		
	function csvFileSizeOK (fileSize) {
		if (ie<10) {
			return true
		} else {
			if (fileSize > MAX_CSV_FILESIZE) {
				return false
			} else {
				return true
			}
		}		
	}	
	
	function duplicateFileName (fileName) {
		var duplicateNameCount = 0
		$(".input-csv-file-name").each(function(index){	
			if ($(this).val() != "") {
				if (ie<10) {			
					if ($(this).val().substr($(this).val().lastIndexOf("\\")+1, $(this).val().length) == fileName) {
						duplicateNameCount++;
						if (duplicateNameCount > 1) $(this).parent("Div").addClass("fileAlert")
					}			
				} else {
					if ($(this)[0].files[0].name == fileName) {
						duplicateNameCount++;
						if (duplicateNameCount > 1) $(this).parent("Div").addClass("fileAlert")
					}			
				}
			}
		}); 
		if (duplicateNameCount > 1) {
			return true;
		} else {
			return false;
		}		
	}	

	function removeFileAlert () {
		$(".input-csv-file-name").each(function(index){	
			$(this).parent("Div").removeClass("fileAlert")			
		});		
	}

	function showCSVErrorMsg(e, msg) {
    	if ($("#error-summary").length) {
    		$("#error-summary").remove();
    	}
    	$(".visibility").show();
    	$("#check-file-button").attr("disabled",true);
	    $(e).before("<p id='error-summary' class='field-error clear' tabindex'-1' role='alert' aria-labelledby='error-heading'>"+msg+".</p>")
	    $(".validation-summary-message a").html(msg)
	    $("#errors").focus();
	}
		
	function validateFile(fileName, fileSize, e) {		
		// Check file name
		if (validFileName(fileName)) {			
			// check file name length
			if (fileName.length <= MAX_FILENAME_LENGTH) {
				// Check file extn
				if (getFileNameExtension(fileName) == "csv") {
					if (csvFileSizeOK(fileSize)) {
						if (!duplicateFileName(fileName)) {							
							// file ok
							return true;
						} else {
							showCSVErrorMsg(e, "Choose a different file &ndash; you've already chosen one with this name");
					    	errors++;
							return false;
						}
					} else {
						showCSVErrorMsg(e, "Check this file (you can only upload files that are "+(MAX_CSV_FILESIZE/1000000)+"MB or less)");
						errors++;
						return false;
					}
				} else {
					showCSVErrorMsg(e, "Choose a different file &ndash; it must be a CSV file");
					errors++;
					return false;
				}				
			} else {
				showCSVErrorMsg(e, "The filename must contain "+ MAX_FILENAME_LENGTH +" characters or less");
				errors++;
				return false;
			}
		} else {
			showCSVErrorMsg(e, "Choose a different file &ndash; the file's name can't contain invalid characters");
			errors++;
			return false;
		}
	}
		
	$(".input-csv-file-name").change(function(e){
		$(".visibility").hide();
		errors = 0;
		$(".input-csv-file-name").each(function(index){	
			if ($(this).val() != "") {
				// extract file name for validation 
				if (ie<10) {			
					var fileName = $(this).val().substr($(this).val().lastIndexOf("\\")+1, $(this).val().length);					
				} else {
					var fileName = $(this)[0].files[0].name;
					var fileSize = $(this)[0].files[0].size;
				}		
				if (fileName != undefined) {
					if (!validateFile(fileName, fileSize, this)) {
						removeFileAlert();
						$(this).parent("Div").addClass("fileAlert"); 
					}						
				}
			}
		});
		if (errors == 0) {
			removeFileAlert();
			removeErrorMsg();
		}
	});
		
	
	    