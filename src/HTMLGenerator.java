package JIRARest;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

public class HTMLGenerator {

	static java.util.HashMap<String, Integer> status = new java.util.HashMap<>();
	// static java.util.HashMap<String, Integer> issueType = new
	// java.util.HashMap<>();
	static java.util.HashMap<String, String> assignees = new java.util.HashMap<>();
	static java.util.Calendar limit = java.util.Calendar.getInstance();
	static JIRAGlobals globals = null;

	static int statusCount = 0;
	static int barID = 0;

	public static String generateGroups() {
		int count = 1;
		String s = "{id: " + 0 + ", content: 'unknown', value: " + (count++) + "},";
		for (Entry<String, String> assignee : assignees.entrySet()) {
			if (assignee.getValue() != null) {
				s = s + "{id: " + assignee.getKey().hashCode() + ", content: '" + assignee.getValue() + "', value: "
						+ (count++) + "},\n";
				System.out.println(s);
			}
		}
		return s;
	}

	public static void generateIcons() {

		for (Entry<String, Integer> statusItem : status.entrySet()) {
			if (statusItem.getKey() != null) {
				generateStatusIcon("Status", statusItem.getKey().trim());
			}
		}

		for (Entry<String, String> issueType : JIRAGlobals.getGroup("IssueType").entrySet()) {
			if (issueType.getKey() != null) {
				generateStatusIcon("IssueType", issueType.getKey().trim());
			}
		}

	}

	public static String generateTooltipp(JiraTicketItem item) {
		String tooltipp = "";
		tooltipp += item.key + " " + item.content + "<br>";
		tooltipp += item.priority + " " + item.status + "<br>";
		tooltipp += item.fixVersions + "<br>";
		tooltipp += item.assignee + "<br>";
		return tooltipp;

	}

	public static String generateTimeline(java.util.HashMap<String, JiraTicketItem> tickets) {
		String s = "";
		limit.add(java.util.Calendar.getInstance().MONTH, -5);
		System.out.println("Limit:" + limit.getTime().toString());
		for (JiraTicketItem item : tickets.values()) {
			item.endDate = null;
			// System.out.println("#+#+#+"+item.key+"
			// ..................."+item.fields.size());

			for (JIRAField field : item.changelogFields) {
				switch (field.name) {
				case "status":

					if (item.endDate != null) {
						item.startDate = (Calendar) item.endDate.clone();
					}
					item.endDate = (Calendar) field.updated.clone();
					if (item.endDate != null && item.startDate != null && item.startDate.after(limit)) {
						// System.out.println("#+#+#+" + item.key + " " +
						// field.value + " "+
						// item.startDate.getTime().toString() + " - " +
						// item.endDate.getTime().toString());

						s += generateBar(item);
					}
					item.status = field.value;

					if (status.get(item.status) == null) {
						status.put(item.status, statusCount);
						statusCount++;
					}

					break;
				case "assignee":
					item.assignee = field.value;
					item.assigneeName = field.value;
					item.group = field.value;

					if (assignees.get(item.assignee) == null) {
						assignees.put(item.assigneeName, item.assigneeName);

					}
					break;
				}

			}

		}
		// System.out.println(s);

		return s;
	}

	public static String itemColor() {
		// java.util.Random rnd = new java.util.Random();
		String myResult = "";
		for (Entry<String, Integer> statusItem : status.entrySet()) {
			Color c = getColorFromText(statusItem.getKey().trim());
			String s = ".vis-item.NAME {   \n  background-color:rgb(" + c.getRed() + "," + c.getGreen() + ","
					+ c.getBlue() + ");\n}\n";
			myResult += s.replaceAll("NAME", statusItem.getKey().trim());

		}
		return myResult;
	}

	public static void generateStatusIcon(String group, String text) {
		File f = new File("jira\\" + group + "_" + text + ".png");
		if (!f.exists()) {

			BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = img.createGraphics();

			// g2d.setFont(font);
			FontMetrics fm = g2d.getFontMetrics();
			int width = fm.stringWidth(text);
			int height = fm.getHeight();
			g2d.dispose();

			// img = new BufferedImage(width, height,
			// BufferedImage.TYPE_INT_ARGB);
			img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);

			g2d = img.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			// g2d.setFont(font);
			fm = g2d.getFontMetrics();
			Color c = getColorFromText(text);
			// g2d.setBackground(c);
			g2d.setPaint(c);
			g2d.fillRect(0, 0, 24, 24);
			c = new Color(0,0,0);
			g2d.setColor(c);

			g2d.drawString(text, 0, fm.getAscent());
			g2d.dispose();
			// write BufferedImage to file
			try {
				ImageIO.write(img, "png", f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("File exists "+f.getPath()+f.getName());
		}
	}

	public static Color getColorFromText(String text) {
		int r = (Math.abs(text.hashCode()) & 0xFF0000) >> 8 >> 8;
		int g = (Math.abs(text.hashCode()) & 0x00FF00) >> 8;
		int b = (Math.abs(text.hashCode()) & 0x0000FF);
		if (r + g + b < 200) {
			if (r < 100) {
				r += 50;
			}
			if (g < 100) {
				g += 50;
			}
			if (b < 100) {
				b += 50;
			}
		}

		return new Color(r, b, g);

	}

	public static String issueTypeImage(JiraTicketItem item) {
		return "<img src=\"" + "IssueType_" + item.issueType.trim() + ".png\" title=\"" + item.issueType.trim() + "\">";

	}

	public static String statsuImage(JiraTicketItem item) {
		return "<img src=\"" + "Status_" + item.status.trim() + ".png\" title=\"" + item.status.trim() + "\">";

	}

	public static String generateBar(JiraTicketItem item) {
		String s = "";
		s = s + "{id: " + (barID++) + ", group: " + item.group.hashCode() + ", content: '" + issueTypeImage(item)
				+ statsuImage(item) + "<a href=\"http://jira.seidenader.de/browse/" + item.key + "\">" + item.key + " "
				+ item.content;
		s = s + "</a>', 'className': '" + item.status.trim() + "', title: '" + generateTooltipp(item) + "',"
				+ " value: 0.7 ," + " start: new Date(" + item.startDate.get(java.util.Calendar.YEAR) + ", "
				+ (item.startDate.get(java.util.Calendar.MONTH)) + ", "
				+ item.startDate.get(java.util.Calendar.DAY_OF_MONTH);
		s = s + "), end: new Date(" + item.endDate.get(java.util.Calendar.YEAR) + ", "
				+ (item.endDate.get(java.util.Calendar.MONTH)) + ", "
				+ item.endDate.get(java.util.Calendar.DAY_OF_MONTH) + ")},\n";
		return s;
	}

	public void createHTML(java.util.HashMap<String, JiraTicketItem> tickets, JIRAGlobals _globals) {
		globals = _globals;
		String table = generateTimeline(tickets);
		String tableGroup = generateGroups();
		generateIcons();
		generateHTMLFile(table, tableGroup);
	}

	public static void generateHTMLFile(String table, String group) {
		try {
			File file = new File("jira\\JiraTimeline.html");
			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter writer = new PrintWriter("jira\\output.html", "UTF-8");

			String st;
			while ((st = br.readLine()) != null) {
				st = st.replaceAll("ITEMTABLE", table);
				st = st.replaceAll("GROUPTABLE", group);
				st = st.replaceAll("STYLER", itemColor());
				writer.println(st);
				// System.out.println(st);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
