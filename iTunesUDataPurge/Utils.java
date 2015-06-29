import java.util.Hashtable;
import java.util.Date;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.*;
import org.apache.http.params.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

class Utils
{
	// the security file stores all connection credentials
	static String CONFIG_FILE_NAME = "config.properties";
	
	// the connection timeout limit - defaults to one minute
	static private int connectionTimeoutMilliseconds = 60000;
	
	// the socket timeout limit - defaults to one minute
	static private int socketTimeoutMilliseconds = 60000;

    /**
     * Caculate ITunesU credentials
     */
    static public Hashtable<String, String> getITunesUCreds(boolean overrideWithAdmin, boolean allSites, String displayName, String emailAddress, String username, String userIdentifier)
    {
    	Hashtable<String, String> configs = getConfigs();
    	if (configs == null)
    		return null;
    	
        String iTunesUSiteId = configs.get("iTunesU_site_id");
    	String siteURL = configs.get("site_URL");
    	String sharedSecret = configs.get("iTunesU_site_sharedSecret");
    	String sectionHandle = configs.get("section_handle");
    	
        String[] credentialsArray = new String[1];

        credentialsArray[0] = configs.get("admin_credential");

        // Define the iTunes U page to browse. Use the domain name that
        // uniquely identifies your site in iTunes U to browse to that site's
        // root page; use a destination string extracted from an iTunes U URL
        // to browse to another iTunes U page; or use a destination string
        // supplied as the "destination" parameter if this program is being
        // invoked as a part of the login web service for your iTunes U site.
        String siteDomain = siteURL.substring(siteURL.lastIndexOf('/') + 1);
        String destination = siteDomain;

        // Append your site's debug suffix to the destination if you
        // want to receive an HTML page providing information about
        // the transmission of identity and credentials between this
        // program and iTunes U. Remove this code after initial
        // testing to instead receive the destination page requested.
        // destination = destination + "/oqr456";

        // Use an ITunesU instance to format the identity and credentials
        // strings and to generate an authorization token for them.
        String identity = getIdentityString(displayName, emailAddress,username, userIdentifier);
        String credentials = getCredentialsString(credentialsArray);
        Date now = new Date();
        byte[] key = getBytes(sharedSecret, "US-ASCII");
        String token = getAuthorizationToken(credentials, identity, now, key);

        // Use the authorization token to connect to iTunes U and obtain
        // from it the HTML that needs to be returned to a user's web
        // browser to have a particular page or item in your iTunes U
        // site displayed to that user in iTunes. Replace "/Browse/" in
        // the code below with "/API/GetBrowseURL/" if you instead want
        // to return the URL that would need to be opened to have that
        // page or item displayed in iTunes.
        String prefix = siteURL.substring(0, siteURL.indexOf(".woa/") + 4);

        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put("prefix", prefix);
        table.put("destination", destination);
        table.put("token", token);

        return table;
    }
    
