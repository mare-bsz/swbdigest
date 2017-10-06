package bsz.swbdigest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class Expo2SolrTest extends TestCase {
	
	private Expo2Solr e2s;
	
	public void setUp() {
		Map<String, String> indexe = new HashMap<>();
		indexe.put("default", "text");
		indexe.put("material", "a_material");
		indexe.put("technik", "a_technik");
		indexe.put("nummer", "a_nummer");
		indexe.put("person", "a_person");
		e2s = new Expo2Solr(indexe);
	}
	
	public void testParse1() {
		assertEquals(e2s.parse("Hallo wach"), null);
				
	}
	
	public void testParse2() {
		assertEquals(e2s.parse("Hallo.adj.wach"), "\"Hallo.adj.wach\"");
	}
	
	public void testParse3() {
		assertEquals(e2s.parse("material.all.silber"), "a_material:\"silber\"");
	}
	
	public void testParse4() {
		assertEquals(e2s.parse("material.all.silber gold"), "a_material:\"silber\" and a_material:\"gold\"");
	}
	
	public void testParse5() {		
		assertEquals(e2s.parse("material.adj.silber gold"), null);		
	}
	
	public void testParse6() {		
		assertEquals(e2s.parse("material.adj.\"sil[ber gold\""), "a_material:\"sil\\[ber gold\"");		
	}
	
	public void testParse7() {		
		assertEquals(e2s.parse("material.all.\"silber gold\"-and-technik.any.\"geschmiedet gegossen\""), 
				"(a_material:\"silber\" and a_material:\"gold\") and (a_technik:\"geschmiedet\" or a_technik:\"gegossen\")");		
	}
	
	public void testParse8() {				
		assertEquals(e2s.parse("material.all.\"silber gold\"-or-technik.any.\"geschmiedet gegossen\""), 
				"(a_material:\"silber\" and a_material:\"gold\") or (a_technik:\"geschmiedet\" or a_technik:\"gegossen\")");		
	}
	
	public void testParse9() {		
		assertEquals(e2s.parse("material.any.\"silber gold\""), "a_material:\"silber\" or a_material:\"gold\"");
	}
	
	public void testParse10() {		
		assertEquals(e2s.parse("\"material any silber gold\""), "text:\"material any silber gold\"");
	}

}
