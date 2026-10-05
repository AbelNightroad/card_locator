package com.gitlab.abelnightroad.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaDecklistLoaderTest {

    private val cedhPage = """
        <html><body>
        <script type="text/javascript">
        function ReloadDecks() {
            document.getElementById("cEDH_decks").innerHTML="<div align=center><img src=/graph/loadingGIF.gif></div>";
            RequestContent("cEDH_decks?f="+f+"&show="+show+"&cid="+color_id+"&meta="+meta+"&gamerid1="+commander_id[1]+"&gamerid2="+commander_id[2]+"&cEDH_cp="+cEDH_cp,"cEDH_decks");
        }
        function MCardSearch(box){
            document.getElementById("card_search_results").innerHTML="searching ...";
            RequestContent("cEDH_card_search?n="+encodeURI(Mch)+"&b="+box,"card_search_results");
        }
        </script>
        <script>
            cEDH_cp=1;
            f="cEDH";
            meta=300;

            color_id="";
            show="pop";
        </script>
        <div id=show_menu style="margin:3px;">
            <div id=show_decks class="Nav_norm S14" onclick="ChangeShow('decks');">Last decks</div>
        </div>
        <div id=cEDH_decks style="min-height:100px;"></div>
        </body></html>
    """.trimIndent()

    private val cedhFragment = """
        <div class=S14 align=center style="margin:10px;">7941 decks</div>
        <div align=center>
            <div style="width:90%;" class=hover_tr>
              <div style="display:flex;align-items:center;margin-bottom:2px;">
                <div style="width:80px;height:40px;background:black;"><img src=/metas_thumbs/1158.jpg></div>
                <div align=center style="width:60%;" class=S14><a href=archetype?a=1158&meta=300&f=cEDH&color_id=&show=pop>Partner WUBR</a></div>
                <div style="width:20%;" class=S14 align=center>88.2 <span class=O14>&permil;</span></div>
                <div style="width:20%;" class=S14 align=center></div>
              </div>
            </div>
            <div style="width:90%;" class=hover_tr>
              <div style="display:flex;align-items:center;margin-bottom:2px;">
                <div style="width:80px;height:40px;background:black;"><img src=/metas_thumbs/1246.jpg></div>
                <div align=center style="width:60%;" class=S14><a href=archetype?a=1246&meta=300&f=cEDH&color_id=&show=pop>Kinnan, Bonder Prodigy</a></div>
                <div style="width:20%;" class=S14 align=center>72.3 <span class=O14>&permil;</span></div>
                <div style="width:20%;" class=S14 align=center></div>
              </div>
            </div>
        </div>
    """.trimIndent()

    private val standardPage = """
        <div style="display:inline-block;width:48%;" class=hover_tr>
          <div style="display:flex;">
            <div style="width:80px;height:40px;background:black;"><img src=/metas_thumbs/346.jpg></div>
            <div align=center style="width:100%;">
              <div class=S14><a href=archetype?a=346&meta=50&f=ST>Boros Aggro</a></div>
              <div>
                <div style="display:inline-block;width:48%;" class=S14>14 %</div>
                <div style="display:inline-block;width:48%;" class=S14></div>
              </div>
            </div>
          </div>
        </div>
    """.trimIndent()

    @Test
    fun `xhr url is built from the format page script`() {
        val url = MetaDecklistLoader.xhrDecksUrl(cedhPage, "cEDH")
        assertEquals(
            "https://mtgtop8.com/cEDH_decks?f=cEDH&show=pop&cid=&meta=300&gamerid1=&gamerid2=&cEDH_cp=1",
            url
        )
    }

    @Test
    fun `xhr url is null when the page loads archetypes server side`() {
        assertNull(MetaDecklistLoader.xhrDecksUrl(standardPage, "ST"))
    }

    @Test
    fun `xhr url is null when the script has no meta window`() {
        val page = """<script>RequestContent("cEDH_decks?f="+f,"cEDH_decks");</script>"""
        assertNull(MetaDecklistLoader.xhrDecksUrl(page, "cEDH"))
    }

    @Test
    fun `parses deck rows returned by the xhr fragment`() {
        val archetypes = MetaDecklistLoader.parseArchetypes(Jsoup.parseBodyFragment(cedhFragment))

        assertEquals(2, archetypes.size)
        assertEquals("Partner WUBR", archetypes[0].name)
        assertEquals("88.2 ‰", archetypes[0].metaPercent)
        assertEquals(1158, archetypes[0].archetypeId)
        assertEquals("https://mtgtop8.com//metas_thumbs/1158.jpg", archetypes[0].coverUrl)
        assertEquals("archetype?a=1158&meta=300&f=cEDH&color_id=&show=pop", archetypes[0].url)
        assertEquals("Kinnan, Bonder Prodigy", archetypes[1].name)
        assertEquals("72.3 ‰", archetypes[1].metaPercent)
        assertEquals(1246, archetypes[1].archetypeId)
    }

    @Test
    fun `parses server rendered archetype rows`() {
        val archetypes = MetaDecklistLoader.parseArchetypes(Jsoup.parseBodyFragment(standardPage))

        assertEquals(1, archetypes.size)
        assertEquals("Boros Aggro", archetypes[0].name)
        assertEquals("14 %", archetypes[0].metaPercent)
        assertEquals(346, archetypes[0].archetypeId)
        assertEquals("archetype?a=346&meta=50&f=ST", archetypes[0].url)
    }

    @Test
    fun `returns nothing when the page has no archetypes`() {
        val page = """
            <div id=show_decks class="Nav_norm S14">Last decks</div>
            <div id=cEDH_decks style="min-height:100px;"></div>
            <tr class=hover_tr><td class=S14><a href=event?e=90006&f=cEDH>Breach the Bay</a></td></tr>
        """.trimIndent()

        assertTrue(MetaDecklistLoader.parseArchetypes(Jsoup.parse(page)).isEmpty())
    }

    @Test
    fun `parses plain archetype links as a fallback`() {
        val page = """
            <div class=S14><a href="/archetype?a=99&meta=50&f=ST"><img src=/metas_thumbs/99.jpg>Boros</a></div>
        """.trimIndent()

        val archetypes = MetaDecklistLoader.parseArchetypes(Jsoup.parse(page))

        assertEquals(1, archetypes.size)
        assertEquals("Boros", archetypes[0].name)
        assertEquals(99, archetypes[0].archetypeId)
        assertEquals("https://mtgtop8.com//metas_thumbs/99.jpg", archetypes[0].coverUrl)
    }

    @Test
    fun `commander resolves to the cEDH format page`() {
        assertEquals("cEDH", MetaDecklistLoader.formatCodes["commander"])
        assertTrue(MetaDecklistLoader.formatCodes.keys.containsAll(listOf("standard", "modern", "pauper")))
    }

    private val commanderDeckPage = """
        <div style="display:flex;align-content:stretch;">
          <div style="margin:3px;flex:1;" align=left>
            <div class=O14>COMMANDER</div>
            <div id=sb16c034 class="deck_line hover_tr">1 <span class=L14>Kraum, Ludevic's Opus</span> </div>
            <div id=sb16c048 class="deck_line hover_tr">1 <span class=L14>Tymna the Weaver</span> </div>
            <div class=O14 style="margin-top:5px;">25 LANDS</div>
            <div id=mdtem007 class="deck_line hover_tr">1 <span class=L14>Ancient Tomb</span> </div>
            <div id=mdzen211 class="deck_line hover_tr">4 <span class=L14>Forest</span> </div>
          </div>
          <div>
            <div class=O14>SIDEBOARD</div>
            <div id=sbabc001 class="deck_line hover_tr">2 <span class=L14>Negate</span> </div>
          </div>
        </div>
    """.trimIndent()

    private val standardDeckPage = """
        <div style="display:flex;align-content:stretch;">
          <div class=O14>17 LANDS</div>
          <div id=mdtem007 class="deck_line hover_tr">4 <span class=L14>Ancient Tomb</span> </div>
          <div id=mdzen211 class="deck_line hover_tr">4 <span class=L14>Forest</span> </div>
          <div class=O14>SIDEBOARD</div>
          <div id=sbabc001 class="deck_line hover_tr">2 <span class=L14>Negate</span> </div>
        </div>
    """.trimIndent()

    @Test
    fun `commander section cards become commanders and come first`() {
        val cards = MetaDecklistLoader.parseDecklist(Jsoup.parseBodyFragment(commanderDeckPage))

        assertEquals(
            listOf("commander", "commander", "mainboard", "mainboard", "sideboard"),
            cards.map { it.slot }
        )
        assertEquals("Kraum, Ludevic's Opus", cards[0].cardName)
        assertEquals("Tymna the Weaver", cards[1].cardName)
        assertEquals(1, cards[0].quantity)
        assertEquals("Ancient Tomb", cards[2].cardName)
        assertEquals(4, cards[3].quantity)
        assertEquals("Negate", cards[4].cardName)
        assertEquals(2, cards[4].quantity)
    }

    @Test
    fun `decks without a commander section keep mainboard and sideboard`() {
        val cards = MetaDecklistLoader.parseDecklist(Jsoup.parseBodyFragment(standardDeckPage))

        assertEquals(listOf("mainboard", "mainboard", "sideboard"), cards.map { it.slot })
        assertEquals("Ancient Tomb", cards[0].cardName)
        assertEquals("Negate", cards[2].cardName)
    }
}
