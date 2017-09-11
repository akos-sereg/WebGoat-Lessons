package org.owasp.webgoat.plugin;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ecs.Element;
import org.apache.ecs.ElementContainer;
import org.apache.ecs.html.Form;
import org.apache.ecs.html.Input;
import org.apache.ecs.html.P;
import org.owasp.webgoat.lessons.Category;
import org.owasp.webgoat.lessons.LessonAdapter;
import org.owasp.webgoat.session.ECSFactory;
import org.owasp.webgoat.session.WebSession;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ImageBomb extends LessonAdapter {

	protected Element createContent(WebSession s) {

        ElementContainer ec = new ElementContainer();

        if ("success".equalsIgnoreCase((String) s.get(IMAGE_DOS))) {
            System.out.println("final success");
            makeSuccess(s);
        }
        try {

            ec.addElement(new P().addElement("Upload new Image"));

            Input input = new Input(Input.FILE, "myfile", "");
            ec.addElement(input);

            Element b = ECSFactory.makeButton("Start Upload");
            ec.addElement(b);


        } catch (Exception e) {
            s.setMessage("Error generating " + this.getClass().getName());
            e.printStackTrace();
        }

        return ec;
    }

    protected Category getDefaultCategory() {
        return Category.DOS;
    }


    public List<String> getHints(WebSession s) {
        List<String> hints = new ArrayList<String>();
        hints.add("Whenever you upload an image, system would load it into memory before creating thumbnail.");
        hints.add("When loading into memory, it takes it's Width and Height from the image's metadata, then allocates memory based on those values ...");
        
        return hints;
    }

    public String getInstructions(WebSession s) {
        return "Server accepts only Image files (jpg, png), \n"
                + "loads them after uploading, to create thumbnail,"
                + "\n it provides 20 MB temporal memory space to handle all request \n"
                + "try do perform DOS attack that consume all temporal memory with one request";
    }

    private final static Integer DEFAULT_RANKING = new Integer(10);
    private static final String IMAGE_DOS = "IMAGE_DOS";

    protected Integer getDefaultRanking() {
        return DEFAULT_RANKING;
    }


    public String getTitle() {
        return ("Image Bomb");
    }

    public void handleRequest(WebSession s) {
        File tmpDir = (File) s.getRequest().getServletContext().getAttribute("javax.servlet.context.tempdir");

        try {
            if (ServletFileUpload.isMultipartContent(s.getRequest())) {

                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(500000);

                ServletFileUpload upload = new ServletFileUpload(factory);


                List /* FileItem */items = upload.parseRequest(s.getRequest());


                java.util.Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    FileItem item = (FileItem) iter.next();

                    if (!item.isFormField()) {

                        File uploadedFile = new File(tmpDir, item.getName());

                        if (item.getSize() < 2000 * 1024) {
                            if (item.getName().endsWith(".jpg") || item.getName().endsWith("png")) {
                                item.write(uploadedFile);

                                long total = loadedSize(uploadedFile);
                                s.setMessage("File uploaded");
                                if (total > 20 * 1024 * 1024) {
                                    s.add(IMAGE_DOS, "success");
                                    System.out.println("success");
                                    makeMessages(s);
                                } else {
                                    s.setMessage("I still have plenty of free storage on the server...");
                                }

                            } else {
                                s.setMessage("Only Iimage files (jpg, png) are accepted");
                            }
                        } else {
                            s.setMessage("Only up to 2 MB files are accepted");
                        }
                    }
                }

            }
            Form form = new Form(getFormAction(), Form.POST).setName("form")
                    .setEncType("multipart/form-data");

            form.addElement(createContent(s));

            setContent(form);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private long loadedSize(File uploadedFile) throws IOException, ImageProcessingException {
    	 Metadata metadata = ImageMetadataReader.readMetadata(uploadedFile);
    	 
    	 long imageWidth = 0;
    	 long imageHeight = 0;
    	 
    	 for (Directory directory : metadata.getDirectories()) {

             for (Tag tag : directory.getTags()) {
            	 
            	 if (tag.getTagName().equals("Image Width")) {
            		 imageWidth = Integer.parseInt(tag.getDescription().replaceAll(" pixels", ""));
            	 }
            	 
            	 if (tag.getTagName().equals("Image Height")) {
            		 imageHeight = Integer.parseInt(tag.getDescription().replaceAll(" pixels", ""));
            	 }
             }
         }
    	 
    	 return imageWidth * imageHeight;
    }


    
}
