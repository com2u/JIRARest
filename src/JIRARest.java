package JIRARest;

//import java.net.MalformedURLException;
//import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
//import java.security.cert.Certificate;
//import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLPeerUnverifiedException;

//import RRSAlarm.RRSTitle;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.List;
import java.util.Locale;
//import java.util.Map.Entry;


import org.json.simple.parser.JSONParser;


public class JIRARest {

	static String JIRAuser = "Max.Mustermann";
	static String JIRApasswd = "Pa$$word123";
	static String JIRAurl = "https://jira.mycompany.com/rest/api/2/search?jql=Project%20%3D%20SVObserver%20and%20fixVersion%20%3D%20\"8.00%20SVO\"%20&maxResults=1000&expand=changelog,fields";

	static java.util.HashMap<String, JiraTicketItem> tickets = new java.util.HashMap<>();
    static JIRAGlobals globals = new JIRAGlobals();
	static int ID = 0;

	

	public static java.util.Calendar convertDate(String date) {
		// 2018-03-07T15:00:22.680+0100""
		Calendar cal = null;
		if (date == null) {
			return null;
		}
		try {
			cal = Calendar.getInstance();
			ZonedDateTime zonedDateTime;
			// System.out.println("###"+date+"###");
			if (date.length() > 14) {
				zonedDateTime = ZonedDateTime.parse(date.substring(0, 26) + ":00");
				cal = GregorianCalendar.from(zonedDateTime);
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				cal.setTime(sdf.parse(date));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cal;
	}

	public static void main(String[] args) {
		
		
		updateJSONcredentials();
		requestJIRA();
		HTMLGenerator html = new HTMLGenerator();
		
		html.createHTML(tickets, globals);
		
	}

	private static void requestJIRA() {
		try {
			System.out.println(JIRAurl);
			String response = getResult(JIRAurl);
			listTickets(response);

			// System.out.println(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void updateJSONcredentials() {

		JSONParser parser = new JSONParser();

		try {
			Object obj = parser.parse(new FileReader("../credentials/credentials.json"));
			org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) obj;
			JIRAuser = (String) jsonObject.get("user");
			JIRApasswd = (String) jsonObject.get("password");
			JIRAurl = (String) jsonObject.get("url");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
	}

	

	public static ArrayList<JIRAField> getFields(JSONObject obj) {
		ArrayList<JIRAField> fields = new ArrayList<JIRAField>();
		try {
			JSONArray historyEntry = obj.getJSONArray("histories");
			for (int historyID = 0; historyID < historyEntry.length(); historyID++) {
				String updateString = historyEntry.getJSONObject(historyID).getString("created");
				java.util.Calendar update = convertDate(updateString);
				JSONArray jsonFields = historyEntry.getJSONObject(historyID).getJSONArray("items");
				for (int fieldID = 0; fieldID < jsonFields.length(); fieldID++) {
					JIRAField field = new JIRAField();
					field.name = jsonFields.getJSONObject(fieldID).getString("field");
					field.value = jsonFields.getJSONObject(fieldID).getString("toString");
					field.updated = update;
					fields.add(field);
					System.out.println(field.name + "  " + field.value);
				}

			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fields;
	}

	private static void listTickets(String content) {
		try {
			JSONObject obj = new JSONObject(content);
			JSONArray arr = obj.getJSONArray("issues");
			for (int i = 0; i < arr.length(); i++) {
				String key = arr.getJSONObject(i).getString("key");
				String summary = arr.getJSONObject(i).getJSONObject("fields").getString("summary");
				JiraTicketItem item = tickets.get(key);
				try {
					if (item == null) {

						item = new JiraTicketItem();
						java.util.Calendar startDate = java.util.Calendar.getInstance();
						item.startDate = startDate;
						item.endDate = (Calendar) startDate.clone();
						item.ID = ID++;
						item.key = key;
						item.content = summary.replaceAll("'", "^");
						item.status = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("status")
								.getString("name");
						globals.add("Status", item.status, item.status);
						
						item.description = arr.getJSONObject(i).getJSONObject("fields").getString("description");
						item.issueType = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("issuetype")
								.getString("name");
						globals.add("IssueType", item.issueType, item.issueType);
						
						item.project = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("project")
								.getString("name");
						item.created = arr.getJSONObject(i).getJSONObject("fields").getString("created");
						item.updated = arr.getJSONObject(i).getJSONObject("fields").getString("updated");
						item.creator = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("creator")
								.getString("name");
						item.creatorName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("creator")
								.getString("key");
						globals.add("Creator", item.assignee, item.creatorName);
						
						item.reporter = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("reporter")
								.getString("name");
						item.reporterName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("reporter")
								.getString("key");
						globals.add("Reporter", item.assignee, item.creatorName);
						//addReporter(item.reporter, item.reporterName);

						try {
							item.changelogFields = getFields(arr.getJSONObject(i).getJSONObject("changelog"));
							item.assignee = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("assignee")
									.getString("name");
							item.assigneeName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("assignee")
									.getString("key");
							globals.add("Assignee", item.assignee, item.assigneeName);
							//addAssignee(item.assignee, item.assigneeName);

							item.priority = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("priority")
									.getString("name");
							item.timespent = arr.getJSONObject(i).getJSONObject("fields").getString("timespent");
							item.resolution = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("resolution")
									.getString("name");
							item.developer = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONObject("customfield_10300").getString("name");
							item.developerName = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONObject("customfield_10300").getString("key");
							globals.add("Developer", item.developer, item.developerName);
							//addDeveloper();
							String lables = "";

							item.developmentStartDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10312");
							// item.startDate = (Calendar)
							// convertDate(item.developmentStartDate).clone();
							System.out.println("New StartDate:" + item.startDate.getTime().toString());
							item.implementationDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10120");
							item.checkInDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10311");
							// item.endDate = (Calendar)
							// convertDate(item.checkInDate).clone();
							System.out.println("New endDate:" + item.endDate.getTime().toString());

							JSONArray lablesArray = arr.getJSONObject(i).getJSONObject("fields").getJSONArray("labels");
							for (int l = 0; l < lablesArray.length(); l++) {
								lables += lablesArray.getJSONObject(l).getString("name") + ", ";
							}
							item.lables = lables;
							String fixVersions = "";
							JSONArray versionsArray = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONArray("fixVersions");
							for (int l = 0; l < versionsArray.length(); l++) {
								fixVersions += versionsArray.getJSONObject(l).getString("name") + ", ";
							}
							item.fixVersions = fixVersions;
							System.out.println("New fixVersions:" + fixVersions);

						} catch (Exception e) {
							// e.printStackTrace();
						}
						tickets.put(key, item);

						// System.out.println(key + " " + summary+"
						// "+item.startDate.get(java.util.Calendar.YEAR)+"."+(item.startDate.get(java.util.Calendar.MONTH)+1)+"."+item.startDate.get(java.util.Calendar.DAY_OF_MONTH));
					} else {
						java.util.Calendar endDate = java.util.Calendar.getInstance();
						item.endDate = (Calendar) endDate.clone();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println(arr.length());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	private static void listTickets(String content, int days, String user) {
		try {
			JSONObject obj = new JSONObject(content);
			JSONArray arr = obj.getJSONArray("issues");
			for (int i = 0; i < arr.length(); i++) {
				String key = arr.getJSONObject(i).getString("key");
				String summary = arr.getJSONObject(i).getJSONObject("fields").getString("summary");
				JiraTicketItem item = tickets.get(key);
				try {
					if (item == null) {

						item = new JiraTicketItem();
						java.util.Calendar startDate = java.util.Calendar.getInstance();
						startDate.add(java.util.Calendar.getInstance().HOUR, -(24) * days);
						item.startDate = startDate;
						item.endDate = (Calendar) startDate.clone();
						item.ID = ID++;
						item.key = key;
						item.masterAssignee = user;
						item.content = summary.replaceAll("'", "^");
						item.status = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("status")
								.getString("name");
						globals.add("Status", item.status, item.status);
						item.description = arr.getJSONObject(i).getJSONObject("fields").getString("description");
						item.issueType = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("issuetype")
								.getString("name");
						globals.add("IssueType", item.issueType, item.issueType);
						item.project = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("project")
								.getString("name");
						item.created = arr.getJSONObject(i).getJSONObject("fields").getString("created");
						item.updated = arr.getJSONObject(i).getJSONObject("fields").getString("updated");
						item.creator = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("creator")
								.getString("name");
						item.creatorName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("creator")
								.getString("key");
						globals.add("Creator", item.assignee, item.creatorName);
						//addCreator(item.assignee, item.creatorName);
						item.reporter = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("reporter")
								.getString("name");
						item.reporterName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("reporter")
								.getString("key");
						globals.add("Reporter", item.reporter, item.reporterName);
						//addReporter(item.reporter, item.reporterName);

						try {
							item.changelogFields = getFields(arr.getJSONObject(i).getJSONObject("changelog"));
							item.assignee = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("assignee")
									.getString("name");
							item.assigneeName = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("assignee")
									.getString("key");
							//addAssignee(item.assignee, item.assigneeName);
							globals.add("Assignee", item.assignee, item.assigneeName);

							item.priority = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("priority")
									.getString("name");
							item.timespent = arr.getJSONObject(i).getJSONObject("fields").getString("timespent");
							item.resolution = arr.getJSONObject(i).getJSONObject("fields").getJSONObject("resolution")
									.getString("name");
							item.developer = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONObject("customfield_10300").getString("name");
							item.developerName = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONObject("customfield_10300").getString("key");
							globals.add("Developer", item.developer, item.developerName);
							//addDeveloper(item.developer, item.developerName);
							item.group = item.developer;
							String lables = "";

							item.developmentStartDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10312");
							// item.startDate = (Calendar)
							// convertDate(item.developmentStartDate).clone();
							System.out.println("New StartDate:" + item.startDate.getTime().toString());
							item.implementationDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10120");
							item.checkInDate = arr.getJSONObject(i).getJSONObject("fields")
									.getString("customfield_10311");
							// item.endDate = (Calendar)
							// convertDate(item.checkInDate).clone();
							System.out.println("New endDate:" + item.endDate.getTime().toString());

							JSONArray lablesArray = arr.getJSONObject(i).getJSONObject("fields").getJSONArray("labels");
							for (int l = 0; l < lablesArray.length(); l++) {
								lables += lablesArray.getJSONObject(l).getString("name") + ", ";
							}
							item.lables = lables;
							String fixVersions = "";
							JSONArray versionsArray = arr.getJSONObject(i).getJSONObject("fields")
									.getJSONArray("fixVersions");
							for (int l = 0; l < versionsArray.length(); l++) {
								fixVersions += versionsArray.getJSONObject(l).getString("name") + ", ";
							}
							item.fixVersions = fixVersions;
							System.out.println("New fixVersions:" + fixVersions);

						} catch (Exception e) {
							// e.printStackTrace();
						}
						tickets.put(key + user, item);

						// System.out.println(key + " " + summary+"
						// "+item.startDate.get(java.util.Calendar.YEAR)+"."+(item.startDate.get(java.util.Calendar.MONTH)+1)+"."+item.startDate.get(java.util.Calendar.DAY_OF_MONTH));
					} else {
						java.util.Calendar endDate = java.util.Calendar.getInstance();
						endDate.add(java.util.Calendar.getInstance().HOUR, -(24) * days);
						item.endDate = (Calendar) endDate.clone();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println(arr.length());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	private static String getResult(String urlString) {
		URL url;
		HttpsURLConnection con = null;
		StringBuilder result = null;
		try {

			url = new URL(urlString);
			con = (HttpsURLConnection) url.openConnection();
			String login = JIRAuser + ":" + JIRApasswd;
			final byte[] authBytes = login.getBytes(StandardCharsets.UTF_8);
			String encoded = Base64.getEncoder().withoutPadding().encodeToString(authBytes);

			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "*/*");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", "Basic " + encoded);
			con.setRequestProperty("startAt", "50");
			con.setRequestProperty("maxResults", "1000");
			con.setUseCaches(false);
			con.setDoOutput(true);
			con.setDoInput(true);
			con.connect();
			if (con != null) {

				BufferedReader in = new BufferedReader(
						new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
				result = new StringBuilder();
				String line;

				while ((line = in.readLine()) != null) {
					result.append(line);
					result.append(System.lineSeparator());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		con.disconnect();
		return result.toString();
	}

}
