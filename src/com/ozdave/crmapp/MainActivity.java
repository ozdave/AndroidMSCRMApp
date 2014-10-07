package com.ozdave.crmapp;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
//import com.microsoft.aad.test.todoapi.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.net.*;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Build;



public class MainActivity extends Activity {
	
	// common idiom in Android apps
	// see http://developer.android.com/reference/android/util/Log.html
    private final static String TAG = "CRMApp";
	
    private ProgressBar mProgressBar;
    
    /**
     * Show this dialog when activity first launches to check if user has login
     * or not.
     */
    private ProgressDialog mLoginProgressDialog;

    private AuthenticationContext mAuthContext;
    
    private AuthenticationResult mToken;
    
    private int mLastRequestId = 0;
    
    private boolean refreshInProgress = false;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        
        // enable the retrieveMultiple op to run on the UI thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        
        // login code stolen from to do example  ------------------------------------------------
        
        // Initialize the progress bar
        mProgressBar = (ProgressBar)findViewById(R.id.loadingProgressBar);
        //mProgressBar.setVisibility(ProgressBar.GONE);

        mLoginProgressDialog = new ProgressDialog(this);
        mLoginProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoginProgressDialog.setMessage("Login in progress...");
        mLoginProgressDialog.show();
        refreshInProgress = false;
        
