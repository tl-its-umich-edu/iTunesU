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

import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

import org.apache.commons.lang.*;

import java.io.*;;


// using SAX
public class DownloadITunesuFile {
	
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
	
    public static void main(String[] args) throws Exception {
		
		
		// site createdOn file
		HashMap<String, String> downloadSiteIds = new HashMap<String, String>();
		String createdFileName = args[0];
		
		String displayName = "Admin DisplayName";//args[1];
		String emailAddress = "Admin Email Address";//args[2];
		String username = "Admin Username";//args[3];
		String userIdentifier = "Admin ID";//args[4];
		
		
		String rootDir = args[1];
		
		File createdFile = new File(createdFileName);
		int row = 0;
		try
		{
			BufferedReader bufRdr  = new BufferedReader(new FileReader(createdFile));
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
				downloadSiteIds.put(siteId, siteTitle);
				
				row++;
			}
			
			// how many site ids 
			System.out.println("The total size of download site id list = " + downloadSiteIds.size());
			
		}
		catch (FileNotFoundException e)
		{
			System.out.println("FileNotFoundException " + createdFileName);
		}
		catch (IOException e)
		{
			System.out.println("IOException " + createdFileName);
		}
		
		if (!downloadSiteIds.isEmpty())
		{
			// get itunesu credential
			Hashtable<String, String> t = getITunesUCreds(true,false, displayName, emailAddress, username, userIdentifier);
    		String prefix = t.get("prefix")!=null?(String) t.get("prefix"):"";
    		String destination= t.get("destination")!=null?(String) t.get("destination"):"";
    		String token = t.get("token")!=null?(String) t.get("token"):"";
    		
    		getSiteDownloadUrls(rootDir, downloadSiteIds, prefix, destination, token, displayName, emailAddress, username, userIdentifier);
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
	
	static public void getSiteDownloadUrls(String rootDir, HashMap<String, String> siteIds, String prefix, String destination, String token, String displayName, String emailAddress, String username, String userIdentifier)
    {  
    	HashMap<String, String> rv = new HashMap<String, String>();
		// the downloadUrl attribute will be availabel on the "most" level
		String showtreeUrl = getShowTreeUrl(prefix, destination, "most", token);
		
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
				int totalSites = siteIds.size();
				int count = 1;
    			for (Map.Entry<String, String> entry : siteIds.entrySet()) {
					String siteId = entry.getKey();
					String siteTitle = entry.getValue();
    				siteId = StringUtils.trimToNull(siteId.replaceAll("[\t\r\n]", ""));
					
					// Create a directory; all ancestor directories must exist
					String siteDirectoryName = rootDir + "/" + siteTitle + "(" + siteId + ")";
					boolean success = false;
					if (!(new File(siteDirectoryName)).exists())
					{
						success = (new File(siteDirectoryName)).mkdirs();
					}
					else
					{
						success = true;
					}
					
					System.out.println("Percent " + count++*100.0/totalSites + ": Downloading materials for site " + siteTitle + " site id=" + siteId);
					
					if (!success) {
						// Directory creation failed
						System.out.println("failed create directory for site " + siteTitle + " id=" + siteId);
					}
					else
					{
						parseShowTreeXMLDoc(doc, siteDirectoryName, siteId, siteTitle, "DownloadUrl", displayName, emailAddress, username, userIdentifier);
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
			System.out.println(e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
    }
	
	static protected String getShowTreeUrl(String prefix, String destination, String keyGroup, String token) {
		// send the http request
		HttpClient httpClient= getHttpClientInstance();
		
		String url = ""; 
		url = prefix + "/API/ShowTree/" + destination;
		
		// add the section handle
		String sectionHandle = getITunesParentHandle();
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
	
	static protected String getITunesParentHandle()
	{
		return "1136688961";
	}
	
	
	static public void parseShowTreeXMLDoc(Document doc, String siteDirectoryName, String siteId, String siteTitle, String lookFor, String displayName, String emailAddress, String username, String userIdentifier)
    {
		String tmpHandle = null;
		boolean idFound = false;
		
		String [] credentialsArray = new String [1];
		credentialsArray[0] = admin_credential;
		
		NodeList nodes = doc.getElementsByTagName("Course");
		
		//TODO: create course folder with course name, id
		
		// Search all course sites
		for (int i = 0; i < nodes.getLength(); i++)
		{
			// reset
			idFound = false;
			
			Node courseNode = nodes.item(i);
			NodeList children = courseNode.getChildNodes();
			
			String courseHandle = "";
			
			// search for identifier to see if this is course
			// wanted and its associated identifier
			for (int j = 0; j < children.getLength(); j++)
			{
				
				Node node = children.item(j);
				String nodeName = node.getNodeName();
				
				//System.out.println("nodeName");
				
				if ("Handle".equals(lookFor) && nodeName.equalsIgnoreCase(lookFor)) 
				{
					tmpHandle = node.getTextContent();
				}
				else if (nodeName.equalsIgnoreCase("Identifier")) 
				{
					String identifier = node.getTextContent(); 
					if (identifier != null && identifier.equalsIgnoreCase(siteId)) 
					{
						idFound = true;
						
						//System.out.println("course identifier=" + identifier + " siteid=" + siteId + " found=" + idFound);
					}
					else
					{
						//System.out.println("break");
						break;
					}
				}
				else if ("DownloadUrl".equals(lookFor) && nodeName.equals("Group") && idFound)
				{
					//System.out.println("in group level");
					String groupHandle = "";
					
					// course-> GROUP-> track
					Node courseGroupNode = node;
					NodeList groupChildNodes =  courseGroupNode.getChildNodes();
					
					String groupName = "";
					String groupDirectoryName = "";
					boolean groupDirectorySuccess = false;
					
					for (int jj = 0; jj < groupChildNodes.getLength(); jj++)
					{
						Node groupChildNode = groupChildNodes.item(jj);
						String groupNodeName = groupChildNode.getNodeName();
						//System.out.println("in group level name=" + groupNodeName);
						if ("Name".equals(groupNodeName))
						{
							// got group handle
							groupName= groupChildNode.getTextContent();
							System.out.println("groupName=" + groupName);
							// create track folder
							groupDirectoryName = siteDirectoryName + "/" + groupName;
							if (!(new File(groupDirectoryName)).exists())
							{
								groupDirectorySuccess = (new File(groupDirectoryName)).mkdirs();
							}
							else {
								groupDirectorySuccess = true;
							}
							
							if (!groupDirectorySuccess) {
								// Directory creation failed
								System.out.println("failed create directory for site " + siteTitle + " id=" + siteId);
							}
							
						}
						else if ("Handle".equals(groupNodeName))
						{
							// got group handle
							groupHandle = groupChildNode.getTextContent();
						}
						else if (groupNodeName.equals("Track")) 
						{
							//System.out.println("in track level");
							
							// now it is time for Track element
							String trackDownloadUrl = "";
							
							// TODO: create Track folder with track name and id
							
							// course->group->TRACK
							Node trackNode = groupChildNode;
							NodeList trackChildNodes =  trackNode.getChildNodes();
							
							for (int jjj = 0; jjj < trackChildNodes.getLength(); jjj++)
							{
								Node trackChildNode = trackChildNodes.item(jjj);
								String trackChildNodeName = trackChildNode.getNodeName();
								
								if ("DownloadURL".equals(trackChildNodeName))
								{
									// got group handle
									trackDownloadUrl = trackChildNode.getTextContent();
								}
							}
							
							if (trackDownloadUrl.length() > 0 && groupDirectorySuccess)
							{
								// current user
								String identity = getIdentityString(displayName, emailAddress,
																	username, userIdentifier);
								String credentials = getCredentialsString(credentialsArray);
								Date now = new Date();
								byte[] key = getBytes(sharedSecret, "US-ASCII");
								String token = getAuthorizationToken(credentials, identity,
																	 now, key);
								trackDownloadUrl+="?" + token;
								
								// now we can download
								HttpPost httppost = new HttpPost(trackDownloadUrl);
								try
								{
									// get HttpClient instance
									HttpClient httpClient = getHttpClientInstance();
									
									HttpProtocolParams.setUserAgent(httpClient.getParams(), "iTunes/10");
									
									//System.out.println(" made download track call");
									// add the course site	
									HttpResponse response = httpClient.execute(httppost);
									// if using Identifier to determine course, call local method
									// otherwise, feed it into the Digester to match on name
									HttpEntity httpEntity = response.getEntity();
						    		if (httpEntity != null)
						    		{
						    			InputStream responseStream = httpEntity.getContent();
										Document trackDoc = readDocumentFromStream(responseStream);
										//System.out.println(trackDoc.toString());
										
										NodeList playArray = trackDoc.getElementsByTagName("array");
										for (int k = 0; k < playArray.getLength(); k++)
										{
											Node arrayNode = playArray.item(k);
											NodeList arrayNodeChildList =  arrayNode.getChildNodes();
											for (int kk = 0; kk < arrayNodeChildList.getLength(); kk++)
											{
												Node arrayNodeChild = arrayNodeChildList.item(kk);
												String arrayNodeChildName = arrayNodeChild.getNodeName();
												
												//System.out.println("hhh" + arrayNodeChildName);
												
												if ("dict".equals(arrayNodeChildName))
												{
													//System.out.println("in dict");
													// go one step further
													NodeList pDictItems =  arrayNodeChild.getChildNodes();
													// all about the item to be downloaded: url, name, suffix
													String itemUrl = "";
													String itemName = "";
													String itemSuffix = "";
													
													for (int kkk= 0; kkk < pDictItems.getLength(); kkk++)
													{
														Node pDictItem = pDictItems.item(kkk);
														String pDicItemName = pDictItem.getNodeName();
														String pDicItemValue = pDictItem.getTextContent();
														
														//System.out.println(" dict names " + pDicItemName + " value=" + pDictItem.getTextContent());
														if ("string".equals(pDicItemName) && pDicItemValue.contains("DownloadTrackFile"))
														{
															itemUrl = pDicItemValue;
														}
														else if ("dict".equals(pDicItemName) && itemUrl.length() > 0)
														{
															NodeList attributeItems =  pDictItem.getChildNodes();
															int itemNameAttributeIndex = 0;
															int itemSuffixAttributeIndex = 0;
															for (int kkkk= 0; kkkk < attributeItems.getLength(); kkkk++)
															{
																Node attributeItem = attributeItems.item(kkkk);
																String attributeName = attributeItem.getNodeName();
																String attributeValue = attributeItem.getTextContent();
																//System.out.println(attributeName + " = " + attributeValue);
																if ("key".equals(attributeName) && "songName".equals(attributeValue))
																{
																	itemNameAttributeIndex = kkkk+1;
																}
																else if ("key".equals(attributeName) && "fileExtension".equals(attributeValue))
																{
																	itemSuffixAttributeIndex = kkkk+1;
																}
																else if ("string".equals(attributeName) && itemNameAttributeIndex > 0 && kkkk == itemNameAttributeIndex)
																{
																	itemName = attributeValue;
																	// in case there is a "/" inside item name, replace it with "_"
																	itemName = itemName.replaceAll("/", "_");
																}
																else if ("string".equals(attributeName) && itemSuffixAttributeIndex > 0 && kkkk == itemSuffixAttributeIndex)
																{
																	itemSuffix = attributeValue;
																	break;
																}
															}
															//TODO: download the actual file with original file name and extension.
														}
													}
													
													System.out.println("item download url= " + itemUrl + " name=" + itemName + " item suffix=" + itemSuffix);
													
													// download the item
													fileUrl(itemUrl, itemName+"." + itemSuffix, groupDirectoryName);
													
												}
											}
										}
										// now that we get the playlist xml, like this one:
										responseStream.close();
										// When HttpClient instance is no longer needed, 
								        // shut down the connection manager to ensure
								        // immediate deallocation of all system resources
								        httpClient.getConnectionManager().shutdown();
						    		}
								}
								catch (FileNotFoundException e)
								{
									System.out.println(e.getMessage());
								}
								catch (IOException e)
								{
									System.out.println(e.getMessage());
								}
								catch (Exception e)
								{
									System.out.println(e.getMessage());
								}
							}
						}
					}
				}	// if-else
			}
			
			// have i found the correct course and handle for it
			// if so, we are done so break out of outer loop
			if (idFound && tmpHandle != null) 
			{
				break;
			}
		} 
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