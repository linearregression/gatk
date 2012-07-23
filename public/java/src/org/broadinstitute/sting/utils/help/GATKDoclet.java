/*
 * Copyright (c) 2012, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.utils.help;

import com.google.gson.Gson;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.broad.tribble.FeatureCodec;
import org.broadinstitute.sting.gatk.Categorize;
import org.broadinstitute.sting.gatk.Category;
import org.broadinstitute.sting.gatk.CommandLineGATK;
import org.broadinstitute.sting.gatk.walkers.qc.DocumentationTest;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.text.XReadLines;

import java.io.*;
import java.lang.annotation.Annotation;
<<<<<<< HEAD
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
=======
>>>>>>> Added the ability to update the Forum
import java.util.*;

/**
 * Javadoc Doclet that combines javadoc, GATK ParsingEngine annotations, and FreeMarker
 * templates to produce html formatted GATKDocs for walkers
 * and other classes.
 * <p/>
 * This document has the following workflow:
 * <p/>
 * 1 -- walk the javadoc heirarchy, looking for class that have the
 * DocumentedGATKFeature annotation or are in the type heirarchy in the
 * static list of things to document, and are to be documented
 * 2 -- construct for each a GATKDocWorkUnit, resulting in the complete
 * set of things to document
 * 3 -- for each unit, actually generate an html page documenting it
 * as well as links to related features via their units.  Writing
 * of a specific class HTML is accomplished by a generate DocumentationHandler
 * 4 -- write out an index of all units, organized by group
 * <p/>
 * The documented classes are restricted to only those with @DocumentedGATKFeature
 * annotation or are in the STATIC_DOCS class.
 */
public class GATKDoclet {
    final protected static Logger logger = Logger.getLogger(GATKDoclet.class);

    /**
     * Where we find the help FreeMarker templates
     */
    final protected static File SETTINGS_DIR = new File("settings/helpTemplates");

    /**
     * Where we write the GATKDoc html directory
     */
    final protected static File DESTINATION_DIR = new File("gatkdocs");

    final private static String FORUM_KEY_FILE = "/local/gsa-engineering/gatkdocs_publisher/forum.key";
    // ----------------------------------------------------------------------
    //
    // Global variables that are set on the command line by javadoc
    //
    // ----------------------------------------------------------------------
    protected static String buildTimestamp = null, absoluteVersion = null;
    protected static boolean showHiddenFeatures = false;

    protected static boolean testOnly = false;

    /**
     * Any class that's in this list will be included in the documentation
     * when the -test argument is provided.  Useful for debugging.
     */
    private static final List<Class<?>> testOnlyKeepers = Arrays.asList(
            DocumentationTest.class, CommandLineGATK.class, UserException.class);

    /**
     * The javadoc root doc
     */
    RootDoc rootDoc;

    /**
     * The set of all things we are going to document
     */
    Set<GATKDocWorkUnit> myWorkUnits;

    /**
     * A static list of DocumentedGATKFeatureObjects.  Any class that is as or extends
     * one of the DocumentedGATKFeatureObjects.clazz of this collection will also
     * be documented, even if it doesn't have the @DocumentedGATKFeature annotation.  Useful
     * when you want to document things that implement an interface (annotations on java
     * interfaces aren't inherited) or whose base class isn't under your control (tribble
     * codecs).
     */
    final static Collection<DocumentedGATKFeatureObject> STATIC_DOCS = new ArrayList<DocumentedGATKFeatureObject>();

    static {
        STATIC_DOCS.add(new DocumentedGATKFeatureObject(FeatureCodec.class,
                "Reference ordered data (ROD) codecs",
                "Tribble codecs for reading reference ordered data such as VCF or BED files"));
    }


    /**
     * Extracts the contents of certain types of javadoc and adds them to an XML file.
     *
     * @param rootDoc The documentation root.
     * @return Whether the JavaDoc run succeeded.
     * @throws java.io.IOException if output can't be written.
     */
    public static boolean start(RootDoc rootDoc) throws IOException {
        logger.setLevel(Level.INFO);

        // load arguments
        for (String[] options : rootDoc.options()) {
            if (options[0].equals("-build-timestamp"))
                buildTimestamp = options[1];
            if (options[0].equals("-absolute-version"))
                absoluteVersion = options[1];
            if (options[0].equals("-include -hidden"))
                showHiddenFeatures = true;
            if (options[0].equals("-test"))
                testOnly = true;
        }

        // process the docs
        new GATKDoclet().processDocs(rootDoc);


        return true;
    }

