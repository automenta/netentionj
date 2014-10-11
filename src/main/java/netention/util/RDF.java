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

package netention.util;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;

/**
 *
 * @author me
 */
public class RDF {

    
    public final InfModel model;
    
    public RDF() {
        
        Model infgraph = ModelFactory.createDefaultModel();        
        Reasoner reasoner = ReasonerRegistry.getOWLMicroReasoner(); //getOWLReasoner();
        //reasoner = reasoner.bindSchema(schema);        
        this.model = ModelFactory.createInfModel(reasoner, infgraph);
        
        
    }
}
