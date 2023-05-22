import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

public class WikiScrape {
    
    int noOfArticles = 0;
    int sourceFileCount = 0;
    HashMap<String, Boolean> pageMap = new HashMap<>();
    
    Hashtable<String, ArrayList<String>> truthFile = new Hashtable<String, ArrayList<String>>();

    public WikiScrape(int i) {
        noOfArticles = i;
        
        //create a folder for the raw data
        new File("rawData").mkdir();
    }

    public void setNofArticles(int articles){
        this.noOfArticles = articles;
    }

    public int getNoOfArticles(){
        return this.noOfArticles;
    }

    private void scrape() {
        Document doc;
        int articles = getNoOfArticles();
        for(int i = 0; i < articles; ) {
            try {

                // This URL brings us to a random wiki article
                String url = Jsoup.connect("https://en.wikinews.org/wiki/Special:Random").followRedirects(true).execute().url().toExternalForm();

                // Connect to the new URL
                doc = Jsoup.connect(url).get();

                // Get the title, and removed invalid characters
                String article_title = doc.title();

                // Get rid of wiki title, if it exists
                if(article_title.contains("- Wikinews, the free news source"))
                    article_title = article_title.replace(" - Wikinews, the free news source", "");

                article_title = article_title.replaceAll("[^a-zA-Z0-9.-]", "_");

                // Perform a simple check to make sure we haven't attempted that page already
                if(pageMap.containsKey(article_title)){
                    continue;
                } else {
                    pageMap.put(article_title, true);
                }

                // Verify all of the sources

                ArrayList<String> sources = sourceCheck(doc);
                
                
                
                
                if(sources != null){
                    truthFile.put(article_title + "$AAA$.html.txt", sources);

                    //FXML_Handler.setUpdateText(Constants.SCRAPE_MODE, "Valid Source Found - " + article_title);

                    saveWikiArticle(doc, article_title);
                   // updateTurtleFile(article_title + ".html", sources);

                    i++;

                    //FXML_Handler.setStatusLabel("Gathering Test Set (" + i + "/" + numToScrape + ")");

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    public ArrayList<String> sourceCheck(Document doc){

        Elements tLink = doc.select("li a.text");

        ArrayList<String> foundSources = new ArrayList<>();
        ArrayList<String> acceptedSources = new ArrayList<>();

        for(Element ele : tLink){
            foundSources.add(ele.attr("href"));
        }

        if(foundSources.isEmpty())
            return null;

        for(String url : foundSources){

            try {
                // See if we are on the page that is intended
                Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                        .timeout(10000)
                        .followRedirects(false)
                        .execute();

                // Status code 200 is success
                int statusCode = response.statusCode();
                if(statusCode == 200) {
                   // acceptedSources.add(url);
                    acceptedSources.add(sourceFileCount + ".html.txt");
                    saveSourceFile(response.parse());
                }


            } catch (Exception e){ } // Execute generates exceptions if a connection hasn't been made
        }

        if(acceptedSources.isEmpty())
            return null;


        return acceptedSources;
    }
    
    public void saveSourceFile(Document doc){

        // Write the doc to the summary file with the number
        //commented for now
        //might need it later
       /* try(BufferedWriter w = new BufferedWriter(new FileWriter(summaryFile, true))) {
            w.write(sourceFileCount + " " + doc.baseUri() + "\n");
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }*/

        // Write the source file
        try {
            File temp = new File("rawData/" + sourceFileCount + ".html");
            BufferedWriter htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
            htmlWriter.write(doc.toString());
            htmlWriter.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        sourceFileCount++;

    }
    
    public void saveWikiArticle(Document doc, String title){
        // Write the wiki file
        try {
            File temp = new File("rawData/" + title + "$AAA$.html");
            BufferedWriter htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
            htmlWriter.write(doc.toString());
            htmlWriter.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private int writeGroundTruth() throws FileNotFoundException, UnsupportedEncodingException {
        
        PrintWriter writer = new PrintWriter("truthfile.txt", "UTF-8");
        List<String> articlesToBeRemoved = new ArrayList<>();
        int count = 0;

        for(String key : truthFile.keySet()) {
            ArrayList<String> sources = truthFile.get(key);
            String content = "";
            boolean flag = false;

            //writer.print(key+"#");
            if (!new File("txtData/" + key).exists()) {
                articlesToBeRemoved.add(key);
                continue;
            }
            content = content + key + "#";
            for (String source : sources) {
                if (!new File("txtData/" + source).exists()) {
                    flag = true;
                    break;
                }
                content = content + " " + source;
                //writer.print(" "+source);
            }
            if (flag == false) {
                writer.print(content);
                writer.println();
                count++;
            }else{
                articlesToBeRemoved.add(key);
                boolean f = new File("txtData/"+key).delete();
            }
        }
        writer.close();

        for(String articleName: articlesToBeRemoved){
            truthFile.remove(articleName);
        }

        return count;
    }

    public Map<String, List<String>> getArcticleSourceMap(){
        //read the truth file
        Map<String, List<String>> truthData = new HashMap<String, List<String>>();

        try {
            Scanner truthFile = new Scanner(new File("truthfile.txt"));


            //get the data and construct a hashtable with it
            while (truthFile.hasNextLine()) {
                String line = truthFile.nextLine();

                String[] split = line.split("# ");
                if (split.length == 2) {

                    String[] sources = split[1].split(" ");
                    List<String> sourcesList = new ArrayList<String>();
                    for (String s : sources) {
                        sourcesList.add(s);
                    }
                    truthData.put(split[0], sourcesList);
                }

            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        for(String article: truthData.keySet()){
            System.out.println("Article: "+article);
            for (String source: truthData.get(article)){
                System.out.println("Source: "+source);
            }
            System.out.println("----");
        }
        return truthData;
    }

    public void writeInputFile() throws FileNotFoundException, UnsupportedEncodingException {
        List<String> allFiles = new ArrayList<>();

        for (String s : truthFile.keySet()) {
            allFiles.add(s);
            allFiles.addAll(truthFile.get(s));
        }

        BufferedReader reader;
        PrintWriter writer = new PrintWriter("input0.txt", "UTF-8");


        for (String fileName : allFiles) {

            try {
                reader = new BufferedReader(new FileReader("txtData/"+fileName));
                String line = reader.readLine();
                writer.print(fileName);
                writer.print("##lda_delimiter##");
                String content = "";
                int count = 0;
                while (line != null) {
                    if (content.isEmpty()){
                        content = content + line;
                    }else{
                        content = content + " " + line;
                    }
                    count++;
                    line = reader.readLine();
                }
                writer.print(content);
                writer.println();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writer.close();

    }


    public void clearRawAndTxtDirectories(){
        File dir1 = new File("txtData");
        for (File f : dir1.listFiles()){
            if(!f.isDirectory()){
                f.delete();
            }
        }

        File dir2 = new File("rawData");
        for (File f : dir2.listFiles()){
            if(!f.isDirectory()){
                f.delete();
            }
        }
    }


    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {


      //  System.out.println("Serial Code");
        //CHANGE ARTICLE NUMBER FROM THE EXCEL SHEET
        WikiScrape ws = new WikiScrape( 3000 );
        long start = System.currentTimeMillis();
        ws.clearRawAndTxtDirectories();
        ws.scrape();
        long end1 = System.currentTimeMillis();
        System.out.println("converting to txt");
        TextConversion.convertToTxt();
        System.out.println("Writing to truthfile");
        int articles = ws.writeGroundTruth();
        System.out.println("Writing to inputfile");
        ws.writeInputFile();
        long end2 = System.currentTimeMillis();

        System.out.println("scrape time: " + (end1-start));
        System.out.println("full time: " + (end2-start));
        System.out.println("Total articles scrapped: " + articles);
    }
}