    /**
     * read configuration variables
     */
    static protected Hashtable<String, String> getConfigs()
    {
    	// Define your site's information.
    	Hashtable<String, String> rv = new Hashtable<String, String>();
    	
    	Properties prop = new Properties();
    	InputStream input = null;
 
    	try {
    		input = new FileInputStream(CONFIG_FILE_NAME);
 
    		// load a properties file
    		prop.load(input);
 
    		// get the property value and print it out
        	rv.put("iTunesU_site_id", prop.getProperty("iTunesU_site_id"));
        	rv.put("site_URL", "https://deimos.apple.com/WebObjects/Core.woa/Browse/" + prop.getProperty("iTunesU_site_id"));
        	rv.put("iTunesU_site_sharedSecret", prop.getProperty("iTunesU_site_sharedSecret"));
        	rv.put("admin_credential", prop.getProperty("admin_credential"));
        	rv.put("section_handle", prop.getProperty("section_handle"));
        	rv.put("template_handle", prop.getProperty("template_handle"));
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	} finally {
    		if (input != null) {
    			try {
    				input.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
    	}
 
    	return rv;
	}

    /**
     * read configuration variables
     */
    static protected Hashtable<String, String> getCtoolsConfigs()
    {
        // Define your site's information.
        Hashtable<String, String> rv = new Hashtable<String, String>();
        
        Properties prop = new Properties();
        InputStream input = null;
 
        try {
            input = new FileInputStream(CONFIG_FILE_NAME);
 
            // load a properties file
            prop.load(input);
 
            // get the property value and print it out
            rv.put("ctools_search_server", "http://" + prop.getProperty("ctools_search_server") + "/");
            rv.put("ctools_admin_username", prop.getProperty("ctools_admin_username"));
            rv.put("ctools_admin_password", prop.getProperty("ctools_admin_password"));

 
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
 
        return rv;
    }


    /**
     * Combine user identity information into an appropriately formatted string.
     *
     * @param displayName The user's name (optional).
     * @param emailAddress The user's email address (optional).
     * @param username The user's username (optional).
     * @param userIdentifier A unique identifier for the user (optional).
     *
     * @return A non-<CODE>null</CODE> user identity string.
     */
    static public String getIdentityString(String displayName, String emailAddress,
                                    String username, String userIdentifier) {
		
        // Create a buffer with which to generate the identity string.
        StringBuffer buffer = new StringBuffer();
		
        // Define the values and delimiters of each of the string's elements.
        String[] values = { displayName, emailAddress,
			username, userIdentifier };
        char[][] delimiters = { { '"', '"' }, { '<', '>' },
			{ '(', ')' }, { '[', ']' } };
		
        // Add each element to the buffer, escaping
        // and delimiting them appropriately.
        for (int i = 0; i < values.length; ++i) {
            if (values[i] != null) {
                if (buffer.length() > 0) buffer.append(' ');
                buffer.append(delimiters[i][0]);
                for (int j = 0, n = values[i].length(); j < n; ++j) {
                    char c = values[i].charAt(j);
                    if (c == delimiters[i][1] || c == '\\') buffer.append('\\');
                    buffer.append(c);
                }
                buffer.append(delimiters[i][1]);
            }
        }
		
        // Return the generated string.
        return buffer.toString();
		
    }
    
	/**
     * Generate and sign an authorization token that you can use to securely
     * communicate to iTunes U a user's identity and credentials. The token
     * includes all the data you need to communicate to iTunes U as well as
     * a creation time stamp and a digital signature for the data and time.
     *
     * @param identity The user's identity string, as
     *                 obtained from getIdentityString().
     * @param credentials The user's credentials string, as
     *                    obtained from getCredentialsString().
     * @param time Token time stamp. The token will only be valid from
     *             its time stamp time and for a short time thereafter
     *             (usually 90 seconds thereafter, this "transfer
     *             timeout" being configurable in the iTunes U server).
     * @param key The bytes of your institution's iTunes U shared secret key.
     *
     * @return The authorization token. The returned token will
     *         be URL-encoded and can be sent to iTunes U with
     *         a <A HREF="http://www.ietf.org/rfc/rfc1866.txt">form
     *         submission</A>. iTunes U will typically respond with
     *         HTML that should be sent to the user's browser.
     */
    static public String getAuthorizationToken(String credentials, String identity,
											   Date time, byte[] key) {
		
        // Create a buffer with which to generate the authorization token.
        StringBuffer buffer = new StringBuffer();
		
        // Generate the authorization token.
        try {
			
            // Start with the appropriately encoded credentials.
            buffer.append("credentials=");
            buffer.append(URLEncoder.encode(credentials, "UTF-8"));
			
            // Add the appropriately encoded identity information.
            buffer.append("&identity=");
            buffer.append(URLEncoder.encode(identity, "UTF-8"));
			
            // Add the appropriately formatted time stamp. Note that
            // the time stamp is expressed in seconds, not milliseconds.
            buffer.append("&time=");
            buffer.append(time.getTime() / 1000);
			
            // Generate and add the token signature.
            String data = buffer.toString();
            buffer.append("&signature=");
            buffer.append(hmacSHA256(data, key));
			
        } catch (UnsupportedEncodingException e) {
			
            // UTF-8 encoding support is required.
            throw new Error(
							"ITunesU.getAuthorizationToken(): "
							+ "UTF-8 encoding not supported!");
			
        }
		
        // Return the signed authorization token.
        return buffer.toString();
		
    }
    
	/**
     * Combine user credentials into an appropriately formatted string.
     *
     * @param credentials An array of credential strings. Credential
     *                    strings may contain any character but ';'
     *                    (semicolon), '\\' (backslash), and control
     *                    characters (with ASCII codes 0-31 and 127).
     *
     * @return <CODE>null</CODE> if and only if any of the credential strings 
     *         are invalid.
     */
    static public String getCredentialsString(String[] credentials) {
		
        // Create a buffer with which to generate the credentials string.
        StringBuffer buffer = new StringBuffer();
		
        // Verify and add each credential to the buffer.
        if (credentials != null) {
            for (int i = 0; i < credentials.length; ++i) {
                if (i > 0) buffer.append(';');
                for (int j = 0, n = credentials[i].length(); j < n; ++j) {
                    char c = credentials[i].charAt(j);
                    if (c != ';' && c != '\\' && c >= ' ' && c != 127) {
                        buffer.append(c);
                    } else {
                        return null;
                    }
                }
            }
        }
		
        // Return the credentials string.
        return buffer.toString();
		
    }
    
    /**
     * Get the US-ASCII or UTF-8 representation of a string.
     *
     * @param string The string to encode.
     * @param encoding "US-ASCII" or "UTF-8".
     *
     * @return A byte array with the appropriate representation of the
     *         string, or <CODE>null</CODE> if the requested encoding is
     *         not "US-ASCII" or "UTF-8". Instead of raising an exception
     *         if the requested encoding is not supported, as String's
     *         getBytes(String encoding) does, this method raises an error,
     *         which simplifies the code of this class and is appropriate
     *         because this class cannot function without US-ASCII or UTF-8.
     */
    static public byte[] getBytes(String string, String encoding) {
        byte[] bytes = null;
        if ("US-ASCII".equals(encoding) || "UTF-8".equals(encoding)) {
            try {
                bytes = string.getBytes(encoding);
            } catch (UnsupportedEncodingException e) {
                throw new Error(
								"ITunesU.getBytes(): "
								+ encoding
								+ " encoding not supported!");
            }
        }
        return bytes;
    }
    
	/**
     * Generate the HMAC-SHA256 signature of a message string, as defined in
     * <A HREF="http://www.ietf.org/rfc/rfc2104.txt">RFC 2104</A>.
     *
     * @param message The string to sign.
     * @param key The bytes of the key to sign it with.
     *
     * @return A hexadecimal representation of the signature.
     */
    static public String hmacSHA256(String message, byte[] key) {
		
        // Start by getting an object to generate SHA-256 hashes with.
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("hmacSHA256(): SHA-256 algorithm not found!");
        }
		
        // Hash the key if necessary to make it fit in a block (see RFC 2104).
        if (key.length > 64) {
            sha256.update(key);
            key = sha256.digest();
            sha256.reset();
        }
		
        // Pad the key bytes to a block (see RFC 2104).
        byte block[] = new byte[64];
        for (int i = 0; i < key.length; ++i) block[i] = key[i];
        for (int i = key.length; i < block.length; ++i) block[i] = 0;
		
        // Calculate the inner hash, defined in RFC 2104 as
        // SHA-256(KEY ^ IPAD + MESSAGE)), where IPAD is 64 bytes of 0x36.
        for (int i = 0; i < 64; ++i) block[i] ^= 0x36;
        sha256.update(block);
        sha256.update(Utils.getBytes(message, "UTF-8"));
        byte[] hash = sha256.digest();
        sha256.reset();
		
        // Calculate the outer hash, defined in RFC 2104 as
        // SHA-256(KEY ^ OPAD + INNER_HASH), where OPAD is 64 bytes of 0x5c.
        for (int i = 0; i < 64; ++i) block[i] ^= (0x36 ^ 0x5c);
        sha256.update(block);
        sha256.update(hash);
        hash = sha256.digest();
		
        // The outer hash is the message signature...
        // convert its bytes to hexadecimals.
        char[] hexadecimals = new char[hash.length * 2];
        for (int i = 0; i < hash.length; ++i) {
            for (int j = 0; j < 2; ++j) {
                int value = (hash[i] >> (4 - 4 * j)) & 0xf;
                char base = (value < 10) ? ('0') : ('a' - 10);
                hexadecimals[i * 2 + j] = (char)(base + value);
            }
        }
		
        // Return a hexadecimal string representation of the message signature.
        return new String(hexadecimals);
		
    }
    
	static protected String getShowTreeUrl(String prefix, String destination, String keyGroup, String token) {
		// send the http request
		HttpClient httpClient= getHttpClientInstance();
		
		String url = ""; 
		url = prefix + "/API/ShowTree/" + destination;
		
		// add the section handle
		String sectionHandle = null;
		Hashtable<String, String> configs = getConfigs();
    	if (configs != null)
    		sectionHandle = configs.get("section_handle");
		if (sectionHandle != null)
		{
			url = url + "." + sectionHandle;
		}
		
		// if pulling handle based on site id, get 'most' XML file
		// otherwise get minimal
		// NOTE: minimal returns Name, Handle, AggregateSize only
		url = url + "?keyGroup=" + keyGroup + "&" + token;
		return url;
	}
	
	
	/**
	 * Read a DOM Document from xml in a stream.
	 * 
	 * @param in
	 *        The stream containing the XML
	 * @return A new DOM Document with the xml contents.
	 */
	static public Document readDocumentFromStream(InputStream in)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(in);
			Document doc = docBuilder.parse(inputSource);
			return doc;
		}
		catch (Exception any)
		{
			System.out.println("readDocumentFromStream: " + any.toString());
			return null;
		}
	}
	
