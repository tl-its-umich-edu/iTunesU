import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Date;
import java.util.Map;


import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.*;
import org.apache.http.params.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;

import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

import org.apache.commons.lang.*;

import java.io.*;;


import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

// using SAX
public class RemoveITunesUCourses {
	
	// contact info
	static HashMap<String, String> siteDownloadUrl = new HashMap<String, String>();
	
	// the connection timeout limit - defaults to one minute
	static private int connectionTimeoutMilliseconds = 60000;
	
	// the socket timeout limit - defaults to one minute
	static private int socketTimeoutMilliseconds = 60000;
	
	final static int size=1024;
		
	// Define your site's information. 
	static String iTunesUSiteId = "iTunesU_site_id";
	static String siteURL = "https://deimos.apple.com/WebObjects/Core.woa/Browse/" + iTunesUSiteId;
	static String sharedSecret = "iTunesU_site_sharedSecret";
	static String admin_credential="admin_credential";
	static String sectionHandle = "";
	
	static final String WS_DELETE_COURSE = "DeleteCourse";
	
    public static void main(String[] args) throws Exception {
		
		
		// site createdOn file
		HashMap<String, String> removeCoursesIds = new HashMap<String, String>();
		String removeCoursesFileName = args[0];
		
		String displayName = "Admin DisplayName";//args[1];
		String emailAddress = "Admin Email Address";//args[2];
		String username = "Admin Username";//args[3];
		String userIdentifier = "Admin ID";//args[4];
		
		File removeCoursesFile = new File(removeCoursesFileName);
		int row = 0;
		try
		{
			BufferedReader bufRdr  = new BufferedReader(new FileReader(removeCoursesFile));
			String line = null;
			//read each line of text file
			int count = 0;
			while((line = bufRdr.readLine()) != null)
			{	
				StringTokenizer st = new StringTokenizer(line,"\t");
				String siteId = "";
				String siteTitle = "";
				// reset count
				count = 0;
				while (st.hasMoreTokens())
				{
					String tokenValue = st.nextToken().trim();
					if (count == 0)
					{
						// this is the site id
						siteTitle = tokenValue;
					}
					else if (count == 1)
					{
						// this is the site id
						siteId = tokenValue;
					}
					count++;
				}
				removeCoursesIds.put(siteId, siteTitle);
				
				row++;
			}
			
			// how many site ids 
			System.out.println("The total size of download site id list = " + removeCoursesIds.size());
			
		}
		catch (FileNotFoundException e)
		{
			System.out.println("FileNotFoundException " + removeCoursesFileName);
		}
		catch (IOException e)
		{
			System.out.println("IOException " + removeCoursesFileName);
		}
		
		if (!removeCoursesIds.isEmpty())
		{
			Hashtable<String, String> t = getITunesUCreds(true,false, displayName, emailAddress, username, userIdentifier);
			String prefix = t.get("prefix")!=null?(String) t.get("prefix"):"";
			String destination= t.get("destination")!=null?(String) t.get("destination"):"";
    		removeCourses(removeCoursesIds, prefix, destination, displayName, emailAddress, username, userIdentifier);
		}
    	
	}
	
	static public String getCredentialToken(String displayName, String emailAddress, String username, String userIdentifier)
	{
		// get itunesu credential
		Hashtable<String, String> t = getITunesUCreds(true,false, displayName, emailAddress, username, userIdentifier);
		return t.get("token")!=null?(String) t.get("token"):"";
	}
	
