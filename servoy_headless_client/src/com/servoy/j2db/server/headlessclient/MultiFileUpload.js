/**
 *  Change default file upload implementation to try to use HTML5 input 'multiple' attribute 
 *      [for multiple file upload]
 *         Andrei Costescu
 */
function MultipleFileUploadInterceptor(multiSelector, translatedMessages)
{
	var oldAddElement = multiSelector.addElement;
	multiSelector.addElement = function(element)
	{
		if(element.tagName.toLowerCase() == 'input' && element.type.toLowerCase() == 'file') {
			element.multiple = "multiple";
			if (Wicket.Browser.isOpera()) {
				// in Opera 12.02, changing 'multiple' this way does not update the field
				element.type = 'button';
				element.type = 'file';
			}
			element.addEventListener("change", function () {
				var temp_message_element = document.createElement('div');
				temp_message_element.id = "temp_message";
                temp_message_element.textContent = translatedMessages[0];
                if (element.files && element.files.length > 0) {
                    if (typeof element.files[0].lastModifiedDate === 'undefined') {
                        // the browser doesn\'t support the lastModifiedDate property so last modified date will not be available');
                    } else {
                    	for (var i=0; i<element.files.length; i++){
                    		var actionURL = element.form.getAttribute("action");
                    		actionURL+="&last_modified_"+element.getAttribute("name")+"_"+encodeURIComponent(element.files[i].name)+"="+element.files[i].lastModifiedDate.getTime();
                    		element.form.setAttribute("action",actionURL)
                    	}
                    }
                    if (!(element.parentNode.lastElementChild.lastElementChild.id == "file_list")){
                    	// add temporary message
                    	element.parentNode.insertBefore(temp_message_element, element);
                    }
                }
            });
           
            var button = document.getElementById("filebutton");
			button.addEventListener("click", function() {
       	 		var elements_to_hide = element.parentNode.children;
       	 		for(var i =0; i<elements_to_hide.length; i++) {
       	 			elements_to_hide[i].hidden = true;
       	 		}
       	 		var temp_upload_message = document.createElement('span');
				temp_upload_message.id = "temp_message";
      	  		temp_upload_message.textContent = translatedMessages[1];
       	 		element.parentNode.appendChild(temp_upload_message);
			});
		}
		oldAddElement.call(this, element);
	}.bind(multiSelector);
	
	multiSelector.addListRow = function( element ){

		// Row div
		var new_row = document.createElement('tr');
		new_row.id = "file_list";
		var contentsColumn = document.createElement('td');
		var buttonColumn = document.createElement('td');

		// Delete button
		var capturedForm = element.form;
		var lastModifiedParamName = "&last_modified_" + element.name;
		var delete_button = document.createElement( 'input' );
		delete_button.type = 'button';
		delete_button.value = this.delete_label;

		// References
		new_row.element = element;

		// Delete function
		delete_button.onclick= function(){

			// retrieve the parent panel
			var panel = this.parentNode.parentNode.parentNode.parentNode;
			
			// Remove element from form
			this.parentNode.parentNode.element.parentNode.removeChild( this.parentNode.parentNode.element );

			// Remove this row from the list
			this.parentNode.parentNode.parentNode.removeChild( this.parentNode.parentNode );

			// Decrement counter
			this.parentNode.parentNode.element.multi_selector.count--;
			
			// remove temporary message
			if(!(panel.lastElementChild.lastElementChild.id == "file_list")) {
				panel.removeChild(document.getElementById("temp_message"));
			}
			
			// Re-enable input element (if it's disabled)
			this.parentNode.parentNode.element.multi_selector.current_element.disabled = false;
			
			if(element.files && capturedForm !=null ){
				for (var i=0; i<element.files.length; i++){
					var actionURL = capturedForm.getAttribute("action");
					var regex = new RegExp((lastModifiedParamName+"_"+encodeURIComponent(element.files[i].name)).replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1")+"=[0-9]+");
					actionURL =actionURL.replace(regex,"");
					capturedForm.setAttribute("action",actionURL)
				}
			}
			
			// Appease Safari
			//    without it Safari wants to reload the browser window
			//    which nixes your already queued uploads
			return false;
		};

		// Set row value
		contentsColumn.innerHTML = this.getOnlyFileNames(element);
		new_row.appendChild( contentsColumn );
		
		// Add button
		buttonColumn.innerHTML = "&nbsp&nbsp&nbsp";
		buttonColumn.appendChild( delete_button );
		new_row.appendChild( buttonColumn );

		// Add it to the list
		this.list_target.appendChild( new_row );
		
	}.bind(multiSelector);
	
	multiSelector.getOnlyFileNames = function(inputElement)
	{
		if (inputElement.files && inputElement.files.length > 0)
		{
			var files = inputElement.files;
			var retVal = "";
			for (var i = 0; i < files.length; i++)
			{
				retVal += this.getOnlyFileName(files[i].name) + '<br>';
			}
			return retVal.slice(0, retVal.length - 4);
		}
		else
		{
			return this.getOnlyFileName(inputElement.value);
		}
	}.bind(multiSelector);

	multiSelector.getOnlyFileName = function(stringValue)
	{
		var separatorIndex1 = stringValue.lastIndexOf('\\');
		var separatorIndex2 = stringValue.lastIndexOf('/');
		separatorIndex1 = Math.max(separatorIndex1, separatorIndex2);
		return separatorIndex1 >= 0 ? stringValue.slice(separatorIndex1 + 1, stringValue.length) : stringValue;
	}.bind(multiSelector);
	
	return multiSelector;
}