        // Ask for token and provide callback
        try {
            Constants.setupKeyForSample();
            mAuthContext = new AuthenticationContext(MainActivity.this, Constants.AUTHORITY_URL, false);
            Log.d(TAG, "About to call AuthContext.acquireToken");
            mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID,
                    		Constants.CLIENT_ID, Constants.REDIRECT_URL, Constants.USER_HINT, mAuthCallback);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Encryption is failed", Toast.LENGTH_SHORT)
                    .show();
        }
        
        // end login code ----------------------------------------------------------------------
        Log.d(TAG, "onCreate completing");
    }
    
    
    /**
     * mAuthContext.acquireToken starts AuthenticationActivty, which fires up a web browser and goes through authentication
     * it does this using activty.startActivtyForResult(), in OUR context
     * When the activity completes, the system calls onActivityResult(), which we need to override and pass onto mAuthContext
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called - pass to mAuthContext.onActivityResut");
        mAuthContext.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getApplicationContext(), "Login completed", Toast.LENGTH_SHORT).show();
    }
    
    
    // declare the authentication callback that will be called in acquire Token
    private AuthenticationCallback<AuthenticationResult> mAuthCallback = 
    		new AuthenticationCallback<AuthenticationResult>() {

		        @Override
		        public void onError(Exception exc) {
		        	Log.d(TAG, "Authentication failed: ", exc);
		            if (mLoginProgressDialog.isShowing()) {
		                mLoginProgressDialog.dismiss();
		            }
		            Toast.makeText(getApplicationContext(), TAG + "Login Error: " + exc.getMessage(), Toast.LENGTH_LONG).show();
		            navigateToLogOut();
		        }
		
		        @Override
		        public void onSuccess(AuthenticationResult result) {
		        	Log.d(TAG,  "Authentication OK: " + result.getIdToken());
		            if (mLoginProgressDialog.isShowing()) {
		                mLoginProgressDialog.dismiss();
		            }
		            if (result != null && !result.getAccessToken().isEmpty()) {
		                setLocalToken(result);
		                Toast.makeText(getApplicationContext(), TAG + "Login OK", Toast.LENGTH_LONG).show();
		                sendRequest();
		            } else {
		                navigateToLogOut();
		            }
		        }
    };
    
    
    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first

        Log.d(TAG, "onResume called");
        // User can click logout, it will come back here
        // It should refresh list again
        refreshInProgress = false;
        sendRequest();
    }
    
    
    // -------------  getting data from CRM
    
    private void sendRequest() {
        if (refreshInProgress || mToken == null || mToken.getAccessToken().isEmpty())
            return;

        refreshInProgress = true;
        String res = retrieveMultiple(mToken.getAccessToken(), "account", new String[] { "name", "emailaddress1", "telephone1" });
        List<CRMEntity> entities = parseEntities(res);
        
        List<AccountModel> accounts = new ArrayList<AccountModel>();
        // wrap the CRM Entity in AccountModel
        for (CRMEntity ent : entities) {
        	AccountModel a = new AccountModel(ent);
        	accounts.add(a);
        }
        ArrayAdapter<AccountModel> itemsAdapter = 
        	    new ArrayAdapter<AccountModel>(this, android.R.layout.simple_list_item_1, accounts);
        ListView listView = (ListView) findViewById(R.id.listView1);
        listView.setAdapter(itemsAdapter);
    }
    
    
    
    
    
    /// <summary>
    /// Retrieve entity record data from the organization web service. 
    /// </summary>
    /// <param name="accessToken">The web service authentication access token.</param>
    /// <param name="entity">The target entity for which the data should be retreived.</param>
    /// <param name="Columns">The entity attributes to retrieve.</param>
    /// <returns>Response from the web service.</returns>
    /// <remarks>Builds a SOAP HTTP request using passed parameters and sends the request to the server.</remarks>
    public static String retrieveMultiple(String accessToken, String entity, String[] Columns)
    {
        // Build a list of entity attributes to retrieve as a string.
        String columnsSet = "";
        for (String Column : Columns)
        {
            columnsSet += "<b:string>" + Column + "</b:string>";
        }
        // Default SOAP envelope string. This XML code was obtained using the SOAPLogger tool.
        String xmlSOAP =
         "<s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'>"  
           + "<s:Body>"
           + "<RetrieveMultiple xmlns='http://schemas.microsoft.com/xrm/2011/Contracts/Services' xmlns:i='http://www.w3.org/2001/XMLSchema-instance'>"
           + "<query i:type='a:QueryExpression' xmlns:a='http://schemas.microsoft.com/xrm/2011/Contracts'><a:ColumnSet>"
           + "<a:AllColumns>false</a:AllColumns><a:Columns xmlns:b='http://schemas.microsoft.com/2003/10/Serialization/Arrays'>" 
           + columnsSet
           + "</a:Columns></a:ColumnSet><a:Criteria><a:Conditions /><a:FilterOperator>And</a:FilterOperator><a:Filters /></a:Criteria>"
           + "<a:Distinct>false</a:Distinct><a:EntityName>" 
           + entity 
           + "</a:EntityName><a:LinkEntities /><a:Orders />"
           + "<a:PageInfo><a:Count>0</a:Count><a:PageNumber>0</a:PageNumber><a:PagingCookie i:nil='true' />"
           + "<a:ReturnTotalRecordCount>false</a:ReturnTotalRecordCount>"
           + "</a:PageInfo><a:NoLock>false</a:NoLock></query>"
           + "</RetrieveMultiple>"
           +  "</s:Body>"
           + "</s:Envelope>";

        // Use the RetrieveMultiple CRM message as the SOAP action.
        String SOAPAction = "http://schemas.microsoft.com/xrm/2011/Contracts/Services/IOrganizationService/RetrieveMultiple";
        
        // The URL for the SOAP endpoint of the organization web service.
        URL url;
        HttpsURLConnection conn = null;
        try {
            url = new URL(Constants.SERVICE_URL  + "/XRMServices/2011/Organization.svc/web");
            conn = (HttpsURLConnection)url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("SOAPAction", SOAPAction);
            conn.setRequestProperty("content-type", "text/xml; charset=utf-8");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            
            Log.d(TAG, "retrieveMultiple: url auth: " + url.getAuthority());
  
            //get the output stream to POST our data
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(xmlSOAP);
            wr.flush();
            wr.close();
            
            // read the response
            InputStreamReader rdr = new InputStreamReader(conn.getInputStream(), "UTF-8");        
            char[] buffer = new char[10000];
            int readCnt = 0;
            StringBuffer sbuf = new StringBuffer();
            while ((readCnt = rdr.read(buffer)) != -1) {
            	Log.d(TAG, "retrieveMult: readCnt = " + readCnt);
            	for (int i = 0; i < readCnt; i++) {
            		sbuf.append(buffer[i]);
            	}
            }
            String res = sbuf.toString();
            Log.d(TAG, "retrieveMultiple: results: " + res);      
            return  res;
        } catch(Exception ex) {
        	Log.d(TAG, "Exception in retrieveMultiple", ex);
        }
        finally {
        	if (conn != null) {
        		Log.d(TAG, "retrieveMultipel: disconnecting");
        		conn.disconnect();
        	}
        }
        return "";
    }
//        
//
//        // Create a new HTTP request.
//        HttpClient httpClient = new HttpClient();
//
//        // Set the HTTP authorization header using the access token.
//        httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
//
//        // Finish setting up the HTTP request.
//        HttpRequestMessage req = new HttpRequestMessage(HttpMethod.Post, url);
//        req.Headers.Add("SOAPAction", SOAPAction);
//        req.Method = HttpMethod.Post;
//        req.Content = new StringContent(xmlSOAP);
//        req.Content.Headers.ContentType = MediaTypeHeaderValue.Parse("text/xml; charset=utf-8");
//
//        // Send the request asychronously and wait for the response.
//        HttpResponseMessage response;
//        response = await httpClient.SendAsync(req);
//        var responseBodyAsText = await response.Content.ReadAsStringAsync();
//
//        return responseBodyAsText;
//    }
        
        
    
    private List<CRMEntity> parseEntities(String xml) {
    	
    	List<CRMEntity> ents = new ArrayList<CRMEntity>();
    	try {
	    	XmlPullParser parser = Xml.newPullParser();
	        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	        StringReader rdr = new StringReader(xml);
	        parser.setInput(rdr);
	        parser.nextTag();
            Log.d(TAG, "parseEntities: initial name: " + parser.getName());
	        while (parser.next() != XmlPullParser.END_TAG) {
	            if (parser.getEventType() != XmlPullParser.START_TAG) {
	                continue;
	            }
	            String name = parser.getName();
	            Log.d(TAG, "parseEntities: name: " + name);
	            // Starts by looking for the Entity tag
	            if (name.equals("a:Entity")) {
	            	CRMEntity e = new CRMEntity(parser);
	                ents.add(e);
	            } 
	        }
    	} catch(Exception ex) {
    		Log.d(TAG, "Exception in parseEntities: ", ex);
    	}
    	return ents;
    }
    
    
    
    
//    Accounts = new ObservableCollection<AccountsModel>();
//
//    // Converting response string to xDocument.
//    XDocument xdoc = XDocument.Parse(AccountsResponseBody.ToString(), LoadOptions.None);
//    XNamespace s = "http://schemas.xmlsoap.org/soap/envelope/";//Envelop namespace s
//    XNamespace a = "http://schemas.microsoft.com/xrm/2011/Contracts";//a namespace
//    XNamespace b = "http://schemas.datacontract.org/2004/07/System.Collections.Generic";//b namespace
//
//    foreach (var entity in xdoc.Descendants(s + "Body").Descendants(a + "Entities").Descendants(a + "Entity"))
//    {
//        AccountsModel account = new AccountsModel();
//        foreach (var KeyvaluePair in entity.Descendants(a + "KeyValuePairOfstringanyType"))
//        {
//            if (KeyvaluePair.Element(b + "key").Value == "name")
//            {
//                account.Name = KeyvaluePair.Element(b + "value").Value;
//            }
//            else if (KeyvaluePair.Element(b + "key").Value == "emailaddress1")
//            {
//                account.Email = KeyvaluePair.Element(b + "value").Value;
//            }
//            else if (KeyvaluePair.Element(b + "key").Value == "telephone1")
//            {
//                account.Phone = KeyvaluePair.Element(b + "value").Value;
//            }
//        }
//        Accounts.Add(account);
//    }
//    return Accounts;

    
    
    
    
    // token management
    
    private void getToken(final AuthenticationCallback callback) {
        // one of the acquireToken overloads
    	Log.d(TAG, "getToken");
        mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID, Constants.CLIENT_ID,
                Constants.REDIRECT_URL, Constants.USER_HINT, callback);
        mLastRequestId = callback.hashCode();
    }

    private AuthenticationResult getLocalToken() {
        return mToken;
    }

    private void setLocalToken(AuthenticationResult newToken) {
    	Log.d(TAG, "setLocalToken: " + newToken.getIdToken());
        mToken = newToken;
    }
    
    // move to the log out activity
    private void navigateToLogOut() {
    	Log.d(TAG, "navigateToLogOut");
    	int ii = 1;
    	ii = ii + 1;
//        // Show logout page
//        // Go to logout page
//        Intent intent = new Intent(MainActivity.this, LogOutActivity.class);
//        startActivity(intent);
//
//        // Close this activity
//        finish();
    }
    
    
    


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
    
    
    
}