	static public void removeCourses(HashMap<String, String> siteIds, String prefix, String destination, String displayName, String emailAddress, String username, String userIdentifier)
    {  
    	HashMap<String, String> rv = new HashMap<String, String>();
		// the downloadUrl attribute will be availabel on the "most" level
		String token = getCredentialToken(displayName, emailAddress, username, userIdentifier);
		String showtreeUrl = getShowTreeUrl(prefix, destination, "minimal", token);
		System.out.println("showtree=" + showtreeUrl);
		
		HttpPost httppost = new HttpPost(showtreeUrl);
		try
		{
			// get HttpClient instance
			HttpClient httpClient = getHttpClientInstance();
			
			// add the course site	
			HttpResponse response = httpClient.execute(httppost);
			System.out.println("getITunesUCourseHandle get ShowTree response");
			// if using Identifier to determine course, call local method
			// otherwise, feed it into the Digester to match on name
			HttpEntity httpEntity = response.getEntity();
    		if (httpEntity != null)
    		{
    			InputStream responseStream = httpEntity.getContent();
    			Document doc = readDocumentFromStream(responseStream);
				// get the mapping of site id and site handler
				HashMap<String, String> map = parseShowTreeXMLDoc(doc);
				
				int totalSites = siteIds.size();
				int count = 0;
    			for (Map.Entry<String, String> entry : siteIds.entrySet()) {
					String siteId = entry.getKey();
					String siteTitle = entry.getValue();
    				siteId = StringUtils.trimToNull(siteId.replaceAll("[\t\r\n]", ""));
					String siteHandle = map.containsKey(siteId)? map.get(siteId):"";
					if (!siteHandle.isEmpty()) 
					{
						count++;
						System.out.println("found " + count + " title=" + siteTitle + " id=" + siteId + "\tsite handle=" + siteHandle);
						String xmlDocument = getDeleteCourseXml(siteHandle);
						
						token = getCredentialToken(displayName, emailAddress, username, userIdentifier);
						
						// upload url
						String uploadURL = getUploadURL(siteHandle, prefix, destination, token);
						
						// delete course xml
						//System.out.println(xmlDocument);
						//wsCall(WS_DELETE_COURSE, uploadURL, xmlDocument, prefix, destination, token);
					}
    			}
				responseStream.close();
				// When HttpClient instance is no longer needed, 
		        // shut down the connection manager to ensure
		        // immediate deallocation of all system resources
		        httpClient.getConnectionManager().shutdown();
    		}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("removeCourses FileNotFoundException " + e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println("removeCourses IOException " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("removeCourses Exception " + e.getMessage());
		}
    }
	
	/**
	 * read the HttpEntity content stream into String object
	 * @param entity
	 * @return
	 * @throws IOException
	 */
	private static String getHttpEntityString(HttpEntity entity) throws IOException {
		String rv;
		StringBuffer buffer = new StringBuffer();
		InputStream instream = entity.getContent(); 
		int size = 2048;
		byte[] data = new byte[size];
		while ((size = instream.read(data, 0, data.length)) > 0)
			buffer.append(new String(data, 0, size));
		instream.close();
		rv = buffer.toString();
		return rv;
	}
	
	/**
     * @{inherit}
     */
	public static String getUploadURL(String handle, String prefix, String destination, String token) 
	{
		String rv = null;
		
		String url = prefix + "/API/getUploadURL/" + destination;
		if (handle != null)
		{
			url = url + "." + handle;
		}
		url = url + "?type=XMLControlFile&" + token;
		try
		{
			// get HttpClient instance
			HttpClient httpClient = getHttpClientInstance();
			
			// add the course site
			HttpPost httppost = new HttpPost(url);
			HttpResponse response = httpClient.execute(httppost);
			//System.out.println("get uploadurl " + response.getStatusLine());
			HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				rv = getHttpEntityString(entity);
			    // When HttpClient instance is no longer needed, 
		        // shut down the connection manager to ensure
		        // immediate deallocation of all system resources
		        httpClient.getConnectionManager().shutdown();
			}
		}
		catch (IOException e)
		{
			System.out.println("getUploadURL IOException " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("getUploadURL Exception " + e.getMessage());
		}
		
		return rv;
	}
	
	/**
     *{@inherit}
     */
	public static String wsCall(String operation, String uploadURL, String xmlDocument, String prefix, String destination, String token) 
	{
		String rv = null;
		System.out.println("in wsCall");
		
		System.out.println("sendUploadRequest(String " + operation + ", String "
					  + uploadURL + ", String " + xmlDocument + ", String "
					  + prefix + ", String " + destination + ", String " + token
					  + ")");
        
		try
		{
			if (xmlDocument != null)
			{
				File tempFile = File.createTempFile("wsTmp", ".xml");
				tempFile.deleteOnExit();
				FileOutputStream fout = new FileOutputStream(tempFile);
				try {
	    			fout.write(xmlDocument.getBytes());
				}
				catch (Exception fileException)
				{
					System.out.println(" wscall: problem with writing FileOutputStream " + fileException.getMessage());
				}	
				finally {
					try
					{
		    			fout.flush();
						fout.close(); // The file channel needs to be closed before the deletion.
					}
					catch (IOException ioException)
					{
						System.out.println(" wscall: problem closing FileOutputStream " + ioException.getMessage());
					}
				}
				
				// get HttpClient instance
				HttpClient httpClient = getHttpClientInstance();
				MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
				FileBody bin = new FileBody(tempFile);  
				reqEntity.addPart("file", bin );
		        HttpPost httppost = new HttpPost(uploadURL);	
				httppost.setEntity(reqEntity);
			    HttpResponse response = httpClient.execute(httppost);
			    if (response.getStatusLine() != null)
			    {
				    int status =response.getStatusLine().getStatusCode();
				    if (status == 200)
				    {
						HttpEntity httpEntity = response.getEntity();
						if (httpEntity != null)
						{
							rv = getHttpEntityString(httpEntity);
							
							// When HttpClient instance is no longer needed, 
							// shut down the connection manager to ensure
							// immediate deallocation of all system resources
							httpClient.getConnectionManager().shutdown();
						}
						System.out.println("Upload success ");
				    }
				    else
				    {
						System.out.println("Upload failed response = " + response.getStatusLine().toString());
				    }
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("wsCall Exception"  + e.getMessage());
		}
		
		return rv;
	}
	
	/**
	 * Write a DOM Document to an output stream.
	 * 
	 * @param doc
	 *        The DOM Document to write.
	 * @param out
	 *        The output stream.
	 */
	public static String writeDocumentToString(Document doc)
	{
		try
		{
			
			StringWriter sw = new StringWriter();
			
			DocumentBuilderFactory factory 
			= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			
			
			DOMImplementationLS feature = (DOMImplementationLS) impl.getFeature("LS",
																				"3.0");
			LSSerializer serializer = feature.createLSSerializer();
			LSOutput output = feature.createLSOutput();
			output.setCharacterStream(sw);
			output.setEncoding("UTF-8");
			serializer.write(doc, output);
			
			sw.flush();
			return sw.toString();
		}
		catch (Exception any)
		{
			System.out.println("writeDocumentToString: " + any.toString());
			return null;
		}
	}
	
	/**
	 * Utility routine to write a string node to the DOM.
	 */
	protected static void writeStringNodeToDom(Document doc, Element parent, String nodeName, String nodeValue)
	{
		if (nodeValue != null && nodeValue.length() != 0)
		{
			Element name = doc.createElement(nodeName);
			Text t = doc.createTextNode(nodeValue);
			name.appendChild(t);
			parent.appendChild(name);
		}
		
		return;
	}
	
    /**
	 * {@inheritDoc}
	 * 
	 */
	public static String getDeleteCourseXml(String courseHandle)
	{
		// create xml doc for requry the course information
		
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("ITunesUDocument");
			doc.appendChild(root);
			
			// AddPermission node one: for the maintianer's permission
			Element deleteCourseNode = doc.createElement("DeleteCourse");
			writeStringNodeToDom(doc, deleteCourseNode, "CourseHandle", courseHandle);
			writeStringNodeToDom(doc, deleteCourseNode, "CoursePath", "");
			root.appendChild(deleteCourseNode);
			
			return writeDocumentToString(doc);
		}
		catch (Exception any)
		{
			System.out.println("createDocument: " + any.toString());
			return null;
		}
		
	}
	
	/**
     * {@inheritDoc}}
     */
	static public Hashtable<String, String> getITunesUCreds(boolean overrideWithAdmin, boolean allSites, String displayName, String emailAddress, String username, String userIdentifier)
    {
        String [] credentialsArray = new String [1];
		
		credentialsArray[0] = admin_credential;
        
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
	
	
	static protected String getShowTreeUrl(String prefix, String destination, String keyGroup, String token) {
		// send the http request
		HttpClient httpClient= getHttpClientInstance();
		
		String url = ""; 
		url = prefix + "/API/ShowTree/" + destination;
		
		// add the section handle
		if (sectionHandle != null)
		{
			url = url + "." + sectionHandle;
		}
		
		// if pulling handle based on site id, get 'most' XML file
		// otherwise get minimal
		// NOTE: minimal returns Name, Handle, AggregateSize only
		url = url + "?keyGroup=" + keyGroup + "&" + token;
		
		//System.out.println("getITunesUCourseHandle: url=" + url);
		return url;
	}
	
	static public HashMap<String, String> parseShowTreeXMLDoc(Document doc)
    {
		// to return a map of site id and handler
		HashMap<String, String> rv = new HashMap<String, String>();
		
		String [] credentialsArray = new String [1];
		credentialsArray[0] = admin_credential;
		
		NodeList nodes = doc.getElementsByTagName("Course");
		
		//TODO: create course folder with course name, id
		
		// Search all course sites
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node courseNode = nodes.item(i);
			NodeList children = courseNode.getChildNodes();
			
			String courseHandle = "";
			String courseId = "";
			
			// search for identifier to see if this is course
			// wanted and its associated identifier
			for (int j = 0; j < children.getLength(); j++)
			{
				
				Node node = children.item(j);
				String nodeName = node.getNodeName();
				
				//System.out.println("nodeName");
				
				if ("Handle".equals(nodeName)) 
				{
					courseHandle = node.getTextContent();
				}
				else if (nodeName.equalsIgnoreCase("Identifier")) 
				{
					courseId = node.getTextContent();
				}
			}
			
			rv.put(courseId, courseHandle);
			//System.out.println(courseId + "\t" + courseHandle);
		} 
		return rv;
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
	
	
	static public HttpClient getHttpClientInstance() {
		// use default connection pool setting
		HttpClient httpClient = new DefaultHttpClient();
		HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMilliseconds);
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMilliseconds);
		return httpClient;
	}
	
	/**
	 * Read a DOM Document from xml in a stream.
	 * 
	 * @param in
	 *        The stream containing the XML
	 * @return A new DOM Document with the xml contents.
	 */
	public static Document readDocumentFromStream(InputStream in)
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
        sha256.update(getBytes(message, "UTF-8"));
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
	
	public static void fileUrl(String fAddress, String localFileName, String destinationDir) {
		OutputStream outStream = null;
		URLConnection  uCon = null;
		
		InputStream is = null;
		try {
			URL Url;
			byte[] buf;
			int ByteRead,ByteWritten=0;
			Url= new URL(fAddress);
			outStream = new BufferedOutputStream(new FileOutputStream(destinationDir+"/"+localFileName));
			
			uCon = Url.openConnection();
			is = uCon.getInputStream();
			buf = new byte[size];
			while ((ByteRead = is.read(buf)) != -1) {
				outStream.write(buf, 0, ByteRead);
				ByteWritten += ByteRead;
			}
			//System.out.println("Downloaded Successfully.");
			System.out.println
			("File name:\""+localFileName+ " No ofbytes :" + ByteWritten);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				is.close();
				outStream.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}