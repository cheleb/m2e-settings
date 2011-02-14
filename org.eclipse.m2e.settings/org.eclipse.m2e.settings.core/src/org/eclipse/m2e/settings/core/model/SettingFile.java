package org.eclipse.m2e.settings.core.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;


public class SettingFile {
	private String path;
	private String contents;
	private String location;
	private URL url;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public static List<SettingFile> fromXpp3doms(Xpp3Dom[] files) {
		List<SettingFile> settingFiles = new ArrayList<SettingFile>();
		for (Xpp3Dom file : files) {
			SettingFile settingFile = new SettingFile();

			Xpp3Dom name = file.getChild("name");
			settingFile.setPath(name.getValue());
			Xpp3Dom content = file.getChild("content");
			if (content != null) {
				settingFile.setContents(content.getValue());

			} else if ((content = file.getChild("url")) != null) {
				URL url;
				try {
					url = new URL(content.getValue());
					settingFile.setURL(url);
				} catch (MalformedURLException e) {
					System.err.println(e.getMessage());
				}

			} else if ((content = file.getChild("location")) != null) {
				settingFile.setLocation(content.getValue());
			} else {
				System.err.println("Could not find content for: " + settingFile);
				continue;
			}
			settingFiles.add(settingFile);
		}
		return settingFiles;
	}

	public String getLocation() {
		return location;
	}

	public URL getUrl() {
		return url;
	}

	private void setLocation(String location) {
		this.location = location;

	}

	private void setURL(URL url) {
		this.url = url;

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getPath());
		if (getContents() != null) {
			int l = getContents().length();
			if (l > 20) {
				builder.append("[content:").append(getContents())
						.append(getContents().substring(0, 20)).append("]");
			} else {
				builder.append("[content:").append(getContents()).append("]");
			}
		} else if (getUrl() != null) {
			builder.append("[URL: ").append(getUrl() + ")]");
		} else if (getLocation() != null) {
			builder.append("[location: ").append(getLocation() + ")]");
		}
		return builder.toString();
	}

}
