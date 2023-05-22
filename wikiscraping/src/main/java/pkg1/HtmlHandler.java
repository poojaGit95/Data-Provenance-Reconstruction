package pkg1;


import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import java.io.*;

public class HtmlHandler implements ITextExtractionHandler {

	private static HtmlHandler instance = null;

	private HtmlHandler() {
	}

	public static HtmlHandler getInstance() {
		if (instance == null) {
			instance = new HtmlHandler();
		}
		return instance;
	}

	@Override
	public void Extract(File from, File to) {
		try {
			Document doc = Jsoup.parse(from, "UTF-8");

			Elements paragraphs = doc.select("p");
			Element body = doc.select("div.ViewDetails_BodyDiv").first();

			if ((paragraphs==null || paragraphs.size()==0) && (body==null)){
				return;
			}


			BufferedWriter bw = new BufferedWriter(new FileWriter(to.getAbsoluteFile()));
			bw.write(doc.title());
			bw.newLine();

			for (Element p : paragraphs) {
				// remove punctuation and digits and covert to lowercase
				String plainText = p.text().replaceAll("[^\\w]", " ")
						                   .replaceAll("\\s+", " ")
						                   .toLowerCase();
				bw.write(plainText);
			}
			// special handle for http://www.maannews.net/eng/ViewDetails.aspx?ID=670619
			// paragraph is not in the <p> tag
			// future improvement needed
			if (body != null){
				String plainText = body.text().replaceAll("[^\\w]", " ")
		                   .replaceAll("\\s+", " ")
		                   .toLowerCase();
				bw.write(plainText);
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
