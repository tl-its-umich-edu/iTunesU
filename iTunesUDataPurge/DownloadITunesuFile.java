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

import org.apache.http.client.*;
import org.apache.http.params.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

import org.apache.commons.lang.*;

import java.io.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


// using SAX
public class DownloadITunesuFile {
	
	// contact info
	static HashMap<String, String> siteDownloadUrl = new HashMap<String, String>();
	
	final static int size=1024;
	
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
			Hashtable<String, String> t = Utils.getITunesUCreds(true,false, displayName, emailAddress, username, userIdentifier);
    		String prefix = t.get("prefix")!=null?(String) t.get("prefix"):"";
    		String destination= t.get("destination")!=null?(String) t.get("destination"):"";
    		String token = t.get("token")!=null?(String) t.get("token"):"";
    		
    		getSiteDownloadUrls(rootDir, downloadSiteIds, prefix, destination, token, displayName, emailAddress, username, userIdentifier);
		}
    	
	}
	
	static public void getSiteDownloadUrls(String rootDir, HashMap<String, String> siteIds, String prefix, String destination, String token, String displayName, String emailAddress, String username, String userIdentifier)
    {  
    	HashMap<String, String> rv = new HashMap<String, String>();
		// the downloadUrl attribute will be availabel on the "most" level
		Document doc = Utils.getShowtreeDocument(prefix, destination, "most", token);
		if (doc != null)
		{
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
		}
    }
	
	static public void parseShowTreeXMLDoc(Document doc, String siteDirectoryName, String siteId, String siteTitle, String lookFor, String displayName, String emailAddress, String username, String userIdentifier)
    {
		String tmpHandle = null;
		boolean idFound = false;
		
		String [] credentialsArray = new String [1];
		Hashtable<String, String> configs = Utils.getConfigs();
		if (configs == null)
			return;
		credentialsArray[0] = configs.get("admin_credential");
		
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
								String identity = Utils.getIdentityString(displayName, emailAddress,
																	username, userIdentifier);
								String credentials = Utils.getCredentialsString(credentialsArray);
								Date now = new Date();
								byte[] key = Utils.getBytes(configs.get("iTunesU_site_sharedSecret"), "US-ASCII");
								String token = Utils.getAuthorizationToken(credentials, identity,
																	 now, key);
								trackDownloadUrl+="?" + token;
								
								// now we can download
								HttpPost httppost = new HttpPost(trackDownloadUrl);
								try
								{
									// get HttpClient instance
									HttpClient httpClient = Utils.getHttpClientInstance();
									
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
										Document trackDoc = Utils.readDocumentFromStream(responseStream);
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
													System.out.println("Group Directory Name= " + groupDirectoryName);

													//Check to see if file to be downloaded is already in local directory
													if(isFilePresent(groupDirectoryName, itemName+"."+itemSuffix)){
														DateFormat df = new SimpleDateFormat("_yyyyMMddHHmmssSSS");
														String dateText = df.format(Calendar.getInstance().getTime());
														// if file is present, modify itemName to be downloaded to include current timestamp (concatenate suffix here)
														itemName = itemName+dateText+"."+itemSuffix;
													}
													else{
														// otherwise keep itemName to be downloaded as is from iTunes U (concatonate suffix here)
														itemName = itemName+"."+itemSuffix;
													}
													// download the item
													fileUrl(itemUrl, itemName, groupDirectoryName);
													
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

	public static boolean isFilePresent(String directory, String fileName){

		boolean result = false;

		String completeFileName = directory + "/" + fileName;
		
		File file = new File(completeFileName);

		if(file.exists() && !file.isDirectory()){
			System.out.println("File Exists!");
			result = true;
		}
		else{
			result = false;
		}
		return result;
	}
	
}