    /**
     * Validate the given options against options supported by this doclet.
     *
     * @param option Option to validate.
     * @return Number of potential parameters; 0 if not supported.
     */
    public static int optionLength(String option) {
        if (option.equals("-build-timestamp") ||
                option.equals("-absolute-version") ||
                option.equals("-include-hidden")) {
            return 2;
        } else if (option.equals("-test"))
            return 1;
        else
            return 0;
    }

    /**
     * Are we supposed to include @Hidden annotations in our documented output?
     *
     * @return
     */
    public boolean showHiddenFeatures() {
        return showHiddenFeatures;
    }

    /**
     * @param rootDoc
     */
    private void processDocs(RootDoc rootDoc) {
        // setup the global access to the root
        this.rootDoc = rootDoc;

        try {
            // basic setup
            DESTINATION_DIR.mkdirs();
            FileUtils.copyFile(new File(SETTINGS_DIR + "/bootstrap.min.css"), new File(DESTINATION_DIR + "/bootstrap.min.css"));
            FileUtils.copyFile(new File(SETTINGS_DIR + "/bootstrap.min.js"), new File(DESTINATION_DIR + "/bootstrap.min.js"));
            FileUtils.copyFile(new File(SETTINGS_DIR + "/jquery.min.js"), new File(DESTINATION_DIR + "/jquery.min.js"));
            // print the Version number
            FileUtils.writeByteArrayToFile(new File(DESTINATION_DIR + "/current.version.txt"), getSimpleVersion(absoluteVersion).getBytes());

            /* ------------------------------------------------------------------- */
            /* You should do this ONLY ONCE in the whole application life-cycle:   */

            Configuration cfg = new Configuration();
            // Specify the data source where the template files come from.
            cfg.setDirectoryForTemplateLoading(SETTINGS_DIR);
            // Specify how templates will see the data-model. This is an advanced topic...
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            myWorkUnits = computeWorkUnits();

            Map<Category, List<Map<String, String>>> catNav = getCategories(new ArrayList<GATKDocWorkUnit>(myWorkUnits));

            processCategories(catNav, myWorkUnits);

            for (GATKDocWorkUnit workUnit : myWorkUnits) {
                processDocWorkUnit(cfg, workUnit, catNav);
            }

            processIndex(cfg, new ArrayList<GATKDocWorkUnit>(myWorkUnits));

            File forumKeyFile = new File(FORUM_KEY_FILE);
            if (forumKeyFile.exists()) {
                String forumKey = null;
                // Read ing a one-line file so we can do a for loop
                for (String line : new XReadLines(forumKeyFile))
                    forumKey = line;
                updateForum(myWorkUnits, forumKey);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateForum(Set<GATKDocWorkUnit> docWorkUnits, String forumKey) {
        //first get list of posts that need to be added
        List<String> old = ForumAPIUtils.getPostedTools(forumKey);

        for (String s : old)
            System.out.println(s);

        System.out.printf("Forum has %d items%n", old.size());
        System.out.printf("Docs have %d items%n", docWorkUnits.size());

        List<GATKDocWorkUnit> toAdd = new ArrayList<GATKDocWorkUnit>();
        for (GATKDocWorkUnit tool : docWorkUnits) {
            if (!old.contains(tool.name)) {
                System.out.println("WILL POST: " + tool.name + " TO FORUM");
                toAdd.add(tool);
            }
        }

        //update using list
        for (GATKDocWorkUnit tool : toAdd) {
            //if ( tool.name.equals("ApplyRecalibration") )
            ForumAPIUtils.postToForum(tool, forumKey);
        }
    }

    private void processCategories(Map<Category, List<Map<String, String>>> getCategories, Set<GATKDocWorkUnit> classes) throws IOException {
        List<Object> categories = new LinkedList<Object>();
        for (Category cat : getCategories.keySet()) {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("Name", cat.getDescription());
            properties.put("UrlSlug", cat.name().toLowerCase().replace('_', '-'));
            categories.add(properties);
        }
        writeJson(DESTINATION_DIR + "/categories.json", categories);

        List<Object> posts = new LinkedList<Object>();
        for (GATKDocWorkUnit unit : classes) {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("Post Title", unit.name + " Documentation");
            properties.put("Categories", unit.categories);
            List<String> tags = new ArrayList<String>();
            tags.add(unit.name);
            tags.add(unit.group);
            properties.put("Tags", tags);
            String body = String.format("<a href='%s'>%s</a><hr /><p>%s</p>",
                    GATKDocUtils.URL_ROOT_FOR_RELEASE_GATKDOCS + unit.filename, unit.name + " Documentation", unit.summary);
            properties.put("Body", body);
            posts.add(properties);
        }

    }

    private void writeJson(String filename, Object source) throws IOException {
        Gson gson = new Gson();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename))), 1024 * 1024);

        out.write(gson.toJson(source));

