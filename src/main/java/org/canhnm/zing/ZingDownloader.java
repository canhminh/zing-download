package org.canhnm.zing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class ZingDownloader {

	public static void download(String playlistURL) throws Exception {
		org.jsoup.nodes.Document doc = Jsoup.connect(playlistURL).get();
		Elements newsHeadlines = doc.select("#html5player");
		if (newsHeadlines.isEmpty()) {
			System.out.println("Khong tim thay html5player");
			return;
		}
		String dataXmlUrl = newsHeadlines.get(0).attr("data-xml");
		if (dataXmlUrl == null || dataXmlUrl.isEmpty()) {
			System.out.println("Khong co du lieu data-xml");
			return;
		}
		
		String playListName = "new playlist";
		
		try {
			playListName = toSafeFilename(doc.select(".info-content").select("meta[itemprop=\"name\"]").get(0).attr("content"));
			System.out.println(playListName);
		} catch (Exception e) {
		}
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(dataXmlUrl);
		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toString(entity) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};
		String responseBody = httpclient.execute(httpget, responseHandler);

		Document document = DocumentHelper.parseText(responseBody);
		Element root = document.getRootElement();

		List<Element> elements = root.elements("item");
		File out = new File("out/" + playListName);
		if (!out.exists())
			out.mkdir();

		int i = 1;
		for (Element element : elements) {
			try {
				String title = element.elementText("title");
				String mp3Url = element.elementText("source");
				HttpGet httpget2 = new HttpGet(mp3Url);
				String filename = out + "/" + i++ + " " + toSafeFilename(title) + ".mp3";
				filename = FilenameUtils.normalize(filename);
				System.out.println(filename);
				File file = new File(filename);
				if (file.exists())
					continue;
				CloseableHttpResponse response1 = httpclient.execute(httpget2);
				HttpEntity entity1;
				InputStream is = null;
				try {
					System.out.println(response1.getStatusLine());
					entity1 = response1.getEntity();
					is = entity1.getContent();
					FileUtils.copyInputStreamToFile(is, file);
				} finally {
					response1.close();
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static String toSafeFilename(String title) {
		title = title.replaceAll("[\\{\\}/\\\"\\,\\?\\:\\*\\<\\>\\|]", "");
		title = title.replaceAll("\\s+", " ");
		title = title.trim();
		return title;
	}

	public static void main(String[] args) throws IOException, Exception {
		if (args.length == 0) {
			System.out.println("Can truyen tham so URL cua playlist");
			return;
		}
		download(args[0]);
	}
}
