package JIRARest;

public class JIRAGlobals {

	static java.util.HashMap<String, String> issueType = new java.util.HashMap<>();
	static java.util.HashMap<String, String> developers = new java.util.HashMap<>();
	static java.util.HashMap<String, String> reporters = new java.util.HashMap<>();
	static java.util.HashMap<String, String> assignees = new java.util.HashMap<>();
	static java.util.HashMap<String, String> creators = new java.util.HashMap<>();

	static java.util.HashMap<String, java.util.HashMap<String, String>> jira = new java.util.HashMap<>();

	public void add(String group, String key, String name) {
		java.util.HashMap<String, String> type = jira.get(group);
		if (type == null) {
			type = new java.util.HashMap<String, String>();
			type.put(key, name);
			jira.put(group, type);

		} else {
			type.put(key, name);
		}
	}

	public static java.util.HashMap<String, String> getGroup(String group) {
		return jira.get(group);
	}

	public static void addDeveloper(String key, String name) {
		if (developers.get(key) == null) {
			developers.put(key, name);
		}
	}

	public static void addAssignee(String key, String name) {
		if (assignees.get(key) == null) {
			assignees.put(key, name);
		}
	}

	public static void addReporter(String key, String name) {
		if (reporters.get(key) == null) {
			reporters.put(key, name);
		}
	}

	public static void addCreator(String key, String name) {
		if (creators.get(key) == null) {
			creators.put(key, name);
		}
	}

}