        out.close();

    }

    /**
     * Returns the set of all GATKDocWorkUnits that we are going to generate docs for.
     *
     * @return
     */
    private Set<GATKDocWorkUnit> computeWorkUnits() {
        TreeSet<GATKDocWorkUnit> m = new TreeSet<GATKDocWorkUnit>();

        for (ClassDoc doc : rootDoc.classes()) {
            //logger.debug("Considering " + doc);
            Class clazz = getClassForClassDoc(doc);

            // don't add anything that's not DocumentationTest if we are in test mode
            if (clazz != null && testOnly && !testOnlyKeepers.contains(clazz))
                continue;

            //if ( clazz != null && clazz.getName().equals("org.broadinstitute.sting.gatk.walkers.annotator.AlleleBalance"))
            //    logger.debug("foo");

            DocumentedGATKFeatureObject feature = getFeatureForClassDoc(doc);
            DocumentedGATKFeatureHandler handler = createHandler(doc, feature);
            if (handler != null && handler.includeInDocs(doc)) {
                logger.info("Generating documentation for class " + doc);
                String filename = handler.getDestinationFilename(doc, clazz);
                Category[] categories = getClassAnnotationValue(clazz, Categorize.class, "value");
                GATKDocWorkUnit unit = new GATKDocWorkUnit(doc.name(),
                        filename, feature.groupName(), (categories == null) ? new ArrayList<Category>() : Arrays.asList(categories),
                        feature, handler, doc, clazz,
                        buildTimestamp, absoluteVersion);
                m.add(unit);
            }
        }

        return m;
    }

    /**
     * Create a handler capable of documenting the class doc according to feature.  Returns
     * null if no appropriate handler is found or doc shouldn't be documented at all.
     *
     * @param doc
     * @param feature
     * @return
     */
    private DocumentedGATKFeatureHandler createHandler(ClassDoc doc, DocumentedGATKFeatureObject feature) {
        if (feature != null) {
            if (feature.enable()) {
                DocumentedGATKFeatureHandler handler = new GenericDocumentationHandler();
                handler.setDoclet(this);
                return handler;
            } else {
                logger.info("Skipping disabled Documentation for " + doc);
            }
        }

        return null;
    }

    /**
     * Returns the instantiated DocumentedGATKFeatureObject that describes the GATKDoc
     * structure we will apply to Doc.
     *
     * @param doc
     * @return null if this proves inappropriate or doc shouldn't be documented
     */
    private DocumentedGATKFeatureObject getFeatureForClassDoc(ClassDoc doc) {
        Class<? extends Object> docClass = getClassForClassDoc(doc);

        if (docClass == null)
            return null; // not annotated so it shouldn't be documented

        if (docClass.isAnnotationPresent(DocumentedGATKFeature.class)) {
            DocumentedGATKFeature f = docClass.getAnnotation(DocumentedGATKFeature.class);
            return new DocumentedGATKFeatureObject(docClass, f.enable(), f.groupName(), f.summary(), f.extraDocs());
        } else {
            for (DocumentedGATKFeatureObject staticDocs : STATIC_DOCS) {
                if (staticDocs.getClassToDoc().isAssignableFrom(docClass)) {
                    return new DocumentedGATKFeatureObject(docClass, staticDocs.enable(), staticDocs.groupName(), staticDocs.summary(), staticDocs.extraDocs());
                }
            }
            return null;
        }
    }

    /**
     * Return the Java class described by the ClassDoc doc
     *
     * @param doc
     * @return
     */
    private Class<? extends Object> getClassForClassDoc(ClassDoc doc) {
        try {
            // todo -- what do I need the ? extends Object to pass the compiler?
            return (Class<? extends Object>) HelpUtils.getClassForDoc(doc);
        } catch (ClassNotFoundException e) {
            //logger.warn("Couldn't find class for ClassDoc " + doc);
            // we got a classdoc for a class we can't find.  Maybe in a library or something
            return null;
        } catch (NoClassDefFoundError e) {
            return null;
        } catch (UnsatisfiedLinkError e) {
            return null; // naughty BWA bindings
        }
    }

    /**
     * Create the html index listing all of the GATKDocs features
     *
     * @param cfg
     * @param indexData
     * @throws IOException
     */
    private void processIndex(Configuration cfg, List<GATKDocWorkUnit> indexData) throws IOException {
        /* Get or create a template */
        Template temp = cfg.getTemplate("generic.index.template.html");

        /* Merge data-model with template */
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(DESTINATION_DIR + "/index.html")));
        try {
            temp.process(groupIndexData(indexData), out);
            out.flush();
        } catch (TemplateException e) {
            throw new ReviewedStingException("Failed to create GATK documentation", e);
        }
    }

    /**
     * Helpful function to create the html index.  Given all of the already run GATKDocWorkUnits,
     * create the high-level grouping data listing individual features by group.
     *
     * @param indexData
     * @return
     */
    private Map<String, Object> groupIndexData(List<GATKDocWorkUnit> indexData) {
        //
        // root -> data -> { summary -> y, filename -> z }, etc
        //      -> groups -> group1, group2, etc.
        Map<String, Object> root = new HashMap<String, Object>();

        Collections.sort(indexData);

        List<Map<String, String>> groups = new ArrayList<Map<String, String>>();
        Set<String> seenDocumentationFeatures = new HashSet<String>();
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (GATKDocWorkUnit workUnit : indexData) {
            data.add(workUnit.indexDataMap());
            if (!seenDocumentationFeatures.contains(workUnit.annotation.groupName())) {
                groups.add(toMap(workUnit.annotation));
                seenDocumentationFeatures.add(workUnit.annotation.groupName());
            }
        }

        root.put("data", data);
        root.put("groups", groups);
        root.put("timestamp", buildTimestamp);
        root.put("version", absoluteVersion);

        root.put("categories", getCategories(indexData));

        return root;
    }

    /**
     * Trivial helper routine that returns the map of name and summary given the annotation
     *
     * @param annotation
     * @return
     */
    private static final Map<String, String> toMap(DocumentedGATKFeatureObject annotation) {
        Map<String, String> root = new HashMap<String, String>();
        root.put("id", annotation.groupName().replaceAll("\\W", ""));
        root.put("name", annotation.groupName());
        root.put("summary", annotation.summary());
        return root;
    }

    /**
     * Helper function that finding the GATKDocWorkUnit associated with class from among all of the work units
     *
     * @param c the class we are looking for
     * @return the GATKDocWorkUnit whose .clazz.equals(c), or null if none could be found
     */
    public final GATKDocWorkUnit findWorkUnitForClass(Class c) {
        for (final GATKDocWorkUnit unit : this.myWorkUnits)
            if (unit.clazz.equals(c))
                return unit;
        return null;
    }

    /**
     * Return the ClassDoc associated with clazz
     *
     * @param clazz
     * @return
     */
    public ClassDoc getClassDocForClass(Class clazz) {
        return rootDoc.classNamed(clazz.getName());
    }

    /**
     * High-level function that processes a single DocWorkUnit unit using its handler
     *
     * @param cfg
     * @param unit
     * @throws IOException
     */
    private void processDocWorkUnit(Configuration cfg, GATKDocWorkUnit unit, Map<Category, List<Map<String, String>>> categories)
            throws IOException {
        //System.out.printf("Processing documentation for class %s%n", unit.classDoc);

        unit.handler.processOne(unit);
        unit.forTemplate.put("catNav", categories);
        // Get or create a template
        Template temp = cfg.getTemplate(unit.handler.getTemplateName(unit.classDoc));

        // Merge data-model with template
        File outputPath = new File(DESTINATION_DIR + "/" + unit.filename);
        try {
            Writer out = new OutputStreamWriter(new FileOutputStream(outputPath));
            temp.process(unit.forTemplate, out);
            out.flush();
        } catch (TemplateException e) {
            throw new ReviewedStingException("Failed to create GATK documentation", e);
        }
    }

    // hack - todo - is there a better way to process annotations?
    private Category[] getClassAnnotationValue(Class classType, Class annotationType, String attributeName) {
        Category[] value = null;

        Annotation annotation = classType.getAnnotation(annotationType);
        if (annotation != null) {
            try {
                value = (Category[]) annotation.annotationType().getMethod(attributeName).invoke(annotation);
            } catch (Exception ex) {
            }
        }

        return value;
    }

    private Map<Category, List<Map<String, String>>> getCategories(List<GATKDocWorkUnit> classes) {
        Map<Category, List<Map<String, String>>> output = new TreeMap<Category, List<Map<String, String>>>();

        for (Category cat : Category.values()) {
            output.put(cat, new ArrayList<Map<String, String>>());
        }

        for (GATKDocWorkUnit unit : classes) {
            Category[] categories = getClassAnnotationValue(unit.clazz, Categorize.class, "value");

            if (categories != null) {
                for (Category cat : categories) {
                    unit.handler.processOne(unit);

                    Map<String, String> datum = unit.indexDataMap();

                    output.get(cat).add(datum);
                }
            }
        }

        return output;
    }

    private static String getSimpleVersion(String absoluteVersion) {
        String[] parts = absoluteVersion.split("-");

        // by skipping i=0, there is no trailing separator
        for (int i = 1; i < 2; i++) {
            parts[0] = parts[0].concat("-");
            parts[0] = parts[0].concat(parts[i]);
        }

        return parts[0];
    }
}