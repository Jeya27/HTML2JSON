package com.t2s.html2json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

public class html2json {

    public static void main(String[] args) throws IOException {

        Document doc;
        try {

            //Source path of HTML files
            String sourcePath = "/home/touch2s/Desktop/HTML2JSON_INPUT_OUTPUTFILES/" +
                    "HTML2JSON_INPUT_OUTPUTFILES/T2S_Foodhub_Revamp-master/about_us.html";

            //Path to store JSON files
            String destPath = "/home/touch2s/JSON/";

            File input = new File(sourcePath);

            //LinkedHashMap is used instead of JSONObject inorder to maintain the original
            // order in which html file is created
            // JSONObject by default stores object in alphabetical order

            LinkedHashMap child = new LinkedHashMap();
            LinkedHashMap main = new LinkedHashMap();

            //the extension of the html page is removed
            // for eg : filename - about.html, .html is removed
            // So, removeExtn = about

            String removeExtn = input.getName().substring(0, input.getName().lastIndexOf('.'));

            // Parse the HTML document
            doc = Jsoup.parse(input,"UTF-8");

            //Adding the default elements required in JSON
            main.put("name", removeExtn);
            main.put("pageTitle", doc.title());
            main.put("defaultLocator", "Web");

            // Counts for giving a name for elements which does not consist text or valid name
            int count_link = 1;
            int count_text = 1;
            int count_input = 1;
            int count_button = 1;
            int count_select = 1;

            //Scraping tag by tag
            for(Element element: doc.select("*")) //doc.select(*) to select all tags from the HTML page
            {
                String tag = element.tagName();
                String text = element.text();

                switch (tag)
                {

                    case "a":

                        if(text.length() == 0)
                        {
                            text = "link" + Integer.toString(count_link);
                            count_link++;
                        }

                        //To remove white spaces from the text
                        text = text.replaceAll("\\s+", "");
                        child.put(text, getAttributes(element, "clickOnLink", doc));

                        break;

                    case "input":

                        if( text.length() == 0 )
                        {
                            String name, value;

                            name = element.attr("name");
                            value = element.attr("value");

                            //Checks for value attribute or name attribute
                            if(value.length() != 0)
                            {
                                text = element.attr("value");
                            }
                            else if(name.length() != 0)
                            {
                                text = element.attr("name");
                            }
                            //Assigns a common name
                            else
                            {
                                text = "input" + Integer.toString(count_input);
                                count_input++;
                            }
                        }

                        String type = element.attr("type");
                        String action;

                        //Sets the action variable based on button type
                        if(type == "radio" || type == "checkbox")
                        {
                            action = "clickOnInput";
                        }
                        else if(type == "submit")
                        {
                            action = "clickOnButton";
                        }
                        else if(type == "date")
                        {
                            action = "selectDate";
                        }
                        else
                        {
                            action = "enterInput";
                        }

                        text = text.replaceAll("\\s+", "");
                        child.put(text, getAttributes(element, action, doc));
                        break;

                    case "button":
                        // If text is not available
                        if(element.text().length() == 0)
                        {
                            String name, value;

                            name = element.attr("name");
                            value = element.attr("value");

                            //Checks for value attribute or name attribute
                            if(value.length() != 0)
                            {
                                text = element.attr("value");
                            }
                            else if(name.length() != 0)
                            {
                                text = element.attr("name");
                            }
                            // If text, value and name are not available, it generates a common name
                            else
                            {
                                text = "button" + Integer.toString(count_button);
                                count_button++;
                            }
                        }

                        text = text.replaceAll("\\s+", "");
                        child.put(text, getAttributes(element, "clickOnButton", doc));
                        break;


                    case "p":

                        // checks if text is too long
                        if( text.split(" ").length >= 5 )
                        {
                            text = "text" + Integer.toString(count_text);
                            count_text++;
                        }
                        else if(text.length() == 0)
                            break;

                        text = text.replaceAll("\\s+", "");
                        child.put(text,getAttributes(element, "getText", doc));
                        break;

                    case "img":
                        // If text is not available
                        if(text.length() == 0)
                        {
                            // Image name is taken from src attribute
                            // The extension is removed
                            String imgRemoveExtn = element.attr("src").
                                    substring(0, element.attr("src").lastIndexOf('.'));

                            imgRemoveExtn = imgRemoveExtn.replaceAll("(.*\\/)*","");
                            text = imgRemoveExtn;
                        }

                        text = text.replaceAll("\\s+", "");
                        child.put(text, getAttributes(element, "verifyDisplay", doc));
                        break;

                    case "select":
                        // If text is not available
                        if(text.length() == 0)
                        {
                            text = "select" + Integer.toString(count_select);
                            count_select++;
                        }

                        text = text.replaceAll("\\s+", "");
                        child.put(text, getAttributes(element, "dropDown", doc));
                        break;

                }

            }

            main.put("elements", child);

            //To pretty print JSON - Object Mapper class is used
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(main);

            try
            {
                //Writing JSON output to file
                //The JSON file has the same name as the HTML file

                File output = new File(destPath + removeExtn + ".json");
                output.createNewFile(); //creates new file if it doesn't exist.....Other wise it simply opens the file

                FileWriter fileWriter = new FileWriter(output);
                fileWriter.write(json);

                fileWriter.flush();
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Function to get the attributes of an element

    public static JSONObject getAttributes(Element element, String action, Document doc)
    {
        JSONObject child1 = new JSONObject();

        // Iterating element by element
        for(Attribute attribute : element.attributes())
        {
            child1.put(attribute.getKey(), attribute.getValue());
        }

        child1.put("locators", getXpathOfElement(element,doc));
        child1.put("actionEvent", action);

        return child1;
    }

    /*Function to generate xpath of an HTML element*/

    public static JSONObject getXpathOfElement(Element element, Document doc)
    {
        JSONObject locators = new JSONObject();

        String xpath="//" + element.tagName();
        int count = 0;

        String id = element.attr("id");
        String classname = element.attr("class");
        String name = element.attr("name");

        //If element has id attribute xpath is generated with id
        if(!id.isEmpty())
        {
            xpath += "[@id= '" + id + "']";
            //System.out.println(xpath);
        }
        //If element does not have id attribute, xpath is generated with name attribute
        else if(!name.isEmpty())
        {
            xpath += "[@name='" + name + "']";
        }
        //If element doesn't have id as well as name, xpath is generated with class attribute
        else if(!classname.isEmpty())
        {
            //Checks if the class name is unique
            for (Element ele : doc.select("*[class=" + classname +"]"))
            {
                if(ele.attr("class") == classname)
                    count++;
            }

            //If class name is unique, classname is used to generate xpath
            if(count == 1)
            {
                xpath += "[@class='" +classname + "']";
                //System.out.println(xpath);
            }
        }

        //When all other conditions fail, the element checks for the elements's text
        else if(!element.text().isEmpty())
        {
            String text = element.text();

            if( text.split(" ").length >= 7)
            {
                String[] texts = text.split(" ");
                text = "";

                //Trimming the strings to 7 words in case of very long string

                for(int i=0; i< 7 && i< texts.length; i++)
                {
                    text += texts[i] + " ";
                }

            }

            xpath += "[contains(text(),'" +text +"')]";
            //System.out.println(xpath);

        }

        // If none is possible, it generates xpath using other attributes
        else
        {
            xpath += "[";

            for(Attribute attribute : element.attributes())
            {
                if(attribute.getKey() != "")
                xpath += "@" + attribute.getKey() + "='" + attribute.getValue() + "' or ";
            }

            xpath = xpath.substring(0, xpath.lastIndexOf("or"));

            xpath += "]";

        }

        //Creates the locators of element
        locators.put("Web", xpath);
        locators.put("Android", "");
        locators.put("IOS","");

        return locators;

    }
}