	static public HttpClient getHttpClientInstance() {
		// use default connection pool setting
		HttpClient httpClient = new DefaultHttpClient();
		HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMilliseconds);
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMilliseconds);
		return httpClient;
	}
	
	static Document getShowtreeDocument(String prefix, String destination, String showtree_degree, String token)
	{
		Document doc = null;
		
		String showtreeUrl = getShowTreeUrl(prefix, destination, showtree_degree, token);
		System.out.println("showtree=" + showtreeUrl);
		
		HttpPost httppost = new HttpPost(showtreeUrl);
		try
		{
			// get HttpClient instance
			HttpClient httpClient = getHttpClientInstance();
			
			// add the course site	
			HttpResponse response = httpClient.execute(httppost);
			// if using Identifier to determine course, call local method
			// otherwise, feed it into the Digester to match on name
			HttpEntity httpEntity = response.getEntity();
    		if (httpEntity != null)
    		{
    			InputStream responseStream = httpEntity.getContent();
    			doc = Utils.readDocumentFromStream(responseStream);
			responseStream.close();
			// When HttpClient instance is no longer needed, 
		        // shut down the connection manager to ensure
		        // immediate deallocation of all system resources
		        httpClient.getConnectionManager().shutdown();
    		}
		}
		catch (IOException e)
		{
			System.out.println("removeCourses IOException " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("removeCourses Exception " + e.getMessage());
		}
		
		return doc;
	}
}
