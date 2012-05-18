
   // Used in apply_auth()
   function browserSelector() {
        var browserName = navigator.appName;
        var browserNum = parseInt(navigator.appVersion);
        if (browserName == "Netscape") {
            document.CertReqForm.hiddenbrowser.value = "netscape";
        } else if ((browserName == "Microsoft Internet Explorer") && (browserNum>= 4)) {
            document.CertReqForm.hiddenbrowser.value = "explorer";
        }
    }

	// Used in apply_exp.jspf
	function showCSPActiveX() {
		if (navigator.appName.indexOf("Explorer") != -1) {
		    if ( navigator.userAgent.indexOf("Windows NT 6") != -1 ) {
				document.writeln("<object classid=\"clsid:884e2049-217d-11da-b2a4-000e7bbb2b09\" id=\"g_objClassFactory\" height=\"0\" width=\"0\" ></object>");
			} else {
				document.writeln("<object classid=\"clsid:127698e4-e730-4e5c-a2b1-21490a70c8a1\" id=\"newencoder\" codebase=\"/CertControl/xenroll.cab#Version=5,131,3659,0\" height=\"0\" width=\"0\" ></object>");
				document.writeln("<object classid=\"clsid:43F8F289-7A20-11D0-8F06-00C04FC295E1\" id=\"oldencoder\" height=\"0\" width=\"0\" ></object>");
			}
		}
	}

    // Used by apply_nav.jspf, and cardCertApply.jsp
	function myDeclare() {
		if (navigator.appName.indexOf("Explorer") == -1) {
			explorer = false;
			plugin = navigator.mimeTypes["application/x-iid"];
		} else {
			explorer = true;
			if ( navigator.userAgent.indexOf("Windows NT 6") == -1 ) {
				plugin = ControlExists("IID.iIDCtl");
			} else {
				plugin = IsCSPInstalled("Net iD - CSP");
			}
		}
		if (plugin) {
			if (explorer) {
				document.writeln("<object name=\"iID\" classid=\"CLSID:5BF56AD2-E297-416E-BC49-00B327C4426E\" width=\"0\" height=\"0\"></object>");
			} else {
				document.writeln("<object name=\"iID\" type=\"application/x-iid\" width=\"0\" height=\"0\"></object>");
			}
		} else {
			document.writeln("The CryptoAPI component is not installed.");
		}
	}

    function selectKey() {
       if ( plugin ) {
          //document.writeln("Plugin installed<br>");
          var doTryNext = true;
          for ( i=0; doTryNext; i++ ) {
             sKey = document.iID.EnumProperty('Key',i);
             doTryNext = sKey!="";
             if ( doTryNext ) {
                aKey = sKey.split(";");
            if ( parseInt(aKey[2],16)<0x47 )
                   document.writeln("<option value=\""+sKey+"\">Slot: "+aKey[0]+". Key label: "+aKey[3]+". Key type: "+aKey[4]+". Key size: "+aKey[6]+".</option>");
             }
          }
       }
    }

    function generate_pkcs10() {
       if ( plugin ) {
          document.iID.SetProperty('Base64', 'true');
          document.iID.SetProperty('URLEncode', 'false');
          document.iID.SetProperty('Subject', "2.5.4.5=197205250777");
          aKey = document.netIdForm.tokenKey.value.split(";");
          document.iID.SetProperty('KeyId', aKey[2]); 
          document.iID.SetProperty('ActiveSlot', aKey[0]); 
          rv = document.iID.Invoke('CreateRequest');
          if (rv == 0) {
             document.netIdForm.iidPkcs10.value = document.iID.GetProperty("Request");
             document.netIdForm.submit();
          } else
             alert("Error when fetching certificate request from NetID: "+rv+". Slot: "+aKey[0]+". KeyId: "+aKey[2]+".");
       }
    }

    // Used by cardCertApply.jsp
    function generate_card_pkcs10()
    {
        document.iID.SetProperty('Base64', 'true');
        document.iID.SetProperty('URLEncode', 'false');
        document.iID.SetProperty('Password', '');
        document.iID.SetProperty('TokenLabel', "Prime EID IP1 (basic PIN)");
        document.iID.SetProperty('Subject', "2.5.4.5=197205250777");
        document.iID.SetProperty('KeyId', '45');
    
        rv = document.iID.Invoke('CreateRequest');
        if (rv == 0)
            document.form1.authpkcs10.value = document.iID.GetProperty("Request");
        else
            document.form1.authpkcs10.value = rv;
    
        document.iID.SetProperty('Base64', 'true');
        document.iID.SetProperty('URLEncode', 'false');
        document.iID.SetProperty('Password', '');
        document.iID.SetProperty('TokenLabel', "Prime EID IP1 (signature PIN)");
        document.iID.SetProperty('Subject', "2.5.4.5=197205250777");
        document.iID.SetProperty('KeyId', '46');
    
        rv = document.iID.Invoke('CreateRequest');
        if (rv == 0)
            document.form1.signpkcs10.value = document.iID.GetProperty("Request");
        else
            document.form1.signpkcs10.value = rv;

        document.form1.submit();    
    }
    
    // Used by apply_nav.jspf, apply_exp.jspf
	function show_NetID_form(username, password) {
        if (plugin) {
            document.writeln("<p>Since you have NetID installed you may download a certificate for a ");
            document.writeln("key on your smart card.</p>");
            document.writeln("<form name=\"netIdForm\" action=\"../certreq\" enctype=\"x-www-form-encoded\" method=\"post\">");
            document.writeln("  <fieldset>");
            document.writeln("    <legend>Key length</legend>");
            document.writeln("    <input name=\"user\" type=\"hidden\" value=\""+username+"\" />");
            document.writeln("    <input name=\"password\" type=\"hidden\" value=\""+password+"\" />");
            document.writeln("    <input name=\"iidPkcs10\" type=\"hidden\" />");
            if (navigator.appName.indexOf("Explorer") != -1 && navigator.userAgent.indexOf("Windows NT 6") != -1 ) {
				document.writeln("    <input name=\"classid\" type=\"hidden\" value=\"clsid:884e2049-217d-11da-b2a4-000e7bbb2b09\" />");
			}
            document.writeln("    <label for=\"tokenkey\">Please select key:</label> ");
            document.writeln("    <select name=\"tokenKey\" accesskey=\"p\">");
            selectKey();
            document.writeln("    </select>");
            document.writeln("    <label for=\"dummy\"></label>");
            document.writeln("    <input type=\"button\" value=\"Fetch Certificate\" onclick=\"generate_pkcs10()\" />");
            document.writeln("  </fieldset>");
            document.writeln("</form>");
        } else {
            document.writeln("<p>NetID not installed.</p>");
        }
    }
