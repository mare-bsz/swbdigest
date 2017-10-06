package bsz.swbdigest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Servlet implementation class Items
 */
@WebServlet("/search")
public class Search extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	final Pattern cllPattern = Pattern.compile("^[a-z][a-z0-9_\\-\\.]*$");
	final Pattern fstPattern = Pattern.compile("^[1-9][0-9]*$");
	final Pattern lenPattern = Pattern.compile("^[1-9][0-9]*$");
	final Pattern srtPattern = Pattern.compile("^s_[a-z]+$");
	final Pattern fctPattern = Pattern.compile("^x_[a-z]+$");
	final Pattern minPattern = Pattern.compile("^[12][0-9]{3}$");
	final Pattern maxPattern = Pattern.compile("^[12][0-9]{3}$");
	final Pattern lanPattern = Pattern.compile("^de|en$");
	final Pattern extPattern = Pattern.compile("^1|0$");
	
	Expo2Solr e2s; 
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		Map<String, String> indexe = new HashMap<>();
		indexe.put("default", "text");
		indexe.put("material", "a_material");
		indexe.put("technik", "a_technik");
		indexe.put("nummer", "a_nummer");
		indexe.put("person", "a_person");
		e2s = new Expo2Solr(indexe);
		super.init(config);
	}	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String cll = request.getParameter("cll"); //Collection	
		final String qry = request.getParameter("qry"); //Query idx1.any.trm1 trm2-and-idx2.all.trm3-or-idx3.any.trm4-not-idx4.all.trm5
		final String fst = request.getParameter("fst"); //Erster Record		
		final String len = request.getParameter("len"); //Anzahl Datensätze
		final String srt = request.getParameter("srt"); //Sortierung
		final String fct = request.getParameter("fct"); //Facette
		final String flt = request.getParameter("flt"); //Filter 
		final String min = request.getParameter("min"); //Frühestes Jahr
		final String max = request.getParameter("max"); //Spätestes Jahr
		final String lan = request.getParameter("lan"); //Sprache
		final String ext = request.getParameter("ext"); //Exakte Suche?
				
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");	
		ServletOutputStream out = response.getOutputStream();		
		
		/* Überpr�fung der �bergebenen Parameter */
		final StringBuilder error = new StringBuilder();
		if (cll == null || ! cllPattern.matcher(cll).matches()) {
			error.append(" cll-Parameter nicht angegeben oder entspricht nicht der Konvention.");
		}
		if (fst != null && ! fstPattern.matcher(fst).matches()) {
			error.append(" fst-Parameter ist keine Zahl.");
		}
		if (len != null && ! lenPattern.matcher(len).matches()) {
			error.append(" len-Parameter ist keine Zahl.");
		}
		if (srt != null && ! srtPattern.matcher(srt).matches()) {
			error.append(" srt-Parameter entspricht nicht der Konvention.");
		}
		if (fct != null && ! fctPattern.matcher(fct).matches()) {
			error.append(" fct-Parameter entspricht nicht der Konvention.");
		}
		if (min != null && ! minPattern.matcher(min).matches()) {
			error.append(" min-Parameter ist keine Zahl zwischen 1000 und 2999.");
		}
		if (max != null && ! maxPattern.matcher(max).matches()) {
			error.append(" max-Parameter ist keine Zahl zwischen 1000 und 2999.");
		}
		if (lan != null && ! lanPattern.matcher(lan).matches()) {
			error.append(" lan-Parameter ist nicht de oder en.");
		}
		boolean quotes = false;
		if (ext != null && ! extPattern.matcher(ext).matches()) {
			error.append(" ext-Parameter ist nicht 1 oder 0.");
		} else {
			quotes = true;
		}		
		final String solrQueryString = (qry!=null?e2s.parse(qry):"*:*");
		if (solrQueryString == null) {
			error.append(" qry-Parameter entspricht nicht der Konvention " + qry + " " + ext);
		}
		
		/* Falls keine Fehler festgestellt wurden, wird eine SolrQuery parametrisiert. */
		if (error.length() == 0) {							
				
			/* der Solr-Core wird bestimmt */
			final String solrCore = (cll.indexOf(".") < 0 ? cll : cll.substring(0, cll.indexOf(".")));		
			
						
			final SolrQuery solrQuery = new SolrQuery();		
			
			/* die Collection wird zur Teilsammlung */
			solrQuery.addFilterQuery("coll:" + cll + "*");		
			
			/* Die Solr-Query wird aus dem Query-Parameter und falls gegeben aus einer zeitlichen Einschränkung gebildet */
			if (min != null || max != null) {
				final String range = "r_eingang:[" + (min != null?min:"*") + " TO " + (max != null?max:"*") + "]";
				if ("*.*".equals(solrQueryString)) {
					solrQuery.setQuery(range);
				} else {
					solrQuery.setQuery(solrQueryString + " AND " + range);
				}
			} else {
				solrQuery.setQuery(solrQueryString);
			}
			
			/* Erster und letzer Record wird eingetragen */
			solrQuery.setRows(len != null ? Integer.parseInt(len) : 12); 
			solrQuery.setStart(fst != null ? Integer.parseInt(fst) - 1 : 0);					
			
			/* Gegebenenfalls wird eine Sortierordnung eingetragen */			
			if (srt != null) {
				solrQuery.setSort(srt, SolrQuery.ORDER.asc);
			}
			
			/* Die Feldliste wird festgelegt */
			solrQuery.setFields("id", "coll", "display", "a_*", "m_*", "image", "pdf", "lit");		
			
			/* sofern zur aktuellen Facette ein Filter mitgeteilt wurde, wird dieser hinzugefügt. */
			if (flt != null && ! flt.isEmpty()) {
				solrQuery.addFilterQuery(fct + ":\"" + SolrUtil.prepareSolr(flt) + "\"");
			}	
			
			/* falls ein Index angeboten werden soll, werden die Indexbegriffe abgefragt */
			if (! (fct == null || fct.isEmpty())) {
				solrQuery.setFacet(true);
				solrQuery.addFacetField(fct);
				solrQuery.setFacetMinCount(1);
				solrQuery.setFacetLimit(-1);
		    }			
			
			
			/* Die Query wird ausgeführt */			
			
			try (SolrClient client = new HttpSolrClient(request.getServletContext().getInitParameter("solrUrl") + solrCore + "live")) {
				
				final QueryResponse res = client.query(solrQuery);			
			
				final SolrDocumentList solrDocumentList = res.getResults();
				
				
				
				final Map<String, Object> jsonGeneratorProperties = new HashMap<>(1);
				jsonGeneratorProperties.put(JsonGenerator.PRETTY_PRINTING, true);
				final JsonGeneratorFactory factory = Json.createGeneratorFactory(jsonGeneratorProperties);		
				final JsonGenerator generator = factory.createGenerator(out, StandardCharsets.UTF_8);
		
				generator
			     .writeStartObject()	     
			     	.write("query", solrQuery.getQuery())
			     	.write("start", solrQuery.getStart())
			     	.write("rows", solrQuery.getRows());				
					if (solrQuery.getSortField() != null) {
						generator.write("sort", solrQuery.getSortField());
					}					
					generator.writeStartArray("filters");
					for (String filter :solrQuery.getFilterQueries()) {
						generator.write(filter);
					}
					generator.writeEnd();
					if (fct != null && ! fct.isEmpty()) {
						generator.writeStartArray("facets");
						for (String facetField : solrQuery.getFacetFields()) {
							generator.write(facetField);
						}
						generator.writeEnd();
					}
			     	generator.write("reccount", solrDocumentList.getNumFound())
			     	.writeStartArray("records");
					for (SolrDocument doc : solrDocumentList) {
						generator.writeStartObject();
						fld2json(generator, doc, "id");
						fld2json(generator, doc, "coll");
						fld2json(generator, doc, "display");
						String titel = "";
						if (doc.getFieldValue("a_titel") != null) {
							titel = doc.getFieldValue("a_titel").toString();
						}
						String text = "";
						if (doc.getFieldValue("a_textde") != null) {
							text = doc.getFieldValue("a_textde").toString();
						} else if (doc.getFieldValue("a_text") != null) {
							text = doc.getFieldValue("a_text").toString();
						}
						if ("en".equals(lan) && doc.getFieldValue("a_titelen") != null) {
							titel = doc.getFieldValue("a_titelen").toString();
						}
						if ("en".equals(lan) && doc.getFieldValue("a_texten") != null) {
							text = doc.getFieldValue("a_texten").toString();
						}
						if (titel != null) {
							generator.write("titel", titel);
						}
						if (text != null) {
							generator.write("text", text);
						}
						
						for (String fld : doc.getFieldNames()) {
							if (fld.startsWith("a_") && ! fld.startsWith("a_titel") && ! fld.startsWith("a_text")) {
								fld2json(generator, doc, fld);													
							}
						}
						
						if (doc.getFieldValues("m_testimonial") != null) {
							generator.writeStartArray("favourites");
							for (Object testimonialObject : doc.getFieldValues("m_testimonial")) {								
								final String testimonial = testimonialObject.toString();
								final String[] testimonialParts = testimonial.split("\\s:\\s");
								if (testimonialParts.length == 4) {
									generator.writeStartObject(); 
									if (testimonialParts[1] != null) {
										generator.write("curator", testimonialParts[1]);
									}
									if (testimonialParts[2] != null) {
										generator.write("curatorfunction", testimonialParts[2]);
									}
									if (testimonialParts[0] != null) {	
										generator.write("key", testimonialParts[0]);
									}
									if (testimonialParts[3] != null) {
										generator.write("testimonial", testimonialParts[3]);
									}
									generator.writeEnd();
								}
							}
							generator.writeEnd();
						} 
						if (doc.getFieldValues("image") != null) {
							generator.writeStartArray("images");
							for (int pos = 0; pos < doc.getFieldValues("image").size(); pos++) {
								generator.write("https://swbexpo.bsz-bw.de/image/" + solrCore + "?id=" + doc.getFieldValue("id").toString() + "&img=" + (pos + 1));
							}
							generator.writeEnd();
						}
						if (doc.getFieldValues("pdf") != null) {
							generator.writeStartArray("pdfs");
							for (int pos = 0; pos < doc.getFieldValues("pdf").size(); pos++) {
								generator.write("http://swbexpo.bsz-bw.de/pdf/" + solrCore + "?id=" + doc.getFieldValue("id").toString() + "&pos=" + (pos + 1));
							}
							generator.writeEnd();
						}
						if (doc.getFieldValues("lit") != null) {
							generator.writeStartArray("lits");
							for (Object lit : doc.getFieldValues("lit")) {
								generator.write(lit.toString());
							}
							generator.writeEnd();
						}
						generator.writeEnd();					
					}
					generator.writeEnd();
					if (! (fct == null || fct.isEmpty())) {
						generator.writeStartArray("index");
						FacetField ff = res.getFacetField(fct);
						for (Count c : ff.getValues()) {
							generator.write(c.getName());
						}
						generator.writeEnd();
					}				
				generator.writeEnd();			
				generator.close();
			} catch (SolrServerException e) {
				throw new ServletException(e);
			}
			response.flushBuffer();
			out.close();
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, error.toString().trim());
		}		
	}

	private void fld2json(final JsonGenerator generator, SolrDocument doc, String fld) {
		if (doc.getFieldValue(fld) != null) {
			generator.write(fld, doc.getFieldValue(fld).toString());
		}
	}	
			
}
