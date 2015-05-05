import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.HashSet;


// using SAX
public class ReadITunesU {
	
	static HashSet<String> siteSet = new HashSet<String>();
	
	static double gigabyte=1073741824;
	
	class HowToHandler extends DefaultHandler {
		
		int totalITunesUSiteCount=0;
		boolean course = false;
		
		// course name
		int nameIndex = 0;
		boolean name = false;
		String nameValue = "";
		
		// course handle
		int handleIndex = 0;
		boolean handle = false;
		String handleValue = "";
		
		// course identifier
		int identifierIndex = 0;
		boolean identifier   = false;
		String identifierValue = "";
		
		// course instructor
		int instructorIndex = 0;
		boolean instructor   = false;
		String instructorValue = "";
		
		// file size
		double courseTotalFileSize = 0;
		boolean fileSize = false;
		
		public void startElement(String nsURI, String strippedName,
								 String tagName, Attributes attributes)
		throws SAXException {
			
			if (tagName.equalsIgnoreCase("Course"))
			{
				course = true;
				totalITunesUSiteCount++;
				
				//reset
				nameIndex = 0;
				handleIndex = 0;
				identifierIndex = 0;
				instructorIndex = 0;
				courseTotalFileSize=0;
				nameValue="";
				handleValue = "";
				identifierValue="";
				instructorValue="";
			}
			
			
			if (tagName.equalsIgnoreCase("Name"))
			{
				name=true;
				nameIndex++;
				if (nameIndex == 1) {
					// course name
					//System.out.println(" start Name: ");
				}
			}
			
			if (tagName.equalsIgnoreCase("Handle"))
			{
				handle=true;
				handleIndex++;
				if (handleIndex == 1) {
					// course handle
					//System.out.println(" start Handler: ");
				}
			}
			
			if (tagName.equalsIgnoreCase("Identifier"))
			{
				identifier=true;
				identifierIndex++;
				if (identifierIndex == 1) {
					// course identifier
					//System.out.println("start Identifier");
				}
			}
			
			if (tagName.equalsIgnoreCase("Instructor"))
			{
				instructor=true;
				instructorIndex++;
				
				//System.out.println("start Instructor " + instructorIndex + course + instructor);
				if (instructorIndex == 1) {
					// course identifier
				}
			}
			
			if (tagName.equalsIgnoreCase("AggregateFileSize"))
			{
				fileSize=true;
			}
			
			
		}
		
		public void characters(char[] ch, int start, int length) {
			if (course) {
				//System.out.println("Course");
				if (name && nameIndex == 1) {
					// course name
					nameValue = new String(ch, start, length);
					name=false;
				}
				else if (handle && handleIndex == 1) {
					// course handle
					handleValue = new String(ch, start, length);
					handle=false;
				}
				else if (identifier && identifierIndex == 1) {
					// course identifier
					identifierValue = new String(ch, start,length);
                    identifier=false;
				}
				else if (instructor && instructorIndex == 1) {
					
					//System.out.println(new String(ch, start,length));
					// course instructor
					instructorValue = (new String(ch, start,length)).trim();
					instructorValue=(!instructorValue.isEmpty() && instructorValue.indexOf(",")==instructorValue.length()-1)?instructorValue.substring(0, instructorValue.length()-1):instructorValue;
					//System.out.println(instructorValue);
					instructor=false;
				}
				else if (fileSize)
				{
					// get the file size and add it to total file size of the course element
					String sString = (new String(ch, start,length)).trim();
					double s = sString.isEmpty()?0:Double.valueOf(sString);
					//System.out.println("File Size: " + s);
					courseTotalFileSize += s;
					
					// clean up
					fileSize = false;
				}
			}
			
		}
		
		public void endElement(String uri, String localName,
							   String tagName)
		throws SAXException {
			
			//System.out.println("End Element :" + qName);
			if (tagName.equalsIgnoreCase("Course"))
			{
				course = false;
				//nameIndex = 0;
				//handleIndex = 0;
				//identifierIndex = 0;
				//instructorIndex = 0;
				
				// output course total file size
				if (siteSet.contains(identifierValue))
                System.out.print(totalITunesUSiteCount + "\t" + attachValue(nameValue) + "\t" + attachValue(handleValue) + "\t" + attachValue(identifierValue) + "\t"+ attachValue(instructorValue) + "\t" + courseTotalFileSize/gigabyte+"\n");
                
				// reset
				//courseTotalFileSize=0;
				//nameValue="";
				//handleValue = "";
				//identifierValue="";
				//instructorValue="";
			}
			
		}
    }
	
	public String attachValue(String v)
	{
		return v.isEmpty()?"N/A":v;
	}
	
	
    public void list(String showTreeFileName  ) throws Exception {
		XMLReader parser =
		XMLReaderFactory.createXMLReader();
		parser.setContentHandler(new HowToHandler( ));
		parser.parse(showTreeFileName);
	}
	
	
    public static void main(String[] args) throws Exception {
		
        String showTreeFileName = args[0];
        String siteIdFileName = args[1];
        
		// read in site contact info
		File siteIdFile = new File(siteIdFileName);
		int row = 0;
		try
		{
			BufferedReader bufRdr  = new BufferedReader(new FileReader(siteIdFile));
			String line = null;
			//read each line of text file
			int count = 0;
			while((line = bufRdr.readLine()) != null)
			{	
				StringTokenizer st = new StringTokenizer(line,"\t");
				String siteId = "";
				// reset count
				count = 0;
				while (st.hasMoreTokens())
				{
					String tokenValue = st.nextToken().trim();
					if (count == 0)
					{
						// this is the site id
						siteId = tokenValue;
                        siteSet.add(siteId);
					}
					count++;
				}
				row++;
			}
			
		}
		catch (FileNotFoundException e)
		{
			System.out.println("FileNotFoundException " + siteIdFileName);
		}
		catch (IOException e)
		{
			System.out.println("IOException " + siteIdFileName);
		}
        
		// parse the itunesu xml file
		System.out.print("COUNT\tSITE_NAME\tHANDLE\tSITE_ID\tINSTRUCTOR_NAME\tTOTAL_FILE_SIZE\n");//\tCONTACT_NAME\tCONTACT_EMAIL\tTERM\tTERM_ID\tCREATED_ON\n");
		new ReadITunesU().list(showTreeFileName );
	}
	
	public class Course
	{
		String m_name;
		String m_identifier;
		int m_totalSize;
		
		
		// constructor
		public Course()
		{
		}
		
		public Course(String name, String identifier, int totalSize)
		{
			m_name = name;
			m_identifier=identifier;
			m_totalSize=totalSize;
		}
		
		public void setName(String name)
		{
			m_name = name;
		}
		
		public String getName()
		{
			return m_name;
		}
		
		public void setIdentifier(String identifier)
		{
			m_identifier=identifier;
		}
		
		public String getIdentifier()
		{
			return m_identifier;
		}
		
		public void setTotalSize(int totalSize)
		{
			m_totalSize = totalSize;
		}
		
		public int getTotalSize()
		{
			return m_totalSize;
		}
		
		
	}
}