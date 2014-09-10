package nars.web.util;


import au.com.bytecode.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static java.util.stream.Collectors.toList;
import nars.web.core.Core;
import nars.web.core.NClass;
import nars.web.core.NObject;
import nars.web.core.NProperty;



/**
 * Schema.org and ActivityStreams Ontology import
 * @author me
 */
public class SchemaOrg {

    public SchemaOrg(Core core) throws IOException {        
        ArrayList<NObject> toAdd = new ArrayList();
        
        String [] line;
        CSVReader reader = new CSVReader(new FileReader("data/schema.org/all-classes.csv"),',','\"');            
        int c = 0;
        while ((line = reader.readNext()) != null) {            
            if (c++ == 0) { /* skip first line */  continue; } 
            
            //System.out.println("  " + Arrays.asList(line));
            String id = line[0];
            String label = line[1];
            String comment = line[2];
            //List<String> ancestors = Arrays.asList(line[3].split(" "));
            List<String> supertypes = Arrays.asList(line[4].split(" "));
            //List<String> subtypes = Arrays.asList(line[5].split(" "));
            //List<String> properties;
            /*if ((line.length >= 7) && (line[6].length() > 0))
            properties = Arrays.asList(line[6].split(" "));
            else
            properties = Collections.EMPTY_LIST;*/
            //System.out.println(id + " " + label);
            //System.out.println("  " + supertypes);
            //System.out.println("  " + properties);
            if (id.equals("Action"))
                supertypes = Collections.EMPTY_LIST;
            
            NClass t = new NClass(id, label, supertypes);
            t.description = comment;
            toAdd.add(t);
        }
        reader.close();
        
        reader = new CSVReader(new FileReader("data/schema.org/all-properties.csv"),',','\"');
        c = 0;
        while ((line = reader.readNext()) != null) {            
            if (c++ == 0) { /* skip first line */  continue; } 
            
            //System.out.println("  " + Arrays.asList(line));
            //[id, label, comment, domains, ranges]
            String id = line[0].trim();
            String label = "";
            String comment = "";
            if (line.length > 1) {
                label = line[1];
            }
            if (line.length > 2) {
                comment = line[2];
            }
            List<String> domains;
            List<String> ranges;
            if ((line.length >= 4) && (line[3].length() > 0)) {
                domains = Arrays.asList(line[3].split(" "));
            } else {
                domains = Collections.EMPTY_LIST;
            }
            if ((line.length >= 5) && (line[4].length() > 0)) {
                ranges = Arrays.asList(line[4].split(" "));
                ranges = ranges.stream().map(s -> {
                    if (Core.isPrimitive(s.toLowerCase()))
                        return s.toLowerCase();
                    return s;
                }).collect(toList());
                    
            } else {
                ranges = Collections.EMPTY_LIST;
            }
            NProperty p = new NProperty(id, label, domains, ranges);
            p.description = comment;
            toAdd.add(p);
        }
        reader.close();

        reader = new CSVReader(new FileReader("data/activitystreams/verbs.csv"),',','\"');
        c = 0;
        while ((line = reader.readNext()) != null) {                        
            //System.out.println("  " + Arrays.asList(line));
            //[id, label, comment, domains, ranges]
            String id = line[0].trim();
            if (id.length() == 0)
                continue;
            String iduppercase = id.substring(0, 1).toUpperCase() + id.substring(1, id.length());
            String description = line[1];
            
            NClass p = new NClass(id, iduppercase, "Action");
            p.description = description;
            
            toAdd.add(p);
        }
        reader.close();
        
        core.addObjects(toAdd);

    }
    
}
