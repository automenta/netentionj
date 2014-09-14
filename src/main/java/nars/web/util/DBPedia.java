/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.web.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.tinkerpop.blueprints.Vertex;
import java.util.concurrent.ExecutorService;
import nars.web.core.Core;
import static nars.web.core.Core.u;
import org.apache.jena.riot.RDFDataMgr;
import org.boon.HTTP;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;


/**
 *
 * @author me
 */
public class DBPedia extends HandlerThread<Message> {
    private final Core core;
    
 
    //http://dbpedia.org/sparql?output=json&format=application/json&timeout=0&query=
    
    //SELECT%20DISTINCT%20*%20%20WHERE%20%20%7B%7B%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FVienna%3E%20%3Fproperty%20%3Fobject.%20FILTER(!isLiteral(%3Fobject))%7D%20UNION%20%09%20%7B%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FVienna%3E%20%3Fproperty%20%09%20%3Fobject.FILTER(isLiteral(%3Fobject)).FILTER(lang(%3Fobject)%20%3D%22it%22)%7D%20UNION%20%09%20%7B%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FVienna%3E%20%3Fproperty%20%09%20%3Fobject.FILTER(isLiteral(%3Fobject)).FILTER(lang(%3Fobject)%20%3D%22en%22)%7D%7D%20%20ORDER%20BY%20%3Fproperty&callback=lodlive
    
    //String query = "SELECT DISTINCT *  WHERE  {{<http://dbpedia.org/resource/Vienna> ?property ?object. FILTER(!isLiteral(?object))} UNION 	 {<http://dbpedia.org/resource/Vienna> ?property 	 ?object.FILTER(isLiteral(?object)).FILTER(lang(?object) =\"it\")} UNION 	 {<http://dbpedia.org/resource/Vienna> ?property 	 ?object.FILTER(isLiteral(?object)).FILTER(lang(?object) =\"en\")}}  ORDER BY ?property";
    //SELECT DISTINCT * WHERE {?object ?t <http://dbpedia.org/resource/Wolfgang_Amadeus_Mozart> } LIMIT 100 
    private final EventBus bus;    
    
    public DBPedia(ExecutorService e, Core c, EventBus b) {
        super(e);
        this.bus = b;
        this.core = c;
        b.registerHandler("wikipedia", this);
    }
    
    public boolean learnedRecently(String wikiID) {
        //TODO check actual date or use LRU cache
        Vertex v = core.vertex(u(wikiID), false);
        if (v == null) {            
            return false;
        }
        
        if ( !core.cached(v, "dbpedia") )  { 
            core.cache(v, "dbpedia"); //cache even if dbpedia fails, because it has low availability
            core.commit();
            return false;
        }
        else {            
            System.out.println("dbpedia cached " + wikiID);
            core.commit();
            return true;
        }
    }
    
    @Override
    public void run(Message e) {
        String wikiURL = e.body().toString();

        if (!learnedRecently(wikiURL))
            learn(wikiURL);
        bus.publish("interest", u(wikiURL));
    }
    
    public void learn(String wikiURL) {
        String id = wikiURL.substring(wikiURL.lastIndexOf("/")+1, wikiURL.length());
        
        try {
            /*Query query = QueryFactory.create(getDBPediaQuery(wikiURL)); //s2 = the query above
            QueryExecution qExe = QueryExecutionFactory.sparqlService( "http://dbpedia.org/sparql", query );

            qExe.setTimeout(timeout);

            
            //qmodel.write(System.out, "N3");
            core.addRDF(qmodel, wikiURL);*/
            
            String dataURL = "http://dbpedia.org/data/" + id + ".n3";
            Model qmodel = ModelFactory.createDefaultModel();            
            RDFDataMgr.read(qmodel, dataURL);
            
            
            core.addRDF(qmodel, wikiURL);
            core.commit();
            
            
            System.out.println("DBPedia finished learning " + wikiURL + " via " + dataURL + " .. RDF model size: +" + qmodel.size());            
        }
        catch (Exception e) {
            System.out.println("DBPedia timed out " + wikiURL);          
            learnWikipediaCategories(id);
        }

    }

    //TODO implement this
    public void learnWikipediaCategories(String id) {
        int max = 32;
        //http://en.wikipedia.org/w/api.php?action=query&prop=categories|info&format=json&cllimit=10&titles=id
        String url = "http://en.wikipedia.org/w/api.php?action=query&prop=categories|info&format=json&cllimit=" + max + "&titles=" + id;
        HTTP.get(url);
    }
    
    public static String getDBPediaQuery(String wikiURL) {
        //String q = "SELECT DISTINCT * WHERE {?object ?t <http://dbpedia.org/resource/" + res + "> ";
        /*String q = "DESCRIBE * WHERE { ";
        q += "<http://dbpedia.org/resource/" + res + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type ; ";
        q += "<http://www.w3.org/2000/01/rdf-schema#label> ?label ; ";
        q += "<http://purl.org/dc/terms/subject> ?subject ; ";
        
        q += "FILTER ( lang(?label) = 'en' ) \n";
        q += " } ";
        */
        //LIMIT 100";
        
        String q = "DESCRIBE <" + wikiURL + ">";
        return q;
        
        //http://dbpedia.org/resource/
        /*
        String q = "CONSTRUCT WHERE { <" + wikiURL + "> ";
        q += "a ?o";
        q += "; <http://www.w3.org/2000/01/rdf-schema#label> ?p";
        q += "; <http://purl.org/dc/terms/subject> ?s";
        //q += " .\n ";
        //q += "FILTER (langMatches(lang(?o),\"en\"))\n }";
        q += " }";
        System.out.println("dbpedia sparql: " + q);
        return q;
        */
        
/*        CONSTRUCT WHERE { 
<http://dbpedia.org/resource/Life> 

  a ?o ;
  <http://www.w3.org/2000/01/rdf-schema#label> ?p; 
  <http://purl.org/dc/terms/subject> ?s
.

FILTER (lang(?p) = 'en')
 }*/ 

    }
    
//    public static void main(String[] args) {
        /*String s2 = "PREFIX yago: \n" +
        "PREFIX onto: \n" +
        "PREFIX rdf: \n" +
        "PREFIX dbpedia: \n" +
        "PREFIX owl: \n" +
        "PREFIX dbpedia-owl: \n" +
        "PREFIX rdfs: \n" +
        "PREFIX dbpprop: \n" +
        "PREFIX foaf: \n" +

        "SELECT DISTINCT *\n" +
        "WHERE {\n" +
        "?city rdf:type dbpedia-owl:PopulatedPlace .\n" +
        "?city rdfs:label ?label.\n" +
        "?city dbpedia-owl:country ?country .\n" +
        "?country dbpprop:commonName ?country_name.\n" +

        "OPTIONAL { ?city foaf:isPrimaryTopicOf ?web }.\n" +

        "FILTER ( lang(?label) = ‘en’ && regex(?country, ‘Germany’) && regex(?label, ‘Homburg’)) \n" +
        "} \n" +
        "";*/

//        Query query = QueryFactory.create(getDBPediaQuery("Life")); //s2 = the query above
//        QueryExecution qExe = QueryExecutionFactory.sparqlService( "http://dbpedia.org/sparql", query );
//        
//        Model qmodel = qExe.execDescribe();
//        //model.write(System.out, "N3");
//        
//        model.add(qmodel);
        //infmodel.write(System.out, "JSON-LD");
        //infmodel.write(System.out, "RDF/JSON");
        

        //ResultSet results = qExe.execSelect();        
        //ResultSetFormatter.out(System.out, results, query) ;

//    }
}
