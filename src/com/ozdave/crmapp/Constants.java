package com.ozdave.crmapp;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.microsoft.aad.adal.AuthenticationSettings;




public class Constants {
	
    // -------------------------------AAD
    // PARAMETERS----------------------------------
	
	// using the authority URLcopied from the Windows app
    static final String AUTHORITY_URL = "https://login.windows.net/8ffacab8-e7fe-4f54-b08f-9da391ab5006/oauth2/authorize";
    
    // the redirect URL MUST match the URL specified in Azure AD when client app is registered
    // You can setup redirectUri as your packagename. It is not required to be provided for the acquireToken call.
    static final String REDIRECT_URL = "http://com.ozdave.crmapp";

    // get the client ID from Azure Active Directory after the app is registered
    static final String CLIENT_ID = "68ff5a72-95ad-4ec6-be8e-9ebbc3887a80";

    static final String RESOURCE_ID = "https://ip1409.crm.dynamics.com";


    // optional parameter suggesting the user login
    static String USER_HINT = "ip1409@ip1409.onmicrosoft.com";

    // Endpoint we are targeting for the deployed WebAPI service
    static final String SERVICE_URL = "https://ip1409.crm.dynamics.com";
	
    
    
    public static void setupKeyForSample() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
		    // use same key for tests
		    SecretKeyFactory keyFactory = SecretKeyFactory
		            .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
		    SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
		            "abcdedfdfd".getBytes("UTF-8"), 100, 256));
		    SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
		    AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
		}
	}

}
